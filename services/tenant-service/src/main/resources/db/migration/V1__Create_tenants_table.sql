-- Create tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    plan_type VARCHAR(50) NOT NULL DEFAULT 'BASIC',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_users INTEGER DEFAULT 10,
    max_storage_gb INTEGER DEFAULT 100,
    custom_domain VARCHAR(255),
    logo_url VARCHAR(500),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    billing_email VARCHAR(255),
    subscription_expires_at TIMESTAMP,
    trial_ends_at TIMESTAMP,
    is_trial BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_plan_type CHECK (plan_type IN ('BASIC', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM')),
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'PENDING_ACTIVATION')),
    CONSTRAINT valid_subdomain CHECK (subdomain ~ '^[a-z0-9][a-z0-9-]*[a-z0-9]$'),
    CONSTRAINT valid_primary_color CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT valid_secondary_color CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT valid_max_users CHECK (max_users IS NULL OR max_users > 0),
    CONSTRAINT valid_max_storage CHECK (max_storage_gb IS NULL OR max_storage_gb > 0)
);

-- Create indexes for tenants table
CREATE UNIQUE INDEX idx_tenants_subdomain ON tenants(subdomain);
CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_plan_type ON tenants(plan_type);
CREATE INDEX idx_tenants_custom_domain ON tenants(custom_domain) WHERE custom_domain IS NOT NULL;
CREATE INDEX idx_tenants_subscription_expires ON tenants(subscription_expires_at) WHERE subscription_expires_at IS NOT NULL;
CREATE INDEX idx_tenants_trial_ends ON tenants(trial_ends_at) WHERE trial_ends_at IS NOT NULL;
CREATE INDEX idx_tenants_created_at ON tenants(created_at);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();