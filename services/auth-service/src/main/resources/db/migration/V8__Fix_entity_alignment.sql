-- =====================================================
-- AUTH SERVICE - FIX ENTITY ALIGNMENT
-- =====================================================
-- This migration fixes misalignments between entities and database schema

-- =====================================================
-- 1. FIX USER_SESSIONS TABLE TO MATCH ENTITY
-- =====================================================

-- Add missing columns to user_sessions table
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS device_type VARCHAR(50);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS browser VARCHAR(100);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS operating_system VARCHAR(100);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS location VARCHAR(255);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(500);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Update tenant_id from user_credentials where missing
UPDATE user_sessions SET tenant_id = uc.tenant_id 
FROM user_credentials uc 
WHERE user_sessions.user_id = uc.user_id AND user_sessions.tenant_id IS NULL;

-- Rename device_info to device_type if it exists
DO $
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_sessions' AND column_name = 'device_info') THEN
        UPDATE user_sessions SET device_type = device_info WHERE device_type IS NULL;
        ALTER TABLE user_sessions DROP COLUMN device_info;
    END IF;
END $;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_user_sessions_device_type ON user_sessions(device_type);
CREATE INDEX IF NOT EXISTS idx_user_sessions_browser ON user_sessions(browser);
CREATE INDEX IF NOT EXISTS idx_user_sessions_tenant_id ON user_sessions(tenant_id);

-- =====================================================
-- 2. FIX OAUTH2_CLIENTS TABLE TO MATCH ENTITY
-- =====================================================

-- Add missing columns to oauth2_clients table
ALTER TABLE oauth2_clients ADD COLUMN IF NOT EXISTS access_token_validity_seconds INTEGER DEFAULT 3600;
ALTER TABLE oauth2_clients ADD COLUMN IF NOT EXISTS refresh_token_validity_seconds INTEGER DEFAULT 2592000;
ALTER TABLE oauth2_clients ADD COLUMN IF NOT EXISTS auto_approve BOOLEAN DEFAULT FALSE;

-- Update existing records with default values
UPDATE oauth2_clients SET 
    access_token_validity_seconds = 3600 WHERE access_token_validity_seconds IS NULL,
    refresh_token_validity_seconds = 2592000 WHERE refresh_token_validity_seconds IS NULL,
    auto_approve = FALSE WHERE auto_approve IS NULL;

-- =====================================================
-- 3. CREATE OAUTH2 CLIENT COLLECTION TABLES
-- =====================================================

-- Create oauth2_client_redirect_uris table
CREATE TABLE IF NOT EXISTS oauth2_client_redirect_uris (
    oauth2_client_id UUID NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    PRIMARY KEY (oauth2_client_id, redirect_uri),
    CONSTRAINT fk_oauth2_client_redirect_uris_client 
        FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_clients(id) ON DELETE CASCADE
);

-- Create oauth2_client_scopes table
CREATE TABLE IF NOT EXISTS oauth2_client_scopes (
    oauth2_client_id UUID NOT NULL,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (oauth2_client_id, scope),
    CONSTRAINT fk_oauth2_client_scopes_client 
        FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_clients(id) ON DELETE CASCADE
);

-- Create oauth2_client_grant_types table
CREATE TABLE IF NOT EXISTS oauth2_client_grant_types (
    oauth2_client_id UUID NOT NULL,
    grant_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (oauth2_client_id, grant_type),
    CONSTRAINT fk_oauth2_client_grant_types_client 
        FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_clients(id) ON DELETE CASCADE,
    CONSTRAINT valid_oauth2_grant_type 
        CHECK (grant_type IN ('AUTHORIZATION_CODE', 'CLIENT_CREDENTIALS', 'REFRESH_TOKEN', 'IMPLICIT', 'PASSWORD'))
);

-- =====================================================
-- 4. FIX TOKEN_BLACKLIST TABLE TO MATCH ENTITY
-- =====================================================

-- Rename token_id to jti if it exists
DO $
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'token_blacklist' AND column_name = 'token_id') THEN
        ALTER TABLE token_blacklist RENAME COLUMN token_id TO jti;
    END IF;
END $;

-- Add missing columns to token_blacklist table
ALTER TABLE token_blacklist ADD COLUMN IF NOT EXISTS revoked_by UUID;

-- Update column names to match entity
DO $
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'token_blacklist' AND column_name = 'blacklisted_at') THEN
        ALTER TABLE token_blacklist RENAME COLUMN blacklisted_at TO created_at;
    END IF;
END $;

-- Add constraint for token_type
ALTER TABLE token_blacklist ADD CONSTRAINT IF NOT EXISTS valid_blacklist_token_type 
    CHECK (token_type IN ('ACCESS', 'REFRESH', 'OAUTH2_ACCESS', 'OAUTH2_REFRESH'));

-- =====================================================
-- 5. CREATE PASSWORD_HISTORY TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) REFERENCES user_credentials(user_id) ON DELETE CASCADE
);

-- Add indexes for password_history
CREATE INDEX IF NOT EXISTS idx_password_history_user_id ON password_history(user_id);
CREATE INDEX IF NOT EXISTS idx_password_history_created_at ON password_history(created_at);

-- =====================================================
-- 6. FIX OAUTH2_ACCESS_TOKENS TABLE TO MATCH ENTITY
-- =====================================================

-- Add missing columns to oauth2_access_tokens table
ALTER TABLE oauth2_access_tokens ADD COLUMN IF NOT EXISTS token_type VARCHAR(20) DEFAULT 'BEARER';
ALTER TABLE oauth2_access_tokens ADD COLUMN IF NOT EXISTS revoked BOOLEAN DEFAULT FALSE;
ALTER TABLE oauth2_access_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;

-- Rename is_revoked to revoked if it exists
DO $
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'oauth2_access_tokens' AND column_name = 'is_revoked') THEN
        UPDATE oauth2_access_tokens SET revoked = is_revoked WHERE revoked IS NULL;
        ALTER TABLE oauth2_access_tokens DROP COLUMN is_revoked;
    END IF;
END $;

-- Add constraint for token_type
ALTER TABLE oauth2_access_tokens ADD CONSTRAINT IF NOT EXISTS valid_oauth2_token_type 
    CHECK (token_type IN ('BEARER', 'MAC'));

-- =====================================================
-- 7. FIX OAUTH2_AUTHORIZATION_CODES TABLE TO MATCH ENTITY
-- =====================================================

-- Rename is_used to used if it exists
DO $
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'oauth2_authorization_codes' AND column_name = 'is_used') THEN
        UPDATE oauth2_authorization_codes SET used = is_used WHERE used IS NULL;
        ALTER TABLE oauth2_authorization_codes DROP COLUMN is_used;
    END IF;
END $;

-- =====================================================
-- 8. ADD MISSING INDEXES
-- =====================================================

-- Add indexes for password_reset_tokens
CREATE INDEX IF NOT EXISTS idx_password_reset_attempts ON password_reset_tokens(attempts);
CREATE INDEX IF NOT EXISTS idx_password_reset_used ON password_reset_tokens(is_used);

-- Add indexes for email_verification_tokens
CREATE INDEX IF NOT EXISTS idx_email_verification_type_user ON email_verification_tokens(verification_type, user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_verified ON email_verification_tokens(is_verified);

-- Add indexes for oauth2_access_tokens
CREATE INDEX IF NOT EXISTS idx_oauth2_access_tokens_revoked ON oauth2_access_tokens(revoked);
CREATE INDEX IF NOT EXISTS idx_oauth2_access_tokens_grant_type ON oauth2_access_tokens(grant_type);

-- Add indexes for oauth2_authorization_codes
CREATE INDEX IF NOT EXISTS idx_oauth2_auth_codes_used ON oauth2_authorization_codes(used);

-- =====================================================
-- 9. UPDATE ROW-LEVEL SECURITY POLICIES
-- =====================================================

-- Enable RLS on password_history table
ALTER TABLE password_history ENABLE ROW LEVEL SECURITY;

-- Create user isolation policy for password_history
CREATE POLICY IF NOT EXISTS user_isolation_password_history ON password_history
    USING (user_id = current_setting('app.current_user_id', true)::UUID);

-- =====================================================
-- 10. ADD AUDIT TRIGGERS TO NEW TABLES
-- =====================================================

-- Add audit trigger to password_history table
CREATE TRIGGER IF NOT EXISTS audit_password_history_trigger
    AFTER INSERT OR UPDATE OR DELETE ON password_history
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 11. CLEANUP FUNCTIONS UPDATE
-- =====================================================

-- Update cleanup function to handle new tables
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
    WHERE expires_at < NOW() AND used = FALSE;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean up expired access tokens
    DELETE FROM oauth2_access_tokens 
    WHERE expires_at < NOW() AND revoked = FALSE;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean up expired blacklisted tokens
    DELETE FROM token_blacklist 
    WHERE expires_at < NOW();
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean up expired user sessions
    DELETE FROM user_sessions 
    WHERE status = 'EXPIRED' 
    OR (status = 'ACTIVE' AND expires_at < NOW());
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Log cleanup activity
    INSERT INTO security_audit_log (
        tenant_id, event_type, event_description, status, timestamp
    ) VALUES (
        '00000000-0000-0000-0000-000000000000'::UUID,
        'SYSTEM_CLEANUP',
        'Cleaned up ' || deleted_count || ' expired tokens and sessions',
        'SUCCESS',
        NOW()
    );
    
    RETURN deleted_count;
END;
$ LANGUAGE plpgsql;

-- =====================================================
-- 12. COMMENTS FOR DOCUMENTATION
-- =====================================================

COMMENT ON TABLE password_history IS 'Password history for preventing password reuse';
COMMENT ON TABLE oauth2_client_redirect_uris IS 'OAuth2 client redirect URIs collection';
COMMENT ON TABLE oauth2_client_scopes IS 'OAuth2 client scopes collection';
COMMENT ON TABLE oauth2_client_grant_types IS 'OAuth2 client grant types collection';

-- =====================================================
-- 13. DATA MIGRATION FOR EXISTING RECORDS
-- =====================================================

-- Migrate existing redirect_uris from JSON to collection table
DO $
DECLARE
    client_record RECORD;
    uri_array TEXT[];
    uri TEXT;
BEGIN
    FOR client_record IN SELECT id, client_id, redirect_uris FROM oauth2_clients WHERE redirect_uris IS NOT NULL LOOP
        -- Parse JSON array (assuming it's stored as JSON text)
        BEGIN
            SELECT array_agg(value::text) INTO uri_array 
            FROM json_array_elements_text(client_record.redirect_uris::json);
            
            FOREACH uri IN ARRAY uri_array LOOP
                INSERT INTO oauth2_client_redirect_uris (oauth2_client_id, redirect_uri) 
                VALUES (client_record.id, uri) 
                ON CONFLICT DO NOTHING;
            END LOOP;
        EXCEPTION
            WHEN OTHERS THEN
                -- If JSON parsing fails, treat as single URI
                INSERT INTO oauth2_client_redirect_uris (oauth2_client_id, redirect_uri) 
                VALUES (client_record.id, client_record.redirect_uris) 
                ON CONFLICT DO NOTHING;
        END;
    END LOOP;
END $;

-- Migrate existing scopes from JSON to collection table
DO $
DECLARE
    client_record RECORD;
    scope_array TEXT[];
    scope TEXT;
BEGIN
    FOR client_record IN SELECT id, client_id, scopes FROM oauth2_clients WHERE scopes IS NOT NULL LOOP
        -- Parse JSON array (assuming it's stored as JSON text)
        BEGIN
            SELECT array_agg(value::text) INTO scope_array 
            FROM json_array_elements_text(client_record.scopes::json);
            
            FOREACH scope IN ARRAY scope_array LOOP
                INSERT INTO oauth2_client_scopes (oauth2_client_id, scope) 
                VALUES (client_record.id, scope) 
                ON CONFLICT DO NOTHING;
            END LOOP;
        EXCEPTION
            WHEN OTHERS THEN
                -- If JSON parsing fails, treat as single scope
                INSERT INTO oauth2_client_scopes (oauth2_client_id, scope) 
                VALUES (client_record.id, client_record.scopes) 
                ON CONFLICT DO NOTHING;
        END;
    END LOOP;
END $;

-- Migrate existing grant_types from JSON to collection table
DO $
DECLARE
    client_record RECORD;
    grant_array TEXT[];
    grant_type TEXT;
BEGIN
    FOR client_record IN SELECT id, client_id, grant_types FROM oauth2_clients WHERE grant_types IS NOT NULL LOOP
        -- Parse JSON array (assuming it's stored as JSON text)
        BEGIN
            SELECT array_agg(value::text) INTO grant_array 
            FROM json_array_elements_text(client_record.grant_types::json);
            
            FOREACH grant_type IN ARRAY grant_array LOOP
                INSERT INTO oauth2_client_grant_types (oauth2_client_id, grant_type) 
                VALUES (client_record.id, grant_type) 
                ON CONFLICT DO NOTHING;
            END LOOP;
        EXCEPTION
            WHEN OTHERS THEN
                -- If JSON parsing fails, add default grant type
                INSERT INTO oauth2_client_grant_types (oauth2_client_id, grant_type) 
                VALUES (client_record.id, 'AUTHORIZATION_CODE') 
                ON CONFLICT DO NOTHING;
        END;
    END LOOP;
END $;