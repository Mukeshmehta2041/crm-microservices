-- =====================================================
-- ACCOUNTS SERVICE - COMPREHENSIVE SCHEMA ENHANCEMENTS
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
-- 1. CREATE AUDIT LOG TABLE FOR ACCOUNTS SERVICE
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

-- Enable RLS on accounts table
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policy for accounts
CREATE POLICY tenant_isolation_accounts ON accounts
    USING (tenant_id = get_current_tenant_id());

-- Enable RLS on account_relationships table
ALTER TABLE account_relationships ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policy for account_relationships
CREATE POLICY tenant_isolation_account_relationships ON account_relationships
    USING (tenant_id = get_current_tenant_id());

-- =====================================================
-- 4. ADD AUDIT TRIGGERS TO EXISTING TABLES
-- =====================================================

-- Add audit trigger to accounts table
CREATE TRIGGER audit_accounts_trigger
    AFTER INSERT OR UPDATE OR DELETE ON accounts
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Add audit trigger to account_relationships table
CREATE TRIGGER audit_account_relationships_trigger
    AFTER INSERT OR UPDATE OR DELETE ON account_relationships
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 5. CREATE ACCOUNT TERRITORIES TABLE
-- =====================================================

CREATE TABLE account_territories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    territory_type VARCHAR(50) DEFAULT 'GEOGRAPHIC',
    parent_territory_id UUID REFERENCES account_territories(id) ON DELETE SET NULL,
    hierarchy_level INTEGER DEFAULT 0,
    hierarchy_path TEXT,
    geographic_bounds JSONB,
    rules JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    manager_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_territory_type CHECK (territory_type IN (
        'GEOGRAPHIC', 'INDUSTRY', 'REVENUE', 'CUSTOM'
    )),
    CONSTRAINT valid_hierarchy_level CHECK (hierarchy_level >= 0 AND hierarchy_level <= 10),
    CONSTRAINT no_self_parent CHECK (id != parent_territory_id),
    CONSTRAINT uk_tenant_territory_name UNIQUE (tenant_id, name)
);

-- Indexes for account_territories table
CREATE INDEX idx_account_territories_tenant ON account_territories(tenant_id);
CREATE INDEX idx_account_territories_parent ON account_territories(parent_territory_id);
CREATE INDEX idx_account_territories_manager ON account_territories(manager_id);
CREATE INDEX idx_account_territories_type ON account_territories(tenant_id, territory_type);
CREATE INDEX idx_account_territories_active ON account_territories(tenant_id, is_active);
CREATE INDEX idx_account_territories_hierarchy ON account_territories(hierarchy_level, hierarchy_path);

-- Enable RLS and add audit trigger for account_territories
ALTER TABLE account_territories ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_account_territories ON account_territories
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_account_territories_trigger
    AFTER INSERT OR UPDATE OR DELETE ON account_territories
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 6. CREATE ACCOUNT REVENUE HISTORY TABLE
-- =====================================================

CREATE TABLE account_revenue_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    revenue_period VARCHAR(20) NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    revenue_amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    revenue_type VARCHAR(50) DEFAULT 'ACTUAL',
    notes TEXT,
    source VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_revenue_period CHECK (revenue_period IN (
        'MONTHLY', 'QUARTERLY', 'YEARLY', 'CUSTOM'
    )),
    CONSTRAINT valid_revenue_type CHECK (revenue_type IN (
        'ACTUAL', 'PROJECTED', 'BUDGET', 'FORECAST'
    )),
    CONSTRAINT valid_period_dates CHECK (period_end_date >= period_start_date),
    CONSTRAINT valid_revenue_amount CHECK (revenue_amount >= 0),
    CONSTRAINT uk_account_revenue_period UNIQUE (tenant_id, account_id, revenue_period, period_start_date)
);

-- Indexes for account_revenue_history table
CREATE INDEX idx_account_revenue_tenant ON account_revenue_history(tenant_id);
CREATE INDEX idx_account_revenue_account ON account_revenue_history(account_id);
CREATE INDEX idx_account_revenue_period ON account_revenue_history(tenant_id, revenue_period);
CREATE INDEX idx_account_revenue_dates ON account_revenue_history(period_start_date, period_end_date);
CREATE INDEX idx_account_revenue_type ON account_revenue_history(tenant_id, revenue_type);
CREATE INDEX idx_account_revenue_amount ON account_revenue_history(tenant_id, revenue_amount DESC);

-- Enable RLS and add audit trigger for account_revenue_history
ALTER TABLE account_revenue_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_account_revenue_history ON account_revenue_history
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_account_revenue_history_trigger
    AFTER INSERT OR UPDATE OR DELETE ON account_revenue_history
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 7. ENHANCED INDEXES FOR PERFORMANCE
-- =====================================================

-- Additional composite indexes for accounts table
CREATE INDEX idx_accounts_tenant_status_type ON accounts(tenant_id, status, account_type);
CREATE INDEX idx_accounts_tenant_owner_status ON accounts(tenant_id, owner_id, status);
CREATE INDEX idx_accounts_tenant_industry_revenue ON accounts(tenant_id, industry, annual_revenue DESC NULLS LAST);
CREATE INDEX idx_accounts_tenant_territory_status ON accounts(tenant_id, territory_id, status) WHERE territory_id IS NOT NULL;
CREATE INDEX idx_accounts_parent_hierarchy ON accounts(tenant_id, parent_account_id, hierarchy_level) WHERE parent_account_id IS NOT NULL;
CREATE INDEX idx_accounts_employee_count ON accounts(tenant_id, employee_count DESC NULLS LAST) WHERE employee_count IS NOT NULL;

-- GIN indexes for array and JSONB fields
CREATE INDEX idx_accounts_tags_gin ON accounts USING GIN(tags) WHERE array_length(tags, 1) > 0;
CREATE INDEX idx_accounts_custom_fields_gin ON accounts USING GIN(custom_fields) WHERE custom_fields != '{}'::jsonb;
CREATE INDEX idx_accounts_billing_address_gin ON accounts USING GIN(billing_address) WHERE billing_address IS NOT NULL;
CREATE INDEX idx_accounts_shipping_address_gin ON accounts USING GIN(shipping_address) WHERE shipping_address IS NOT NULL;

-- Partial indexes for active accounts
CREATE INDEX idx_accounts_active_by_revenue ON accounts(tenant_id, annual_revenue DESC NULLS LAST) 
    WHERE status = 'ACTIVE';
CREATE INDEX idx_accounts_active_by_employees ON accounts(tenant_id, employee_count DESC NULLS LAST) 
    WHERE status = 'ACTIVE' AND employee_count IS NOT NULL;
CREATE INDEX idx_accounts_customers_by_revenue ON accounts(tenant_id, annual_revenue DESC NULLS LAST) 
    WHERE status = 'CUSTOMER';

-- Text search indexes
CREATE INDEX idx_accounts_name_search ON accounts USING GIN(to_tsvector('english', name));
CREATE INDEX idx_accounts_description_search ON accounts USING GIN(to_tsvector('english', description)) 
    WHERE description IS NOT NULL;

-- =====================================================
-- 8. VALIDATION FUNCTIONS
-- =====================================================

-- Website URL validation function
CREATE OR REPLACE FUNCTION is_valid_url(url TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN url ~ '^https?://[^\s/$.?#].[^\s]*$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Phone validation function
CREATE OR REPLACE FUNCTION is_valid_phone(phone TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN phone ~ '^\+?[1-9]\d{1,14}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Account hierarchy validation function
CREATE OR REPLACE FUNCTION validate_account_hierarchy(
    account_id UUID,
    parent_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    current_parent UUID;
    depth INTEGER := 0;
    max_depth INTEGER := 10;
BEGIN
    -- Cannot be parent of itself
    IF account_id = parent_id THEN
        RETURN FALSE;
    END IF;
    
    -- Check for circular reference
    current_parent := parent_id;
    WHILE current_parent IS NOT NULL AND depth < max_depth LOOP
        IF current_parent = account_id THEN
            RETURN FALSE; -- Circular reference detected
        END IF;
        
        SELECT parent_account_id INTO current_parent 
        FROM accounts 
        WHERE id = current_parent;
        
        depth := depth + 1;
    END LOOP;
    
    -- Check max depth
    IF depth >= max_depth THEN
        RETURN FALSE;
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 9. ACCOUNT MANAGEMENT FUNCTIONS
-- =====================================================

-- Function to update account hierarchy path
CREATE OR REPLACE FUNCTION update_account_hierarchy_path(account_id UUID)
RETURNS void AS $$
DECLARE
    account_record RECORD;
    parent_path TEXT;
    new_path TEXT;
    new_level INTEGER;
BEGIN
    -- Get account record
    SELECT * INTO account_record FROM accounts WHERE id = account_id;
    
    IF account_record IS NULL THEN
        RETURN;
    END IF;
    
    -- Calculate new hierarchy path and level
    IF account_record.parent_account_id IS NULL THEN
        new_path := account_id::TEXT;
        new_level := 0;
    ELSE
        -- Get parent's hierarchy path
        SELECT hierarchy_path, hierarchy_level INTO parent_path, new_level
        FROM accounts 
        WHERE id = account_record.parent_account_id;
        
        IF parent_path IS NULL THEN
            new_path := account_record.parent_account_id::TEXT || '/' || account_id::TEXT;
            new_level := 1;
        ELSE
            new_path := parent_path || '/' || account_id::TEXT;
            new_level := new_level + 1;
        END IF;
    END IF;
    
    -- Update account hierarchy
    UPDATE accounts 
    SET hierarchy_path = new_path,
        hierarchy_level = new_level,
        updated_at = NOW(),
        updated_by = COALESCE(current_setting('app.current_user_id', true)::UUID, updated_by)
    WHERE id = account_id;
    
    -- Update all child accounts recursively
    UPDATE accounts 
    SET hierarchy_path = new_path || substring(hierarchy_path from length(account_id::TEXT) + 1),
        hierarchy_level = hierarchy_level + (new_level - account_record.hierarchy_level),
        updated_at = NOW(),
        updated_by = COALESCE(current_setting('app.current_user_id', true)::UUID, updated_by)
    WHERE hierarchy_path LIKE account_record.hierarchy_path || '/%';
END;
$$ LANGUAGE plpgsql;

-- Function to calculate account score based on various factors
CREATE OR REPLACE FUNCTION calculate_account_score(account_id UUID)
RETURNS INTEGER AS $$
DECLARE
    account_record RECORD;
    score INTEGER := 0;
    contact_count INTEGER;
    deal_count INTEGER;
    recent_activity_count INTEGER;
BEGIN
    -- Get account record
    SELECT * INTO account_record FROM accounts WHERE id = account_id;
    
    IF account_record IS NULL THEN
        RETURN 0;
    END IF;
    
    -- Base score from revenue (0-40 points)
    IF account_record.annual_revenue IS NOT NULL THEN
        score := score + LEAST(40, (account_record.annual_revenue / 1000000)::INTEGER);
    END IF;
    
    -- Score from employee count (0-20 points)
    IF account_record.employee_count IS NOT NULL THEN
        score := score + LEAST(20, (account_record.employee_count / 100)::INTEGER);
    END IF;
    
    -- Score from account type (0-15 points)
    CASE account_record.account_type
        WHEN 'CUSTOMER' THEN score := score + 15;
        WHEN 'PROSPECT' THEN score := score + 10;
        WHEN 'PARTNER' THEN score := score + 12;
        WHEN 'RESELLER' THEN score := score + 8;
        ELSE score := score + 5;
    END CASE;
    
    -- Score from status (0-10 points)
    CASE account_record.status
        WHEN 'ACTIVE' THEN score := score + 10;
        WHEN 'CUSTOMER' THEN score := score + 10;
        WHEN 'PROSPECT' THEN score := score + 7;
        ELSE score := score + 3;
    END CASE;
    
    -- Additional scoring could be added here for:
    -- - Number of contacts
    -- - Number of deals
    -- - Recent activity
    -- - Custom field values
    
    RETURN LEAST(100, score);
END;
$$ LANGUAGE plpgsql;

-- Function to find similar accounts
CREATE OR REPLACE FUNCTION find_similar_accounts(
    account_id UUID,
    similarity_threshold INTEGER DEFAULT 70
) RETURNS TABLE (
    similar_account_id UUID,
    similarity_score INTEGER,
    matching_criteria TEXT[]
) AS $$
DECLARE
    target_account RECORD;
BEGIN
    -- Get target account
    SELECT * INTO target_account FROM accounts WHERE id = account_id;
    
    IF target_account IS NULL THEN
        RETURN;
    END IF;
    
    RETURN QUERY
    SELECT 
        a.id as similar_account_id,
        (
            CASE WHEN a.industry = target_account.industry THEN 25 ELSE 0 END +
            CASE WHEN a.account_type = target_account.account_type THEN 20 ELSE 0 END +
            CASE WHEN a.employee_count IS NOT NULL AND target_account.employee_count IS NOT NULL 
                 AND abs(a.employee_count - target_account.employee_count) < (target_account.employee_count * 0.3) 
                 THEN 15 ELSE 0 END +
            CASE WHEN a.annual_revenue IS NOT NULL AND target_account.annual_revenue IS NOT NULL 
                 AND abs(a.annual_revenue - target_account.annual_revenue) < (target_account.annual_revenue * 0.5) 
                 THEN 20 ELSE 0 END +
            CASE WHEN a.territory_id = target_account.territory_id THEN 10 ELSE 0 END +
            CASE WHEN array_length(a.tags & target_account.tags, 1) > 0 THEN 10 ELSE 0 END
        ) as similarity_score,
        ARRAY[
            CASE WHEN a.industry = target_account.industry THEN 'industry' END,
            CASE WHEN a.account_type = target_account.account_type THEN 'account_type' END,
            CASE WHEN a.employee_count IS NOT NULL AND target_account.employee_count IS NOT NULL 
                 AND abs(a.employee_count - target_account.employee_count) < (target_account.employee_count * 0.3) 
                 THEN 'employee_count' END,
            CASE WHEN a.annual_revenue IS NOT NULL AND target_account.annual_revenue IS NOT NULL 
                 AND abs(a.annual_revenue - target_account.annual_revenue) < (target_account.annual_revenue * 0.5) 
                 THEN 'annual_revenue' END,
            CASE WHEN a.territory_id = target_account.territory_id THEN 'territory' END,
            CASE WHEN array_length(a.tags & target_account.tags, 1) > 0 THEN 'tags' END
        ]::TEXT[] as matching_criteria
    FROM accounts a
    WHERE a.tenant_id = target_account.tenant_id
    AND a.id != account_id
    AND a.status = 'ACTIVE'
    HAVING (
        CASE WHEN a.industry = target_account.industry THEN 25 ELSE 0 END +
        CASE WHEN a.account_type = target_account.account_type THEN 20 ELSE 0 END +
        CASE WHEN a.employee_count IS NOT NULL AND target_account.employee_count IS NOT NULL 
             AND abs(a.employee_count - target_account.employee_count) < (target_account.employee_count * 0.3) 
             THEN 15 ELSE 0 END +
        CASE WHEN a.annual_revenue IS NOT NULL AND target_account.annual_revenue IS NOT NULL 
             AND abs(a.annual_revenue - target_account.annual_revenue) < (target_account.annual_revenue * 0.5) 
             THEN 20 ELSE 0 END +
        CASE WHEN a.territory_id = target_account.territory_id THEN 10 ELSE 0 END +
        CASE WHEN array_length(a.tags & target_account.tags, 1) > 0 THEN 10 ELSE 0 END
    ) >= similarity_threshold
    ORDER BY similarity_score DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 10. MONITORING VIEWS
-- =====================================================

-- View for account statistics
CREATE OR REPLACE VIEW account_statistics AS
SELECT 
    tenant_id,
    COUNT(*) as total_accounts,
    COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_accounts,
    COUNT(*) FILTER (WHERE status = 'CUSTOMER') as customer_accounts,
    COUNT(*) FILTER (WHERE status = 'PROSPECT') as prospect_accounts,
    COUNT(*) FILTER (WHERE account_type = 'CUSTOMER') as customer_type_accounts,
    COUNT(*) FILTER (WHERE account_type = 'PARTNER') as partner_accounts,
    COUNT(*) FILTER (WHERE annual_revenue IS NOT NULL) as accounts_with_revenue,
    AVG(annual_revenue) FILTER (WHERE annual_revenue IS NOT NULL) as avg_annual_revenue,
    SUM(annual_revenue) FILTER (WHERE annual_revenue IS NOT NULL) as total_annual_revenue,
    COUNT(*) FILTER (WHERE employee_count IS NOT NULL) as accounts_with_employees,
    AVG(employee_count) FILTER (WHERE employee_count IS NOT NULL) as avg_employee_count,
    COUNT(*) FILTER (WHERE created_at >= CURRENT_DATE - INTERVAL '30 days') as new_accounts_30d,
    COUNT(*) FILTER (WHERE updated_at >= CURRENT_DATE - INTERVAL '7 days') as updated_accounts_7d
FROM accounts
GROUP BY tenant_id;

-- View for account hierarchy summary
CREATE OR REPLACE VIEW account_hierarchy_summary AS
SELECT 
    tenant_id,
    hierarchy_level,
    COUNT(*) as accounts_at_level,
    COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_accounts_at_level,
    AVG(annual_revenue) FILTER (WHERE annual_revenue IS NOT NULL) as avg_revenue_at_level
FROM accounts
GROUP BY tenant_id, hierarchy_level
ORDER BY tenant_id, hierarchy_level;

-- =====================================================
-- 11. MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to update all account hierarchy paths
CREATE OR REPLACE FUNCTION refresh_account_hierarchies(
    tenant_uuid UUID DEFAULT NULL
) RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER := 0;
    account_record RECORD;
BEGIN
    -- Process root accounts first (no parent)
    FOR account_record IN 
        SELECT id FROM accounts 
        WHERE (tenant_uuid IS NULL OR tenant_id = tenant_uuid)
        AND parent_account_id IS NULL
        ORDER BY created_at
    LOOP
        PERFORM update_account_hierarchy_path(account_record.id);
        updated_count := updated_count + 1;
    END LOOP;
    
    -- Process child accounts level by level
    FOR account_record IN 
        SELECT id FROM accounts 
        WHERE (tenant_uuid IS NULL OR tenant_id = tenant_uuid)
        AND parent_account_id IS NOT NULL
        ORDER BY hierarchy_level, created_at
    LOOP
        PERFORM update_account_hierarchy_path(account_record.id);
        updated_count := updated_count + 1;
    END LOOP;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up orphaned account relationships
CREATE OR REPLACE FUNCTION cleanup_orphaned_account_relationships()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Delete relationships where referenced accounts don't exist
    DELETE FROM account_relationships ar
    WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.id = ar.account_id)
    OR NOT EXISTS (SELECT 1 FROM accounts a WHERE a.id = ar.related_account_id);
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Add trigger to automatically update hierarchy when parent changes
CREATE OR REPLACE FUNCTION account_hierarchy_trigger()
RETURNS TRIGGER AS $$
BEGIN
    -- Only update if parent_account_id changed
    IF TG_OP = 'UPDATE' AND (OLD.parent_account_id IS DISTINCT FROM NEW.parent_account_id) THEN
        -- Validate hierarchy
        IF NEW.parent_account_id IS NOT NULL AND NOT validate_account_hierarchy(NEW.id, NEW.parent_account_id) THEN
            RAISE EXCEPTION 'Invalid account hierarchy: circular reference or max depth exceeded';
        END IF;
        
        -- Update hierarchy path
        PERFORM update_account_hierarchy_path(NEW.id);
    ELSIF TG_OP = 'INSERT' AND NEW.parent_account_id IS NOT NULL THEN
        -- Validate hierarchy for new accounts
        IF NOT validate_account_hierarchy(NEW.id, NEW.parent_account_id) THEN
            RAISE EXCEPTION 'Invalid account hierarchy: circular reference or max depth exceeded';
        END IF;
        
        -- Update hierarchy path
        PERFORM update_account_hierarchy_path(NEW.id);
    END IF;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Add hierarchy trigger to accounts table
CREATE TRIGGER account_hierarchy_update_trigger
    AFTER INSERT OR UPDATE OF parent_account_id ON accounts
    FOR EACH ROW EXECUTE FUNCTION account_hierarchy_trigger();

-- Add comments for documentation
COMMENT ON TABLE account_territories IS 'Territory management for account assignment and sales organization';
COMMENT ON TABLE account_revenue_history IS 'Historical revenue tracking per account by period';
COMMENT ON FUNCTION update_account_hierarchy_path(UUID) IS 'Updates hierarchy path and level for account and all children';
COMMENT ON FUNCTION calculate_account_score(UUID) IS 'Calculates account score based on revenue, size, type, and status';
COMMENT ON FUNCTION find_similar_accounts(UUID, INTEGER) IS 'Finds accounts similar to target account based on multiple criteria';
COMMENT ON FUNCTION refresh_account_hierarchies(UUID) IS 'Refreshes hierarchy paths for all accounts in tenant';