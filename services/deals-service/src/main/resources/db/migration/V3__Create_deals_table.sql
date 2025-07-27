-- Create deals table
CREATE TABLE deals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    account_id UUID,
    contact_id UUID,
    pipeline_id UUID NOT NULL,
    stage_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) CHECK (amount >= 0),
    currency VARCHAR(3) DEFAULT 'USD' CHECK (currency ~ '^[A-Z]{3}$'),
    probability DECIMAL(5,2) CHECK (probability >= 0 AND probability <= 100),
    expected_close_date DATE,
    actual_close_date DATE,
    deal_type VARCHAR(50) CHECK (deal_type IN ('new_business', 'existing_business', 'renewal')),
    lead_source VARCHAR(100),
    next_step TEXT,
    description TEXT,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    is_won BOOLEAN NOT NULL DEFAULT FALSE,
    tags TEXT[],
    custom_fields JSONB DEFAULT '{}',
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT fk_deals_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines(id),
    CONSTRAINT fk_deals_stage FOREIGN KEY (stage_id) REFERENCES pipeline_stages(id),
    CONSTRAINT valid_closed_won CHECK (NOT is_won OR is_closed),
    CONSTRAINT close_date_consistency CHECK (NOT is_closed OR actual_close_date IS NOT NULL),
    CONSTRAINT deal_name_not_empty CHECK (TRIM(name) != '')
);

-- Create indexes for deals
CREATE INDEX idx_deals_tenant_id ON deals(tenant_id);
CREATE INDEX idx_deals_pipeline_stage ON deals(pipeline_id, stage_id);
CREATE INDEX idx_deals_owner ON deals(owner_id);
CREATE INDEX idx_deals_account ON deals(account_id);
CREATE INDEX idx_deals_contact ON deals(contact_id);
CREATE INDEX idx_deals_amount ON deals(tenant_id, amount);
CREATE INDEX idx_deals_close_date ON deals(tenant_id, expected_close_date);
CREATE INDEX idx_deals_actual_close_date ON deals(tenant_id, actual_close_date);
CREATE INDEX idx_deals_created_at ON deals(tenant_id, created_at);
CREATE INDEX idx_deals_updated_at ON deals(tenant_id, updated_at);
CREATE INDEX idx_deals_status ON deals(tenant_id, is_closed, is_won);
CREATE INDEX idx_deals_probability ON deals(tenant_id, probability);
CREATE INDEX idx_deals_currency ON deals(tenant_id, currency);
CREATE INDEX idx_deals_deal_type ON deals(tenant_id, deal_type);
CREATE INDEX idx_deals_lead_source ON deals(tenant_id, lead_source);
CREATE INDEX idx_deals_tags ON deals USING GIN(tags);
CREATE INDEX idx_deals_custom_fields ON deals USING GIN(custom_fields);

-- Create partial indexes for performance
CREATE INDEX idx_deals_open ON deals(tenant_id, pipeline_id, stage_id) WHERE is_closed = FALSE;
CREATE INDEX idx_deals_closed_won ON deals(tenant_id, actual_close_date) WHERE is_closed = TRUE AND is_won = TRUE;
CREATE INDEX idx_deals_closed_lost ON deals(tenant_id, actual_close_date) WHERE is_closed = TRUE AND is_won = FALSE;

-- Add comments
COMMENT ON TABLE deals IS 'Sales deals/opportunities';
COMMENT ON COLUMN deals.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN deals.account_id IS 'Reference to associated account';
COMMENT ON COLUMN deals.contact_id IS 'Reference to primary contact';
COMMENT ON COLUMN deals.pipeline_id IS 'Reference to pipeline';
COMMENT ON COLUMN deals.stage_id IS 'Reference to current pipeline stage';
COMMENT ON COLUMN deals.name IS 'Deal name/title';
COMMENT ON COLUMN deals.amount IS 'Deal value in specified currency';
COMMENT ON COLUMN deals.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN deals.probability IS 'Probability of closing (0-100%)';
COMMENT ON COLUMN deals.expected_close_date IS 'Expected close date';
COMMENT ON COLUMN deals.actual_close_date IS 'Actual close date (set when deal is closed)';
COMMENT ON COLUMN deals.deal_type IS 'Type of deal (new_business, existing_business, renewal)';
COMMENT ON COLUMN deals.lead_source IS 'Source of the lead that generated this deal';
COMMENT ON COLUMN deals.next_step IS 'Next action to be taken';
COMMENT ON COLUMN deals.description IS 'Deal description/notes';
COMMENT ON COLUMN deals.is_closed IS 'Whether the deal is closed';
COMMENT ON COLUMN deals.is_won IS 'Whether the deal was won (only valid if is_closed is true)';
COMMENT ON COLUMN deals.tags IS 'Array of tags for categorization';
COMMENT ON COLUMN deals.custom_fields IS 'JSON object containing custom field values';
COMMENT ON COLUMN deals.owner_id IS 'User who owns this deal';