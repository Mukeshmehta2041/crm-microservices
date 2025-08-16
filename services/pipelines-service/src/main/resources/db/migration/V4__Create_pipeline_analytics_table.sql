-- Create pipeline_analytics table for performance metrics
CREATE TABLE pipeline_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    pipeline_id UUID NOT NULL,
    stage_id UUID,
    metric_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,4) NOT NULL,
    metric_unit VARCHAR(20),
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}',
    
    CONSTRAINT fk_pipeline_analytics_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines(id) ON DELETE CASCADE,
    CONSTRAINT fk_pipeline_analytics_stage FOREIGN KEY (stage_id) REFERENCES pipeline_stages(id) ON DELETE CASCADE,
    CONSTRAINT valid_metric_type CHECK (metric_type IN (
        'CONVERSION_RATE', 'AVERAGE_TIME_IN_STAGE', 'DEAL_COUNT', 'DEAL_VALUE',
        'WIN_RATE', 'LOSS_RATE', 'VELOCITY', 'FORECAST_ACCURACY'
    )),
    CONSTRAINT valid_period CHECK (period_end >= period_start),
    CONSTRAINT unique_metric_per_period UNIQUE (tenant_id, pipeline_id, stage_id, metric_type, period_start, period_end)
);

-- Create indexes for pipeline_analytics
CREATE INDEX idx_pipeline_analytics_tenant ON pipeline_analytics(tenant_id);
CREATE INDEX idx_pipeline_analytics_pipeline ON pipeline_analytics(pipeline_id);
CREATE INDEX idx_pipeline_analytics_stage ON pipeline_analytics(stage_id);
CREATE INDEX idx_pipeline_analytics_metric ON pipeline_analytics(metric_type);
CREATE INDEX idx_pipeline_analytics_period ON pipeline_analytics(period_start, period_end);
CREATE INDEX idx_pipeline_analytics_calculated ON pipeline_analytics(calculated_at);

-- Add comments
COMMENT ON TABLE pipeline_analytics IS 'Analytics and performance metrics for pipelines and stages';
COMMENT ON COLUMN pipeline_analytics.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN pipeline_analytics.pipeline_id IS 'Reference to pipeline';
COMMENT ON COLUMN pipeline_analytics.stage_id IS 'Reference to stage (null for pipeline-level metrics)';
COMMENT ON COLUMN pipeline_analytics.metric_type IS 'Type of metric being tracked';
COMMENT ON COLUMN pipeline_analytics.metric_name IS 'Human-readable metric name';
COMMENT ON COLUMN pipeline_analytics.metric_value IS 'Calculated metric value';
COMMENT ON COLUMN pipeline_analytics.metric_unit IS 'Unit of measurement (days, percentage, count, etc.)';
COMMENT ON COLUMN pipeline_analytics.period_start IS 'Start of measurement period';
COMMENT ON COLUMN pipeline_analytics.period_end IS 'End of measurement period';
COMMENT ON COLUMN pipeline_analytics.calculated_at IS 'When the metric was calculated';
COMMENT ON COLUMN pipeline_analytics.metadata IS 'Additional metadata about the metric calculation';