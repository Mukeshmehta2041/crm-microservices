-- Create lead score history table for tracking scoring changes
CREATE TABLE lead_score_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID NOT NULL,
    previous_score INTEGER NOT NULL,
    new_score INTEGER NOT NULL,
    score_change INTEGER NOT NULL,
    reason VARCHAR(255),
    rule_name VARCHAR(100),
    rule_category VARCHAR(50),
    triggered_by VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    
    -- Constraints
    CONSTRAINT valid_previous_score CHECK (previous_score >= 0 AND previous_score <= 100),
    CONSTRAINT valid_new_score CHECK (new_score >= 0 AND new_score <= 100),
    CONSTRAINT valid_score_change CHECK (score_change = new_score - previous_score),
    CONSTRAINT valid_rule_category CHECK (rule_category IS NULL OR rule_category IN (
        'DEMOGRAPHIC', 'BEHAVIORAL', 'ENGAGEMENT', 'FIRMOGRAPHIC', 'INTENT', 'NEGATIVE'
    ))
);

-- Create indexes for performance
CREATE INDEX idx_lead_score_history_tenant_id ON lead_score_history(tenant_id);
CREATE INDEX idx_lead_score_history_lead_id ON lead_score_history(lead_id);
CREATE INDEX idx_lead_score_history_tenant_lead ON lead_score_history(tenant_id, lead_id);
CREATE INDEX idx_lead_score_history_created_at ON lead_score_history(created_at);
CREATE INDEX idx_lead_score_history_tenant_created_at ON lead_score_history(tenant_id, created_at);
CREATE INDEX idx_lead_score_history_rule_name ON lead_score_history(tenant_id, rule_name) WHERE rule_name IS NOT NULL;
CREATE INDEX idx_lead_score_history_rule_category ON lead_score_history(tenant_id, rule_category) WHERE rule_category IS NOT NULL;
CREATE INDEX idx_lead_score_history_score_change ON lead_score_history(tenant_id, score_change);
CREATE INDEX idx_lead_score_history_positive_changes ON lead_score_history(tenant_id, lead_id, created_at) WHERE score_change > 0;
CREATE INDEX idx_lead_score_history_negative_changes ON lead_score_history(tenant_id, lead_id, created_at) WHERE score_change < 0;

-- Composite indexes for analytics
CREATE INDEX idx_lead_score_history_lead_created_desc ON lead_score_history(lead_id, created_at DESC);
CREATE INDEX idx_lead_score_history_tenant_rule_created ON lead_score_history(tenant_id, rule_name, created_at) WHERE rule_name IS NOT NULL;

-- Add foreign key constraint (assuming leads table exists)
ALTER TABLE lead_score_history 
ADD CONSTRAINT fk_lead_score_history_lead 
FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;

-- Add comments for documentation
COMMENT ON TABLE lead_score_history IS 'Tracks all changes to lead scores with reasons and rules';
COMMENT ON COLUMN lead_score_history.score_change IS 'Calculated difference between new_score and previous_score';
COMMENT ON COLUMN lead_score_history.rule_name IS 'Name of the scoring rule that triggered this change';
COMMENT ON COLUMN lead_score_history.rule_category IS 'Category of the scoring rule (demographic, behavioral, etc.)';
COMMENT ON COLUMN lead_score_history.triggered_by IS 'What action or event triggered this score change';
COMMENT ON COLUMN lead_score_history.details IS 'Additional details about the score change in JSON format';