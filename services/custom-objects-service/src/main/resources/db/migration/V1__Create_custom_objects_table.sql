-- Create custom_objects table
CREATE TABLE custom_objects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    plural_label VARCHAR(255) NOT NULL,
    description TEXT,
    api_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    allow_reports BOOLEAN NOT NULL DEFAULT TRUE,
    allow_activities BOOLEAN NOT NULL DEFAULT TRUE,
    record_name_field VARCHAR(100),
    icon VARCHAR(50),
    color VARCHAR(7),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT unique_custom_object_name UNIQUE (tenant_id, name),
    CONSTRAINT unique_custom_object_api_name UNIQUE (tenant_id, api_name),
    CONSTRAINT valid_api_name CHECK (api_name ~ '^[a-z][a-z0-9_]*[a-z0-9]$'),
    CONSTRAINT valid_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Create indexes for custom_objects
CREATE INDEX idx_custom_objects_tenant_id ON custom_objects(tenant_id);
CREATE INDEX idx_custom_objects_tenant_active ON custom_objects(tenant_id, is_active);
CREATE INDEX idx_custom_objects_tenant_name ON custom_objects(tenant_id, name);
CREATE INDEX idx_custom_objects_tenant_api_name ON custom_objects(tenant_id, api_name);
CREATE INDEX idx_custom_objects_created_at ON custom_objects(tenant_id, created_at);
CREATE INDEX idx_custom_objects_updated_at ON custom_objects(tenant_id, updated_at);
CREATE INDEX idx_custom_objects_allow_reports ON custom_objects(tenant_id, allow_reports, is_active);
CREATE INDEX idx_custom_objects_allow_activities ON custom_objects(tenant_id, allow_activities, is_active);

-- Add comments
COMMENT ON TABLE custom_objects IS 'User-defined custom objects for extending CRM functionality';
COMMENT ON COLUMN custom_objects.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN custom_objects.name IS 'Internal name of the custom object';
COMMENT ON COLUMN custom_objects.label IS 'Display label for the custom object';
COMMENT ON COLUMN custom_objects.plural_label IS 'Plural display label for the custom object';
COMMENT ON COLUMN custom_objects.api_name IS 'API-friendly name for the custom object';
COMMENT ON COLUMN custom_objects.is_active IS 'Whether the custom object is active';
COMMENT ON COLUMN custom_objects.allow_reports IS 'Whether reports can be created for this object';
COMMENT ON COLUMN custom_objects.allow_activities IS 'Whether activities can be associated with this object';
COMMENT ON COLUMN custom_objects.record_name_field IS 'Field name to use as the record name';
COMMENT ON COLUMN custom_objects.icon IS 'Icon identifier for UI display';
COMMENT ON COLUMN custom_objects.color IS 'Color code for UI display';