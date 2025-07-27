-- Create contact relationships table for managing contact-to-contact relationships
CREATE TABLE contact_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    related_contact_id UUID NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_relationship_type CHECK (relationship_type IN (
        'COLLEAGUE', 'MANAGER', 'SUBORDINATE', 'PARTNER', 'VENDOR', 
        'CUSTOMER', 'FRIEND', 'FAMILY', 'REFERRAL', 'INFLUENCER', 
        'DECISION_MAKER', 'GATEKEEPER', 'OTHER'
    )),
    CONSTRAINT no_self_relationship CHECK (contact_id != related_contact_id),
    CONSTRAINT unique_relationship UNIQUE (tenant_id, contact_id, related_contact_id, relationship_type)
);

-- Create indexes for relationship queries
CREATE INDEX idx_contact_relationships_contact ON contact_relationships(contact_id);
CREATE INDEX idx_contact_relationships_related ON contact_relationships(related_contact_id);
CREATE INDEX idx_contact_relationships_type ON contact_relationships(relationship_type);
CREATE INDEX idx_contact_relationships_tenant ON contact_relationships(tenant_id);
CREATE INDEX idx_contact_relationships_primary ON contact_relationships(tenant_id, is_primary) WHERE is_primary = TRUE;

-- Add foreign key constraints (assuming contacts table exists)
ALTER TABLE contact_relationships 
ADD CONSTRAINT fk_contact_relationships_contact 
FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE;

ALTER TABLE contact_relationships 
ADD CONSTRAINT fk_contact_relationships_related_contact 
FOREIGN KEY (related_contact_id) REFERENCES contacts(id) ON DELETE CASCADE;

-- Add comments
COMMENT ON TABLE contact_relationships IS 'Contact-to-contact relationship mapping table';
COMMENT ON COLUMN contact_relationships.relationship_type IS 'Type of relationship between contacts';
COMMENT ON COLUMN contact_relationships.is_primary IS 'Indicates if this is the primary relationship of this type';
COMMENT ON COLUMN contact_relationships.description IS 'Optional description of the relationship';