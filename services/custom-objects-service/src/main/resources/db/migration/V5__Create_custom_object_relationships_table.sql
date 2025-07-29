-- Create custom_object_relationships table
CREATE TABLE custom_object_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    from_object_id UUID NOT NULL REFERENCES custom_objects(id) ON DELETE CASCADE,
    to_object_id UUID NOT NULL REFERENCES custom_objects(id) ON DELETE CASCADE,
    relationship_name VARCHAR(100) NOT NULL,
    relationship_label VARCHAR(255) NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    cascade_delete BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT unique_relationship_per_objects UNIQUE (tenant_id, from_object_id, to_object_id, relationship_name),
    CONSTRAINT valid_relationship_type CHECK (relationship_type IN (
        'ONE_TO_ONE', 'ONE_TO_MANY', 'MANY_TO_ONE', 'MANY_TO_MANY', 'LOOKUP', 'MASTER_DETAIL'
    )),
    CONSTRAINT no_self_relationship CHECK (from_object_id != to_object_id)
);

-- Create indexes for custom_object_relationships
CREATE INDEX idx_custom_object_relationships_tenant_id ON custom_object_relationships(tenant_id);
CREATE INDEX idx_custom_object_relationships_from_object ON custom_object_relationships(from_object_id);
CREATE INDEX idx_custom_object_relationships_to_object ON custom_object_relationships(to_object_id);
CREATE INDEX idx_custom_object_relationships_tenant_from ON custom_object_relationships(tenant_id, from_object_id);
CREATE INDEX idx_custom_object_relationships_tenant_to ON custom_object_relationships(tenant_id, to_object_id);
CREATE INDEX idx_custom_object_relationships_tenant_active ON custom_object_relationships(tenant_id, is_active);
CREATE INDEX idx_custom_object_relationships_tenant_from_active ON custom_object_relationships(tenant_id, from_object_id, is_active);
CREATE INDEX idx_custom_object_relationships_tenant_to_active ON custom_object_relationships(tenant_id, to_object_id, is_active);
CREATE INDEX idx_custom_object_relationships_relationship_type ON custom_object_relationships(tenant_id, relationship_type, is_active);
CREATE INDEX idx_custom_object_relationships_required ON custom_object_relationships(tenant_id, from_object_id, is_required, is_active);
CREATE INDEX idx_custom_object_relationships_cascade_delete ON custom_object_relationships(tenant_id, from_object_id, cascade_delete, is_active);
CREATE INDEX idx_custom_object_relationships_created_at ON custom_object_relationships(tenant_id, created_at);
CREATE INDEX idx_custom_object_relationships_updated_at ON custom_object_relationships(tenant_id, updated_at);

-- Add comments
COMMENT ON TABLE custom_object_relationships IS 'Relationships between custom objects';
COMMENT ON COLUMN custom_object_relationships.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN custom_object_relationships.from_object_id IS 'Source custom object in the relationship';
COMMENT ON COLUMN custom_object_relationships.to_object_id IS 'Target custom object in the relationship';
COMMENT ON COLUMN custom_object_relationships.relationship_name IS 'Internal name of the relationship';
COMMENT ON COLUMN custom_object_relationships.relationship_label IS 'Display label for the relationship';
COMMENT ON COLUMN custom_object_relationships.relationship_type IS 'Type of relationship (ONE_TO_ONE, ONE_TO_MANY, etc.)';
COMMENT ON COLUMN custom_object_relationships.is_required IS 'Whether the relationship is required';
COMMENT ON COLUMN custom_object_relationships.cascade_delete IS 'Whether to cascade delete related records';
COMMENT ON COLUMN custom_object_relationships.is_active IS 'Whether the relationship is active';
COMMENT ON COLUMN custom_object_relationships.description IS 'Description of the relationship';