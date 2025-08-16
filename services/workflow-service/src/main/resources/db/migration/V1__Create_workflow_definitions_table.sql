-- Create workflow definitions table
CREATE TABLE workflow_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    is_published BOOLEAN DEFAULT FALSE,
    workflow_json JSONB NOT NULL,
    trigger_config JSONB,
    variables_schema JSONB,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT unique_workflow_name_version UNIQUE (tenant_id, name, version),
    CONSTRAINT valid_version CHECK (version > 0)
);

-- Create indexes for workflow definitions
CREATE INDEX idx_workflow_definitions_tenant ON workflow_definitions(tenant_id);
CREATE INDEX idx_workflow_definitions_active ON workflow_definitions(tenant_id, is_active);
CREATE INDEX idx_workflow_definitions_published ON workflow_definitions(tenant_id, is_published);
CREATE INDEX idx_workflow_definitions_category ON workflow_definitions(tenant_id, category);
CREATE INDEX idx_workflow_definitions_created_by ON workflow_definitions(created_by);
CREATE INDEX idx_workflow_definitions_name ON workflow_definitions(tenant_id, name);

-- Create workflow executions table
CREATE TABLE workflow_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_definition_id UUID NOT NULL REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    execution_key VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    trigger_type VARCHAR(100),
    trigger_data JSONB,
    variables JSONB,
    current_step VARCHAR(255),
    progress_percentage INTEGER DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    error_details JSONB,
    created_by UUID,
    
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'SUSPENDED')),
    CONSTRAINT valid_progress CHECK (progress_percentage >= 0 AND progress_percentage <= 100),
    CONSTRAINT completion_consistency CHECK (
        (status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND completed_at IS NOT NULL) OR
        (status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED') AND completed_at IS NULL)
    )
);

-- Create indexes for workflow executions
CREATE INDEX idx_workflow_executions_tenant ON workflow_executions(tenant_id);
CREATE INDEX idx_workflow_executions_definition ON workflow_executions(workflow_definition_id);
CREATE INDEX idx_workflow_executions_status ON workflow_executions(tenant_id, status);
CREATE INDEX idx_workflow_executions_started_at ON workflow_executions(tenant_id, started_at);
CREATE INDEX idx_workflow_executions_execution_key ON workflow_executions(tenant_id, execution_key);
CREATE INDEX idx_workflow_executions_trigger_type ON workflow_executions(tenant_id, trigger_type);

-- Create workflow step executions table
CREATE TABLE workflow_step_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_execution_id UUID NOT NULL REFERENCES workflow_executions(id) ON DELETE CASCADE,
    step_id VARCHAR(255) NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    step_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    input_data JSONB,
    output_data JSONB,
    error_message TEXT,
    error_details JSONB,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    retry_count INTEGER DEFAULT 0,
    
    CONSTRAINT valid_step_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED', 'CANCELLED')),
    CONSTRAINT valid_retry_count CHECK (retry_count >= 0),
    CONSTRAINT step_completion_consistency CHECK (
        (status IN ('COMPLETED', 'FAILED', 'SKIPPED', 'CANCELLED') AND completed_at IS NOT NULL) OR
        (status NOT IN ('COMPLETED', 'FAILED', 'SKIPPED', 'CANCELLED'))
    )
);

-- Create indexes for workflow step executions
CREATE INDEX idx_workflow_step_executions_tenant ON workflow_step_executions(tenant_id);
CREATE INDEX idx_workflow_step_executions_execution ON workflow_step_executions(workflow_execution_id);
CREATE INDEX idx_workflow_step_executions_status ON workflow_step_executions(tenant_id, status);
CREATE INDEX idx_workflow_step_executions_step_id ON workflow_step_executions(workflow_execution_id, step_id);
CREATE INDEX idx_workflow_step_executions_started_at ON workflow_step_executions(tenant_id, started_at);

-- Enable row level security
ALTER TABLE workflow_definitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_step_executions ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
CREATE POLICY tenant_isolation_workflow_definitions ON workflow_definitions
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

CREATE POLICY tenant_isolation_workflow_executions ON workflow_executions
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

CREATE POLICY tenant_isolation_workflow_step_executions ON workflow_step_executions
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);