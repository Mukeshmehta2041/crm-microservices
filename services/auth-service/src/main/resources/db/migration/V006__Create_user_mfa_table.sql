CREATE TABLE user_mfa (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    mfa_type VARCHAR(20) NOT NULL,
    secret_key VARCHAR(255),
    backup_codes TEXT[],
    phone_number VARCHAR(50),
    email VARCHAR(255),
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    trusted_device_ids TEXT,
    mfa_metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version BIGINT DEFAULT 1 NOT NULL,
    
    CONSTRAINT user_mfa_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_mfa_tenant_id_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT user_mfa_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT user_mfa_type_check CHECK (mfa_type IN ('totp', 'sms', 'email', 'backup_codes')),
    CONSTRAINT user_mfa_phone_format CHECK (phone_number ~* '^\+[1-9]\d{1,14}$'),
    CONSTRAINT user_mfa_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    UNIQUE(user_id, mfa_type)
);

-- Optimized indexes
CREATE INDEX idx_user_mfa_user_id ON user_mfa(user_id);
CREATE INDEX idx_user_mfa_type ON user_mfa(mfa_type);
CREATE INDEX idx_user_mfa_enabled ON user_mfa(user_id, is_enabled) WHERE is_enabled = true;
CREATE INDEX idx_user_mfa_tenant_id ON user_mfa(tenant_id);