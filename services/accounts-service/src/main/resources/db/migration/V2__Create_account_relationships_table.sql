-- Create account relationships table for complex relationship mapping
CREATE TABLE account_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    from_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    to_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    relationship_type VARCHAR(50) NOT NULL,
    description TEXT,
    start_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    end_date TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    strength INTEGER CHECK (strength >= 1 AND strength <= 10),
    custom_fields JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    -- Constraints
    CONSTRAINT valid_relationship_type CHECK (relationship_type IN (
        'PARENT_CHILD', 'SUBSIDIARY', 'PARTNER', 'VENDOR', 'CUSTOMER', 
        'COMPETITOR', 'ALLIANCE', 'JOINT_VENTURE', 'ACQUISITION_TARGET', 
        'MERGER', 'SUPPLIER', 'DISTRIBUTOR', 'RESELLER', 'INTEGRATOR', 
        'CONSULTANT', 'REFERRAL_SOURCE', 'STRATEGIC_PARTNER', 
        'TECHNOLOGY_PARTNER', 'CHANNEL_PARTNER', 'OTHER'
    )),
    CONSTRAINT no_self_relationship CHECK (from_account_id != to_account_id),
    CONSTRAINT valid_date_range CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Create indexes for performance
CREATE INDEX idx_account_relationships_tenant ON account_relationships(tenant_id);
CREATE INDEX idx_account_relationships_from ON account_relationships(from_account_id);
CREATE INDEX idx_account_relationships_to ON account_relationships(to_account_id);
CREATE INDEX idx_account_relationships_type ON account_relationships(relationship_type);
CREATE INDEX idx_account_relationships_active ON account_relationships(tenant_id, is_active);
CREATE INDEX idx_account_relationships_strength ON account_relationships(tenant_id, strength DESC);
CREATE INDEX idx_account_relationships_created_at ON account_relationships(tenant_id, created_at);
CREATE INDEX idx_account_relationships_custom_fields ON account_relationships USING GIN(custom_fields);

-- Create unique constraint to prevent duplicate relationships
CREATE UNIQUE INDEX idx_account_relationships_unique ON account_relationships(
    tenant_id, from_account_id, to_account_id, relationship_type
) WHERE is_active = TRUE;

-- Create composite index for bidirectional relationship queries
CREATE INDEX idx_account_relationships_bidirectional ON account_relationships(
    tenant_id, from_account_id, to_account_id, relationship_type
);

-- Create index for relationship analytics
CREATE INDEX idx_account_relationships_analytics ON account_relationships(
    tenant_id, relationship_type, is_active, strength
);

-- Add comments for documentation
COMMENT ON TABLE account_relationships IS 'Stores complex relationships between accounts';
COMMENT ON COLUMN account_relationships.relationship_type IS 'Type of relationship between accounts';
COMMENT ON COLUMN account_relationships.strength IS 'Relationship strength on scale of 1-10';
COMMENT ON COLUMN account_relationships.is_active IS 'Whether the relationship is currently active';
COMMENT ON COLUMN account_relationships.custom_fields IS 'JSON object storing custom relationship data';