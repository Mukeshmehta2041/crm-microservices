-- =====================================================
-- CONTACTS SERVICE - COMPREHENSIVE SCHEMA ENHANCEMENTS
-- =====================================================

-- Import common functions and utilities
-- Function to set tenant context for RLS
CREATE OR REPLACE FUNCTION set_tenant_context(tenant_uuid UUID)
RETURNS void AS $$
BEGIN
    PERFORM set_config('app.current_tenant_id', tenant_uuid::text, true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get current tenant context
CREATE OR REPLACE FUNCTION get_current_tenant_id()
RETURNS UUID AS $$
BEGIN
    RETURN current_setting('app.current_tenant_id', true)::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 1. CREATE AUDIT LOG TABLE FOR CONTACTS SERVICE
-- =====================================================

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    user_id UUID,
    session_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    correlation_id VARCHAR(255),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT valid_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    CONSTRAINT valid_table_name CHECK (table_name ~ '^[a-z_]+$')
);

-- Indexes for audit log table
CREATE INDEX idx_audit_log_tenant ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_operation ON audit_log(operation);
CREATE INDEX idx_audit_log_correlation ON audit_log(correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_audit_log_session ON audit_log(session_id) WHERE session_id IS NOT NULL;

-- Composite indexes for common audit queries
CREATE INDEX idx_audit_log_tenant_table_time ON audit_log(tenant_id, table_name, timestamp);
CREATE INDEX idx_audit_log_user_time ON audit_log(user_id, timestamp) WHERE user_id IS NOT NULL;
CREATE INDEX idx_audit_log_record_time ON audit_log(table_name, record_id, timestamp);

-- =====================================================
-- 2. CREATE AUDIT TRIGGER FUNCTION
-- =====================================================

CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    old_data JSONB;
    new_data JSONB;
    changed_fields TEXT[];
    tenant_id_val UUID;
    user_id_val UUID;
    session_id_val VARCHAR(255);
    ip_address_val INET;
    user_agent_val TEXT;
    correlation_id_val VARCHAR(255);
BEGIN
    -- Extract tenant_id from the record
    IF TG_OP = 'DELETE' THEN
        tenant_id_val := OLD.tenant_id;
    ELSE
        tenant_id_val := NEW.tenant_id;
    END IF;
    
    -- Get audit context from session variables
    BEGIN
        user_id_val := current_setting('app.current_user_id', true)::UUID;
    EXCEPTION
        WHEN OTHERS THEN
            user_id_val := NULL;
    END;
    
    BEGIN
        session_id_val := current_setting('app.current_session_id', true);
    EXCEPTION
        WHEN OTHERS THEN
            session_id_val := NULL;
    END;
    
    BEGIN
        ip_address_val := current_setting('app.current_ip_address', true)::INET;
    EXCEPTION
        WHEN OTHERS THEN
            ip_address_val := NULL;
    END;
    
    BEGIN
        user_agent_val := current_setting('app.current_user_agent', true);
    EXCEPTION
        WHEN OTHERS THEN
            user_agent_val := NULL;
    END;
    
    BEGIN
        correlation_id_val := current_setting('app.correlation_id', true);
    EXCEPTION
        WHEN OTHERS THEN
            correlation_id_val := NULL;
    END;
    
    -- Handle different operations
    IF TG_OP = 'DELETE' THEN
        old_data := to_jsonb(OLD);
        new_data := NULL;
        changed_fields := NULL;
    ELSIF TG_OP = 'INSERT' THEN
        old_data := NULL;
        new_data := to_jsonb(NEW);
        changed_fields := NULL;
    ELSIF TG_OP = 'UPDATE' THEN
        old_data := to_jsonb(OLD);
        new_data := to_jsonb(NEW);
        
        -- Calculate changed fields
        SELECT array_agg(key) INTO changed_fields
        FROM (
            SELECT key
            FROM jsonb_each(old_data) old_kv
            FULL OUTER JOIN jsonb_each(new_data) new_kv USING (key)
            WHERE old_kv.value IS DISTINCT FROM new_kv.value
        ) changed;
    END IF;
    
    -- Insert audit record
    INSERT INTO audit_log (
        tenant_id, table_name, record_id, operation,
        old_values, new_values, changed_fields,
        user_id, session_id, ip_address, user_agent, correlation_id
    ) VALUES (
        tenant_id_val, TG_TABLE_NAME, 
        COALESCE(NEW.id, OLD.id), TG_OP,
        old_data, new_data, changed_fields,
        user_id_val, session_id_val, ip_address_val, user_agent_val, correlation_id_val
    );
    
    -- Return appropriate record
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Log error but don't fail the main operation
        RAISE WARNING 'Audit trigger failed: %', SQLERRM;
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        ELSE
            RETURN NEW;
        END IF;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 3. ENABLE ROW-LEVEL SECURITY
-- =====================================================

-- Enable RLS on contacts table
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policy for contacts
CREATE POLICY tenant_isolation_contacts ON contacts
    USING (tenant_id = get_current_tenant_id());

-- Enable RLS on contact_relationships table
ALTER TABLE contact_relationships ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policy for contact_relationships
CREATE POLICY tenant_isolation_contact_relationships ON contact_relationships
    USING (tenant_id = get_current_tenant_id());

-- =====================================================
-- 4. ADD AUDIT TRIGGERS TO EXISTING TABLES
-- =====================================================

-- Add audit trigger to contacts table
CREATE TRIGGER audit_contacts_trigger
    AFTER INSERT OR UPDATE OR DELETE ON contacts
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Add audit trigger to contact_relationships table
CREATE TRIGGER audit_contact_relationships_trigger
    AFTER INSERT OR UPDATE OR DELETE ON contact_relationships
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 5. CREATE CONTACT ACTIVITIES TABLE
-- =====================================================

CREATE TABLE contact_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    activity_date TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INTEGER,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    outcome VARCHAR(100),
    follow_up_date TIMESTAMP WITH TIME ZONE,
    external_activity_id VARCHAR(255),
    metadata JSONB DEFAULT '{}',
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_activity_type CHECK (activity_type IN (
        'CALL', 'EMAIL', 'MEETING', 'TASK', 'NOTE', 'SMS', 'SOCIAL', 'OTHER'
    )),
    CONSTRAINT valid_status CHECK (status IN (
        'PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'DEFERRED'
    )),
    CONSTRAINT valid_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT valid_duration CHECK (duration_minutes IS NULL OR duration_minutes >= 0)
);

-- Indexes for contact_activities table
CREATE INDEX idx_contact_activities_tenant ON contact_activities(tenant_id);
CREATE INDEX idx_contact_activities_contact ON contact_activities(contact_id);
CREATE INDEX idx_contact_activities_type ON contact_activities(tenant_id, activity_type);
CREATE INDEX idx_contact_activities_date ON contact_activities(tenant_id, activity_date);
CREATE INDEX idx_contact_activities_owner ON contact_activities(owner_id);
CREATE INDEX idx_contact_activities_status ON contact_activities(tenant_id, status);
CREATE INDEX idx_contact_activities_follow_up ON contact_activities(follow_up_date) WHERE follow_up_date IS NOT NULL;
CREATE INDEX idx_contact_activities_external ON contact_activities(external_activity_id) WHERE external_activity_id IS NOT NULL;

-- Enable RLS and add audit trigger for contact_activities
ALTER TABLE contact_activities ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_contact_activities ON contact_activities
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_contact_activities_trigger
    AFTER INSERT OR UPDATE OR DELETE ON contact_activities
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 6. CREATE CONTACT DUPLICATES TABLE
-- =====================================================

CREATE TABLE contact_duplicates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    master_contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    duplicate_contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    similarity_score DECIMAL(5,2) NOT NULL,
    matching_fields TEXT[] NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    merge_strategy JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT valid_similarity_score CHECK (similarity_score >= 0 AND similarity_score <= 100),
    CONSTRAINT valid_duplicate_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'MERGED')),
    CONSTRAINT different_contacts CHECK (master_contact_id != duplicate_contact_id),
    CONSTRAINT uk_contact_duplicate_pair UNIQUE (tenant_id, master_contact_id, duplicate_contact_id)
);

-- Indexes for contact_duplicates table
CREATE INDEX idx_contact_duplicates_tenant ON contact_duplicates(tenant_id);
CREATE INDEX idx_contact_duplicates_master ON contact_duplicates(master_contact_id);
CREATE INDEX idx_contact_duplicates_duplicate ON contact_duplicates(duplicate_contact_id);
CREATE INDEX idx_contact_duplicates_status ON contact_duplicates(tenant_id, status);
CREATE INDEX idx_contact_duplicates_score ON contact_duplicates(tenant_id, similarity_score DESC);

-- Enable RLS and add audit trigger for contact_duplicates
ALTER TABLE contact_duplicates ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_contact_duplicates ON contact_duplicates
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_contact_duplicates_trigger
    AFTER INSERT OR UPDATE OR DELETE ON contact_duplicates
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 7. ENHANCED INDEXES FOR PERFORMANCE
-- =====================================================

-- Additional composite indexes for contacts table
CREATE INDEX idx_contacts_tenant_status_score ON contacts(tenant_id, contact_status, lead_score DESC);
CREATE INDEX idx_contacts_tenant_owner_status ON contacts(tenant_id, owner_id, contact_status);
CREATE INDEX idx_contacts_tenant_account_status ON contacts(tenant_id, account_id, contact_status) WHERE account_id IS NOT NULL;
CREATE INDEX idx_contacts_tenant_source_status ON contacts(tenant_id, lead_source, contact_status) WHERE lead_source IS NOT NULL;
CREATE INDEX idx_contacts_full_name ON contacts(tenant_id, (first_name || ' ' || last_name));
CREATE INDEX idx_contacts_communication_prefs ON contacts(tenant_id, do_not_call, do_not_email, email_opt_out);

-- GIN indexes for array and JSONB fields
CREATE INDEX idx_contacts_tags_gin ON contacts USING GIN(tags) WHERE array_length(tags, 1) > 0;
CREATE INDEX idx_contacts_custom_fields_gin ON contacts USING GIN(custom_fields) WHERE custom_fields != '{}'::jsonb;
CREATE INDEX idx_contacts_social_profiles_gin ON contacts USING GIN(social_profiles) WHERE social_profiles != '{}'::jsonb;
CREATE INDEX idx_contacts_mailing_address_gin ON contacts USING GIN(mailing_address) WHERE mailing_address IS NOT NULL;

-- Partial indexes for active contacts
CREATE INDEX idx_contacts_active_by_score ON contacts(tenant_id, lead_score DESC) 
    WHERE contact_status = 'ACTIVE';
CREATE INDEX idx_contacts_active_by_created ON contacts(tenant_id, created_at DESC) 
    WHERE contact_status = 'ACTIVE';
CREATE INDEX idx_contacts_active_by_updated ON contacts(tenant_id, updated_at DESC) 
    WHERE contact_status = 'ACTIVE';

-- =====================================================
-- 8. VALIDATION FUNCTIONS
-- =====================================================

-- Email validation function
CREATE OR REPLACE FUNCTION is_valid_email(email TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Phone validation function
CREATE OR REPLACE FUNCTION is_valid_phone(phone TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Basic phone validation (can be enhanced based on requirements)
    RETURN phone ~ '^\+?[1-9]\d{1,14}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Contact name validation function
CREATE OR REPLACE FUNCTION is_valid_contact_name(first_name TEXT, last_name TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN TRIM(first_name) != '' AND TRIM(last_name) != '' 
        AND length(TRIM(first_name)) >= 1 AND length(TRIM(last_name)) >= 1;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- 9. CONTACT MANAGEMENT FUNCTIONS
-- =====================================================

-- Function to calculate contact similarity score
CREATE OR REPLACE FUNCTION calculate_contact_similarity(
    contact1_id UUID,
    contact2_id UUID
) RETURNS DECIMAL(5,2) AS $$
DECLARE
    c1 RECORD;
    c2 RECORD;
    score DECIMAL(5,2) := 0;
    max_score DECIMAL(5,2) := 100;
BEGIN
    -- Get contact records
    SELECT * INTO c1 FROM contacts WHERE id = contact1_id;
    SELECT * INTO c2 FROM contacts WHERE id = contact2_id;
    
    IF c1 IS NULL OR c2 IS NULL THEN
        RETURN 0;
    END IF;
    
    -- Email match (40 points)
    IF c1.email IS NOT NULL AND c2.email IS NOT NULL AND lower(c1.email) = lower(c2.email) THEN
        score := score + 40;
    END IF;
    
    -- Phone match (30 points)
    IF c1.phone IS NOT NULL AND c2.phone IS NOT NULL AND c1.phone = c2.phone THEN
        score := score + 30;
    ELSIF c1.mobile IS NOT NULL AND c2.mobile IS NOT NULL AND c1.mobile = c2.mobile THEN
        score := score + 30;
    ELSIF c1.phone IS NOT NULL AND c2.mobile IS NOT NULL AND c1.phone = c2.mobile THEN
        score := score + 25;
    ELSIF c1.mobile IS NOT NULL AND c2.phone IS NOT NULL AND c1.mobile = c2.phone THEN
        score := score + 25;
    END IF;
    
    -- Name similarity (20 points)
    IF lower(c1.first_name) = lower(c2.first_name) AND lower(c1.last_name) = lower(c2.last_name) THEN
        score := score + 20;
    ELSIF lower(c1.first_name) = lower(c2.first_name) OR lower(c1.last_name) = lower(c2.last_name) THEN
        score := score + 10;
    END IF;
    
    -- Company match (10 points)
    IF c1.account_id IS NOT NULL AND c2.account_id IS NOT NULL AND c1.account_id = c2.account_id THEN
        score := score + 10;
    END IF;
    
    RETURN LEAST(score, max_score);
END;
$$ LANGUAGE plpgsql;

-- Function to find potential duplicate contacts
CREATE OR REPLACE FUNCTION find_duplicate_contacts(
    tenant_uuid UUID,
    min_similarity_score DECIMAL(5,2) DEFAULT 70.0
) RETURNS TABLE (
    master_id UUID,
    duplicate_id UUID,
    similarity_score DECIMAL(5,2),
    matching_fields TEXT[]
) AS $$
BEGIN
    RETURN QUERY
    WITH contact_pairs AS (
        SELECT 
            c1.id as master_id,
            c2.id as duplicate_id,
            calculate_contact_similarity(c1.id, c2.id) as similarity_score
        FROM contacts c1
        CROSS JOIN contacts c2
        WHERE c1.tenant_id = tenant_uuid 
        AND c2.tenant_id = tenant_uuid
        AND c1.id < c2.id  -- Avoid duplicates and self-comparison
        AND c1.contact_status = 'ACTIVE'
        AND c2.contact_status = 'ACTIVE'
    )
    SELECT 
        cp.master_id,
        cp.duplicate_id,
        cp.similarity_score,
        ARRAY['email', 'phone', 'name']::TEXT[] as matching_fields
    FROM contact_pairs cp
    WHERE cp.similarity_score >= min_similarity_score
    ORDER BY cp.similarity_score DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to merge duplicate contacts
CREATE OR REPLACE FUNCTION merge_contacts(
    master_contact_id UUID,
    duplicate_contact_id UUID,
    merge_strategy JSONB DEFAULT '{}'::JSONB
) RETURNS BOOLEAN AS $$
DECLARE
    master_contact RECORD;
    duplicate_contact RECORD;
BEGIN
    -- Get contact records
    SELECT * INTO master_contact FROM contacts WHERE id = master_contact_id;
    SELECT * INTO duplicate_contact FROM contacts WHERE id = duplicate_contact_id;
    
    IF master_contact IS NULL OR duplicate_contact IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Update master contact with merged data
    UPDATE contacts SET
        email = COALESCE(master_contact.email, duplicate_contact.email),
        phone = COALESCE(master_contact.phone, duplicate_contact.phone),
        mobile = COALESCE(master_contact.mobile, duplicate_contact.mobile),
        title = COALESCE(master_contact.title, duplicate_contact.title),
        department = COALESCE(master_contact.department, duplicate_contact.department),
        mailing_address = COALESCE(master_contact.mailing_address, duplicate_contact.mailing_address),
        social_profiles = master_contact.social_profiles || duplicate_contact.social_profiles,
        lead_score = GREATEST(master_contact.lead_score, duplicate_contact.lead_score),
        tags = array(SELECT DISTINCT unnest(master_contact.tags || duplicate_contact.tags)),
        custom_fields = master_contact.custom_fields || duplicate_contact.custom_fields,
        notes = CASE 
            WHEN master_contact.notes IS NOT NULL AND duplicate_contact.notes IS NOT NULL 
            THEN master_contact.notes || E'\n\n--- MERGED FROM DUPLICATE ---\n' || duplicate_contact.notes
            ELSE COALESCE(master_contact.notes, duplicate_contact.notes)
        END,
        updated_at = NOW(),
        updated_by = current_setting('app.current_user_id', true)::UUID
    WHERE id = master_contact_id;
    
    -- Update references to point to master contact
    UPDATE contact_activities SET contact_id = master_contact_id WHERE contact_id = duplicate_contact_id;
    UPDATE contact_relationships SET contact_id = master_contact_id WHERE contact_id = duplicate_contact_id;
    UPDATE contact_relationships SET related_contact_id = master_contact_id WHERE related_contact_id = duplicate_contact_id;
    
    -- Mark duplicate as merged
    UPDATE contacts SET 
        contact_status = 'INACTIVE',
        notes = COALESCE(notes, '') || E'\n\n--- MERGED INTO CONTACT: ' || master_contact_id || ' ---',
        updated_at = NOW(),
        updated_by = current_setting('app.current_user_id', true)::UUID
    WHERE id = duplicate_contact_id;
    
    -- Update duplicate record status
    UPDATE contact_duplicates SET 
        status = 'MERGED',
        reviewed_by = current_setting('app.current_user_id', true)::UUID,
        reviewed_at = NOW(),
        merge_strategy = merge_strategy
    WHERE master_contact_id = master_contact_id AND duplicate_contact_id = duplicate_contact_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 10. MONITORING VIEWS
-- =====================================================

-- View for contact statistics
CREATE OR REPLACE VIEW contact_statistics AS
SELECT 
    tenant_id,
    COUNT(*) as total_contacts,
    COUNT(*) FILTER (WHERE contact_status = 'ACTIVE') as active_contacts,
    COUNT(*) FILTER (WHERE contact_status = 'INACTIVE') as inactive_contacts,
    COUNT(*) FILTER (WHERE email IS NOT NULL) as contacts_with_email,
    COUNT(*) FILTER (WHERE phone IS NOT NULL OR mobile IS NOT NULL) as contacts_with_phone,
    COUNT(*) FILTER (WHERE account_id IS NOT NULL) as contacts_with_account,
    AVG(lead_score) as avg_lead_score,
    COUNT(*) FILTER (WHERE lead_score >= 70) as high_score_contacts,
    COUNT(*) FILTER (WHERE created_at >= CURRENT_DATE - INTERVAL '30 days') as new_contacts_30d,
    COUNT(*) FILTER (WHERE updated_at >= CURRENT_DATE - INTERVAL '7 days') as updated_contacts_7d
FROM contacts
GROUP BY tenant_id;

-- View for contact activity summary
CREATE OR REPLACE VIEW contact_activity_summary AS
SELECT 
    c.tenant_id,
    c.id as contact_id,
    c.first_name,
    c.last_name,
    c.email,
    COUNT(ca.id) as total_activities,
    COUNT(ca.id) FILTER (WHERE ca.activity_type = 'CALL') as calls_count,
    COUNT(ca.id) FILTER (WHERE ca.activity_type = 'EMAIL') as emails_count,
    COUNT(ca.id) FILTER (WHERE ca.activity_type = 'MEETING') as meetings_count,
    MAX(ca.activity_date) as last_activity_date,
    COUNT(ca.id) FILTER (WHERE ca.activity_date >= CURRENT_DATE - INTERVAL '30 days') as activities_30d
FROM contacts c
LEFT JOIN contact_activities ca ON c.id = ca.contact_id
WHERE c.contact_status = 'ACTIVE'
GROUP BY c.tenant_id, c.id, c.first_name, c.last_name, c.email;

-- =====================================================
-- 11. MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to clean up inactive contacts
CREATE OR REPLACE FUNCTION cleanup_inactive_contacts(
    retention_days INTEGER DEFAULT 365
) RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date TIMESTAMP WITH TIME ZONE;
BEGIN
    cutoff_date := NOW() - (retention_days || ' days')::INTERVAL;
    
    -- Delete contacts that have been inactive for the retention period
    DELETE FROM contacts 
    WHERE contact_status = 'INACTIVE' 
    AND updated_at < cutoff_date;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to update contact lead scores based on activities
CREATE OR REPLACE FUNCTION update_contact_lead_scores(
    tenant_uuid UUID DEFAULT NULL
) RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
    contact_record RECORD;
    new_score INTEGER;
BEGIN
    updated_count := 0;
    
    FOR contact_record IN 
        SELECT c.id, c.tenant_id, c.lead_score,
               COUNT(ca.id) as activity_count,
               COUNT(ca.id) FILTER (WHERE ca.activity_date >= CURRENT_DATE - INTERVAL '30 days') as recent_activities,
               COUNT(ca.id) FILTER (WHERE ca.activity_type = 'MEETING') as meetings_count
        FROM contacts c
        LEFT JOIN contact_activities ca ON c.id = ca.contact_id
        WHERE c.contact_status = 'ACTIVE'
        AND (tenant_uuid IS NULL OR c.tenant_id = tenant_uuid)
        GROUP BY c.id, c.tenant_id, c.lead_score
    LOOP
        -- Calculate new score based on activities
        new_score := LEAST(100, 
            GREATEST(0, 
                contact_record.lead_score + 
                (contact_record.recent_activities * 5) + 
                (contact_record.meetings_count * 10)
            )
        );
        
        -- Update if score changed
        IF new_score != contact_record.lead_score THEN
            UPDATE contacts 
            SET lead_score = new_score, 
                updated_at = NOW(),
                updated_by = '00000000-0000-0000-0000-000000000000'::UUID
            WHERE id = contact_record.id;
            
            updated_count := updated_count + 1;
        END IF;
    END LOOP;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE contact_activities IS 'Activity tracking for contacts including calls, emails, meetings, etc.';
COMMENT ON TABLE contact_duplicates IS 'Potential duplicate contact pairs with similarity scores';
COMMENT ON FUNCTION calculate_contact_similarity(UUID, UUID) IS 'Calculates similarity score between two contacts';
COMMENT ON FUNCTION find_duplicate_contacts(UUID, DECIMAL) IS 'Finds potential duplicate contacts above similarity threshold';
COMMENT ON FUNCTION merge_contacts(UUID, UUID, JSONB) IS 'Merges duplicate contact into master contact';
COMMENT ON FUNCTION update_contact_lead_scores(UUID) IS 'Updates contact lead scores based on recent activities';