-- Create pipeline_stages table
CREATE TABLE pipeline_stages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    default_probability DECIMAL(5,2) CHECK (default_probability >= 0 AND default_probability <= 100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    is_won BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL,
    color VARCHAR(7) CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    automation_rules JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT fk_pipeline_stages_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines(id) ON DELETE CASCADE,
    CONSTRAINT unique_stage_name_per_pipeline UNIQUE (pipeline_id, name),
    CONSTRAINT unique_stage_order_per_pipeline UNIQUE (pipeline_id, display_order),
    CONSTRAINT valid_won_stage CHECK (NOT is_won OR is_closed),
    CONSTRAINT valid_display_order CHECK (display_order >= 0)
);

-- Create indexes for pipeline_stages
CREATE INDEX idx_pipeline_stages_pipeline ON pipeline_stages(pipeline_id);
CREATE INDEX idx_pipeline_stages_order ON pipeline_stages(pipeline_id, display_order);
CREATE INDEX idx_pipeline_stages_active ON pipeline_stages(pipeline_id, is_active);
CREATE INDEX idx_pipeline_stages_closed ON pipeline_stages(pipeline_id, is_closed);
CREATE INDEX idx_pipeline_stages_won ON pipeline_stages(pipeline_id, is_won);

-- Add comments
COMMENT ON TABLE pipeline_stages IS 'Stages within sales pipelines';
COMMENT ON COLUMN pipeline_stages.pipeline_id IS 'Reference to parent pipeline';
COMMENT ON COLUMN pipeline_stages.name IS 'Stage name';
COMMENT ON COLUMN pipeline_stages.description IS 'Stage description';
COMMENT ON COLUMN pipeline_stages.default_probability IS 'Default probability percentage for deals in this stage';
COMMENT ON COLUMN pipeline_stages.is_active IS 'Whether the stage is active';
COMMENT ON COLUMN pipeline_stages.is_closed IS 'Whether this stage represents a closed deal';
COMMENT ON COLUMN pipeline_stages.is_won IS 'Whether this stage represents a won deal (only valid if is_closed is true)';
COMMENT ON COLUMN pipeline_stages.display_order IS 'Display order within the pipeline';
COMMENT ON COLUMN pipeline_stages.color IS 'Hex color code for UI display';
COMMENT ON COLUMN pipeline_stages.automation_rules IS 'JSON configuration for stage automation rules';