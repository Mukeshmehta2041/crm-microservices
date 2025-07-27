-- Create contacts table with comprehensive fields and indexing
CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    account_id UUID,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    title VARCHAR(100),
    department VARCHAR(100),
    mailing_address JSONB,
    social_profiles JSONB DEFAULT '{}',
    lead_source VARCHAR(100),
    contact_status VARCHAR(50) DEFAULT 'ACTIVE',
    lead_score INTEGER DEFAULT 0,
    do_not_call BOOLEAN DEFAULT FALSE,
    do_not_email BOOLEAN DEFAULT FALSE,
    email_opt_out BOOLEAN DEFAULT FALSE,
    preferred_contact_method VARCHAR(20) DEFAULT 'EMAIL',
    timezone VARCHAR(50),
    language VARCHAR(10) DEFAULT 'en-US',
    tags TEXT[],
    notes TEXT,
    custom_fields JSONB DEFAULT '{}',
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_contact_status CHECK (contact_status IN ('ACTIVE', 'INACTIVE', 'DECEASED')),
    CONSTRAINT valid_email CHECK (email IS NULL OR email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT valid_lead_score CHECK (lead_score >= 0 AND lead_score <= 100),
    CONSTRAINT contact_name_not_empty CHECK (TRIM(first_name) != '' AND TRIM(last_name) != ''),
    CONSTRAINT valid_preferred_contact_method CHECK (preferred_contact_method IN ('EMAIL', 'PHONE', 'MOBILE', 'MAIL'))
);

-- Create indexes for optimal query performance
CREATE UNIQUE INDEX idx_contacts_email_tenant ON contacts(tenant_id, email) WHERE email IS NOT NULL;
CREATE INDEX idx_contacts_tenant_id ON contacts(tenant_id);
CREATE INDEX idx_contacts_account_id ON contacts(account_id);
CREATE INDEX idx_contacts_name ON contacts(tenant_id, last_name, first_name);
CREATE INDEX idx_contacts_owner ON contacts(owner_id);
CREATE INDEX idx_contacts_status ON contacts(tenant_id, contact_status);
CREATE INDEX idx_contacts_lead_score ON contacts(tenant_id, lead_score DESC);
CREATE INDEX idx_contacts_tags ON contacts USING GIN(tags);
CREATE INDEX idx_contacts_custom_fields ON contacts USING GIN(custom_fields);
CREATE INDEX idx_contacts_created_at ON contacts(tenant_id, created_at);
CREATE INDEX idx_contacts_updated_at ON contacts(tenant_id, updated_at);
CREATE INDEX idx_contacts_lead_source ON contacts(tenant_id, lead_source);
CREATE INDEX idx_contacts_phone ON contacts(tenant_id, phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_contacts_mobile ON contacts(tenant_id, mobile) WHERE mobile IS NOT NULL;

-- Create partial indexes for common queries
CREATE INDEX idx_contacts_active ON contacts(tenant_id, created_at) WHERE contact_status = 'ACTIVE';
CREATE INDEX idx_contacts_high_score ON contacts(tenant_id, lead_score DESC) WHERE lead_score >= 70;

-- Add comments for documentation
COMMENT ON TABLE contacts IS 'Contact management table with comprehensive contact information and multi-tenant support';
COMMENT ON COLUMN contacts.tenant_id IS 'Tenant identifier for multi-tenant data isolation';
COMMENT ON COLUMN contacts.account_id IS 'Reference to associated account/company';
COMMENT ON COLUMN contacts.lead_score IS 'Lead scoring value from 0-100';
COMMENT ON COLUMN contacts.custom_fields IS 'JSONB field for tenant-specific custom fields';
COMMENT ON COLUMN contacts.tags IS 'Array of tags for categorization and filtering';
COMMENT ON COLUMN contacts.social_profiles IS 'JSONB field for social media profile links';
COMMENT ON COLUMN contacts.mailing_address IS 'JSONB field for structured address information';