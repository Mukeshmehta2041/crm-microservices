-- Create custom_object_records table
CREATE TABLE custom_object_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    custom_object_id UUID NOT NULL REFERENCES custom_objects(id) ON DELETE CASCADE,
    record_name VARCHAR(255),
    field_values JSONB DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    owner_id UUID NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for custom_object_records
CREATE INDEX idx_custom_object_records_tenant_id ON custom_object_records(tenant_id);
CREATE INDEX idx_custom_object_records_custom_object_id ON custom_object_records(custom_object_id);
CREATE INDEX idx_custom_object_records_tenant_object ON custom_object_records(tenant_id, custom_object_id);
CREATE INDEX idx_custom_object_records_tenant_active ON custom_object_records(tenant_id, is_active);
CREATE INDEX idx_custom_object_records_tenant_object_active ON custom_object_records(tenant_id, custom_object_id, is_active);
CREATE INDEX idx_custom_object_records_owner_id ON custom_object_records(owner_id);
CREATE INDEX idx_custom_object_records_tenant_owner ON custom_object_records(tenant_id, owner_id);
CREATE INDEX idx_custom_object_records_tenant_owner_active ON custom_object_records(tenant_id, owner_id, is_active);
CREATE INDEX idx_custom_object_records_record_name ON custom_object_records(tenant_id, custom_object_id, record_name);
CREATE INDEX idx_custom_object_records_created_at ON custom_object_records(tenant_id, custom_object_id, created_at);
CREATE INDEX idx_custom_object_records_updated_at ON custom_object_records(tenant_id, custom_object_id, updated_at);

-- Create GIN index for JSONB field_values for efficient querying
CREATE INDEX idx_custom_object_records_field_values ON custom_object_records USING GIN(field_values);

-- Create partial indexes for common queries
CREATE INDEX idx_custom_object_records_active_by_object ON custom_object_records(tenant_id, custom_object_id, created_at DESC) 
    WHERE is_active = TRUE;
CREATE INDEX idx_custom_object_records_active_by_owner ON custom_object_records(tenant_id, owner_id, created_at DESC) 
    WHERE is_active = TRUE;

-- Add comments
COMMENT ON TABLE custom_object_records IS 'Records/instances of custom objects with dynamic field values';
COMMENT ON COLUMN custom_object_records.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN custom_object_records.custom_object_id IS 'Reference to the custom object definition';
COMMENT ON COLUMN custom_object_records.record_name IS 'Display name for the record';
COMMENT ON COLUMN custom_object_records.field_values IS 'JSON object containing all field values for this record';
COMMENT ON COLUMN custom_object_records.is_active IS 'Whether the record is active (soft delete)';
COMMENT ON COLUMN custom_object_records.owner_id IS 'User who owns this record';
COMMENT ON COLUMN custom_object_records.created_by IS 'User who created this record';
COMMENT ON COLUMN custom_object_records.updated_by IS 'User who last updated this record';