CREATE TABLE oauth_access_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    scopes TEXT,
    token_type VARCHAR(20) DEFAULT 'BEARER',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    usage_count INTEGER DEFAULT 0,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version BIGINT DEFAULT 1 NOT NULL,
    
    CONSTRAINT oauth_access_tokens_client_id_fk FOREIGN KEY (client_id) REFERENCES oauth_applications(client_id),
    CONSTRAINT oauth_access_tokens_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT oauth_access_tokens_tenant_id_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT oauth_access_tokens_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT oauth_access_tokens_type_check CHECK (token_type IN ('Bearer', 'MAC')),
    CONSTRAINT oauth_access_tokens_usage_count_positive CHECK (usage_count >= 0),
    UNIQUE(token_hash)
);

CREATE INDEX idx_oauth_access_tokens_tenant_id ON oauth_access_tokens(tenant_id);
CREATE INDEX idx_oauth_access_tokens_client_id ON oauth_access_tokens(client_id);
CREATE INDEX idx_oauth_access_tokens_user_id ON oauth_access_tokens(user_id);
CREATE INDEX idx_oauth_access_tokens_expires_at ON oauth_access_tokens(expires_at);
CREATE INDEX idx_oauth_access_tokens_token_hash ON oauth_access_tokens(token_hash);