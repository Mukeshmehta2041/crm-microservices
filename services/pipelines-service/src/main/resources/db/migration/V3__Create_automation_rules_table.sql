-- Create automation_rules table
CREATE TABLE automation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    pipeline_id UUID,
    stage_id UUID,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    trigger_type VARCHAR(50) NOT NULL,
    trigger_conditions JSONB NOT NULL DEFAULT '{}',
    actions JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    execution_order INTEGER NOT NULL DEFAULT 0,
    last_executed_at TIMESTAMP WITH TIME ZONE,
    execution_count BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT fk_automation_rules_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines(id) ON DELETE CASCADE,
    CONSTRAINT fk_automation_rules_stage FOREIGN KEY (stage_id) REFERENCES pipeline_stages(id) ON DELETE CASCADE,
    CONSTRAINT valid_trigger_type CHECK (trigger_type IN (
        'DEAL_CREATED', 'DEAL_UPDATED', 'STAGE_CHANGED', 'PROBABILITY_CHANGED',
        'AMOUNT_CHANGED', 'DATE_CHANGED', 'FIELD_UPDATED', 'TIME_BASED',
        'ACTIVITY_COMPLETED', 'EMAIL_OPENED', 'EMAIL_CLICKED'
    )),
    CONSTRAINT valid_execution_order CHECK (execution_order >= 0),
    CONSTRAINT pipeline_or_stage_required CHECK (pipeline_id IS NOT NULL OR stage_id IS NOT NULL)
);

-- Create indexes for automation_rules
CREATE INDEX idx_automation_rules_tenant ON automation_rules(tenant_id);
CREATE INDEX idx_automation_rules_pipeline ON automation_rules(pipeline_id);
CREATE INDEX idx_automation_rules_stage ON automation_rules(stage_id);
CREATE INDEX idx_automation_rules_trigger ON automation_rules(trigger_type);
CREATE INDEX idx_automation_rules_active ON automation_rules(tenant_id, is_active);
CREATE INDEX idx_automation_rules_execution_order ON automation_rules(pipeline_id, execution_order);

-- Add comments
COMMENT ON TABLE automation_rules IS 'Automation rules for pipeline and stage workflows';
COMMENT ON COLUMN automation_rules.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN automation_rules.pipeline_id IS 'Reference to pipeline (for pipeline-level rules)';
COMMENT ON COLUMN automation_rules.stage_id IS 'Reference to stage (for stage-level rules)';
COMMENT ON COLUMN automation_rules.name IS 'Rule name';
COMMENT ON COLUMN automation_rules.description IS 'Rule description';
COMMENT ON COLUMN automation_rules.trigger_type IS 'Type of trigger that activates this rule';
COMMENT ON COLUMN automation_rules.trigger_conditions IS 'JSON conditions that must be met for rule execution';
COMMENT ON COLUMN automation_rules.actions IS 'JSON actions to execute when rule is triggered';
COMMENT ON COLUMN automation_rules.is_active IS 'Whether the rule is active';
COMMENT ON COLUMN automation_rules.execution_order IS 'Order of execution when multiple rules are triggered';
COMMENT ON COLUMN automation_rules.last_executed_at IS 'Timestamp of last rule execution';
COMMENT ON COLUMN automation_rules.execution_count IS 'Number of times rule has been executed';
COMMENT ON COLUMN automation_rules.error_count IS 'Number of execution errors';