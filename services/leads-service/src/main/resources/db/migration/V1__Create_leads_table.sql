-- Create leads table with comprehensive fields and constraints
CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    company VARCHAR(255),
    title VARCHAR(100),
    industry VARCHAR(100),
    website VARCHAR(255),
    lead_source VARCHAR(100),
    lead_source_detail VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    qualification_status VARCHAR(50) DEFAULT 'UNQUALIFIED',
    lead_score INTEGER NOT NULL DEFAULT 0,
    annual_revenue DECIMAL(15,2),
    number_of_employees INTEGER,
    budget DECIMAL(15,2),
    purchase_timeframe VARCHAR(50),
    decision_maker BOOLEAN DEFAULT FALSE,
    pain_points TEXT,
    interests TEXT,
    notes TEXT,
    do_not_call BOOLEAN DEFAULT FALSE,
    do_not_email BOOLEAN DEFAULT FALSE,
    email_opt_out BOOLEAN DEFAULT FALSE,
    preferred_contact_method VARCHAR(20) DEFAULT 'email',
    timezone VARCHAR(50),
    language VARCHAR(10) DEFAULT 'en-US',
    converted_contact_id UUID,
    converted_account_id UUID,
    converted_deal_id UUID,
    converted_at TIMESTAMP WITH TIME ZONE,
    owner_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE,
    last_activity_at TIMESTAMP WITH TIME ZONE,
    next_follow_up_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    -- Constraints
    CONSTRAINT valid_lead_status CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'UNQUALIFIED', 'NURTURING', 'CONVERTED', 'LOST', 'INACTIVE')),
    CONSTRAINT valid_qualification_status CHECK (qualification_status IN ('UNQUALIFIED', 'MARKETING_QUALIFIED', 'SALES_QUALIFIED', 'QUALIFIED', 'DISQUALIFIED')),
    CONSTRAINT valid_email CHECK (email IS NULL OR email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT valid_lead_score CHECK (lead_score >= 0 AND lead_score <= 100),
    CONSTRAINT valid_annual_revenue CHECK (annual_revenue IS NULL OR annual_revenue >= 0),
    CONSTRAINT valid_number_of_employees CHECK (number_of_employees IS NULL OR number_of_employees >= 0),
    CONSTRAINT valid_budget CHECK (budget IS NULL OR budget >= 0),
    CONSTRAINT valid_preferred_contact_method CHECK (preferred_contact_method IN ('email', 'phone', 'mobile', 'mail')),
    CONSTRAINT lead_name_not_empty CHECK (TRIM(first_name) != '' AND TRIM(last_name) != ''),
    CONSTRAINT conversion_consistency CHECK (
        (converted_contact_id IS NULL AND converted_account_id IS NULL AND converted_deal_id IS NULL AND converted_at IS NULL) OR
        (converted_at IS NOT NULL AND (converted_contact_id IS NOT NULL OR converted_account_id IS NOT NULL OR converted_deal_id IS NOT NULL))
    )
);

-- Create indexes for performance
CREATE INDEX idx_leads_tenant_id ON leads(tenant_id);
CREATE UNIQUE INDEX idx_leads_email_tenant ON leads(tenant_id, email) WHERE email IS NOT NULL;
CREATE INDEX idx_leads_phone ON leads(phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_leads_mobile ON leads(mobile) WHERE mobile IS NOT NULL;
CREATE INDEX idx_leads_company ON leads(tenant_id, company) WHERE company IS NOT NULL;
CREATE INDEX idx_leads_name ON leads(tenant_id, last_name, first_name);
CREATE INDEX idx_leads_status ON leads(tenant_id, status);
CREATE INDEX idx_leads_qualification_status ON leads(tenant_id, qualification_status);
CREATE INDEX idx_leads_score ON leads(tenant_id, lead_score DESC);
CREATE INDEX idx_leads_source ON leads(tenant_id, lead_source) WHERE lead_source IS NOT NULL;
CREATE INDEX idx_leads_owner ON leads(owner_id);
CREATE INDEX idx_leads_created_at ON leads(tenant_id, created_at);
CREATE INDEX idx_leads_updated_at ON leads(tenant_id, updated_at);
CREATE INDEX idx_leads_assigned_at ON leads(tenant_id, assigned_at) WHERE assigned_at IS NOT NULL;
CREATE INDEX idx_leads_last_activity_at ON leads(tenant_id, last_activity_at) WHERE last_activity_at IS NOT NULL;
CREATE INDEX idx_leads_next_follow_up_at ON leads(tenant_id, next_follow_up_at) WHERE next_follow_up_at IS NOT NULL;
CREATE INDEX idx_leads_converted_at ON leads(tenant_id, converted_at) WHERE converted_at IS NOT NULL;
CREATE INDEX idx_leads_converted_contact ON leads(converted_contact_id) WHERE converted_contact_id IS NOT NULL;
CREATE INDEX idx_leads_converted_account ON leads(converted_account_id) WHERE converted_account_id IS NOT NULL;
CREATE INDEX idx_leads_converted_deal ON leads(converted_deal_id) WHERE converted_deal_id IS NOT NULL;

-- Composite indexes for common query patterns
CREATE INDEX idx_leads_tenant_status_score ON leads(tenant_id, status, lead_score DESC);
CREATE INDEX idx_leads_tenant_owner_status ON leads(tenant_id, owner_id, status);
CREATE INDEX idx_leads_tenant_qualification_score ON leads(tenant_id, qualification_status, lead_score DESC);
CREATE INDEX idx_leads_search_text ON leads USING gin(to_tsvector('english', 
    COALESCE(first_name, '') || ' ' || 
    COALESCE(last_name, '') || ' ' || 
    COALESCE(email, '') || ' ' || 
    COALESCE(company, '') || ' ' || 
    COALESCE(title, '')
));

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_leads_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_leads_updated_at
    BEFORE UPDATE ON leads
    FOR EACH ROW
    EXECUTE FUNCTION update_leads_updated_at();

-- Add comments for documentation
COMMENT ON TABLE leads IS 'Stores lead information for the CRM system';
COMMENT ON COLUMN leads.lead_score IS 'Lead score from 0-100 based on qualification criteria';
COMMENT ON COLUMN leads.qualification_status IS 'Current qualification status of the lead';
COMMENT ON COLUMN leads.converted_at IS 'Timestamp when lead was converted to contact/account/deal';
COMMENT ON COLUMN leads.next_follow_up_at IS 'Scheduled next follow-up date and time';
COMMENT ON COLUMN leads.last_activity_at IS 'Timestamp of last activity related to this lead';