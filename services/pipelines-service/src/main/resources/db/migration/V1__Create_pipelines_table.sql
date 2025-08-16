-- Create pipelines table
CREATE TABLE pipelines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    template_id UUID,
    configuration JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT unique_pipeline_name_per_tenant UNIQUE (tenant_id, name),
    CONSTRAINT unique_default_pipeline_per_tenant UNIQUE (tenant_id, is_default) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_pipelines_template FOREIGN KEY (template_id) REFERENCES pipelines(id) ON DELETE SET NULL
);

-- Create indexes for pipelines
CREATE INDEX idx_pipelines_tenant_id ON pipelines(tenant_id);
CREATE INDEX idx_pipelines_name ON pipelines(tenant_id, name);
CREATE INDEX idx_pipelines_active ON pipelines(tenant_id, is_active);
CREATE INDEX idx_pipelines_display_order ON pipelines(tenant_id, display_order);
CREATE INDEX idx_pipelines_template ON pipelines(template_id);

-- Add comments
COMMENT ON TABLE pipelines IS 'Sales pipelines for organizing deal stages';
COMMENT ON COLUMN pipelines.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN pipelines.name IS 'Pipeline name';
COMMENT ON COLUMN pipelines.description IS 'Pipeline description';
COMMENT ON COLUMN pipelines.is_active IS 'Whether the pipeline is active';
COMMENT ON COLUMN pipelines.is_default IS 'Whether this is the default pipeline for the tenant';
COMMENT ON COLUMN pipelines.display_order IS 'Display order for UI sorting';
COMMENT ON COLUMN pipelines.template_id IS 'Reference to template pipeline for cloning';
COMMENT ON COLUMN pipelines.configuration IS 'JSON configuration for pipeline settings';