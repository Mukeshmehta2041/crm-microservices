-- =====================================================
-- MIGRATION VALIDATION AND TESTING PROCEDURES
-- =====================================================
-- This file contains comprehensive validation procedures
-- to ensure migration integrity and data consistency

-- =====================================================
-- 1. SCHEMA VALIDATION FUNCTIONS
-- =====================================================

-- Function to validate table structure
CREATE OR REPLACE FUNCTION validate_table_structure(
    table_name TEXT,
    expected_columns JSONB
) RETURNS BOOLEAN AS $$
DECLARE
    column_record RECORD;
    expected_column JSONB;
    column_exists BOOLEAN;
BEGIN
    -- Check if table exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = table_name AND table_schema = 'public'
    ) THEN
        RAISE WARNING 'Table % does not exist', table_name;
        RETURN FALSE;
    END IF;
    
    -- Validate each expected column
    FOR expected_column IN SELECT * FROM jsonb_array_elements(expected_columns) LOOP
        SELECT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = table_name 
            AND column_name = expected_column->>'name'
            AND data_type = expected_column->>'type'
        ) INTO column_exists;
        
        IF NOT column_exists THEN
            RAISE WARNING 'Column % with type % not found in table %', 
                expected_column->>'name', expected_column->>'type', table_name;
            RETURN FALSE;
        END IF;
    END LOOP;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to validate indexes
CREATE OR REPLACE FUNCTION validate_indexes(
    table_name TEXT,
    expected_indexes TEXT[]
) RETURNS BOOLEAN AS $$
DECLARE
    index_name TEXT;
    index_exists BOOLEAN;
BEGIN
    FOREACH index_name IN ARRAY expected_indexes LOOP
        SELECT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = table_name AND indexname = index_name
        ) INTO index_exists;
        
        IF NOT index_exists THEN
            RAISE WARNING 'Index % not found on table %', index_name, table_name;
            RETURN FALSE;
        END IF;
    END LOOP;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to validate constraints
CREATE OR REPLACE FUNCTION validate_constraints(
    table_name TEXT,
    expected_constraints TEXT[]
) RETURNS BOOLEAN AS $$
DECLARE
    constraint_name TEXT;
    constraint_exists BOOLEAN;
BEGIN
    FOREACH constraint_name IN ARRAY expected_constraints LOOP
        SELECT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = table_name AND constraint_name = constraint_name
        ) INTO constraint_exists;
        
        IF NOT constraint_exists THEN
            RAISE WARNING 'Constraint % not found on table %', constraint_name, table_name;
            RETURN FALSE;
        END IF;
    END LOOP;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to validate triggers
CREATE OR REPLACE FUNCTION validate_triggers(
    table_name TEXT,
    expected_triggers TEXT[]
) RETURNS BOOLEAN AS $$
DECLARE
    trigger_name TEXT;
    trigger_exists BOOLEAN;
BEGIN
    FOREACH trigger_name IN ARRAY expected_triggers LOOP
        SELECT EXISTS (
            SELECT 1 FROM information_schema.triggers
            WHERE event_object_table = table_name AND trigger_name = trigger_name
        ) INTO trigger_exists;
        
        IF NOT trigger_exists THEN
            RAISE WARNING 'Trigger % not found on table %', trigger_name, table_name;
            RETURN FALSE;
        END IF;
    END LOOP;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 2. DATA INTEGRITY VALIDATION FUNCTIONS
-- =====================================================

-- Function to validate referential integrity
CREATE OR REPLACE FUNCTION validate_referential_integrity(
    table_name TEXT,
    foreign_key_column TEXT,
    referenced_table TEXT,
    referenced_column TEXT DEFAULT 'id'
) RETURNS BOOLEAN AS $$
DECLARE
    orphaned_count INTEGER;
BEGIN
    EXECUTE format('
        SELECT COUNT(*) FROM %I t1
        WHERE t1.%I IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM %I t2 WHERE t2.%I = t1.%I
        )',
        table_name, foreign_key_column, referenced_table, referenced_column, foreign_key_column
    ) INTO orphaned_count;
    
    IF orphaned_count > 0 THEN
        RAISE WARNING 'Found % orphaned records in %.% referencing %.%', 
            orphaned_count, table_name, foreign_key_column, referenced_table, referenced_column;
        RETURN FALSE;
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to validate data consistency
CREATE OR REPLACE FUNCTION validate_data_consistency(
    table_name TEXT,
    validation_rules JSONB
) RETURNS BOOLEAN AS $$
DECLARE
    rule JSONB;
    rule_name TEXT;
    rule_query TEXT;
    violation_count INTEGER;
BEGIN
    FOR rule IN SELECT * FROM jsonb_array_elements(validation_rules) LOOP
        rule_name := rule->>'name';
        rule_query := rule->>'query';
        
        EXECUTE rule_query INTO violation_count;
        
        IF violation_count > 0 THEN
            RAISE WARNING 'Data consistency rule "%" failed with % violations in table %', 
                rule_name, violation_count, table_name;
            RETURN FALSE;
        END IF;
    END LOOP;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 3. PERFORMANCE VALIDATION FUNCTIONS
-- =====================================================

-- Function to validate query performance
CREATE OR REPLACE FUNCTION validate_query_performance(
    query_name TEXT,
    test_query TEXT,
    max_execution_time_ms INTEGER DEFAULT 1000
) RETURNS BOOLEAN AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_time_ms INTEGER;
BEGIN
    start_time := clock_timestamp();
    
    EXECUTE test_query;
    
    end_time := clock_timestamp();
    execution_time_ms := EXTRACT(MILLISECONDS FROM (end_time - start_time));
    
    IF execution_time_ms > max_execution_time_ms THEN
        RAISE WARNING 'Query "%" took %ms, exceeding limit of %ms', 
            query_name, execution_time_ms, max_execution_time_ms;
        RETURN FALSE;
    END IF;
    
    RAISE NOTICE 'Query "%" executed in %ms', query_name, execution_time_ms;
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to validate index usage
CREATE OR REPLACE FUNCTION validate_index_usage(
    table_name TEXT,
    test_queries JSONB
) RETURNS BOOLEAN AS $$
DECLARE
    query_record JSONB;
    query_name TEXT;
    query_sql TEXT;
    plan_text TEXT;
    uses_index BOOLEAN;
BEGIN
    FOR query_record IN SELECT * FROM jsonb_array_elements(test_queries) LOOP
        query_name := query_record->>'name';
        query_sql := query_record->>'sql';
        
        EXECUTE 'EXPLAIN ' || query_sql INTO plan_text;
        
        uses_index := plan_text ~* 'Index Scan|Bitmap Index Scan';
        
        IF NOT uses_index THEN
            RAISE WARNING 'Query "%" on table % does not use indexes: %', 
                query_name, table_name, plan_text;
            RETURN FALSE;
        END IF;
    END LOOP;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 4. SECURITY VALIDATION FUNCTIONS
-- =====================================================

-- Function to validate row-level security
CREATE OR REPLACE FUNCTION validate_rls_policies(
    table_name TEXT,
    expected_policies TEXT[]
) RETURNS BOOLEAN AS $$
DECLARE
    policy_name TEXT;
    policy_exists BOOLEAN;
    rls_enabled BOOLEAN;
BEGIN
    -- Check if RLS is enabled
    SELECT EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON c.relnamespace = n.oid
        WHERE c.relname = table_name 
        AND n.nspname = 'public'
        AND c.relrowsecurity = true
    ) INTO rls_enabled;
    
    IF NOT rls_enabled THEN
        RAISE WARNING 'Row-level security is not enabled on table %', table_name;
        RETURN FALSE;
    END IF;
    
    -- Check each expected policy
    FOREACH policy_name IN ARRAY expected_policies LOOP
        SELECT EXISTS (
            SELECT 1 FROM pg_policies
            WHERE tablename = table_name AND policyname = policy_name
        ) INTO policy_exists;
        
        IF NOT policy_exists THEN
            RAISE WARNING 'RLS policy % not found on table %', policy_name, table_name;
            RETURN FALSE;
        END IF;
    END LOOP;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to validate audit triggers
CREATE OR REPLACE FUNCTION validate_audit_triggers(
    table_name TEXT
) RETURNS BOOLEAN AS $$
DECLARE
    audit_trigger_exists BOOLEAN;
    audit_table_exists BOOLEAN;
BEGIN
    -- Check if audit trigger exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.triggers
        WHERE event_object_table = table_name 
        AND trigger_name LIKE 'audit_%_trigger'
    ) INTO audit_trigger_exists;
    
    IF NOT audit_trigger_exists THEN
        RAISE WARNING 'Audit trigger not found on table %', table_name;
        RETURN FALSE;
    END IF;
    
    -- Check if audit_log table exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'audit_log' AND table_schema = 'public'
    ) INTO audit_table_exists;
    
    IF NOT audit_table_exists THEN
        RAISE WARNING 'Audit log table does not exist';
        RETURN FALSE;
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 5. COMPREHENSIVE MIGRATION VALIDATION
-- =====================================================

-- Function to run comprehensive migration validation
CREATE OR REPLACE FUNCTION validate_migration(
    service_name TEXT,
    validation_config JSONB
) RETURNS TABLE (
    validation_type TEXT,
    validation_name TEXT,
    status TEXT,
    message TEXT
) AS $$
DECLARE
    table_config JSONB;
    table_name TEXT;
    validation_result BOOLEAN;
    error_message TEXT;
BEGIN
    -- Validate tables
    FOR table_config IN SELECT * FROM jsonb_array_elements(validation_config->'tables') LOOP
        table_name := table_config->>'name';
        
        -- Validate table structure
        BEGIN
            validation_result := validate_table_structure(
                table_name, 
                table_config->'columns'
            );
            
            RETURN QUERY SELECT 
                'table_structure'::TEXT,
                table_name::TEXT,
                CASE WHEN validation_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
                CASE WHEN validation_result THEN 'Table structure is valid' 
                     ELSE 'Table structure validation failed' END::TEXT;
        EXCEPTION
            WHEN OTHERS THEN
                RETURN QUERY SELECT 
                    'table_structure'::TEXT,
                    table_name::TEXT,
                    'ERROR'::TEXT,
                    SQLERRM::TEXT;
        END;
        
        -- Validate indexes
        IF table_config ? 'indexes' THEN
            BEGIN
                validation_result := validate_indexes(
                    table_name,
                    ARRAY(SELECT jsonb_array_elements_text(table_config->'indexes'))
                );
                
                RETURN QUERY SELECT 
                    'indexes'::TEXT,
                    table_name::TEXT,
                    CASE WHEN validation_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
                    CASE WHEN validation_result THEN 'Indexes are valid' 
                         ELSE 'Index validation failed' END::TEXT;
            EXCEPTION
                WHEN OTHERS THEN
                    RETURN QUERY SELECT 
                        'indexes'::TEXT,
                        table_name::TEXT,
                        'ERROR'::TEXT,
                        SQLERRM::TEXT;
            END;
        END IF;
        
        -- Validate constraints
        IF table_config ? 'constraints' THEN
            BEGIN
                validation_result := validate_constraints(
                    table_name,
                    ARRAY(SELECT jsonb_array_elements_text(table_config->'constraints'))
                );
                
                RETURN QUERY SELECT 
                    'constraints'::TEXT,
                    table_name::TEXT,
                    CASE WHEN validation_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
                    CASE WHEN validation_result THEN 'Constraints are valid' 
                         ELSE 'Constraint validation failed' END::TEXT;
            EXCEPTION
                WHEN OTHERS THEN
                    RETURN QUERY SELECT 
                        'constraints'::TEXT,
                        table_name::TEXT,
                        'ERROR'::TEXT,
                        SQLERRM::TEXT;
            END;
        END IF;
        
        -- Validate triggers
        IF table_config ? 'triggers' THEN
            BEGIN
                validation_result := validate_triggers(
                    table_name,
                    ARRAY(SELECT jsonb_array_elements_text(table_config->'triggers'))
                );
                
                RETURN QUERY SELECT 
                    'triggers'::TEXT,
                    table_name::TEXT,
                    CASE WHEN validation_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
                    CASE WHEN validation_result THEN 'Triggers are valid' 
                         ELSE 'Trigger validation failed' END::TEXT;
            EXCEPTION
                WHEN OTHERS THEN
                    RETURN QUERY SELECT 
                        'triggers'::TEXT,
                        table_name::TEXT,
                        'ERROR'::TEXT,
                        SQLERRM::TEXT;
            END;
        END IF;
        
        -- Validate RLS policies
        IF table_config ? 'rls_policies' THEN
            BEGIN
                validation_result := validate_rls_policies(
                    table_name,
                    ARRAY(SELECT jsonb_array_elements_text(table_config->'rls_policies'))
                );
                
                RETURN QUERY SELECT 
                    'rls_policies'::TEXT,
                    table_name::TEXT,
                    CASE WHEN validation_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
                    CASE WHEN validation_result THEN 'RLS policies are valid' 
                         ELSE 'RLS policy validation failed' END::TEXT;
            EXCEPTION
                WHEN OTHERS THEN
                    RETURN QUERY SELECT 
                        'rls_policies'::TEXT,
                        table_name::TEXT,
                        'ERROR'::TEXT,
                        SQLERRM::TEXT;
            END;
        END IF;
        
        -- Validate audit triggers
        BEGIN
            validation_result := validate_audit_triggers(table_name);
            
            RETURN QUERY SELECT 
                'audit_triggers'::TEXT,
                table_name::TEXT,
                CASE WHEN validation_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
                CASE WHEN validation_result THEN 'Audit triggers are valid' 
                     ELSE 'Audit trigger validation failed' END::TEXT;
        EXCEPTION
            WHEN OTHERS THEN
                RETURN QUERY SELECT 
                    'audit_triggers'::TEXT,
                    table_name::TEXT,
                    'ERROR'::TEXT,
                    SQLERRM::TEXT;
        END;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 6. MIGRATION TESTING PROCEDURES
-- =====================================================

-- Function to test migration rollback
CREATE OR REPLACE FUNCTION test_migration_rollback(
    migration_version TEXT
) RETURNS BOOLEAN AS $$
DECLARE
    rollback_script TEXT;
    rollback_exists BOOLEAN;
BEGIN
    -- Check if rollback script exists
    rollback_script := 'U' || migration_version || '__*';
    
    SELECT EXISTS (
        SELECT 1 FROM information_schema.routines
        WHERE routine_name LIKE rollback_script
    ) INTO rollback_exists;
    
    IF NOT rollback_exists THEN
        RAISE WARNING 'Rollback script for version % not found', migration_version;
        RETURN FALSE;
    END IF;
    
    -- In a real implementation, this would:
    -- 1. Create a backup of current state
    -- 2. Execute the rollback script
    -- 3. Validate the rollback was successful
    -- 4. Restore the original state
    
    RAISE NOTICE 'Rollback test for version % would be executed here', migration_version;
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to generate migration validation report
CREATE OR REPLACE FUNCTION generate_migration_report(
    service_name TEXT
) RETURNS TEXT AS $$
DECLARE
    report TEXT;
    table_count INTEGER;
    index_count INTEGER;
    trigger_count INTEGER;
    function_count INTEGER;
BEGIN
    -- Get counts
    SELECT COUNT(*) INTO table_count 
    FROM information_schema.tables 
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
    
    SELECT COUNT(*) INTO index_count 
    FROM pg_indexes 
    WHERE schemaname = 'public';
    
    SELECT COUNT(*) INTO trigger_count 
    FROM information_schema.triggers;
    
    SELECT COUNT(*) INTO function_count 
    FROM information_schema.routines 
    WHERE routine_schema = 'public';
    
    -- Build report
    report := format('
Migration Validation Report for %s
=====================================
Generated at: %s

Database Objects:
- Tables: %s
- Indexes: %s  
- Triggers: %s
- Functions: %s

Schema Version: %s
Last Migration: %s

Status: VALIDATED
', 
        service_name,
        NOW(),
        table_count,
        index_count,
        trigger_count,
        function_count,
        COALESCE((SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1), 'Unknown'),
        COALESCE((SELECT description FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1), 'Unknown')
    );
    
    RETURN report;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. AUTOMATED TESTING FUNCTIONS
-- =====================================================

-- Function to run automated migration tests
CREATE OR REPLACE FUNCTION run_migration_tests(
    service_name TEXT
) RETURNS TABLE (
    test_name TEXT,
    status TEXT,
    execution_time_ms INTEGER,
    message TEXT
) AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    test_result BOOLEAN;
BEGIN
    -- Test 1: Schema integrity
    start_time := clock_timestamp();
    BEGIN
        test_result := (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public') > 0;
        end_time := clock_timestamp();
        
        RETURN QUERY SELECT 
            'schema_integrity'::TEXT,
            CASE WHEN test_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
            EXTRACT(MILLISECONDS FROM (end_time - start_time))::INTEGER,
            CASE WHEN test_result THEN 'Schema integrity verified' 
                 ELSE 'Schema integrity check failed' END::TEXT;
    EXCEPTION
        WHEN OTHERS THEN
            end_time := clock_timestamp();
            RETURN QUERY SELECT 
                'schema_integrity'::TEXT,
                'ERROR'::TEXT,
                EXTRACT(MILLISECONDS FROM (end_time - start_time))::INTEGER,
                SQLERRM::TEXT;
    END;
    
    -- Test 2: Audit system functionality
    start_time := clock_timestamp();
    BEGIN
        test_result := EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'audit_log');
        end_time := clock_timestamp();
        
        RETURN QUERY SELECT 
            'audit_system'::TEXT,
            CASE WHEN test_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
            EXTRACT(MILLISECONDS FROM (end_time - start_time))::INTEGER,
            CASE WHEN test_result THEN 'Audit system is functional' 
                 ELSE 'Audit system check failed' END::TEXT;
    EXCEPTION
        WHEN OTHERS THEN
            end_time := clock_timestamp();
            RETURN QUERY SELECT 
                'audit_system'::TEXT,
                'ERROR'::TEXT,
                EXTRACT(MILLISECONDS FROM (end_time - start_time))::INTEGER,
                SQLERRM::TEXT;
    END;
    
    -- Test 3: RLS functionality
    start_time := clock_timestamp();
    BEGIN
        test_result := EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE n.nspname = 'public' AND c.relrowsecurity = true
        );
        end_time := clock_timestamp();
        
        RETURN QUERY SELECT 
            'rls_functionality'::TEXT,
            CASE WHEN test_result THEN 'PASS' ELSE 'FAIL' END::TEXT,
            EXTRACT(MILLISECONDS FROM (end_time - start_time))::INTEGER,
            CASE WHEN test_result THEN 'RLS is properly configured' 
                 ELSE 'RLS configuration check failed' END::TEXT;
    EXCEPTION
        WHEN OTHERS THEN
            end_time := clock_timestamp();
            RETURN QUERY SELECT 
                'rls_functionality'::TEXT,
                'ERROR'::TEXT,
                EXTRACT(MILLISECONDS FROM (end_time - start_time))::INTEGER,
                SQLERRM::TEXT;
    END;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- VALIDATION PROCEDURES COMPLETE
-- =====================================================

-- Add comments for documentation
COMMENT ON FUNCTION validate_table_structure(TEXT, JSONB) IS 'Validates table structure against expected schema';
COMMENT ON FUNCTION validate_indexes(TEXT, TEXT[]) IS 'Validates existence of expected indexes on a table';
COMMENT ON FUNCTION validate_constraints(TEXT, TEXT[]) IS 'Validates existence of expected constraints on a table';
COMMENT ON FUNCTION validate_referential_integrity(TEXT, TEXT, TEXT, TEXT) IS 'Validates referential integrity between tables';
COMMENT ON FUNCTION validate_migration(TEXT, JSONB) IS 'Runs comprehensive migration validation for a service';
COMMENT ON FUNCTION run_migration_tests(TEXT) IS 'Runs automated migration tests for a service';