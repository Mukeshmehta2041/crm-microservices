-- =====================================================
-- AUTH SERVICE - ADD MISSING TABLES AND SYNC ENTITIES
-- =====================================================

-- =====================================================
-- 1. CREATE EMAIL VERIFICATION TOKENS TABLE
-- =====================================================

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_type VARCHAR(20) NOT NULL DEFAULT 'REGISTRATION',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    
    CONSTRAINT valid_verification_type CHECK (verification_type IN ('REGISTRATION', 'EMAIL_CHANGE', 'PASSWORD_RESET_VERIFICATION'))
);

-- Indexes for email_verification_tokens
CREATE INDEX idx_email_verification_token ON email_verification_tokens(token);
CREATE INDEX idx_email_verification_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_email ON email_verification_tokens(email);
CREATE INDEX idx_email_verification_expires_at ON email_verification_tokens(expires_at);
CREATE INDEX idx_email_verification_type_user ON email_verification_tokens(verification_type, user_id);

-- =====================================================
-- 2. CREATE PASSWORD RESET TOKENS TABLE
-- =====================================================

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3
);

-- Indexes for password_reset_tokens
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_used ON password_reset_tokens(is_used);

-- =====================================================
-- 3. CREATE OAUTH2 CLIENTS TABLE
-- =====================================================

CREATE TABLE oauth2_clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret VARCHAR(255) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    redirect_uris TEXT NOT NULL, -- JSON array
    scopes TEXT NOT NULL, -- JSON array
    grant_types TEXT NOT NULL, -- JSON array
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_description TEXT,
    logo_url VARCHAR(500),
    website_url VARCHAR(500),
    privacy_policy_url VARCHAR(500),
    terms_of_service_url VARCHAR(500)
);

-- Indexes for oauth2_clients
CREATE INDEX idx_oauth2_clients_client_id ON oauth2_clients(client_id);
CREATE INDEX idx_oauth2_clients_tenant_id ON oauth2_clients(tenant_id);
CREATE INDEX idx_oauth2_clients_active ON oauth2_clients(is_active);

-- =====================================================
-- 4. CREATE OAUTH2 AUTHORIZATION CODES TABLE
-- =====================================================

CREATE TABLE oauth2_authorization_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(255) NOT NULL UNIQUE,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    scope VARCHAR(500),
    state VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    code_challenge VARCHAR(255),
    code_challenge_method VARCHAR(10)
);

-- Indexes for oauth2_authorization_codes
CREATE INDEX idx_oauth2_auth_codes_code ON oauth2_authorization_codes(code);
CREATE INDEX idx_oauth2_auth_codes_client_user ON oauth2_authorization_codes(client_id, user_id);
CREATE INDEX idx_oauth2_auth_codes_expires_at ON oauth2_authorization_codes(expires_at);
CREATE INDEX idx_oauth2_auth_codes_used ON oauth2_authorization_codes(is_used);

-- =====================================================
-- 5. CREATE OAUTH2 ACCESS TOKENS TABLE
-- =====================================================

CREATE TABLE oauth2_access_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    access_token VARCHAR(255) NOT NULL UNIQUE,
    refresh_token VARCHAR(255) UNIQUE,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID,
    tenant_id UUID NOT NULL,
    scope VARCHAR(500),
    expires_at TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP,
    grant_type VARCHAR(50) NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP
);

-- Indexes for oauth2_access_tokens
CREATE INDEX idx_oauth2_access_tokens_token ON oauth2_access_tokens(access_token);
CREATE INDEX idx_oauth2_access_tokens_refresh ON oauth2_access_tokens(refresh_token);
CREATE INDEX idx_oauth2_access_tokens_client ON oauth2_access_tokens(client_id);
CREATE INDEX idx_oauth2_access_tokens_user ON oauth2_access_tokens(user_id);
CREATE INDEX idx_oauth2_access_tokens_expires_at ON oauth2_access_tokens(expires_at);
CREATE INDEX idx_oauth2_access_tokens_revoked ON oauth2_access_tokens(is_revoked);

-- =====================================================
-- 6. CREATE OAUTH2 ACCOUNT LINKS TABLE
-- =====================================================

CREATE TABLE oauth2_account_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    provider_display_name VARCHAR(255),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_provider_user UNIQUE (provider, provider_user_id)
);

-- Indexes for oauth2_account_links
CREATE INDEX idx_oauth2_links_user_id ON oauth2_account_links(user_id);
CREATE INDEX idx_oauth2_links_provider ON oauth2_account_links(provider);
CREATE INDEX idx_oauth2_links_provider_user ON oauth2_account_links(provider, provider_user_id);
CREATE INDEX idx_oauth2_links_active ON oauth2_account_links(is_active);

-- =====================================================
-- 7. CREATE TOKEN BLACKLIST TABLE
-- =====================================================

CREATE TABLE token_blacklist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID,
    token_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255),
    
    CONSTRAINT valid_token_type CHECK (token_type IN ('ACCESS', 'REFRESH', 'MFA'))
);

-- Indexes for token_blacklist
CREATE INDEX idx_token_blacklist_token_id ON token_blacklist(token_id);
CREATE INDEX idx_token_blacklist_user_id ON token_blacklist(user_id);
CREATE INDEX idx_token_blacklist_expires_at ON token_blacklist(expires_at);
CREATE INDEX idx_token_blacklist_type ON token_blacklist(token_type);

-- =====================================================
-- 8. UPDATE USER_CREDENTIALS TABLE TO MATCH JPA ENTITY
-- =====================================================

-- Add missing columns to user_credentials table
ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(255);
ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS backup_codes TEXT;
ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS mfa_method VARCHAR(20);
ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS trusted_devices TEXT;

-- Update column names to match JPA entity
ALTER TABLE user_credentials RENAME COLUMN two_factor_enabled TO mfa_enabled_old;
ALTER TABLE user_credentials RENAME COLUMN two_factor_secret TO mfa_secret_old;

-- Migrate data from old columns to new ones
UPDATE user_credentials SET 
    mfa_enabled = COALESCE(mfa_enabled_old, FALSE),
    mfa_secret = mfa_secret_old
WHERE mfa_enabled_old IS NOT NULL OR mfa_secret_old IS NOT NULL;

-- Drop old columns
ALTER TABLE user_credentials DROP COLUMN IF EXISTS mfa_enabled_old;
ALTER TABLE user_credentials DROP COLUMN IF EXISTS mfa_secret_old;

-- Add constraint for mfa_method
ALTER TABLE user_credentials ADD CONSTRAINT valid_mfa_method 
    CHECK (mfa_method IS NULL OR mfa_method IN ('TOTP', 'SMS', 'EMAIL'));

-- =====================================================
-- 9. CREATE UPDATED_AT TRIGGERS FOR NEW TABLES
-- =====================================================

-- Trigger for oauth2_clients
CREATE TRIGGER update_oauth2_clients_updated_at BEFORE UPDATE ON oauth2_clients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger for oauth2_account_links
CREATE TRIGGER update_oauth2_account_links_updated_at BEFORE UPDATE ON oauth2_account_links
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 10. ADD AUDIT TRIGGERS TO NEW TABLES
-- =====================================================

-- Add audit triggers to new tables
CREATE TRIGGER audit_email_verification_tokens_trigger
    AFTER INSERT OR UPDATE OR DELETE ON email_verification_tokens
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_password_reset_tokens_trigger
    AFTER INSERT OR UPDATE OR DELETE ON password_reset_tokens
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_oauth2_clients_trigger
    AFTER INSERT OR UPDATE OR DELETE ON oauth2_clients
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_oauth2_authorization_codes_trigger
    AFTER INSERT OR UPDATE OR DELETE ON oauth2_authorization_codes
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_oauth2_access_tokens_trigger
    AFTER INSERT OR UPDATE OR DELETE ON oauth2_access_tokens
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_oauth2_account_links_trigger
    AFTER INSERT OR UPDATE OR DELETE ON oauth2_account_links
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_token_blacklist_trigger
    AFTER INSERT OR UPDATE OR DELETE ON token_blacklist
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 11. ADD ROW-LEVEL SECURITY TO NEW TABLES
-- =====================================================

-- Enable RLS on new tables
ALTER TABLE email_verification_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_reset_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE oauth2_clients ENABLE ROW LEVEL SECURITY;
ALTER TABLE oauth2_authorization_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE oauth2_access_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE oauth2_account_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE token_blacklist ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policies for tables with tenant_id
CREATE POLICY tenant_isolation_oauth2_clients ON oauth2_clients
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_oauth2_authorization_codes ON oauth2_authorization_codes
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_oauth2_access_tokens ON oauth2_access_tokens
    USING (tenant_id = get_current_tenant_id());

-- Create user-specific policies for user-related tables
CREATE POLICY user_isolation_email_verification_tokens ON email_verification_tokens
    USING (user_id = current_setting('app.current_user_id', true)::UUID);

CREATE POLICY user_isolation_password_reset_tokens ON password_reset_tokens
    USING (user_id = current_setting('app.current_user_id', true)::UUID);

CREATE POLICY user_isolation_oauth2_account_links ON oauth2_account_links
    USING (user_id = current_setting('app.current_user_id', true)::UUID);

CREATE POLICY user_isolation_token_blacklist ON token_blacklist
    USING (user_id = current_setting('app.current_user_id', true)::UUID);

-- =====================================================
-- 12. CREATE CLEANUP FUNCTIONS FOR NEW TABLES
-- =====================================================

-- Function to clean up expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $
DECLARE
    deleted_count INTEGER := 0;
    temp_count INTEGER;
BEGIN
    -- Clean up expired email verification tokens
    DELETE FROM email_verification_tokens 
    WHERE expires_at < NOW() AND is_verified = FALSE;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean up expired password reset tokens
    DELETE FROM password_reset_tokens 
    WHERE expires_at < NOW() AND is_used = FALSE;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean up expired authorization codes
    DELETE FROM oauth2_authorization_codes 
    WHERE expires_at < NOW() AND is_used = FALSE;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean up expired access tokens
    DELETE FROM oauth2_access_tokens 
    WHERE expires_at < NOW() AND is_revoked = FALSE;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean up expired blacklisted tokens
    DELETE FROM token_blacklist 
    WHERE expires_at < NOW();
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Log cleanup activity
    INSERT INTO security_audit_log (
        tenant_id, event_type, event_description, status, timestamp
    ) VALUES (
        '00000000-0000-0000-0000-000000000000'::UUID,
        'SYSTEM_CLEANUP',
        'Cleaned up ' || deleted_count || ' expired tokens',
        'SUCCESS',
        NOW()
    );
    
    RETURN deleted_count;
END;
$ LANGUAGE plpgsql;

-- =====================================================
-- 13. ADD COMMENTS FOR DOCUMENTATION
-- =====================================================

COMMENT ON TABLE email_verification_tokens IS 'Tokens for email verification during registration and email changes';
COMMENT ON TABLE password_reset_tokens IS 'Tokens for password reset functionality';
COMMENT ON TABLE oauth2_clients IS 'OAuth2 client applications registered in the system';
COMMENT ON TABLE oauth2_authorization_codes IS 'OAuth2 authorization codes for the authorization code flow';
COMMENT ON TABLE oauth2_access_tokens IS 'OAuth2 access and refresh tokens';
COMMENT ON TABLE oauth2_account_links IS 'Links between users and their OAuth2 provider accounts';
COMMENT ON TABLE token_blacklist IS 'Blacklisted tokens that should not be accepted';

COMMENT ON FUNCTION cleanup_expired_tokens() IS 'Maintenance function to clean up expired tokens across all token tables';