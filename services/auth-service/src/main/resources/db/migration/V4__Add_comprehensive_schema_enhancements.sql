-- =====================================================
-- AUTH SERVICE - COMPREHENSIVE SCHEMA ENHANCEMENTS
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
-- 1. CREATE AUDIT LOG TABLE FOR AUTH SERVICE
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

-- Enable RLS on users table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policy for users
CREATE POLICY tenant_isolation_users ON users
    USING (tenant_id = get_current_tenant_id());

-- Enable RLS on user_credentials table
ALTER TABLE user_credentials ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policy for user_credentials
CREATE POLICY tenant_isolation_user_credentials ON user_credentials
    USING (tenant_id = get_current_tenant_id());

-- Enable RLS on user_sessions table
ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;

-- Create policy for user_sessions (users can only see their own sessions)
CREATE POLICY user_sessions_policy ON user_sessions
    USING (user_id = current_setting('app.current_user_id', true)::UUID);

-- Enable RLS on security_audit_log table
ALTER TABLE security_audit_log ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policy for security_audit_log
CREATE POLICY tenant_isolation_security_audit_log ON security_audit_log
    USING (tenant_id = get_current_tenant_id());

-- =====================================================
-- 4. ADD AUDIT TRIGGERS TO EXISTING TABLES
-- =====================================================

-- Add audit trigger to users table
CREATE TRIGGER audit_users_trigger
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Add audit trigger to user_credentials table
CREATE TRIGGER audit_user_credentials_trigger
    AFTER INSERT OR UPDATE OR DELETE ON user_credentials
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Add audit trigger to user_sessions table
CREATE TRIGGER audit_user_sessions_trigger
    AFTER INSERT OR UPDATE OR DELETE ON user_sessions
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 5. ENHANCED INDEXES FOR PERFORMANCE
-- =====================================================

-- Additional composite indexes for users table
CREATE INDEX idx_users_tenant_status_created ON users(tenant_id, status, created_at);
CREATE INDEX idx_users_tenant_email_verified ON users(tenant_id, email_verified, created_at);
CREATE INDEX idx_users_tenant_two_factor ON users(tenant_id, two_factor_enabled);
CREATE INDEX idx_users_failed_attempts ON users(failed_login_attempts) WHERE failed_login_attempts > 0;

-- Additional indexes for user_credentials table
CREATE INDEX idx_user_credentials_tenant_status ON user_credentials(tenant_id, status);
CREATE INDEX idx_user_credentials_password_changed ON user_credentials(password_changed_at);

-- Additional indexes for user_sessions table
CREATE INDEX idx_user_sessions_user_status ON user_sessions(user_id, status);
CREATE INDEX idx_user_sessions_device ON user_sessions(device_info) WHERE device_info IS NOT NULL;
CREATE INDEX idx_user_sessions_last_accessed ON user_sessions(last_accessed_at);

-- Additional indexes for security_audit_log table
CREATE INDEX idx_security_audit_tenant_event_time ON security_audit_log(tenant_id, event_type, timestamp);
CREATE INDEX idx_security_audit_user_event_time ON security_audit_log(user_id, event_type, timestamp);

-- =====================================================
-- 6. VALIDATION FUNCTIONS
-- =====================================================

-- Email validation function
CREATE OR REPLACE FUNCTION is_valid_email(email TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Password strength validation function
CREATE OR REPLACE FUNCTION is_strong_password(password TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Password must be at least 8 characters, contain uppercase, lowercase, number, and special character
    RETURN length(password) >= 8 
        AND password ~ '[A-Z]' 
        AND password ~ '[a-z]' 
        AND password ~ '[0-9]' 
        AND password ~ '[^A-Za-z0-9]';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- 7. PARTITIONING FOR AUDIT LOG TABLE
-- =====================================================

-- Convert audit_log to partitioned table (for future scalability)
-- This will be done when the table grows large

-- Function to create monthly partitions for audit_log
CREATE OR REPLACE FUNCTION create_audit_log_partition(
    start_date DATE
) RETURNS void AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := 'audit_log_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + INTERVAL '1 month';
    
    -- Check if partition already exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = partition_name
    ) THEN
        EXECUTE format('
            CREATE TABLE %I (
                LIKE audit_log INCLUDING ALL,
                CHECK (timestamp >= %L AND timestamp < %L)
            ) INHERITS (audit_log)',
            partition_name, start_date, end_date
        );
        
        -- Create indexes on partition
        EXECUTE format('CREATE INDEX %I ON %I (tenant_id)', 
            partition_name || '_tenant_idx', partition_name);
        EXECUTE format('CREATE INDEX %I ON %I (table_name, record_id)', 
            partition_name || '_table_record_idx', partition_name);
        EXECUTE format('CREATE INDEX %I ON %I (timestamp)', 
            partition_name || '_timestamp_idx', partition_name);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 8. DATABASE ROLES AND PERMISSIONS
-- =====================================================

-- Create database roles for different access levels
DO $$
BEGIN
    -- Admin role with full access
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auth_admin_role') THEN
        CREATE ROLE auth_admin_role;
    END IF;
    
    -- Application role for normal operations
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auth_app_role') THEN
        CREATE ROLE auth_app_role;
    END IF;
    
    -- Read-only role for reporting
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auth_readonly_role') THEN
        CREATE ROLE auth_readonly_role;
    END IF;
END $$;

-- Grant appropriate permissions
GRANT USAGE ON SCHEMA public TO auth_app_role, auth_readonly_role;
GRANT ALL ON SCHEMA public TO auth_admin_role;

-- Grant table permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON users, user_credentials, user_sessions, security_audit_log TO auth_app_role;
GRANT INSERT ON audit_log TO auth_app_role;
GRANT SELECT ON audit_log TO auth_readonly_role;

-- =====================================================
-- 9. MONITORING VIEWS
-- =====================================================

-- View for user login statistics
CREATE OR REPLACE VIEW user_login_stats AS
SELECT 
    u.tenant_id,
    u.id as user_id,
    u.username,
    u.email,
    u.last_login_at,
    u.failed_login_attempts,
    COUNT(s.id) as active_sessions,
    MAX(s.last_accessed_at) as last_session_activity
FROM users u
LEFT JOIN user_sessions s ON u.id = s.user_id AND s.status = 'ACTIVE'
GROUP BY u.tenant_id, u.id, u.username, u.email, u.last_login_at, u.failed_login_attempts;

-- View for security audit summary
CREATE OR REPLACE VIEW security_audit_summary AS
SELECT 
    tenant_id,
    event_type,
    status,
    DATE(timestamp) as audit_date,
    COUNT(*) as event_count,
    COUNT(DISTINCT user_id) as unique_users,
    COUNT(DISTINCT ip_address) as unique_ips
FROM security_audit_log
WHERE timestamp >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY tenant_id, event_type, status, DATE(timestamp)
ORDER BY audit_date DESC, event_count DESC;

-- =====================================================
-- 10. MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to clean up expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_sessions 
    WHERE status = 'EXPIRED' 
    OR (status = 'ACTIVE' AND expires_at < NOW());
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    INSERT INTO security_audit_log (
        tenant_id, event_type, event_description, status, timestamp
    ) VALUES (
        '00000000-0000-0000-0000-000000000000'::UUID,
        'SYSTEM_CLEANUP',
        'Cleaned up ' || deleted_count || ' expired sessions',
        'SUCCESS',
        NOW()
    );
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to archive old audit logs
CREATE OR REPLACE FUNCTION archive_old_audit_logs(
    retention_days INTEGER DEFAULT 365
) RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
    cutoff_date TIMESTAMP;
BEGIN
    cutoff_date := NOW() - (retention_days || ' days')::INTERVAL;
    
    -- Move old records to archive table (create if not exists)
    CREATE TABLE IF NOT EXISTS audit_log_archive (
        LIKE audit_log INCLUDING ALL
    );
    
    WITH moved_records AS (
        DELETE FROM audit_log 
        WHERE timestamp < cutoff_date
        RETURNING *
    )
    INSERT INTO audit_log_archive 
    SELECT * FROM moved_records;
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE audit_log IS 'Comprehensive audit trail for all data changes in auth service';
COMMENT ON FUNCTION audit_trigger_function() IS 'Generic audit trigger function that captures all data changes';
COMMENT ON FUNCTION set_tenant_context(UUID) IS 'Sets tenant context for row-level security';
COMMENT ON FUNCTION cleanup_expired_sessions() IS 'Maintenance function to remove expired user sessions';
COMMENT ON FUNCTION archive_old_audit_logs(INTEGER) IS 'Archives old audit log records to separate table';