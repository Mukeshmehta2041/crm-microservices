-- =====================================================
-- CUSTOM OBJECTS SERVICE - COMPREHENSIVE SCHEMA ENHANCEMENTS
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
-- 1. CREATE AUDIT LOG TABLE FOR CUSTOM OBJECTS SERVICE
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

-- Enable RLS on existing tables
ALTER TABLE custom_objects ENABLE ROW LEVEL SECURITY;
ALTER TABLE custom_fields ENABLE ROW LEVEL SECURITY;
ALTER TABLE custom_field_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE custom_object_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE custom_object_relationships ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policies
CREATE POLICY tenant_isolation_custom_objects ON custom_objects
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_custom_fields ON custom_fields
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_custom_field_options ON custom_field_options
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_custom_object_records ON custom_object_records
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY tenant_isolation_custom_object_relationships ON custom_object_relationships
    USING (tenant_id = get_current_tenant_id());

-- =====================================================
-- 4. ADD AUDIT TRIGGERS TO EXISTING TABLES
-- =====================================================

-- Add audit triggers to all existing tables
CREATE TRIGGER audit_custom_objects_trigger
    AFTER INSERT OR UPDATE OR DELETE ON custom_objects
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_custom_fields_trigger
    AFTER INSERT OR UPDATE OR DELETE ON custom_fields
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_custom_field_options_trigger
    AFTER INSERT OR UPDATE OR DELETE ON custom_field_options
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_custom_object_records_trigger
    AFTER INSERT OR UPDATE OR DELETE ON custom_object_records
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_custom_object_relationships_trigger
    AFTER INSERT OR UPDATE OR DELETE ON custom_object_relationships
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 5. CREATE CUSTOM OBJECT PERMISSIONS TABLE
-- =====================================================

CREATE TABLE custom_object_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    custom_object_id UUID NOT NULL REFERENCES custom_objects(id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,
    permission_level VARCHAR(20) NOT NULL,
    user_id UUID,
    role_name VARCHAR(100),
    field_permissions JSONB DEFAULT '{}',
    record_filters JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_permission_type CHECK (permission_type IN (
        'READ', 'CREATE', 'UPDATE', 'DELETE', 'ADMIN'
    )),
    CONSTRAINT valid_permission_level CHECK (permission_level IN (
        'NONE', 'OWN', 'TEAM', 'ALL'
    )),
    CONSTRAINT user_or_role_required CHECK (
        (user_id IS NOT NULL AND role_name IS NULL) OR 
        (user_id IS NULL AND role_name IS NOT NULL)
    ),
    CONSTRAINT uk_object_permission UNIQUE (tenant_id, custom_object_id, permission_type, user_id, role_name)
);

-- Indexes for custom_object_permissions table
CREATE INDEX idx_custom_object_permissions_tenant ON custom_object_permissions(tenant_id);
CREATE INDEX idx_custom_object_permissions_object ON custom_object_permissions(custom_object_id);
CREATE INDEX idx_custom_object_permissions_user ON custom_object_permissions(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_custom_object_permissions_role ON custom_object_permissions(role_name) WHERE role_name IS NOT NULL;
CREATE INDEX idx_custom_object_permissions_type ON custom_object_permissions(tenant_id, permission_type);
CREATE INDEX idx_custom_object_permissions_active ON custom_object_permissions(tenant_id, is_active);

-- Enable RLS and add audit trigger for custom_object_permissions
ALTER TABLE custom_object_permissions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_custom_object_permissions ON custom_object_permissions
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_custom_object_permissions_trigger
    AFTER INSERT OR UPDATE OR DELETE ON custom_object_permissions
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 6. CREATE CUSTOM OBJECT WORKFLOWS TABLE
-- =====================================================

CREATE TABLE custom_object_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    custom_object_id UUID NOT NULL REFERENCES custom_objects(id) ON DELETE CASCADE,
    workflow_name VARCHAR(255) NOT NULL,
    workflow_description TEXT,
    trigger_type VARCHAR(50) NOT NULL,
    trigger_conditions JSONB NOT NULL DEFAULT '{}',
    workflow_actions JSONB NOT NULL DEFAULT '[]',
    is_active BOOLEAN DEFAULT TRUE,
    execution_order INTEGER DEFAULT 0,
    last_executed_at TIMESTAMP WITH TIME ZONE,
    execution_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_trigger_type CHECK (trigger_type IN (
        'ON_CREATE', 'ON_UPDATE', 'ON_DELETE', 'ON_FIELD_CHANGE', 'SCHEDULED', 'MANUAL'
    )),
    CONSTRAINT valid_execution_order CHECK (execution_order >= 0),
    CONSTRAINT uk_object_workflow_name UNIQUE (tenant_id, custom_object_id, workflow_name)
);

-- Indexes for custom_object_workflows table
CREATE INDEX idx_custom_object_workflows_tenant ON custom_object_workflows(tenant_id);
CREATE INDEX idx_custom_object_workflows_object ON custom_object_workflows(custom_object_id);
CREATE INDEX idx_custom_object_workflows_trigger ON custom_object_workflows(tenant_id, trigger_type);
CREATE INDEX idx_custom_object_workflows_active ON custom_object_workflows(tenant_id, is_active);
CREATE INDEX idx_custom_object_workflows_order ON custom_object_workflows(custom_object_id, execution_order);

-- Enable RLS and add audit trigger for custom_object_workflows
ALTER TABLE custom_object_workflows ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_custom_object_workflows ON custom_object_workflows
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_custom_object_workflows_trigger
    AFTER INSERT OR UPDATE OR DELETE ON custom_object_workflows
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 7. CREATE CUSTOM OBJECT INDEXES TABLE
-- =====================================================

CREATE TABLE custom_object_indexes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    custom_object_id UUID NOT NULL REFERENCES custom_objects(id) ON DELETE CASCADE,
    index_name VARCHAR(255) NOT NULL,
    index_type VARCHAR(50) DEFAULT 'BTREE',
    indexed_fields JSONB NOT NULL,
    index_conditions JSONB DEFAULT '{}',
    is_unique BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    performance_stats JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    CONSTRAINT valid_index_type CHECK (index_type IN (
        'BTREE', 'GIN', 'GIST', 'HASH', 'PARTIAL'
    )),
    CONSTRAINT uk_object_index_name UNIQUE (tenant_id, custom_object_id, index_name)
);

-- Indexes for custom_object_indexes table
CREATE INDEX idx_custom_object_indexes_tenant ON custom_object_indexes(tenant_id);
CREATE INDEX idx_custom_object_indexes_object ON custom_object_indexes(custom_object_id);
CREATE INDEX idx_custom_object_indexes_type ON custom_object_indexes(tenant_id, index_type);
CREATE INDEX idx_custom_object_indexes_active ON custom_object_indexes(tenant_id, is_active);

-- Enable RLS and add audit trigger for custom_object_indexes
ALTER TABLE custom_object_indexes ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_custom_object_indexes ON custom_object_indexes
    USING (tenant_id = get_current_tenant_id());

CREATE TRIGGER audit_custom_object_indexes_trigger
    AFTER INSERT OR UPDATE OR DELETE ON custom_object_indexes
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 8. ENHANCED INDEXES FOR PERFORMANCE
-- =====================================================

-- Additional composite indexes for custom_objects table
CREATE INDEX idx_custom_objects_tenant_active ON custom_objects(tenant_id, is_active);
CREATE INDEX idx_custom_objects_tenant_type ON custom_objects(tenant_id, object_type) WHERE object_type IS NOT NULL;
CREATE INDEX idx_custom_objects_api_name ON custom_objects(tenant_id, api_name);

-- Additional indexes for custom_fields table
CREATE INDEX idx_custom_fields_object_active ON custom_fields(custom_object_id, is_active);
CREATE INDEX idx_custom_fields_tenant_type ON custom_fields(tenant_id, field_type);
CREATE INDEX idx_custom_fields_required ON custom_fields(custom_object_id, is_required) WHERE is_required = true;
CREATE INDEX idx_custom_fields_searchable ON custom_fields(custom_object_id, is_searchable) WHERE is_searchable = true;

-- Additional indexes for custom_object_records table
CREATE INDEX idx_custom_object_records_tenant_object ON custom_object_records(tenant_id, custom_object_id);
CREATE INDEX idx_custom_object_records_owner ON custom_object_records(owner_id) WHERE owner_id IS NOT NULL;
CREATE INDEX idx_custom_object_records_status ON custom_object_records(tenant_id, status) WHERE status IS NOT NULL;

-- GIN indexes for JSONB fields
CREATE INDEX idx_custom_object_records_data_gin ON custom_object_records USING GIN(record_data);
CREATE INDEX idx_custom_fields_validation_gin ON custom_fields USING GIN(validation_rules) WHERE validation_rules != '{}'::jsonb;
CREATE INDEX idx_custom_object_workflows_conditions_gin ON custom_object_workflows USING GIN(trigger_conditions);
CREATE INDEX idx_custom_object_workflows_actions_gin ON custom_object_workflows USING GIN(workflow_actions);

-- =====================================================
-- 9. VALIDATION FUNCTIONS
-- =====================================================

-- JSON schema validation function for custom fields
CREATE OR REPLACE FUNCTION validate_custom_field_data(
    field_definition JSONB,
    field_value JSONB
) RETURNS BOOLEAN AS $$
DECLARE
    field_type TEXT;
    is_required BOOLEAN;
    validation_rules JSONB;
    min_length INTEGER;
    max_length INTEGER;
    min_value DECIMAL;
    max_value DECIMAL;
    allowed_values JSONB;
BEGIN
    -- Extract field properties
    field_type := field_definition->>'field_type';
    is_required := COALESCE((field_definition->>'is_required')::BOOLEAN, false);
    validation_rules := COALESCE(field_definition->'validation_rules', '{}'::JSONB);
    
    -- Check if required field is present
    IF is_required AND (field_value IS NULL OR field_value = 'null'::JSONB) THEN
        RETURN FALSE;
    END IF;
    
    -- Skip validation if field is null and not required
    IF field_value IS NULL OR field_value = 'null'::JSONB THEN
        RETURN TRUE;
    END IF;
    
    -- Validate based on field type
    CASE field_type
        WHEN 'TEXT', 'TEXTAREA', 'EMAIL', 'URL' THEN
            IF jsonb_typeof(field_value) != 'string' THEN
                RETURN FALSE;
            END IF;
            
            -- Length validation
            min_length := (validation_rules->>'min_length')::INTEGER;
            max_length := (validation_rules->>'max_length')::INTEGER;
            
            IF min_length IS NOT NULL AND length(field_value #>> '{}') < min_length THEN
                RETURN FALSE;
            END IF;
            
            IF max_length IS NOT NULL AND length(field_value #>> '{}') > max_length THEN
                RETURN FALSE;
            END IF;
            
            -- Email validation
            IF field_type = 'EMAIL' AND NOT (field_value #>> '{}') ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' THEN
                RETURN FALSE;
            END IF;
            
            -- URL validation
            IF field_type = 'URL' AND NOT (field_value #>> '{}') ~ '^https?://[^\s/$.?#].[^\s]*$' THEN
                RETURN FALSE;
            END IF;
            
        WHEN 'NUMBER', 'CURRENCY' THEN
            IF jsonb_typeof(field_value) != 'number' THEN
                RETURN FALSE;
            END IF;
            
            -- Range validation
            min_value := (validation_rules->>'min_value')::DECIMAL;
            max_value := (validation_rules->>'max_value')::DECIMAL;
            
            IF min_value IS NOT NULL AND (field_value #>> '{}')::DECIMAL < min_value THEN
                RETURN FALSE;
            END IF;
            
            IF max_value IS NOT NULL AND (field_value #>> '{}')::DECIMAL > max_value THEN
                RETURN FALSE;
            END IF;
            
        WHEN 'BOOLEAN' THEN
            IF jsonb_typeof(field_value) != 'boolean' THEN
                RETURN FALSE;
            END IF;
            
        WHEN 'DATE', 'DATETIME' THEN
            IF jsonb_typeof(field_value) != 'string' THEN
                RETURN FALSE;
            END IF;
            
            -- Date format validation would go here
            
        WHEN 'PICKLIST' THEN
            allowed_values := validation_rules->'allowed_values';
            IF allowed_values IS NOT NULL AND NOT (allowed_values ? (field_value #>> '{}')) THEN
                RETURN FALSE;
            END IF;
            
        WHEN 'MULTI_PICKLIST' THEN
            IF jsonb_typeof(field_value) != 'array' THEN
                RETURN FALSE;
            END IF;
            
            allowed_values := validation_rules->'allowed_values';
            IF allowed_values IS NOT NULL THEN
                -- Check each value in array
                FOR i IN 0..jsonb_array_length(field_value) - 1 LOOP
                    IF NOT (allowed_values ? (field_value ->> i)) THEN
                        RETURN FALSE;
                    END IF;
                END LOOP;
            END IF;
            
        ELSE
            -- Unknown field type, skip validation
            NULL;
    END CASE;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to validate complete custom object record
CREATE OR REPLACE FUNCTION validate_custom_object_record(
    custom_object_id UUID,
    record_data JSONB
) RETURNS BOOLEAN AS $$
DECLARE
    field_record RECORD;
    field_value JSONB;
BEGIN
    -- Validate each field in the custom object
    FOR field_record IN 
        SELECT field_name, field_type, is_required, validation_rules, to_jsonb(cf.*) as field_definition
        FROM custom_fields cf
        WHERE cf.custom_object_id = custom_object_id
        AND cf.is_active = true
    LOOP
        field_value := record_data -> field_record.field_name;
        
        IF NOT validate_custom_field_data(field_record.field_definition, field_value) THEN
            RETURN FALSE;
        END IF;
    END LOOP;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 10. CUSTOM OBJECT MANAGEMENT FUNCTIONS
-- =====================================================

-- Function to create dynamic indexes for custom objects
CREATE OR REPLACE FUNCTION create_custom_object_index(
    custom_object_id UUID,
    index_name TEXT,
    indexed_fields JSONB,
    index_type TEXT DEFAULT 'BTREE',
    is_unique BOOLEAN DEFAULT FALSE
) RETURNS BOOLEAN AS $$
DECLARE
    object_record RECORD;
    index_sql TEXT;
    field_path TEXT;
    field_paths TEXT[] := ARRAY[]::TEXT[];
    field_record JSONB;
BEGIN
    -- Get custom object information
    SELECT * INTO object_record FROM custom_objects WHERE id = custom_object_id;
    
    IF object_record IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Build field paths for the index
    FOR field_record IN SELECT * FROM jsonb_array_elements(indexed_fields) LOOP
        field_path := format('(record_data->>%L)', field_record #>> '{}');
        field_paths := array_append(field_paths, field_path);
    END LOOP;
    
    -- Build index SQL
    index_sql := format('CREATE %s INDEX %I ON custom_object_records %s (%s) WHERE custom_object_id = %L',
        CASE WHEN is_unique THEN 'UNIQUE' ELSE '' END,
        'idx_custom_' || object_record.api_name || '_' || index_name,
        CASE WHEN index_type = 'GIN' THEN 'USING GIN' ELSE '' END,
        array_to_string(field_paths, ', '),
        custom_object_id
    );
    
    -- Execute the index creation
    BEGIN
        EXECUTE index_sql;
        
        -- Record the index in our tracking table
        INSERT INTO custom_object_indexes (
            tenant_id, custom_object_id, index_name, index_type,
            indexed_fields, is_unique, created_by, updated_by
        ) VALUES (
            object_record.tenant_id, custom_object_id, index_name, index_type,
            indexed_fields, is_unique,
            current_setting('app.current_user_id', true)::UUID,
            current_setting('app.current_user_id', true)::UUID
        );
        
        RETURN TRUE;
    EXCEPTION
        WHEN OTHERS THEN
            RAISE WARNING 'Failed to create custom object index: %', SQLERRM;
            RETURN FALSE;
    END;
END;
$$ LANGUAGE plpgsql;

-- Function to execute custom object workflows
CREATE OR REPLACE FUNCTION execute_custom_object_workflows(
    custom_object_id UUID,
    trigger_type TEXT,
    record_id UUID,
    old_data JSONB DEFAULT NULL,
    new_data JSONB DEFAULT NULL
) RETURNS INTEGER AS $$
DECLARE
    workflow_record RECORD;
    executed_count INTEGER := 0;
    conditions_met BOOLEAN;
BEGIN
    -- Get active workflows for this trigger type
    FOR workflow_record IN 
        SELECT * FROM custom_object_workflows
        WHERE custom_object_id = custom_object_id
        AND trigger_type = trigger_type
        AND is_active = true
        ORDER BY execution_order
    LOOP
        -- Check if trigger conditions are met
        -- This is a simplified version - real implementation would need
        -- more sophisticated condition evaluation
        conditions_met := true;
        
        IF conditions_met THEN
            -- Execute workflow actions
            -- This would need to be implemented based on action types
            -- For now, just update execution statistics
            
            UPDATE custom_object_workflows
            SET execution_count = execution_count + 1,
                last_executed_at = NOW()
            WHERE id = workflow_record.id;
            
            executed_count := executed_count + 1;
        END IF;
    END LOOP;
    
    RETURN executed_count;
END;
$$ LANGUAGE plpgsql;

-- Function to get custom object statistics
CREATE OR REPLACE FUNCTION get_custom_object_statistics(
    tenant_uuid UUID,
    custom_object_id UUID DEFAULT NULL
) RETURNS TABLE (
    object_id UUID,
    object_name TEXT,
    total_records BIGINT,
    active_records BIGINT,
    total_fields INTEGER,
    required_fields INTEGER,
    total_workflows INTEGER,
    active_workflows INTEGER,
    avg_record_size DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        co.id as object_id,
        co.object_name::TEXT,
        COUNT(cor.id) as total_records,
        COUNT(cor.id) FILTER (WHERE cor.status = 'ACTIVE') as active_records,
        COUNT(DISTINCT cf.id)::INTEGER as total_fields,
        COUNT(DISTINCT cf.id) FILTER (WHERE cf.is_required = true)::INTEGER as required_fields,
        COUNT(DISTINCT cow.id)::INTEGER as total_workflows,
        COUNT(DISTINCT cow.id) FILTER (WHERE cow.is_active = true)::INTEGER as active_workflows,
        AVG(octet_length(cor.record_data::TEXT))::DECIMAL as avg_record_size
    FROM custom_objects co
    LEFT JOIN custom_object_records cor ON co.id = cor.custom_object_id
    LEFT JOIN custom_fields cf ON co.id = cf.custom_object_id
    LEFT JOIN custom_object_workflows cow ON co.id = cow.custom_object_id
    WHERE co.tenant_id = tenant_uuid
    AND (custom_object_id IS NULL OR co.id = custom_object_id)
    AND co.is_active = true
    GROUP BY co.id, co.object_name
    ORDER BY co.object_name;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 11. MONITORING VIEWS
-- =====================================================

-- View for custom object usage statistics
CREATE OR REPLACE VIEW custom_object_usage_stats AS
SELECT 
    co.tenant_id,
    co.id as custom_object_id,
    co.object_name,
    co.api_name,
    COUNT(cor.id) as total_records,
    COUNT(cor.id) FILTER (WHERE cor.created_at >= CURRENT_DATE - INTERVAL '30 days') as new_records_30d,
    COUNT(cor.id) FILTER (WHERE cor.updated_at >= CURRENT_DATE - INTERVAL '7 days') as updated_records_7d,
    COUNT(DISTINCT cf.id) as total_fields,
    COUNT(DISTINCT cow.id) as total_workflows,
    AVG(octet_length(cor.record_data::TEXT)) as avg_record_size_bytes
FROM custom_objects co
LEFT JOIN custom_object_records cor ON co.id = cor.custom_object_id
LEFT JOIN custom_fields cf ON co.id = cf.custom_object_id AND cf.is_active = true
LEFT JOIN custom_object_workflows cow ON co.id = cow.custom_object_id AND cow.is_active = true
WHERE co.is_active = true
GROUP BY co.tenant_id, co.id, co.object_name, co.api_name;

-- View for field usage analysis
CREATE OR REPLACE VIEW custom_field_usage_analysis AS
SELECT 
    cf.tenant_id,
    cf.custom_object_id,
    co.object_name,
    cf.field_name,
    cf.field_type,
    cf.is_required,
    COUNT(cor.id) as total_records,
    COUNT(cor.id) FILTER (WHERE cor.record_data ? cf.field_name) as records_with_value,
    CASE 
        WHEN COUNT(cor.id) > 0 
        THEN (COUNT(cor.id) FILTER (WHERE cor.record_data ? cf.field_name)::DECIMAL / COUNT(cor.id) * 100)
        ELSE 0 
    END as usage_percentage
FROM custom_fields cf
JOIN custom_objects co ON cf.custom_object_id = co.id
LEFT JOIN custom_object_records cor ON cf.custom_object_id = cor.custom_object_id
WHERE cf.is_active = true AND co.is_active = true
GROUP BY cf.tenant_id, cf.custom_object_id, co.object_name, cf.field_name, cf.field_type, cf.is_required;

-- =====================================================
-- 12. MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to clean up unused custom field options
CREATE OR REPLACE FUNCTION cleanup_unused_field_options()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Delete options for inactive fields
    DELETE FROM custom_field_options cfo
    WHERE NOT EXISTS (
        SELECT 1 FROM custom_fields cf 
        WHERE cf.id = cfo.custom_field_id AND cf.is_active = true
    );
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to optimize custom object record storage
CREATE OR REPLACE FUNCTION optimize_custom_object_storage(
    custom_object_id UUID
) RETURNS TEXT AS $$
DECLARE
    result_message TEXT;
    record_count BIGINT;
    avg_size DECIMAL;
BEGIN
    -- Get current statistics
    SELECT COUNT(*), AVG(octet_length(record_data::TEXT))
    INTO record_count, avg_size
    FROM custom_object_records
    WHERE custom_object_id = custom_object_id;
    
    -- Vacuum and analyze the table
    EXECUTE 'VACUUM ANALYZE custom_object_records';
    
    result_message := format('Optimized custom object %s: %s records, avg size %s bytes', 
        custom_object_id, record_count, round(avg_size, 2));
    
    RETURN result_message;
END;
$$ LANGUAGE plpgsql;

-- Add trigger to validate custom object records on insert/update
CREATE OR REPLACE FUNCTION custom_object_record_validation_trigger()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate record data against custom field definitions
    IF NOT validate_custom_object_record(NEW.custom_object_id, NEW.record_data) THEN
        RAISE EXCEPTION 'Custom object record validation failed';
    END IF;
    
    -- Execute workflows
    IF TG_OP = 'INSERT' THEN
        PERFORM execute_custom_object_workflows(
            NEW.custom_object_id, 'ON_CREATE', NEW.id, NULL, NEW.record_data
        );
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM execute_custom_object_workflows(
            NEW.custom_object_id, 'ON_UPDATE', NEW.id, OLD.record_data, NEW.record_data
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER custom_object_record_validation_trigger
    BEFORE INSERT OR UPDATE ON custom_object_records
    FOR EACH ROW EXECUTE FUNCTION custom_object_record_validation_trigger();

-- Add comments for documentation
COMMENT ON TABLE custom_object_permissions IS 'Permission management for custom objects with field-level and record-level controls';
COMMENT ON TABLE custom_object_workflows IS 'Workflow automation for custom objects with trigger-based actions';
COMMENT ON TABLE custom_object_indexes IS 'Index management for custom object performance optimization';
COMMENT ON FUNCTION validate_custom_field_data(JSONB, JSONB) IS 'Validates custom field data against field definition and rules';
COMMENT ON FUNCTION validate_custom_object_record(UUID, JSONB) IS 'Validates complete custom object record against all field definitions';
COMMENT ON FUNCTION create_custom_object_index(UUID, TEXT, JSONB, TEXT, BOOLEAN) IS 'Creates dynamic database indexes for custom object fields';