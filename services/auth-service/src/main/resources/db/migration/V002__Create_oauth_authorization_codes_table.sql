CREATE TABLE oauth_authorization_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    scopes TEXT,
    code_challenge VARCHAR(255),
    code_challenge_method VARCHAR(10),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version BIGINT DEFAULT 1 NOT NULL,
    
    CONSTRAINT oauth_authorization_codes_client_id_fk FOREIGN KEY (client_id) REFERENCES oauth_applications(client_id),
    CONSTRAINT oauth_authorization_codes_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT oauth_authorization_codes_tenant_id_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT oauth_authorization_codes_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT oauth_authorization_codes_challenge_method_check CHECK (code_challenge_method IN ('plain', 'S256')),
    UNIQUE(code)
);

CREATE INDEX idx_oauth_authorization_codes_tenant_id ON oauth_authorization_codes(tenant_id);
CREATE INDEX idx_oauth_authorization_codes_client_id ON oauth_authorization_codes(client_id);
CREATE INDEX idx_oauth_authorization_codes_user_id ON oauth_authorization_codes(user_id);
CREATE INDEX idx_oauth_authorization_codes_expires_at ON oauth_authorization_codes(expires_at);