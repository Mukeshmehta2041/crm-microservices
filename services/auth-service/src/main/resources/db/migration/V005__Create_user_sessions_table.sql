CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    ip_address INET NOT NULL,
    user_agent TEXT,
    device_info JSONB,
    location_info JSONB,
    is_active BOOLEAN DEFAULT true,
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version BIGINT DEFAULT 1 NOT NULL,
    
    CONSTRAINT user_sessions_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_sessions_tenant_id_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT user_sessions_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Optimized indexes
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at) WHERE is_active = true;
CREATE INDEX idx_user_sessions_last_activity_at ON user_sessions(last_activity_at) WHERE is_active = true;
CREATE INDEX idx_user_sessions_tenant_active ON user_sessions(tenant_id, is_active);
