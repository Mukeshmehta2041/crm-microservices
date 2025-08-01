-- Create user_sessions table
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_id VARCHAR(255) UNIQUE NOT NULL,
    refresh_token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_info VARCHAR(255),
    last_accessed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_session_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED', 'LOGGED_OUT')),
    CONSTRAINT valid_expiry_dates CHECK (refresh_expires_at > expires_at)
);

-- Create indexes for user_sessions table
CREATE UNIQUE INDEX idx_user_sessions_token_id ON user_sessions(token_id);
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_user_sessions_refresh_expires_at ON user_sessions(refresh_expires_at);
CREATE INDEX idx_user_sessions_status ON user_sessions(status);
CREATE INDEX idx_user_sessions_refresh_token ON user_sessions(refresh_token);
CREATE INDEX idx_user_sessions_ip_address ON user_sessions(ip_address);
CREATE INDEX idx_user_sessions_created_at ON user_sessions(created_at);