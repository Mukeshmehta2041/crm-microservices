-- Create user_credentials table
CREATE TABLE user_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    tenant_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    password_changed_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked_until TIMESTAMP,
    email_verified BOOLEAN DEFAULT FALSE,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_user_credentials_username ON user_credentials(username);
CREATE INDEX idx_user_credentials_email ON user_credentials(email);
CREATE INDEX idx_user_credentials_tenant_id ON user_credentials(tenant_id);
CREATE INDEX idx_user_credentials_user_id ON user_credentials(user_id);

-- Create updated_at trigger for user_credentials
CREATE TRIGGER update_user_credentials_updated_at BEFORE UPDATE ON user_credentials
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Migrate existing users data to user_credentials
INSERT INTO user_credentials (
    user_id, username, email, password_hash, tenant_id, status,
    last_login_at, password_changed_at, failed_login_attempts,
    account_locked_until, email_verified, two_factor_enabled,
    two_factor_secret, created_at, updated_at
)
SELECT 
    id as user_id, username, email, password_hash, tenant_id, status,
    last_login_at, password_changed_at, failed_login_attempts,
    account_locked_until, email_verified, two_factor_enabled,
    two_factor_secret, created_at, updated_at
FROM users;

-- Update user_sessions to reference user_credentials
-- Note: This assumes user_sessions.user_id should remain as reference to the actual user
-- If you want to reference credentials instead, you would need to update this