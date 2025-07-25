CREATE TABLE oauth_refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    access_token_id UUID NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    scopes VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version BIGINT DEFAULT 1 NOT NULL,
    
    CONSTRAINT oauth_refresh_tokens_access_token_id_fk FOREIGN KEY (access_token_id) REFERENCES oauth_access_tokens(id) ON DELETE CASCADE,
    CONSTRAINT oauth_refresh_tokens_client_id_fk FOREIGN KEY (client_id) REFERENCES oauth_applications(client_id),
    CONSTRAINT oauth_refresh_tokens_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT oauth_refresh_tokens_tenant_id_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT oauth_refresh_tokens_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE(token_hash)
);

CREATE INDEX idx_oauth_refresh_tokens_tenant_id ON oauth_refresh_tokens(tenant_id);
CREATE INDEX idx_oauth_refresh_tokens_access_token_id ON oauth_refresh_tokens(access_token_id);
CREATE INDEX idx_oauth_refresh_tokens_user_id ON oauth_refresh_tokens(user_id);
CREATE INDEX idx_oauth_refresh_tokens_expires_at ON oauth_refresh_tokens(expires_at);