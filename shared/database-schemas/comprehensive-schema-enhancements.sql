-- =====================================================
-- COMPREHENSIVE DATABASE SCHEMA ENHANCEMENTS
-- CRM Microservices Platform
-- =====================================================
-- This file contains comprehensive database schema enhancements
-- for multi-tenant data isolation, audit trails, and performance optimization

-- =====================================================
-- 1. MULTI-TENANT ROW-LEVEL SECURITY (RLS) SETUP
-- =====================================================

-- Enable RLS on all tenant-specific tables across services
-- This will be applied via individual service migrations

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
-- 2. COMPREHENSIVE AUDIT TRAIL FRAMEWORK
-- =====================================================

-- Generic audit log table structure (to be created in each service database)
-- This serves as a template for service-specific audit tables

/*
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
*/

-- Generic audit trigger function
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
    user_id_val := current_setting('app.current_user_id', true)::UUID;
    session_id_val := current_setting('app.current_session_id', true);
    ip_address_val := current_setting('app.current_ip_address', true)::INET;
    user_agent_val := current_setting('app.current_user_agent', true);
    correlation_id_val := current_setting('app.correlation_id', true);
    
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
-- 3. DATABASE PARTITIONING STRATEGIES
-- =====================================================

-- Partitioning strategy for large tables
-- This will be implemented per service based on data volume and access patterns

-- Time-based partitioning for audit logs (monthly partitions)
/*
-- Example for audit_log table partitioning
CREATE TABLE audit_log_template (
    LIKE audit_log INCLUDING ALL
) PARTITION BY RANGE (timestamp);

-- Function to create monthly partitions
CREATE OR REPLACE FUNCTION create_monthly_partition(
    table_name TEXT,
    start_date DATE
) RETURNS void AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + INTERVAL '1 month';
    
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I PARTITION OF %I
        FOR VALUES FROM (%L) TO (%L)',
        partition_name, table_name, start_date, end_date
    );
    
    -- Create indexes on partition
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (tenant_id)', 
        partition_name || '_tenant_idx', partition_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (table_name, record_id)', 
        partition_name || '_table_record_idx', partition_name);
END;
$$ LANGUAGE plpgsql;
*/

-- =====================================================
-- 4. PERFORMANCE OPTIMIZATION INDEXES
-- =====================================================

-- Additional composite indexes for common query patterns
-- These will be added to existing tables via service-specific migrations

-- Common index patterns for multi-tenant applications:
-- 1. (tenant_id, frequently_queried_column)
-- 2. (tenant_id, status, created_at) for filtered lists
-- 3. (tenant_id, owner_id, status) for user-specific queries
-- 4. GIN indexes for JSONB custom_fields
-- 5. Partial indexes for active records only

-- =====================================================
-- 5. DATA VALIDATION AND CONSTRAINTS
-- =====================================================

-- Enhanced validation functions for common data types

-- Email validation function
CREATE OR REPLACE FUNCTION is_valid_email(email TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Phone number validation function
CREATE OR REPLACE FUNCTION is_valid_phone(phone TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Basic phone validation (can be enhanced based on requirements)
    RETURN phone ~ '^\+?[1-9]\d{1,14}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- URL validation function
CREATE OR REPLACE FUNCTION is_valid_url(url TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN url ~ '^https?://[^\s/$.?#].[^\s]*$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- JSON schema validation function for custom fields
CREATE OR REPLACE FUNCTION validate_custom_fields(
    fields JSONB,
    schema JSONB
) RETURNS BOOLEAN AS $$
DECLARE
    field_key TEXT;
    field_value JSONB;
    field_schema JSONB;
    field_type TEXT;
    is_required BOOLEAN;
BEGIN
    -- Validate each field against its schema
    FOR field_key IN SELECT jsonb_object_keys(schema) LOOP
        field_schema := schema->field_key;
        field_type := field_schema->>'type';
        is_required := COALESCE((field_schema->>'required')::BOOLEAN, false);
        
        -- Check if required field is present
        IF is_required AND NOT fields ? field_key THEN
            RETURN false;
        END IF;
        
        -- Validate field type if present
        IF fields ? field_key THEN
            field_value := fields->field_key;
            
            CASE field_type
                WHEN 'string' THEN
                    IF jsonb_typeof(field_value) != 'string' THEN
                        RETURN false;
                    END IF;
                WHEN 'number' THEN
                    IF jsonb_typeof(field_value) NOT IN ('number') THEN
                        RETURN false;
                    END IF;
                WHEN 'boolean' THEN
                    IF jsonb_typeof(field_value) != 'boolean' THEN
                        RETURN false;
                    END IF;
                WHEN 'array' THEN
                    IF jsonb_typeof(field_value) != 'array' THEN
                        RETURN false;
                    END IF;
                ELSE
                    -- Unknown type, skip validation
                    NULL;
            END CASE;
        END IF;
    END LOOP;
    
    RETURN true;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- 6. UTILITY FUNCTIONS FOR SCHEMA MANAGEMENT
-- =====================================================

-- Function to create standard indexes for a tenant-aware table
CREATE OR REPLACE FUNCTION create_standard_tenant_indexes(
    table_name TEXT,
    additional_columns TEXT[] DEFAULT ARRAY[]::TEXT[]
) RETURNS void AS $$
DECLARE
    col TEXT;
BEGIN
    -- Standard tenant index
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_tenant_id ON %I (tenant_id)', 
        table_name, table_name);
    
    -- Tenant + created_at index
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_tenant_created ON %I (tenant_id, created_at)', 
        table_name, table_name);
    
    -- Tenant + updated_at index
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_tenant_updated ON %I (tenant_id, updated_at)', 
        table_name, table_name);
    
    -- Additional column indexes
    FOREACH col IN ARRAY additional_columns LOOP
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_tenant_%I ON %I (tenant_id, %I)', 
            table_name, col, table_name, col);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function to enable RLS and create tenant isolation policy
CREATE OR REPLACE FUNCTION enable_tenant_rls(
    table_name TEXT
) RETURNS void AS $$
BEGIN
    -- Enable RLS
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
    
    -- Create tenant isolation policy
    EXECUTE format('
        CREATE POLICY tenant_isolation_%I ON %I
        USING (tenant_id = get_current_tenant_id())',
        table_name, table_name
    );
    
    -- Create policy for system/admin access (bypass RLS)
    EXECUTE format('
        CREATE POLICY admin_access_%I ON %I
        TO admin_role
        USING (true)',
        table_name, table_name
    );
END;
$$ LANGUAGE plpgsql;

-- Function to add audit trigger to a table
CREATE OR REPLACE FUNCTION add_audit_trigger(
    table_name TEXT
) RETURNS void AS $$
BEGIN
    EXECUTE format('
        CREATE TRIGGER audit_%I_trigger
        AFTER INSERT OR UPDATE OR DELETE ON %I
        FOR EACH ROW EXECUTE FUNCTION audit_trigger_function()',
        table_name, table_name
    );
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. DATABASE ROLES AND PERMISSIONS
-- =====================================================

-- Create database roles for different access levels
DO $$
BEGIN
    -- Admin role with full access
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'admin_role') THEN
        CREATE ROLE admin_role;
    END IF;
    
    -- Application role for normal operations
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_role') THEN
        CREATE ROLE app_role;
    END IF;
    
    -- Read-only role for reporting
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'readonly_role') THEN
        CREATE ROLE readonly_role;
    END IF;
    
    -- Audit role for audit log access
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'audit_role') THEN
        CREATE ROLE audit_role;
    END IF;
END $$;

-- Grant appropriate permissions
GRANT USAGE ON SCHEMA public TO app_role, readonly_role, audit_role;
GRANT ALL ON SCHEMA public TO admin_role;

-- =====================================================
-- 8. MONITORING AND MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to get table statistics
CREATE OR REPLACE FUNCTION get_table_stats(
    schema_name TEXT DEFAULT 'public'
) RETURNS TABLE (
    table_name TEXT,
    row_count BIGINT,
    table_size TEXT,
    index_size TEXT,
    total_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        t.table_name::TEXT,
        (SELECT reltuples::BIGINT FROM pg_class WHERE relname = t.table_name) as row_count,
        pg_size_pretty(pg_total_relation_size(quote_ident(schema_name)||'.'||quote_ident(t.table_name))) as table_size,
        pg_size_pretty(pg_indexes_size(quote_ident(schema_name)||'.'||quote_ident(t.table_name))) as index_size,
        pg_size_pretty(pg_total_relation_size(quote_ident(schema_name)||'.'||quote_ident(t.table_name))) as total_size
    FROM information_schema.tables t
    WHERE t.table_schema = schema_name
    AND t.table_type = 'BASE TABLE'
    ORDER BY pg_total_relation_size(quote_ident(schema_name)||'.'||quote_ident(t.table_name)) DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to analyze query performance
CREATE OR REPLACE FUNCTION get_slow_queries(
    min_duration INTERVAL DEFAULT '1 second'
) RETURNS TABLE (
    query TEXT,
    calls BIGINT,
    total_time DOUBLE PRECISION,
    mean_time DOUBLE PRECISION,
    rows BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pg_stat_statements.query,
        pg_stat_statements.calls,
        pg_stat_statements.total_exec_time,
        pg_stat_statements.mean_exec_time,
        pg_stat_statements.rows
    FROM pg_stat_statements
    WHERE pg_stat_statements.mean_exec_time > EXTRACT(EPOCH FROM min_duration) * 1000
    ORDER BY pg_stat_statements.mean_exec_time DESC
    LIMIT 20;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- END OF COMPREHENSIVE SCHEMA ENHANCEMENTS
-- =====================================================