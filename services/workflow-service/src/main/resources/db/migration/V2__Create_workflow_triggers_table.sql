-- Create workflow triggers table
CREATE TABLE workflow_triggers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_definition_id UUID NOT NULL REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    trigger_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(255),
    conditions JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT valid_trigger_type CHECK (trigger_type IN ('EVENT', 'SCHEDULE', 'MANUAL', 'API', 'WEBHOOK')),
    CONSTRAINT unique_trigger_name UNIQUE (tenant_id, workflow_definition_id, name)
);

-- Create indexes for workflow triggers
CREATE INDEX idx_workflow_triggers_tenant ON workflow_triggers(tenant_id);
CREATE INDEX idx_workflow_triggers_definition ON workflow_triggers(workflow_definition_id);
CREATE INDEX idx_workflow_triggers_active ON workflow_triggers(tenant_id, is_active);
CREATE INDEX idx_workflow_triggers_type ON workflow_triggers(tenant_id, trigger_type);
CREATE INDEX idx_workflow_triggers_event_type ON workflow_triggers(tenant_id, event_type);
CREATE INDEX idx_workflow_triggers_priority ON workflow_triggers(tenant_id, priority DESC);

-- Create business rules table
CREATE TABLE business_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    rule_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    conditions JSONB NOT NULL,
    actions JSONB NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT valid_rule_type CHECK (rule_type IN ('VALIDATION', 'ASSIGNMENT', 'NOTIFICATION', 'FIELD_UPDATE', 'WORKFLOW_TRIGGER')),
    CONSTRAINT unique_rule_name UNIQUE (tenant_id, name)
);

-- Create indexes for business rules
CREATE INDEX idx_business_rules_tenant ON business_rules(tenant_id);
CREATE INDEX idx_business_rules_active ON business_rules(tenant_id, is_active);
CREATE INDEX idx_business_rules_type ON business_rules(tenant_id, rule_type);
CREATE INDEX idx_business_rules_entity_type ON business_rules(tenant_id, entity_type);
CREATE INDEX idx_business_rules_priority ON business_rules(tenant_id, priority DESC);

-- Create rule executions table
CREATE TABLE rule_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    business_rule_id UUID NOT NULL REFERENCES business_rules(id) ON DELETE CASCADE,
    entity_id UUID,
    entity_type VARCHAR(100),
    trigger_event VARCHAR(255),
    input_data JSONB,
    output_data JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    error_details JSONB,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    duration_ms BIGINT,
    
    CONSTRAINT valid_rule_execution_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'SKIPPED'))
);

-- Create indexes for rule executions
CREATE INDEX idx_rule_executions_tenant ON rule_executions(tenant_id);
CREATE INDEX idx_rule_executions_rule ON rule_executions(business_rule_id);
CREATE INDEX idx_rule_executions_entity ON rule_executions(tenant_id, entity_type, entity_id);
CREATE INDEX idx_rule_executions_status ON rule_executions(tenant_id, status);
CREATE INDEX idx_rule_executions_executed_at ON rule_executions(tenant_id, executed_at);

-- Create workflow templates table
CREATE TABLE workflow_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    template_json JSONB NOT NULL,
    variables_schema JSONB,
    is_public BOOLEAN DEFAULT FALSE,
    usage_count INTEGER DEFAULT 0,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT template_tenant_check CHECK (
        (is_public = TRUE AND tenant_id IS NULL) OR 
        (is_public = FALSE AND tenant_id IS NOT NULL)
    )
);

-- Create indexes for workflow templates
CREATE INDEX idx_workflow_templates_tenant ON workflow_templates(tenant_id);
CREATE INDEX idx_workflow_templates_public ON workflow_templates(is_public);
CREATE INDEX idx_workflow_templates_category ON workflow_templates(category);
CREATE INDEX idx_workflow_templates_usage ON workflow_templates(usage_count DESC);

-- Enable row level security for new tables
ALTER TABLE workflow_triggers ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE rule_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_templates ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for new tables
CREATE POLICY tenant_isolation_workflow_triggers ON workflow_triggers
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

CREATE POLICY tenant_isolation_business_rules ON business_rules
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

CREATE POLICY tenant_isolation_rule_executions ON rule_executions
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

CREATE POLICY tenant_isolation_workflow_templates ON workflow_templates
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID OR is_public = TRUE);