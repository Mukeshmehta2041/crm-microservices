-- =====================================================
-- AUTH SERVICE - DEVELOPMENT SEED DATA
-- =====================================================
-- This script inserts seed data for development and testing purposes
-- WARNING: This should only be run in development/test environments

-- Check if we're in a safe environment
DO $$
BEGIN
    IF current_setting('app.environment', true) IN ('production', 'prod') THEN
        RAISE EXCEPTION 'Seed data insertion is not allowed in production environment';
    END IF;
END $$;

-- =====================================================
-- 1. CREATE DEVELOPMENT TENANTS (if not exists)
-- =====================================================

-- Insert development tenant (this would normally come from tenant service)
INSERT INTO users (
    id, username, email, password_hash, tenant_id, status,
    first_name, last_name, phone_number, email_verified,
    created_by, updated_by
) VALUES 
-- System admin user
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'admin',
    'admin@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk', -- password: admin123
    '11111111-1111-1111-1111-111111111111'::UUID, -- dev tenant
    'ACTIVE',
    'System',
    'Administrator',
    '+1-555-0001',
    true,
    '00000000-0000-0000-0000-000000000001'::UUID,
    '00000000-0000-0000-0000-000000000001'::UUID
),
-- Sales manager user
(
    '00000000-0000-0000-0000-000000000002'::UUID,
    'sales.manager',
    'sales.manager@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk', -- password: sales123
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    'Sarah',
    'Johnson',
    '+1-555-0002',
    true,
    '00000000-0000-0000-0000-000000000001'::UUID,
    '00000000-0000-0000-0000-000000000001'::UUID
),
-- Sales rep user
(
    '00000000-0000-0000-0000-000000000003'::UUID,
    'sales.rep',
    'sales.rep@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk', -- password: sales123
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    'Mike',
    'Davis',
    '+1-555-0003',
    true,
    '00000000-0000-0000-0000-000000000001'::UUID,
    '00000000-0000-0000-0000-000000000001'::UUID
),
-- Marketing user
(
    '00000000-0000-0000-0000-000000000004'::UUID,
    'marketing.user',
    'marketing@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk', -- password: marketing123
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    'Lisa',
    'Chen',
    '+1-555-0004',
    true,
    '00000000-0000-0000-0000-000000000001'::UUID,
    '00000000-0000-0000-0000-000000000001'::UUID
),
-- Support user
(
    '00000000-0000-0000-0000-000000000005'::UUID,
    'support.user',
    'support@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk', -- password: support123
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    'David',
    'Wilson',
    '+1-555-0005',
    true,
    '00000000-0000-0000-0000-000000000001'::UUID,
    '00000000-0000-0000-0000-000000000001'::UUID
)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 2. CREATE USER CREDENTIALS
-- =====================================================

INSERT INTO user_credentials (
    user_id, username, email, password_hash, tenant_id, status,
    email_verified, two_factor_enabled, created_at, updated_at
) VALUES 
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'admin',
    'admin@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk',
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    true,
    false,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000000000002'::UUID,
    'sales.manager',
    'sales.manager@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk',
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    true,
    false,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000000000003'::UUID,
    'sales.rep',
    'sales.rep@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk',
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    true,
    false,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000000000004'::UUID,
    'marketing.user',
    'marketing@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk',
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    true,
    false,
    NOW(),
    NOW()
),
(
    '00000000-0000-0000-0000-000000000005'::UUID,
    'support.user',
    'support@crm-platform.dev',
    '$2a$10$N.zmdr9k7uOIW.q8.pklO.z7pklO.z7pklO.z7pklO.z7pklO.z7pk',
    '11111111-1111-1111-1111-111111111111'::UUID,
    'ACTIVE',
    true,
    false,
    NOW(),
    NOW()
)
ON CONFLICT (user_id) DO NOTHING;

-- =====================================================
-- 3. CREATE SAMPLE SECURITY AUDIT LOG ENTRIES
-- =====================================================

INSERT INTO security_audit_log (
    user_id, tenant_id, event_type, event_description, status,
    ip_address, user_agent, timestamp
) VALUES 
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    '11111111-1111-1111-1111-111111111111'::UUID,
    'LOGIN_SUCCESS',
    'Admin user logged in successfully',
    'SUCCESS',
    '127.0.0.1'::INET,
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    NOW() - INTERVAL '1 hour'
),
(
    '00000000-0000-0000-0000-000000000002'::UUID,
    '11111111-1111-1111-1111-111111111111'::UUID,
    'LOGIN_SUCCESS',
    'Sales manager logged in successfully',
    'SUCCESS',
    '192.168.1.100'::INET,
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
    NOW() - INTERVAL '30 minutes'
),
(
    '00000000-0000-0000-0000-000000000003'::UUID,
    '11111111-1111-1111-1111-111111111111'::UUID,
    'LOGIN_FAILED',
    'Failed login attempt - incorrect password',
    'FAILURE',
    '192.168.1.101'::INET,
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    NOW() - INTERVAL '45 minutes'
),
(
    '00000000-0000-0000-0000-000000000003'::UUID,
    '11111111-1111-1111-1111-111111111111'::UUID,
    'LOGIN_SUCCESS',
    'Sales rep logged in successfully after password reset',
    'SUCCESS',
    '192.168.1.101'::INET,
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    NOW() - INTERVAL '40 minutes'
),
(
    '00000000-0000-0000-0000-000000000004'::UUID,
    '11111111-1111-1111-1111-111111111111'::UUID,
    'PASSWORD_CHANGE',
    'Marketing user changed password',
    'SUCCESS',
    '192.168.1.102'::INET,
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
    NOW() - INTERVAL '2 hours'
);

-- =====================================================
-- 4. CREATE SAMPLE USER SESSIONS
-- =====================================================

INSERT INTO user_sessions (
    user_id, token_id, refresh_token, expires_at, refresh_expires_at,
    status, ip_address, user_agent, device_info, last_accessed_at
) VALUES 
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'admin_session_token_001',
    'admin_refresh_token_001',
    NOW() + INTERVAL '8 hours',
    NOW() + INTERVAL '30 days',
    'ACTIVE',
    '127.0.0.1'::INET,
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Windows Desktop',
    NOW() - INTERVAL '5 minutes'
),
(
    '00000000-0000-0000-0000-000000000002'::UUID,
    'sales_manager_session_001',
    'sales_manager_refresh_001',
    NOW() + INTERVAL '8 hours',
    NOW() + INTERVAL '30 days',
    'ACTIVE',
    '192.168.1.100'::INET,
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
    'MacBook Pro',
    NOW() - INTERVAL '10 minutes'
),
(
    '00000000-0000-0000-0000-000000000003'::UUID,
    'sales_rep_session_001',
    'sales_rep_refresh_001',
    NOW() + INTERVAL '8 hours',
    NOW() + INTERVAL '30 days',
    'ACTIVE',
    '192.168.1.101'::INET,
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Windows Laptop',
    NOW() - INTERVAL '15 minutes'
);

-- =====================================================
-- 5. UPDATE USER LAST LOGIN TIMES
-- =====================================================

UPDATE users SET 
    last_login_at = NOW() - INTERVAL '5 minutes',
    updated_at = NOW()
WHERE id = '00000000-0000-0000-0000-000000000001'::UUID;

UPDATE users SET 
    last_login_at = NOW() - INTERVAL '10 minutes',
    updated_at = NOW()
WHERE id = '00000000-0000-0000-0000-000000000002'::UUID;

UPDATE users SET 
    last_login_at = NOW() - INTERVAL '15 minutes',
    updated_at = NOW()
WHERE id = '00000000-0000-0000-0000-000000000003'::UUID;

-- =====================================================
-- 6. LOG SEED DATA COMPLETION
-- =====================================================

INSERT INTO security_audit_log (
    tenant_id, event_type, event_description, status, timestamp
) VALUES (
    '11111111-1111-1111-1111-111111111111'::UUID,
    'SYSTEM_SEED',
    'Development seed data inserted successfully',
    'SUCCESS',
    NOW()
);

-- =====================================================
-- SEED DATA INSERTION COMPLETE
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE 'Development seed data insertion completed for auth service at %', NOW();
    RAISE NOTICE 'Created % users, % credentials, % sessions, % audit entries', 
        (SELECT COUNT(*) FROM users WHERE tenant_id = '11111111-1111-1111-1111-111111111111'::UUID),
        (SELECT COUNT(*) FROM user_credentials WHERE tenant_id = '11111111-1111-1111-1111-111111111111'::UUID),
        (SELECT COUNT(*) FROM user_sessions),
        (SELECT COUNT(*) FROM security_audit_log WHERE tenant_id = '11111111-1111-1111-1111-111111111111'::UUID);
END $$;