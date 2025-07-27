-- Create accounts table with comprehensive fields and hierarchy support
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    account_number VARCHAR(100),
    account_type VARCHAR(50),
    industry VARCHAR(100),
    annual_revenue DECIMAL(15,2),
    employee_count INTEGER,
    website VARCHAR(255),
    phone VARCHAR(50),
    fax VARCHAR(50),
    billing_address JSONB,
    shipping_address JSONB,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    tags TEXT[],
    custom_fields JSONB DEFAULT '{}',
    
    -- Hierarchy support
    parent_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    hierarchy_level INTEGER DEFAULT 0,
    hierarchy_path TEXT,
    
    -- Territory management
    territory_id UUID,
    
    -- Ownership and audit fields
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    -- Constraints
    CONSTRAINT valid_account_type CHECK (account_type IN (
        'PROSPECT', 'CUSTOMER', 'PARTNER', 'VENDOR', 'COMPETITOR', 
        'RESELLER', 'INTEGRATOR', 'INVESTOR', 'OTHER'
    )),
    CONSTRAINT valid_status CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'PROSPECT', 'CUSTOMER', 'FORMER_CUSTOMER', 
        'SUSPENDED', 'ARCHIVED'
    )),
    CONSTRAINT valid_annual_revenue CHECK (annual_revenue IS NULL OR annual_revenue >= 0),
    CONSTRAINT valid_employee_count CHECK (employee_count IS NULL OR employee_count >= 0),
    CONSTRAINT valid_hierarchy_level CHECK (hierarchy_level >= 0 AND hierarchy_level <= 10),
    CONSTRAINT account_name_not_empty CHECK (TRIM(name) != ''),
    CONSTRAINT no_self_parent CHECK (id != parent_account_id)
);

-- Create indexes for performance
CREATE INDEX idx_accounts_tenant_id ON accounts(tenant_id);
CREATE INDEX idx_accounts_parent_id ON accounts(parent_account_id);
CREATE INDEX idx_accounts_name ON accounts(tenant_id, name);
CREATE INDEX idx_accounts_owner ON accounts(owner_id);
CREATE INDEX idx_accounts_type ON accounts(tenant_id, account_type);
CREATE INDEX idx_accounts_status ON accounts(tenant_id, status);
CREATE INDEX idx_accounts_industry ON accounts(tenant_id, industry);
CREATE INDEX idx_accounts_territory ON accounts(territory_id);
CREATE INDEX idx_accounts_hierarchy_level ON accounts(tenant_id, hierarchy_level);
CREATE INDEX idx_accounts_hierarchy_path ON accounts(hierarchy_path);
CREATE INDEX idx_accounts_created_at ON accounts(tenant_id, created_at);
CREATE INDEX idx_accounts_updated_at ON accounts(tenant_id, updated_at);
CREATE INDEX idx_accounts_tags ON accounts USING GIN(tags);
CREATE INDEX idx_accounts_custom_fields ON accounts USING GIN(custom_fields);

-- Create unique constraint for account number within tenant
CREATE UNIQUE INDEX idx_accounts_tenant_account_number ON accounts(tenant_id, account_number) 
WHERE account_number IS NOT NULL;

-- Create partial index for active accounts
CREATE INDEX idx_accounts_active ON accounts(tenant_id, name) WHERE status = 'ACTIVE';

-- Create composite index for common search patterns
CREATE INDEX idx_accounts_search ON accounts(tenant_id, account_type, status, industry);

-- Add comments for documentation
COMMENT ON TABLE accounts IS 'Stores account/organization information with hierarchy support';
COMMENT ON COLUMN accounts.hierarchy_level IS 'Depth level in account hierarchy (0 = root)';
COMMENT ON COLUMN accounts.hierarchy_path IS 'Full path from root to this account (UUID/UUID/...)';
COMMENT ON COLUMN accounts.custom_fields IS 'JSON object storing custom field values';
COMMENT ON COLUMN accounts.tags IS 'Array of string tags for categorization';
COMMENT ON COLUMN accounts.billing_address IS 'JSON object with billing address details';
COMMENT ON COLUMN accounts.shipping_address IS 'JSON object with shipping address details';