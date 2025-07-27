-- Create deal_stage_history table
CREATE TABLE deal_stage_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deal_id UUID NOT NULL,
    from_stage_id UUID,
    to_stage_id UUID NOT NULL,
    pipeline_id UUID NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    changed_by UUID NOT NULL,
    reason VARCHAR(500),
    duration_in_previous_stage_hours BIGINT,
    
    CONSTRAINT fk_deal_stage_history_deal FOREIGN KEY (deal_id) REFERENCES deals(id) ON DELETE CASCADE,
    CONSTRAINT fk_deal_stage_history_from_stage FOREIGN KEY (from_stage_id) REFERENCES pipeline_stages(id),
    CONSTRAINT fk_deal_stage_history_to_stage FOREIGN KEY (to_stage_id) REFERENCES pipeline_stages(id),
    CONSTRAINT fk_deal_stage_history_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines(id),
    CONSTRAINT valid_duration CHECK (duration_in_previous_stage_hours IS NULL OR duration_in_previous_stage_hours >= 0)
);

-- Create indexes for deal_stage_history
CREATE INDEX idx_deal_stage_history_deal ON deal_stage_history(deal_id);
CREATE INDEX idx_deal_stage_history_timestamp ON deal_stage_history(deal_id, changed_at);
CREATE INDEX idx_deal_stage_history_pipeline ON deal_stage_history(pipeline_id);
CREATE INDEX idx_deal_stage_history_from_stage ON deal_stage_history(from_stage_id);
CREATE INDEX idx_deal_stage_history_to_stage ON deal_stage_history(to_stage_id);
CREATE INDEX idx_deal_stage_history_changed_by ON deal_stage_history(changed_by);
CREATE INDEX idx_deal_stage_history_changed_at ON deal_stage_history(changed_at);

-- Add comments
COMMENT ON TABLE deal_stage_history IS 'History of deal stage changes for tracking and analytics';
COMMENT ON COLUMN deal_stage_history.deal_id IS 'Reference to the deal';
COMMENT ON COLUMN deal_stage_history.from_stage_id IS 'Previous stage (null for initial stage)';
COMMENT ON COLUMN deal_stage_history.to_stage_id IS 'New stage';
COMMENT ON COLUMN deal_stage_history.pipeline_id IS 'Pipeline at time of change';
COMMENT ON COLUMN deal_stage_history.changed_at IS 'Timestamp of stage change';
COMMENT ON COLUMN deal_stage_history.changed_by IS 'User who made the change';
COMMENT ON COLUMN deal_stage_history.reason IS 'Reason for stage change';
COMMENT ON COLUMN deal_stage_history.duration_in_previous_stage_hours IS 'Hours spent in previous stage';