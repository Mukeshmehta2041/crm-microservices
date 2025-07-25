-- TODO: Remove this table after testing
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(255) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version BIGINT DEFAULT 1 NOT NULL
);

-- TODO: Remove this table after testing
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version BIGINT DEFAULT 1 NOT NULL,
    
    CONSTRAINT users_tenant_id_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT users_username_tenant_unique UNIQUE(username, tenant_id),
    CONSTRAINT users_email_tenant_unique UNIQUE(email, tenant_id)
);

CREATE TABLE oauth_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    application_type VARCHAR(50) NOT NULL,
    redirect_uris TEXT,
    scopes TEXT,
    is_trusted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT true,
    logo_url VARCHAR(500),
    website_url VARCHAR(500),
    privacy_policy_url VARCHAR(500),
    terms_of_service_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version BIGINT DEFAULT 1 NOT NULL,
    
    CONSTRAINT oauth_applications_tenant_id_fk FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT oauth_applications_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT oauth_applications_type_check CHECK (application_type IN ('web', 'native', 'service'))
);

-- Indexes for performance
CREATE INDEX idx_oauth_applications_tenant_id ON oauth_applications(tenant_id);
CREATE INDEX idx_oauth_applications_client_id ON oauth_applications(client_id);
CREATE INDEX idx_oauth_applications_is_active ON oauth_applications(is_active);
CREATE INDEX idx_oauth_applications_created_by ON oauth_applications(created_by);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_tenants_domain ON tenants(domain);
CREATE INDEX idx_tenants_is_active ON tenants(is_active);