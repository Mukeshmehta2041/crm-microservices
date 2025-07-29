-- =====================================================
-- DEALS SERVICE - COMPREHENSIVE SCHEMA ENHANCEMENTS
-- =====================================================

-- Import common functions and utilities
-- Function to set tenant context for RLS
CREATE OR REPLACE FUNCTION set_tenant_context(tenant_uuid UUID)
RETURNS void AS $$
BEGIN
    PERFORM set_config('app.current_tenant_id', tenant_uuid::text, true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get current tenant context
CREATE OR REPLACE FUNCTION get_current_tenant_id()
RETURNS UUID AS $$
BEGIN
    RETURN current_setting('app.current_tenant_id', true)::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 1. CREATE AUDIT LOG TABLE FOR DEALS SERVICE
-- =====================================================

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    user_id UUID,
    session_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    correlation_id VARCHAR(255),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT valid_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    CONSTRAINT valid_table_name CHECK (table_name ~ '^[a-z_]+$')
);

-- Indexes for audit log table
CREATE INDEX idx_audit_log_tenant ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_operation ON audit_log(operation);
CREATE INDEX idx_audit_log_correlation ON audit_log(correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_audit_log_session ON audit_log(session_id) WHERE session_id IS NOT NULL;

-- Composite indexes for common audit queries
CREATE INDEX idx_audit_log_tenant_table_time ON audit_log(tenant_id, table_name, timestamp);
CREATE INDEX idx_audit_log_user_time ON audit_log(user_id, timestamp) WHERE user_id IS NOT NULL;
CREATE INDEX idx_audit_log_record_time ON audit_log(table_name, record_id, timestamp);

-- =====================================================
-- 2. CREATE AUDIT TRIGGER FUNCTION
-- =====================================================

CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    old_data JSONB;
    new_data JSONB;
    changed_fields TEXT[];
    tenant_id_val UUID;
    user_id_val UUID;
    session_id_val VARCHAR(255);
    ip_address_val INET;
    user_agent_val TEXT;
    correlation_id_val VARCHAR(255);
BEGIN
    -- Extract tenant_id from the record
    IF TG_OP = 'DELETE' THEN
        tenant_id_val := OLD.tenant_id;
    ELSE
        tenant_id_val := NEW.tenant_id;
    END IF;
    
    -- Get audit context from session variables
    BEGIN
        user_id_val := current_setting('app.current_user_id', true)::UUID;
    EXCEPTION
        WHEN OTHERS THEN
            user_id_val := NULL;
    END;
    
    BEGIN
        session_id_val := current_setting('app.current_session_id', true);
    EXCEPTION
        WHEN OTHERS THEN
            session_id_val := NULL;
    END;
    
    BEGIN
        ip_address_val := current_setting('app.current_ip_address', true)::INET;
    EXCEPTION
        WHEN OTHERS THEN
            ip_address_val := NULL;
    END;
    
    BEGIN
        user_agent_val := current_setting('app.current_user_agent', true);
    EXCEPTION
        WHEN OTHERS THEN
            user_agent_val := NULL;
    END;
    
    BEGIN
        correlation_id_val := current_setting('app.correlation_id', true);
    EXCEPTION
        WHEN OTHERS THEN
            correlation_id_val := NULL;
    END;
    
    -- Handle different operations
    IF TG_OP = 'DELETE' THEN
        old_data := to_jsonb(OLD);
        new_data := NULL;
        changed_fields := NULL;
    ELSIF TG_OP = 'INSERT' THEN
        old_data := NULL;
        new_data := to_jsonb(NEW);
        changed_fields := NULL;
    ELSIF TG_OP = 'UPDATE' THEN
        old_data := to_jsonb(OLD);
        new_data := to_jsonb(NEW);
        
        -- Calculate changed fields
        SELECT array_agg(key) INTO changed_fields
        FROM (
            SELECT key
            FROM jsonb_each(old_data) old_kv
            FULL OUTER JOIN jsonb_each(new_data) new_kv USING (key)
            WHERE old_kv.value IS DISTINCT FROM new_kv.value
        ) changed;
    END IF;
    
    -- Insert audit record
    INSERT INTO audit_log (
        tenant_id, table_name, record_id, operation,
        old_values, new_values, changed_fields,
        user_id, session_id, ip_address, user_agent, correlation_id
    ) VALUES (
        tenant_id_val, TG_TABLE_NAME, 
        COALESCE(NEW.id, OLD.id), TG_OP,
        old_data, new_data, changed_fields,
        user_id_val, session_id_val, ip_address_val, user_agent_val, correlation_id_val
    );
    
    -- Return appropriate record
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Log error but don't fail the main operation
        RAISE WARNING 'Audit trigger failed: %', SQLERRM;
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        ELSE
            RETURN NEW;
        END IF;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 3. ENABLE ROW-LEVEL SECURITY
-- =====================================================

-- Enable RLS on existing tables
ALTER TABLE pipelines ENABLE ROW LEVEL SECURITY;
ALTER TABLE pipeline_stages ENABLE ROW LEVEL SECURITY;
ALTER TABLE deals ENABLE ROW LEVEL SECURITY;
ALTER TABLE deal_stage_history ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policies
CREATE POLICY tenant_isolation_pipelines ON pipelines
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_pipeline_stages ON pipeline_stages
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_deals ON deals
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_deal_stage_history ON deal_stage_history
    USING (tenant_id = get_current_tenant_id());

-- =====================================================
-- 4. ADD AUDIT TRIGGERS TO EXISTING TABLES
-- =====================================================

-- Add audit triggers to all existing tables
CREATE TRIGGER audit_pipelines_trigger
    AFTER INSERT OR UPDATE OR DELETE ON pipelines
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_pipeline_stages_trigger
    AFTER INSERT OR UPDATE OR DELETE ON pipeline_stages
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_deals_trigger
    AFTER INSERT OR UPDATE OR DELETE ON deals
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_deal_stage_history_trigger
    AFTER INSERT OR UPDATE OR DELETE ON deal_stage_history
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 5. CREATE DEAL PRODUCTS TABLE
-- =====================================================

CREATE TABLE deal_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    deal_id UUID NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    product_id UUID,
    product_name VARCHAR(255) NOT NULL,
    product_code VARCHAR(100),
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit_price DECIMAL(15,2) NOT NULL,
    discount_percentage DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    total_price DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    product_category VARCHAR(100),
    description TEXT,
    custom_fields JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_quantity CHECK (quantity > 0),
    CONSTRAINT valid_unit_price CHECK (unit_price >= 0),
    CONSTRAINT valid_discount_percentage CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    CONSTRAINT valid_discount_amount CHECK (discount_amount >= 0),
    CONSTRAINT valid_total_price CHECK (total_price >= 0)
);

-- Indexes for deal_products table
CREATE INDEX idx_deal_products_tenant ON deal_products(tenant_id);
CREATE INDEX idx_deal_products_deal ON deal_products(deal_id);
CREATE INDEX idx_deal_products_product ON deal_products(product_id) WHERE product_id IS NOT NULL;
CREATE INDEX idx_deal_products_category ON deal_products(tenant_id, product_category) WHERE product_category IS NOT NULL;
CREATE INDEX idx_deal_products_total_price ON deal_products(tenant_id, total_price DESC);
CREATE INDEX idx_deal_products_custom_fields ON deal_products USING GIN(custom_fields) WHERE custom_fields != '{}'::jsonb;

-- Enable RLS and add audit trigger for deal_products
ALTER TABLE deal_products ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_deal_products ON deal_products
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_deal_products_trigger
    AFTER INSERT OR UPDATE OR DELETE ON deal_products
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 6. CREATE DEAL COMPETITORS TABLE
-- =====================================================

CREATE TABLE deal_competitors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    deal_id UUID NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    competitor_name VARCHAR(255) NOT NULL,
    competitor_type VARCHAR(50) DEFAULT 'DIRECT',
    strength_level VARCHAR(20) DEFAULT 'MEDIUM',
    competitive_advantage TEXT,
    competitive_disadvantage TEXT,
    pricing_info JSONB,
    win_probability_impact DECIMAL(5,2) DEFAULT 0,
    notes TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    identified_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_competitor_type CHECK (competitor_type IN (
        'DIRECT', 'INDIRECT', 'SUBSTITUTE', 'INTERNAL', 'STATUS_QUO'
    )),
    CONSTRAINT valid_strength_level CHECK (strength_level IN (
        'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    )),
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'ELIMINATED', 'UNKNOWN')),
    CONSTRAINT valid_win_probability_impact CHECK (win_probability_impact >= -100 AND win_probability_impact <= 100)
);

-- Indexes for deal_competitors table
CREATE INDEX idx_deal_competitors_tenant ON deal_competitors(tenant_id);
CREATE INDEX idx_deal_competitors_deal ON deal_competitors(deal_id);
CREATE INDEX idx_deal_competitors_name ON deal_competitors(tenant_id, competitor_name);
CREATE INDEX idx_deal_competitors_type ON deal_competitors(tenant_id, competitor_type);
CREATE INDEX idx_deal_competitors_strength ON deal_competitors(tenant_id, strength_level);
CREATE INDEX idx_deal_competitors_status ON deal_competitors(tenant_id, status);

-- Enable RLS and add audit trigger for deal_competitors
ALTER TABLE deal_competitors ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_deal_competitors ON deal_competitors
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_deal_competitors_trigger
    AFTER INSERT OR UPDATE OR DELETE ON deal_competitors
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 7. CREATE DEAL FORECASTING TABLE
-- =====================================================

CREATE TABLE deal_forecasts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    deal_id UUID NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    forecast_period VARCHAR(20) NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    forecasted_amount DECIMAL(15,2) NOT NULL,
    forecasted_close_date DATE,
    win_probability DECIMAL(5,2) NOT NULL,
    forecast_category VARCHAR(50) NOT NULL,
    confidence_level VARCHAR(20) DEFAULT 'MEDIUM',
    forecast_method VARCHAR(50) DEFAULT 'MANUAL',
    assumptions TEXT,
    risk_factors TEXT,
    forecast_by UUID NOT NULL,
    forecast_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT valid_forecast_period CHECK (forecast_period IN (
        'MONTHLY', 'QUARTERLY', 'YEARLY'
    )),
    CONSTRAINT valid_period_dates CHECK (period_end_date >= period_start_date),
    CONSTRAINT valid_forecasted_amount CHECK (forecasted_amount >= 0),
    CONSTRAINT valid_win_probability CHECK (win_probability >= 0 AND win_probability <= 100),
    CONSTRAINT valid_forecast_category CHECK (forecast_category IN (
        'COMMIT', 'BEST_CASE', 'PIPELINE', 'CLOSED'
    )),
    CONSTRAINT valid_confidence_level CHECK (confidence_level IN (
        'LOW', 'MEDIUM', 'HIGH'
    )),
    CONSTRAINT valid_forecast_method CHECK (forecast_method IN (
        'MANUAL', 'ALGORITHM', 'HISTORICAL', 'WEIGHTED'
    ))
);

-- Indexes for deal_forecasts table
CREATE INDEX idx_deal_forecasts_tenant ON deal_forecasts(tenant_id);
CREATE INDEX idx_deal_forecasts_deal ON deal_forecasts(deal_id);
CREATE INDEX idx_deal_forecasts_period ON deal_forecasts(tenant_id, forecast_period);
CREATE INDEX idx_deal_forecasts_dates ON deal_forecasts(period_start_date, period_end_date);
CREATE INDEX idx_deal_forecasts_category ON deal_forecasts(tenant_id, forecast_category);
CREATE INDEX idx_deal_forecasts_probability ON deal_forecasts(tenant_id, win_probability DESC);
CREATE INDEX idx_deal_forecasts_amount ON deal_forecasts(tenant_id, forecasted_amount DESC);

-- Enable RLS and add audit trigger for deal_forecasts
ALTER TABLE deal_forecasts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_deal_forecasts ON deal_forecasts
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_deal_forecasts_trigger
    AFTER INSERT OR UPDATE OR DELETE ON deal_forecasts
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 8. ENHANCED INDEXES FOR PERFORMANCE
-- =====================================================

-- Additional composite indexes for deals table
CREATE INDEX idx_deals_tenant_stage_amount ON deals(tenant_id, current_stage_id, amount DESC);
CREATE INDEX idx_deals_tenant_owner_stage ON deals(tenant_id, owner_id, current_stage_id);
CREATE INDEX idx_deals_tenant_account_stage ON deals(tenant_id, account_id, current_stage_id) WHERE account_id IS NOT NULL;
CREATE INDEX idx_deals_tenant_contact_stage ON deals(tenant_id, primary_contact_id, current_stage_id) WHERE primary_contact_id IS NOT NULL;
CREATE INDEX idx_deals_close_date_amount ON deals(tenant_id, expected_close_date, amount DESC) WHERE expected_close_date IS NOT NULL;
CREATE INDEX idx_deals_probability_amount ON deals(tenant_id, win_probability DESC, amount DESC);
CREATE INDEX idx_deals_source_stage ON deals(tenant_id, lead_source, current_stage_id) WHERE lead_source IS NOT NULL;

-- GIN indexes for array and JSONB fields
CREATE INDEX idx_deals_tags_gin ON deals USING GIN(tags) WHERE array_length(tags, 1) > 0;
CREATE INDEX idx_deals_custom_fields_gin ON deals USING GIN(custom_fields) WHERE custom_fields != '{}'::jsonb;

-- Partial indexes for active deals
CREATE INDEX idx_deals_open_by_amount ON deals(tenant_id, amount DESC) 
    WHERE status IN ('OPEN', 'IN_PROGRESS');
CREATE INDEX idx_deals_open_by_close_date ON deals(tenant_id, expected_close_date) 
    WHERE status IN ('OPEN', 'IN_PROGRESS') AND expected_close_date IS NOT NULL;
CREATE INDEX idx_deals_won_by_amount ON deals(tenant_id, amount DESC, closed_date) 
    WHERE status = 'WON';
CREATE INDEX idx_deals_lost_by_date ON deals(tenant_id, closed_date DESC) 
    WHERE status = 'LOST';

-- Text search indexes
CREATE INDEX idx_deals_name_search ON deals USING GIN(to_tsvector('english', name));
CREATE INDEX idx_deals_description_search ON deals USING GIN(to_tsvector('english', description)) 
    WHERE description IS NOT NULL;

-- =====================================================
-- 9. DEAL MANAGEMENT FUNCTIONS
-- =====================================================

-- Function to calculate deal score based on various factors
CREATE OR REPLACE FUNCTION calculate_deal_score(deal_id UUID)
RETURNS INTEGER AS $$
DECLARE
    deal_record RECORD;
    score INTEGER := 0;
    stage_weight DECIMAL(3,2);
    days_to_close INTEGER;
    competitor_count INTEGER;
    product_count INTEGER;
BEGIN
    -- Get deal record with stage information
    SELECT d.*, ps.probability as stage_probability
    INTO deal_record 
    FROM deals d
    LEFT JOIN pipeline_stages ps ON d.current_stage_id = ps.id
    WHERE d.id = deal_id;
    
    IF deal_record IS NULL THEN
        RETURN 0;
    END IF;
    
    -- Base score from amount (0-30 points)
    IF deal_record.amount IS NOT NULL THEN
        score := score + LEAST(30, (deal_record.amount / 100000)::INTEGER);
    END IF;
    
    -- Score from win probability (0-25 points)
    score := score + (deal_record.win_probability * 0.25)::INTEGER;
    
    -- Score from stage probability (0-20 points)
    IF deal_record.stage_probability IS NOT NULL THEN
        score := score + (deal_record.stage_probability * 0.20)::INTEGER;
    END IF;
    
    -- Score based on close date proximity (0-15 points)
    IF deal_record.expected_close_date IS NOT NULL THEN
        days_to_close := deal_record.expected_close_date - CURRENT_DATE;
        IF days_to_close <= 30 THEN
            score := score + 15;
        ELSIF days_to_close <= 90 THEN
            score := score + 10;
        ELSIF days_to_close <= 180 THEN
            score := score + 5;
        END IF;
    END IF;
    
    -- Deduct points for competitors (0 to -10 points)
    SELECT COUNT(*) INTO competitor_count 
    FROM deal_competitors 
    WHERE deal_id = deal_record.id AND status = 'ACTIVE';
    
    score := score - LEAST(10, competitor_count * 2);
    
    -- Add points for products (0-10 points)
    SELECT COUNT(*) INTO product_count 
    FROM deal_products 
    WHERE deal_id = deal_record.id;
    
    score := score + LEAST(10, product_count * 2);
    
    RETURN LEAST(100, GREATEST(0, score));
END;
$$ LANGUAGE plpgsql;

-- Function to update deal totals from products
CREATE OR REPLACE FUNCTION update_deal_totals(deal_id UUID)
RETURNS void AS $$
DECLARE
    total_amount DECIMAL(15,2);
    product_count INTEGER;
BEGIN
    -- Calculate total from products
    SELECT 
        COALESCE(SUM(total_price), 0),
        COUNT(*)
    INTO total_amount, product_count
    FROM deal_products 
    WHERE deal_id = deal_id;
    
    -- Update deal amount if products exist
    IF product_count > 0 THEN
        UPDATE deals 
        SET amount = total_amount,
            updated_at = NOW(),
            updated_by = COALESCE(current_setting('app.current_user_id', true)::UUID, updated_by)
        WHERE id = deal_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function to advance deal to next stage
CREATE OR REPLACE FUNCTION advance_deal_stage(
    deal_id UUID,
    next_stage_id UUID,
    notes TEXT DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    deal_record RECORD;
    current_stage RECORD;
    next_stage RECORD;
BEGIN
    -- Get deal and stage information
    SELECT * INTO deal_record FROM deals WHERE id = deal_id;
    SELECT * INTO current_stage FROM pipeline_stages WHERE id = deal_record.current_stage_id;
    SELECT * INTO next_stage FROM pipeline_stages WHERE id = next_stage_id;
    
    IF deal_record IS NULL OR next_stage IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Validate stage belongs to same pipeline
    IF current_stage.pipeline_id != next_stage.pipeline_id THEN
        RETURN FALSE;
    END IF;
    
    -- Update deal stage
    UPDATE deals 
    SET current_stage_id = next_stage_id,
        win_probability = next_stage.probability,
        updated_at = NOW(),
        updated_by = current_setting('app.current_user_id', true)::UUID
    WHERE id = deal_id;
    
    -- Record stage history
    INSERT INTO deal_stage_history (
        tenant_id, deal_id, from_stage_id, to_stage_id, 
        changed_by, change_reason, notes
    ) VALUES (
        deal_record.tenant_id, deal_id, deal_record.current_stage_id, next_stage_id,
        current_setting('app.current_user_id', true)::UUID, 'STAGE_ADVANCEMENT', notes
    );
    
    -- If moving to won/lost stage, update deal status and close date
    IF next_stage.stage_type IN ('WON', 'LOST') THEN
        UPDATE deals 
        SET status = next_stage.stage_type,
            closed_date = CURRENT_DATE,
            updated_at = NOW(),
            updated_by = current_setting('app.current_user_id', true)::UUID
        WHERE id = deal_id;
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to find similar deals
CREATE OR REPLACE FUNCTION find_similar_deals(
    deal_id UUID,
    similarity_threshold INTEGER DEFAULT 70
) RETURNS TABLE (
    similar_deal_id UUID,
    similarity_score INTEGER,
    matching_criteria TEXT[]
) AS $$
DECLARE
    target_deal RECORD;
BEGIN
    -- Get target deal
    SELECT * INTO target_deal FROM deals WHERE id = deal_id;
    
    IF target_deal IS NULL THEN
        RETURN;
    END IF;
    
    RETURN QUERY
    SELECT 
        d.id as similar_deal_id,
        (
            CASE WHEN d.account_id = target_deal.account_id THEN 30 ELSE 0 END +
            CASE WHEN d.primary_contact_id = target_deal.primary_contact_id THEN 20 ELSE 0 END +
            CASE WHEN d.lead_source = target_deal.lead_source THEN 15 ELSE 0 END +
            CASE WHEN d.amount IS NOT NULL AND target_deal.amount IS NOT NULL 
                 AND abs(d.amount - target_deal.amount) < (target_deal.amount * 0.5) 
                 THEN 15 ELSE 0 END +
            CASE WHEN d.pipeline_id = target_deal.pipeline_id THEN 10 ELSE 0 END +
            CASE WHEN array_length(d.tags & target_deal.tags, 1) > 0 THEN 10 ELSE 0 END
        ) as similarity_score,
        ARRAY[
            CASE WHEN d.account_id = target_deal.account_id THEN 'account' END,
            CASE WHEN d.primary_contact_id = target_deal.primary_contact_id THEN 'contact' END,
            CASE WHEN d.lead_source = target_deal.lead_source THEN 'lead_source' END,
            CASE WHEN d.amount IS NOT NULL AND target_deal.amount IS NOT NULL 
                 AND abs(d.amount - target_deal.amount) < (target_deal.amount * 0.5) 
                 THEN 'amount' END,
            CASE WHEN d.pipeline_id = target_deal.pipeline_id THEN 'pipeline' END,
            CASE WHEN array_length(d.tags & target_deal.tags, 1) > 0 THEN 'tags' END
        ]::TEXT[] as matching_criteria
    FROM deals d
    WHERE d.tenant_id = target_deal.tenant_id
    AND d.id != deal_id
    AND d.status != 'DELETED'
    HAVING (
        CASE WHEN d.account_id = target_deal.account_id THEN 30 ELSE 0 END +
        CASE WHEN d.primary_contact_id = target_deal.primary_contact_id THEN 20 ELSE 0 END +
        CASE WHEN d.lead_source = target_deal.lead_source THEN 15 ELSE 0 END +
        CASE WHEN d.amount IS NOT NULL AND target_deal.amount IS NOT NULL 
             AND abs(d.amount - target_deal.amount) < (target_deal.amount * 0.5) 
             THEN 15 ELSE 0 END +
        CASE WHEN d.pipeline_id = target_deal.pipeline_id THEN 10 ELSE 0 END +
        CASE WHEN array_length(d.tags & target_deal.tags, 1) > 0 THEN 10 ELSE 0 END
    ) >= similarity_threshold
    ORDER BY similarity_score DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 10. MONITORING VIEWS
-- =====================================================

-- View for deal statistics
CREATE OR REPLACE VIEW deal_statistics AS
SELECT 
    tenant_id,
    COUNT(*) as total_deals,
    COUNT(*) FILTER (WHERE status = 'OPEN') as open_deals,
    COUNT(*) FILTER (WHERE status = 'WON') as won_deals,
    COUNT(*) FILTER (WHERE status = 'LOST') as lost_deals,
    SUM(amount) FILTER (WHERE amount IS NOT NULL) as total_deal_value,
    SUM(amount) FILTER (WHERE status = 'WON' AND amount IS NOT NULL) as won_deal_value,
    SUM(amount) FILTER (WHERE status = 'OPEN' AND amount IS NOT NULL) as pipeline_value,
    AVG(amount) FILTER (WHERE amount IS NOT NULL) as avg_deal_size,
    AVG(win_probability) FILTER (WHERE status = 'OPEN') as avg_win_probability,
    COUNT(*) FILTER (WHERE created_at >= CURRENT_DATE - INTERVAL '30 days') as new_deals_30d,
    COUNT(*) FILTER (WHERE status = 'WON' AND closed_date >= CURRENT_DATE - INTERVAL '30 days') as won_deals_30d
FROM deals
GROUP BY tenant_id;

-- View for pipeline performance
CREATE OR REPLACE VIEW pipeline_performance AS
SELECT 
    p.tenant_id,
    p.id as pipeline_id,
    p.name as pipeline_name,
    COUNT(d.id) as total_deals,
    COUNT(d.id) FILTER (WHERE d.status = 'OPEN') as open_deals,
    COUNT(d.id) FILTER (WHERE d.status = 'WON') as won_deals,
    COUNT(d.id) FILTER (WHERE d.status = 'LOST') as lost_deals,
    SUM(d.amount) FILTER (WHERE d.amount IS NOT NULL) as total_value,
    SUM(d.amount) FILTER (WHERE d.status = 'OPEN' AND d.amount IS NOT NULL) as pipeline_value,
    AVG(d.amount) FILTER (WHERE d.amount IS NOT NULL) as avg_deal_size,
    CASE 
        WHEN COUNT(d.id) FILTER (WHERE d.status IN ('WON', 'LOST')) > 0 
        THEN (COUNT(d.id) FILTER (WHERE d.status = 'WON')::DECIMAL / 
              COUNT(d.id) FILTER (WHERE d.status IN ('WON', 'LOST')) * 100)
        ELSE 0 
    END as win_rate_percentage
FROM pipelines p
LEFT JOIN deals d ON p.id = d.pipeline_id
GROUP BY p.tenant_id, p.id, p.name;

-- =====================================================
-- 11. MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to update deal scores for all deals
CREATE OR REPLACE FUNCTION update_all_deal_scores(
    tenant_uuid UUID DEFAULT NULL
) RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER := 0;
    deal_record RECORD;
    new_score INTEGER;
BEGIN
    FOR deal_record IN 
        SELECT id FROM deals 
        WHERE (tenant_uuid IS NULL OR tenant_id = tenant_uuid)
        AND status = 'OPEN'
    LOOP
        new_score := calculate_deal_score(deal_record.id);
        
        -- Update deal with calculated score (assuming there's a score field)
        -- This would need to be added to the deals table schema
        -- UPDATE deals SET score = new_score WHERE id = deal_record.id;
        
        updated_count := updated_count + 1;
    END LOOP;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up old forecasts
CREATE OR REPLACE FUNCTION cleanup_old_forecasts(
    retention_days INTEGER DEFAULT 365
) RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date DATE;
BEGIN
    cutoff_date := CURRENT_DATE - retention_days;
    
    DELETE FROM deal_forecasts 
    WHERE period_end_date < cutoff_date;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Add trigger to automatically update deal totals when products change
CREATE OR REPLACE FUNCTION deal_products_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM update_deal_totals(OLD.deal_id);
        RETURN OLD;
    ELSE
        PERFORM update_deal_totals(NEW.deal_id);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER deal_products_update_trigger
    AFTER INSERT OR UPDATE OR DELETE ON deal_products
    FOR EACH ROW EXECUTE FUNCTION deal_products_trigger();

-- Add comments for documentation
COMMENT ON TABLE deal_products IS 'Products/services associated with deals including pricing and quantities';
COMMENT ON TABLE deal_competitors IS 'Competitor tracking for deals with competitive analysis';
COMMENT ON TABLE deal_forecasts IS 'Deal forecasting data for sales pipeline management';
COMMENT ON FUNCTION calculate_deal_score(UUID) IS 'Calculates deal score based on amount, probability, stage, and other factors';
COMMENT ON FUNCTION advance_deal_stage(UUID, UUID, TEXT) IS 'Advances deal to next stage with history tracking';
COMMENT ON FUNCTION find_similar_deals(UUID, INTEGER) IS 'Finds deals similar to target deal based on multiple criteria';