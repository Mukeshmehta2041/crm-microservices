-- =====================================================
-- TENANT SERVICE - COMPREHENSIVE SCHEMA ENHANCEMENTS
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
-- 1. CREATE AUDIT LOG TABLE FOR TENANT SERVICE
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
    -- For tenant table, use the record ID as tenant_id
    IF TG_TABLE_NAME = 'tenants' THEN
        IF TG_OP = 'DELETE' THEN
            tenant_id_val := OLD.id;
        ELSE
            tenant_id_val := NEW.id;
        END IF;
    ELSE
        -- For other tables, extract tenant_id from the record
        IF TG_OP = 'DELETE' THEN
            tenant_id_val := OLD.tenant_id;
        ELSE
            tenant_id_val := NEW.tenant_id;
        END IF;
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
-- 3. ADD AUDIT TRIGGERS TO EXISTING TABLES
-- =====================================================

-- Add audit trigger to tenants table
CREATE TRIGGER audit_tenants_trigger
    AFTER INSERT OR UPDATE OR DELETE ON tenants
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Add audit trigger to tenant_configurations table
CREATE TRIGGER audit_tenant_configurations_trigger
    AFTER INSERT OR UPDATE OR DELETE ON tenant_configurations
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 4. ENHANCED INDEXES FOR PERFORMANCE
-- =====================================================

-- Additional composite indexes for tenants table
CREATE INDEX idx_tenants_status_plan ON tenants(status, plan_type);
CREATE INDEX idx_tenants_trial_expires ON tenants(trial_ends_at) WHERE trial_ends_at IS NOT NULL;
CREATE INDEX idx_tenants_subscription_expires ON tenants(subscription_expires_at) WHERE subscription_expires_at IS NOT NULL;
CREATE INDEX idx_tenants_is_trial ON tenants(is_trial, created_at);

-- Additional indexes for tenant_configurations table
CREATE INDEX idx_tenant_config_category_key ON tenant_configurations(category, config_key);
CREATE INDEX idx_tenant_config_system_editable ON tenant_configurations(is_system, is_editable);
CREATE INDEX idx_tenant_config_encrypted ON tenant_configurations(is_encrypted) WHERE is_encrypted = true;

-- =====================================================
-- 5. CREATE TENANT FEATURE FLAGS TABLE
-- =====================================================

CREATE TABLE tenant_feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    feature_key VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN DEFAULT FALSE,
    config_value JSONB DEFAULT '{}',
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT uk_tenant_feature UNIQUE (tenant_id, feature_key)
);

-- Indexes for tenant_feature_flags table
CREATE INDEX idx_tenant_features_tenant ON tenant_feature_flags(tenant_id);
CREATE INDEX idx_tenant_features_key ON tenant_feature_flags(feature_key);
CREATE INDEX idx_tenant_features_enabled ON tenant_feature_flags(tenant_id, is_enabled);
CREATE INDEX idx_tenant_features_expires ON tenant_feature_flags(expires_at) WHERE expires_at IS NOT NULL;

-- Add audit trigger to tenant_feature_flags table
CREATE TRIGGER audit_tenant_feature_flags_trigger
    AFTER INSERT OR UPDATE OR DELETE ON tenant_feature_flags
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Add updated_at trigger to tenant_feature_flags table
CREATE TRIGGER update_tenant_feature_flags_updated_at BEFORE UPDATE ON tenant_feature_flags
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 6. CREATE TENANT USAGE METRICS TABLE
-- =====================================================

CREATE TABLE tenant_usage_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    metric_name VARCHAR(100) NOT NULL,
    metric_value BIGINT NOT NULL DEFAULT 0,
    metric_date DATE NOT NULL DEFAULT CURRENT_DATE,
    additional_data JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT uk_tenant_metric_date UNIQUE (tenant_id, metric_name, metric_date)
);

-- Indexes for tenant_usage_metrics table
CREATE INDEX idx_tenant_metrics_tenant ON tenant_usage_metrics(tenant_id);
CREATE INDEX idx_tenant_metrics_name ON tenant_usage_metrics(metric_name);
CREATE INDEX idx_tenant_metrics_date ON tenant_usage_metrics(metric_date);
CREATE INDEX idx_tenant_metrics_tenant_date ON tenant_usage_metrics(tenant_id, metric_date);

-- Add audit trigger to tenant_usage_metrics table
CREATE TRIGGER audit_tenant_usage_metrics_trigger
    AFTER INSERT OR UPDATE OR DELETE ON tenant_usage_metrics
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 7. VALIDATION FUNCTIONS
-- =====================================================

-- Subdomain validation function
CREATE OR REPLACE FUNCTION is_valid_subdomain(subdomain TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN subdomain ~ '^[a-z0-9][a-z0-9-]*[a-z0-9]$' 
        AND length(subdomain) >= 3 
        AND length(subdomain) <= 63
        AND subdomain NOT LIKE '%-%-%';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Color validation function
CREATE OR REPLACE FUNCTION is_valid_hex_color(color TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN color ~ '^#[0-9A-Fa-f]{6}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- 8. TENANT MANAGEMENT FUNCTIONS
-- =====================================================

-- Function to create a new tenant with default configurations
CREATE OR REPLACE FUNCTION create_tenant_with_defaults(
    tenant_name TEXT,
    tenant_subdomain TEXT,
    plan_type TEXT DEFAULT 'BASIC',
    contact_email TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    new_tenant_id UUID;
    config_record RECORD;
BEGIN
    -- Insert new tenant
    INSERT INTO tenants (name, subdomain, plan_type, contact_email)
    VALUES (tenant_name, tenant_subdomain, plan_type, contact_email)
    RETURNING id INTO new_tenant_id;
    
    -- Copy default configurations from system tenant
    FOR config_record IN 
        SELECT config_key, config_value, config_type, category, description, is_system, is_editable
        FROM tenant_configurations 
        WHERE tenant_id = '00000000-0000-0000-0000-000000000000'::UUID
    LOOP
        INSERT INTO tenant_configurations (
            tenant_id, config_key, config_value, config_type, 
            category, description, is_system, is_editable
        ) VALUES (
            new_tenant_id, config_record.config_key, config_record.config_value, 
            config_record.config_type, config_record.category, config_record.description,
            config_record.is_system, config_record.is_editable
        );
    END LOOP;
    
    -- Initialize default feature flags
    INSERT INTO tenant_feature_flags (tenant_id, feature_key, is_enabled) VALUES
    (new_tenant_id, 'contacts_management', true),
    (new_tenant_id, 'deals_management', true),
    (new_tenant_id, 'leads_management', true),
    (new_tenant_id, 'accounts_management', true),
    (new_tenant_id, 'activities_management', true),
    (new_tenant_id, 'pipelines_management', true),
    (new_tenant_id, 'custom_objects', false),
    (new_tenant_id, 'advanced_analytics', false),
    (new_tenant_id, 'workflow_automation', false),
    (new_tenant_id, 'api_access', true),
    (new_tenant_id, 'mobile_app', true),
    (new_tenant_id, 'email_integration', false),
    (new_tenant_id, 'calendar_integration', false),
    (new_tenant_id, 'social_media_integration', false);
    
    RETURN new_tenant_id;
END;
$$ LANGUAGE plpgsql;

-- Function to update tenant usage metrics
CREATE OR REPLACE FUNCTION update_tenant_usage_metric(
    tenant_uuid UUID,
    metric_name TEXT,
    metric_value BIGINT,
    metric_date DATE DEFAULT CURRENT_DATE
) RETURNS void AS $$
BEGIN
    INSERT INTO tenant_usage_metrics (tenant_id, metric_name, metric_value, metric_date)
    VALUES (tenant_uuid, metric_name, metric_value, metric_date)
    ON CONFLICT (tenant_id, metric_name, metric_date)
    DO UPDATE SET 
        metric_value = EXCLUDED.metric_value,
        created_at = NOW();
END;
$$ LANGUAGE plpgsql;

-- Function to get tenant feature flags
CREATE OR REPLACE FUNCTION get_tenant_features(tenant_uuid UUID)
RETURNS TABLE (
    feature_key TEXT,
    is_enabled BOOLEAN,
    config_value JSONB,
    expires_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tff.feature_key::TEXT,
        tff.is_enabled,
        tff.config_value,
        tff.expires_at
    FROM tenant_feature_flags tff
    WHERE tff.tenant_id = tenant_uuid
    AND (tff.expires_at IS NULL OR tff.expires_at > NOW())
    ORDER BY tff.feature_key;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 9. MONITORING VIEWS
-- =====================================================

-- View for tenant statistics
CREATE OR REPLACE VIEW tenant_statistics AS
SELECT 
    t.id,
    t.name,
    t.subdomain,
    t.plan_type,
    t.status,
    t.is_trial,
    t.created_at,
    COUNT(DISTINCT tc.id) as config_count,
    COUNT(DISTINCT tff.id) as feature_count,
    COUNT(DISTINCT tff.id) FILTER (WHERE tff.is_enabled = true) as enabled_features_count
FROM tenants t
LEFT JOIN tenant_configurations tc ON t.id = tc.tenant_id
LEFT JOIN tenant_feature_flags tff ON t.id = tff.tenant_id
GROUP BY t.id, t.name, t.subdomain, t.plan_type, t.status, t.is_trial, t.created_at;

-- View for tenant usage summary
CREATE OR REPLACE VIEW tenant_usage_summary AS
SELECT 
    t.id as tenant_id,
    t.name as tenant_name,
    t.plan_type,
    tum.metric_date,
    SUM(CASE WHEN tum.metric_name = 'users_count' THEN tum.metric_value ELSE 0 END) as users_count,
    SUM(CASE WHEN tum.metric_name = 'contacts_count' THEN tum.metric_value ELSE 0 END) as contacts_count,
    SUM(CASE WHEN tum.metric_name = 'deals_count' THEN tum.metric_value ELSE 0 END) as deals_count,
    SUM(CASE WHEN tum.metric_name = 'storage_used_mb' THEN tum.metric_value ELSE 0 END) as storage_used_mb,
    SUM(CASE WHEN tum.metric_name = 'api_calls_count' THEN tum.metric_value ELSE 0 END) as api_calls_count
FROM tenants t
LEFT JOIN tenant_usage_metrics tum ON t.id = tum.tenant_id
WHERE tum.metric_date >= CURRENT_DATE - INTERVAL '30 days' OR tum.metric_date IS NULL
GROUP BY t.id, t.name, t.plan_type, tum.metric_date
ORDER BY t.name, tum.metric_date DESC;

-- =====================================================
-- 10. MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to clean up expired feature flags
CREATE OR REPLACE FUNCTION cleanup_expired_feature_flags()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM tenant_feature_flags 
    WHERE expires_at IS NOT NULL AND expires_at < NOW();
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to archive old usage metrics
CREATE OR REPLACE FUNCTION archive_old_usage_metrics(
    retention_days INTEGER DEFAULT 365
) RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
    cutoff_date DATE;
BEGIN
    cutoff_date := CURRENT_DATE - retention_days;
    
    -- Create archive table if not exists
    CREATE TABLE IF NOT EXISTS tenant_usage_metrics_archive (
        LIKE tenant_usage_metrics INCLUDING ALL
    );
    
    WITH moved_records AS (
        DELETE FROM tenant_usage_metrics 
        WHERE metric_date < cutoff_date
        RETURNING *
    )
    INSERT INTO tenant_usage_metrics_archive 
    SELECT * FROM moved_records;
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

-- Insert default feature flags for existing tenants
INSERT INTO tenant_feature_flags (tenant_id, feature_key, is_enabled)
SELECT 
    t.id,
    feature.key,
    feature.default_enabled
FROM tenants t
CROSS JOIN (
    VALUES 
    ('contacts_management', true),
    ('deals_management', true),
    ('leads_management', true),
    ('accounts_management', true),
    ('activities_management', true),
    ('pipelines_management', true),
    ('custom_objects', false),
    ('advanced_analytics', false),
    ('workflow_automation', false),
    ('api_access', true),
    ('mobile_app', true),
    ('email_integration', false),
    ('calendar_integration', false),
    ('social_media_integration', false)
) AS feature(key, default_enabled)
WHERE NOT EXISTS (
    SELECT 1 FROM tenant_feature_flags tff 
    WHERE tff.tenant_id = t.id AND tff.feature_key = feature.key
);

-- Add comments for documentation
COMMENT ON TABLE tenant_feature_flags IS 'Feature flags configuration per tenant';
COMMENT ON TABLE tenant_usage_metrics IS 'Daily usage metrics tracking per tenant';
COMMENT ON FUNCTION create_tenant_with_defaults(TEXT, TEXT, TEXT, TEXT) IS 'Creates new tenant with default configurations and feature flags';
COMMENT ON FUNCTION update_tenant_usage_metric(UUID, TEXT, BIGINT, DATE) IS 'Updates or inserts tenant usage metrics';
COMMENT ON FUNCTION get_tenant_features(UUID) IS 'Returns active feature flags for a tenant';