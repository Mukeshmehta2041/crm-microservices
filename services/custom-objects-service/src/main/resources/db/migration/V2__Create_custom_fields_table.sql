-- Create custom_fields table
CREATE TABLE custom_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    object_type VARCHAR(100) NOT NULL,
    custom_object_id UUID REFERENCES custom_objects(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    field_label VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_unique BOOLEAN NOT NULL DEFAULT FALSE,
    is_indexed BOOLEAN NOT NULL DEFAULT FALSE,
    default_value TEXT,
    help_text TEXT,
    field_order INTEGER NOT NULL DEFAULT 0,
    validation_rules JSONB DEFAULT '{}',
    display_options JSONB DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT unique_field_per_object UNIQUE (tenant_id, object_type, field_name),
    CONSTRAINT unique_field_per_custom_object UNIQUE (tenant_id, custom_object_id, field_name),
    CONSTRAINT valid_field_type CHECK (field_type IN (
        'TEXT', 'TEXTAREA', 'NUMBER', 'DECIMAL', 'CURRENCY', 'PERCENT',
        'DATE', 'DATETIME', 'BOOLEAN', 'PICKLIST', 'MULTIPICKLIST',
        'EMAIL', 'PHONE', 'URL', 'LOOKUP', 'MASTER_DETAIL'
    )),
    CONSTRAINT valid_data_type CHECK (data_type IN (
        'VARCHAR', 'TEXT', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 
        'TIMESTAMP', 'JSONB', 'UUID'
    ))
);

-- Create indexes for custom_fields
CREATE INDEX idx_custom_fields_tenant_id ON custom_fields(tenant_id);
CREATE INDEX idx_custom_fields_tenant_object_type ON custom_fields(tenant_id, object_type);
CREATE INDEX idx_custom_fields_custom_object_id ON custom_fields(custom_object_id);
CREATE INDEX idx_custom_fields_tenant_active ON custom_fields(tenant_id, is_active);
CREATE INDEX idx_custom_fields_tenant_object_active ON custom_fields(tenant_id, object_type, is_active);
CREATE INDEX idx_custom_fields_field_order ON custom_fields(tenant_id, object_type, field_order);
CREATE INDEX idx_custom_fields_required ON custom_fields(tenant_id, object_type, is_required, is_active);
CREATE INDEX idx_custom_fields_unique ON custom_fields(tenant_id, object_type, is_unique, is_active);
CREATE INDEX idx_custom_fields_indexed ON custom_fields(tenant_id, object_type, is_indexed, is_active);
CREATE INDEX idx_custom_fields_field_type ON custom_fields(tenant_id, field_type, is_active);
CREATE INDEX idx_custom_fields_created_at ON custom_fields(tenant_id, created_at);
CREATE INDEX idx_custom_fields_updated_at ON custom_fields(tenant_id, updated_at);

-- Create GIN indexes for JSONB columns
CREATE INDEX idx_custom_fields_validation_rules ON custom_fields USING GIN(validation_rules);
CREATE INDEX idx_custom_fields_display_options ON custom_fields USING GIN(display_options);

-- Add comments
COMMENT ON TABLE custom_fields IS 'Dynamic fields for custom objects and standard objects';
COMMENT ON COLUMN custom_fields.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN custom_fields.object_type IS 'Type of object this field belongs to (custom object name or standard object)';
COMMENT ON COLUMN custom_fields.custom_object_id IS 'Reference to custom object if this is a custom object field';
COMMENT ON COLUMN custom_fields.field_name IS 'Internal name of the field';
COMMENT ON COLUMN custom_fields.field_label IS 'Display label for the field';
COMMENT ON COLUMN custom_fields.field_type IS 'UI field type (TEXT, NUMBER, PICKLIST, etc.)';
COMMENT ON COLUMN custom_fields.data_type IS 'Database data type for storage';
COMMENT ON COLUMN custom_fields.is_required IS 'Whether the field is required';
COMMENT ON COLUMN custom_fields.is_unique IS 'Whether the field must have unique values';
COMMENT ON COLUMN custom_fields.is_indexed IS 'Whether the field should be indexed for performance';
COMMENT ON COLUMN custom_fields.default_value IS 'Default value for the field';
COMMENT ON COLUMN custom_fields.help_text IS 'Help text displayed to users';
COMMENT ON COLUMN custom_fields.field_order IS 'Display order of the field';
COMMENT ON COLUMN custom_fields.validation_rules IS 'JSON object containing validation rules';
COMMENT ON COLUMN custom_fields.display_options IS 'JSON object containing display options';