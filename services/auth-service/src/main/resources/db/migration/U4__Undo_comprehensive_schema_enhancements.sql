-- =====================================================
-- AUTH SERVICE - UNDO COMPREHENSIVE SCHEMA ENHANCEMENTS
-- =====================================================
-- This script undoes the comprehensive schema enhancements
-- applied in V4__Add_comprehensive_schema_enhancements.sql

-- =====================================================
-- 1. REMOVE AUDIT TRIGGERS
-- =====================================================

-- Drop audit triggers from existing tables
DROP TRIGGER IF EXISTS audit_users_trigger ON users;
DROP TRIGGER IF EXISTS audit_user_credentials_trigger ON user_credentials;
DROP TRIGGER IF EXISTS audit_user_sessions_trigger ON user_sessions;

-- =====================================================
-- 2. DISABLE ROW-LEVEL SECURITY
-- =====================================================

-- Drop RLS policies
DROP POLICY IF EXISTS tenant_isolation_users ON users;
DROP POLICY IF EXISTS tenant_isolation_user_credentials ON user_credentials;
DROP POLICY IF EXISTS user_sessions_policy ON user_sessions;
DROP POLICY IF EXISTS tenant_isolation_security_audit_log ON security_audit_log;

-- Disable RLS on tables
ALTER TABLE users DISABLE ROW LEVEL SECURITY;
ALTER TABLE user_credentials DISABLE ROW LEVEL SECURITY;
ALTER TABLE user_sessions DISABLE ROW LEVEL SECURITY;
ALTER TABLE security_audit_log DISABLE ROW LEVEL SECURITY;

-- =====================================================
-- 3. DROP ENHANCED INDEXES
-- =====================================================

-- Drop additional composite indexes for users table
DROP INDEX IF EXISTS idx_users_tenant_status_created;
DROP INDEX IF EXISTS idx_users_tenant_email_verified;
DROP INDEX IF EXISTS idx_users_tenant_two_factor;
DROP INDEX IF EXISTS idx_users_failed_attempts;

-- Drop additional indexes for user_credentials table
DROP INDEX IF EXISTS idx_user_credentials_tenant_status;
DROP INDEX IF EXISTS idx_user_credentials_password_changed;

-- Drop additional indexes for user_sessions table
DROP INDEX IF EXISTS idx_user_sessions_user_status;
DROP INDEX IF EXISTS idx_user_sessions_device;
DROP INDEX IF EXISTS idx_user_sessions_last_accessed;

-- Drop additional indexes for security_audit_log table
DROP INDEX IF EXISTS idx_security_audit_tenant_event_time;
DROP INDEX IF EXISTS idx_security_audit_user_event_time;

-- =====================================================
-- 4. DROP VALIDATION FUNCTIONS
-- =====================================================

-- Drop validation functions
DROP FUNCTION IF EXISTS is_valid_email(TEXT);
DROP FUNCTION IF EXISTS is_strong_password(TEXT);

-- =====================================================
-- 5. DROP PARTITIONING FUNCTIONS
-- =====================================================

-- Drop partitioning functions
DROP FUNCTION IF EXISTS create_audit_log_partition(DATE);

-- =====================================================
-- 6. DROP DATABASE ROLES
-- =====================================================

-- Drop database roles (be careful in production)
-- DROP ROLE IF EXISTS auth_admin_role;
-- DROP ROLE IF EXISTS auth_app_role;
-- DROP ROLE IF EXISTS auth_readonly_role;

-- =====================================================
-- 7. DROP MONITORING VIEWS
-- =====================================================

-- Drop monitoring views
DROP VIEW IF EXISTS user_login_stats;
DROP VIEW IF EXISTS security_audit_summary;

-- =====================================================
-- 8. DROP MAINTENANCE FUNCTIONS
-- =====================================================

-- Drop maintenance functions
DROP FUNCTION IF EXISTS cleanup_expired_sessions();
DROP FUNCTION IF EXISTS archive_old_audit_logs(INTEGER);

-- =====================================================
-- 9. DROP AUDIT TRIGGER FUNCTION
-- =====================================================

-- Drop audit trigger function
DROP FUNCTION IF EXISTS audit_trigger_function();

-- =====================================================
-- 10. DROP TENANT CONTEXT FUNCTIONS
-- =====================================================

-- Drop tenant context functions
DROP FUNCTION IF EXISTS set_tenant_context(UUID);
DROP FUNCTION IF EXISTS get_current_tenant_id();

-- =====================================================
-- 11. DROP AUDIT LOG TABLE
-- =====================================================

-- Drop audit log table (WARNING: This will lose all audit data)
-- Uncomment only if you're sure you want to lose audit history
-- DROP TABLE IF EXISTS audit_log;

-- =====================================================
-- 12. REMOVE COMMENTS
-- =====================================================

-- Remove table and function comments
COMMENT ON TABLE audit_log IS NULL;
COMMENT ON FUNCTION audit_trigger_function() IS NULL;
COMMENT ON FUNCTION set_tenant_context(UUID) IS NULL;
COMMENT ON FUNCTION cleanup_expired_sessions() IS NULL;
COMMENT ON FUNCTION archive_old_audit_logs(INTEGER) IS NULL;

-- =====================================================
-- ROLLBACK COMPLETE
-- =====================================================

-- Log the rollback completion
DO $$
BEGIN
    RAISE NOTICE 'Comprehensive schema enhancements rollback completed for auth service at %', NOW();
END $$;