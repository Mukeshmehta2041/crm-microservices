-- =====================================================
-- DATABASE PERFORMANCE OPTIMIZATION
-- =====================================================
-- This file contains comprehensive performance optimization
-- procedures, query tuning, and monitoring functions

-- =====================================================
-- 1. QUERY PERFORMANCE ANALYSIS FUNCTIONS
-- =====================================================

-- Function to analyze slow queries
CREATE OR REPLACE FUNCTION analyze_slow_queries(
    min_execution_time_ms INTEGER DEFAULT 1000,
    limit_results INTEGER DEFAULT 20
)
RETURNS TABLE (
    query_hash TEXT,
    query_text TEXT,
    calls BIGINT,
    total_exec_time DOUBLE PRECISION,
    mean_exec_time DOUBLE PRECISION,
    max_exec_time DOUBLE PRECISION,
    rows_returned BIGINT,
    performance_score DECIMAL,
    optimization_priority TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        md5(pss.query) as query_hash,
        pss.query as query_text,
        pss.calls,
        pss.total_exec_time,
        pss.mean_exec_time,
        pss.max_exec_time,
        pss.rows,
        -- Performance score calculation
        (pss.mean_exec_time * pss.calls / 1000.0)::DECIMAL as performance_score,
        CASE 
            WHEN pss.mean_exec_time > 10000 THEN 'CRITICAL'
            WHEN pss.mean_exec_time > 5000 THEN 'HIGH'
            WHEN pss.mean_exec_time > 1000 THEN 'MEDIUM'
            ELSE 'LOW'
        END as optimization_priority
    FROM pg_stat_statements pss
    WHERE pss.mean_exec_time > min_execution_time_ms
    AND pss.calls > 5
    ORDER BY (pss.mean_exec_time * pss.calls) DESC
    LIMIT limit_results;
END;
$$ LANGUAGE plpgsql;

-- Function to analyze index usage efficiency
CREATE OR REPLACE FUNCTION analyze_index_efficiency()
RETURNS TABLE (
    schema_name TEXT,
    table_name TEXT,
    index_name TEXT,
    index_size TEXT,
    index_scans BIGINT,
    tuples_read BIGINT,
    tuples_fetched BIGINT,
    efficiency_ratio DECIMAL,
    usage_category TEXT,
    recommendation TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        psi.schemaname::TEXT,
        psi.tablename::TEXT,
        psi.indexname::TEXT,
        pg_size_pretty(pg_relation_size(psi.indexrelid))::TEXT,
        psi.idx_scan,
        psi.idx_tup_read,
        psi.idx_tup_fetch,
        CASE 
            WHEN psi.idx_tup_read > 0 
            THEN (psi.idx_tup_fetch::DECIMAL / psi.idx_tup_read * 100)
            ELSE 0 
        END as efficiency_ratio,
        CASE 
            WHEN psi.idx_scan = 0 THEN 'UNUSED'
            WHEN psi.idx_scan < 100 THEN 'LOW_USAGE'
            WHEN psi.idx_scan < 1000 THEN 'MODERATE_USAGE'
            ELSE 'HIGH_USAGE'
        END as usage_category,
        CASE 
            WHEN psi.idx_scan = 0 THEN 'Consider dropping this unused index'
            WHEN psi.idx_tup_read > 0 AND (psi.idx_tup_fetch::DECIMAL / psi.idx_tup_read) < 0.1 
            THEN 'Index may be inefficient - review query patterns'
            WHEN psi.idx_scan > 10000 AND pg_relation_size(psi.indexrelid) > 100000000 
            THEN 'High-usage large index - monitor for optimization opportunities'
            ELSE 'Index usage appears optimal'
        END as recommendation
    FROM pg_stat_user_indexes psi
    WHERE psi.schemaname = 'public'
    ORDER BY psi.idx_scan DESC, pg_relation_size(psi.indexrelid) DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to analyze table bloat
CREATE OR REPLACE FUNCTION analyze_table_bloat()
RETURNS TABLE (
    schema_name TEXT,
    table_name TEXT,
    table_size TEXT,
    estimated_bloat_bytes BIGINT,
    estimated_bloat_ratio DECIMAL,
    bloat_category TEXT,
    recommendation TEXT
) AS $$
DECLARE
    table_record RECORD;
    table_stats RECORD;
    bloat_bytes BIGINT;
    bloat_ratio DECIMAL;
BEGIN
    FOR table_record IN 
        SELECT schemaname, tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
    LOOP
        -- Get table statistics
        SELECT 
            pg_total_relation_size(table_record.schemaname||'.'||table_record.tablename) as total_size,
            pg_relation_size(table_record.schemaname||'.'||table_record.tablename) as data_size,
            (SELECT reltuples FROM pg_class WHERE relname = table_record.tablename) as row_count
        INTO table_stats;
        
        -- Estimate bloat (simplified calculation)
        -- In practice, this would use more sophisticated bloat detection
        IF table_stats.row_count > 0 THEN
            bloat_bytes := GREATEST(0, table_stats.data_size - (table_stats.row_count * 100)::BIGINT);
            bloat_ratio := (bloat_bytes::DECIMAL / table_stats.data_size * 100);
        ELSE
            bloat_bytes := 0;
            bloat_ratio := 0;
        END IF;
        
        RETURN QUERY SELECT 
            table_record.schemaname::TEXT,
            table_record.tablename::TEXT,
            pg_size_pretty(table_stats.total_size)::TEXT,
            bloat_bytes,
            bloat_ratio,
            CASE 
                WHEN bloat_ratio > 50 THEN 'HIGH'
                WHEN bloat_ratio > 25 THEN 'MODERATE'
                WHEN bloat_ratio > 10 THEN 'LOW'
                ELSE 'MINIMAL'
            END::TEXT,
            CASE 
                WHEN bloat_ratio > 50 THEN 'Consider VACUUM FULL or table rebuild'
                WHEN bloat_ratio > 25 THEN 'Schedule regular VACUUM operations'
                WHEN bloat_ratio > 10 THEN 'Monitor bloat growth'
                ELSE 'Bloat levels are acceptable'
            END::TEXT;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 2. INDEX OPTIMIZATION FUNCTIONS
-- =====================================================

-- Function to suggest missing indexes
CREATE OR REPLACE FUNCTION suggest_missing_indexes()
RETURNS TABLE (
    table_name TEXT,
    column_names TEXT,
    index_type TEXT,
    estimated_benefit TEXT,
    create_statement TEXT
) AS $$
DECLARE
    table_record RECORD;
    column_record RECORD;
BEGIN
    -- This is a simplified version - real implementation would analyze query patterns
    FOR table_record IN 
        SELECT tablename FROM pg_tables WHERE schemaname = 'public'
    LOOP
        -- Check for foreign key columns without indexes
        FOR column_record IN
            SELECT 
                tc.column_name,
                tc.data_type
            FROM information_schema.table_constraints tco
            JOIN information_schema.key_column_usage kcu 
                ON kcu.constraint_name = tco.constraint_name
            JOIN information_schema.columns tc 
                ON tc.column_name = kcu.column_name 
                AND tc.table_name = kcu.table_name
            WHERE tco.constraint_type = 'FOREIGN KEY'
            AND kcu.table_name = table_record.tablename
            AND NOT EXISTS (
                SELECT 1 FROM pg_indexes 
                WHERE tablename = table_record.tablename 
                AND indexdef LIKE '%' || kcu.column_name || '%'
            )
        LOOP
            RETURN QUERY SELECT 
                table_record.tablename::TEXT,
                column_record.column_name::TEXT,
                'BTREE'::TEXT,
                'HIGH - Foreign key without index'::TEXT,
                format('CREATE INDEX CONCURRENTLY idx_%s_%s ON %s (%s);',
                    table_record.tablename, column_record.column_name,
                    table_record.tablename, column_record.column_name)::TEXT;
        END LOOP;
        
        -- Check for commonly queried columns (tenant_id, created_at, etc.)
        FOR column_record IN
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_name = table_record.tablename
            AND column_name IN ('tenant_id', 'created_at', 'updated_at', 'status', 'owner_id')
            AND NOT EXISTS (
                SELECT 1 FROM pg_indexes 
                WHERE tablename = table_record.tablename 
                AND indexdef LIKE '%' || column_name || '%'
            )
        LOOP
            RETURN QUERY SELECT 
                table_record.tablename::TEXT,
                column_record.column_name::TEXT,
                'BTREE'::TEXT,
                'MEDIUM - Common query column'::TEXT,
                format('CREATE INDEX CONCURRENTLY idx_%s_%s ON %s (%s);',
                    table_record.tablename, column_record.column_name,
                    table_record.tablename, column_record.column_name)::TEXT;
        END LOOP;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Function to create optimal indexes
CREATE OR REPLACE FUNCTION create_optimal_indexes(
    table_name TEXT,
    dry_run BOOLEAN DEFAULT TRUE
)
RETURNS TABLE (
    action TEXT,
    statement TEXT,
    estimated_impact TEXT
) AS $$
DECLARE
    index_suggestion RECORD;
    execution_result TEXT;
BEGIN
    FOR index_suggestion IN 
        SELECT * FROM suggest_missing_indexes() 
        WHERE suggest_missing_indexes.table_name = create_optimal_indexes.table_name
    LOOP
        IF dry_run THEN
            RETURN QUERY SELECT 
                'DRY_RUN'::TEXT,
                index_suggestion.create_statement,
                index_suggestion.estimated_benefit;
        ELSE
            BEGIN
                EXECUTE index_suggestion.create_statement;
                execution_result := 'SUCCESS';
            EXCEPTION
                WHEN OTHERS THEN
                    execution_result := 'FAILED: ' || SQLERRM;
            END;
            
            RETURN QUERY SELECT 
                'EXECUTED'::TEXT,
                index_suggestion.create_statement,
                execution_result::TEXT;
        END IF;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 3. QUERY OPTIMIZATION FUNCTIONS
-- =====================================================

-- Function to optimize query execution plans
CREATE OR REPLACE FUNCTION analyze_query_plan(
    query_text TEXT
)
RETURNS TABLE (
    plan_line TEXT,
    optimization_notes TEXT
) AS $$
DECLARE
    plan_record RECORD;
    plan_text TEXT;
BEGIN
    -- Get query execution plan
    EXECUTE 'EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) ' || query_text INTO plan_text;
    
    -- Parse and analyze plan (simplified version)
    FOR plan_record IN 
        SELECT unnest(string_to_array(plan_text, E'\n')) as line
    LOOP
        RETURN QUERY SELECT 
            plan_record.line::TEXT,
            CASE 
                WHEN plan_record.line LIKE '%Seq Scan%' THEN 'Consider adding index for sequential scan'
                WHEN plan_record.line LIKE '%Nested Loop%' AND plan_record.line LIKE '%cost=%' 
                THEN 'Nested loop may benefit from index optimization'
                WHEN plan_record.line LIKE '%Sort%' AND plan_record.line LIKE '%cost=%'
                THEN 'Sort operation may benefit from index on sort columns'
                WHEN plan_record.line LIKE '%Hash%' AND plan_record.line LIKE '%cost=%'
                THEN 'Hash operation - consider join optimization'
                ELSE 'Plan step appears optimal'
            END::TEXT;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Function to suggest query optimizations
CREATE OR REPLACE FUNCTION suggest_query_optimizations(
    query_text TEXT
)
RETURNS TABLE (
    optimization_type TEXT,
    suggestion TEXT,
    estimated_impact TEXT
) AS $$
BEGIN
    -- Analyze query structure and suggest optimizations
    
    -- Check for SELECT *
    IF query_text ~* 'SELECT\s+\*' THEN
        RETURN QUERY SELECT 
            'COLUMN_SELECTION'::TEXT,
            'Avoid SELECT * - specify only needed columns'::TEXT,
            'HIGH - Reduces I/O and network traffic'::TEXT;
    END IF;
    
    -- Check for missing WHERE clauses on large tables
    IF query_text ~* 'FROM\s+(contacts|accounts|deals|activities)' 
       AND NOT (query_text ~* 'WHERE') THEN
        RETURN QUERY SELECT 
            'FILTERING'::TEXT,
            'Add WHERE clause to limit result set'::TEXT,
            'CRITICAL - Prevents full table scans'::TEXT;
    END IF;
    
    -- Check for LIKE patterns without leading wildcards
    IF query_text ~* 'LIKE\s+''%' THEN
        RETURN QUERY SELECT 
            'PATTERN_MATCHING'::TEXT,
            'LIKE patterns starting with % cannot use indexes efficiently'::TEXT,
            'MEDIUM - Consider full-text search or trigram indexes'::TEXT;
    END IF;
    
    -- Check for OR conditions that could use UNION
    IF query_text ~* '\sOR\s' AND query_text ~* 'WHERE' THEN
        RETURN QUERY SELECT 
            'LOGICAL_OPERATORS'::TEXT,
            'Consider rewriting OR conditions as UNION for better index usage'::TEXT,
            'MEDIUM - May improve index utilization'::TEXT;
    END IF;
    
    -- Check for subqueries that could be JOINs
    IF query_text ~* 'IN\s*\(\s*SELECT' THEN
        RETURN QUERY SELECT 
            'SUBQUERIES'::TEXT,
            'Consider rewriting IN subqueries as JOINs'::TEXT,
            'MEDIUM - May improve execution plan'::TEXT;
    END IF;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 4. CONNECTION POOLING AND RESOURCE MANAGEMENT
-- =====================================================

-- Function to analyze connection usage
CREATE OR REPLACE FUNCTION analyze_connection_usage()
RETURNS TABLE (
    metric_name TEXT,
    current_value INTEGER,
    max_value INTEGER,
    utilization_percent DECIMAL,
    status TEXT,
    recommendation TEXT
) AS $$
DECLARE
    max_connections INTEGER;
    current_connections INTEGER;
    active_connections INTEGER;
    idle_connections INTEGER;
BEGIN
    -- Get connection statistics
    SELECT setting::INTEGER INTO max_connections 
    FROM pg_settings WHERE name = 'max_connections';
    
    SELECT COUNT(*) INTO current_connections 
    FROM pg_stat_activity;
    
    SELECT COUNT(*) INTO active_connections 
    FROM pg_stat_activity WHERE state = 'active';
    
    SELECT COUNT(*) INTO idle_connections 
    FROM pg_stat_activity WHERE state = 'idle';
    
    -- Return connection metrics
    RETURN QUERY SELECT 
        'total_connections'::TEXT,
        current_connections,
        max_connections,
        (current_connections::DECIMAL / max_connections * 100),
        CASE 
            WHEN current_connections::DECIMAL / max_connections > 0.8 THEN 'WARNING'
            WHEN current_connections::DECIMAL / max_connections > 0.9 THEN 'CRITICAL'
            ELSE 'OK'
        END::TEXT,
        CASE 
            WHEN current_connections::DECIMAL / max_connections > 0.8 
            THEN 'Consider increasing max_connections or implementing connection pooling'
            ELSE 'Connection usage is within acceptable limits'
        END::TEXT;
    
    RETURN QUERY SELECT 
        'active_connections'::TEXT,
        active_connections,
        max_connections,
        (active_connections::DECIMAL / max_connections * 100),
        CASE 
            WHEN active_connections::DECIMAL / max_connections > 0.5 THEN 'WARNING'
            ELSE 'OK'
        END::TEXT,
        CASE 
            WHEN active_connections::DECIMAL / max_connections > 0.5 
            THEN 'High number of active connections - investigate long-running queries'
            ELSE 'Active connection count is acceptable'
        END::TEXT;
    
    RETURN QUERY SELECT 
        'idle_connections'::TEXT,
        idle_connections,
        max_connections,
        (idle_connections::DECIMAL / max_connections * 100),
        CASE 
            WHEN idle_connections > max_connections * 0.3 THEN 'WARNING'
            ELSE 'OK'
        END::TEXT,
        CASE 
            WHEN idle_connections > max_connections * 0.3 
            THEN 'High number of idle connections - check connection pool configuration'
            ELSE 'Idle connection count is acceptable'
        END::TEXT;
END;
$$ LANGUAGE plpgsql;

-- Function to identify long-running queries
CREATE OR REPLACE FUNCTION identify_long_running_queries(
    min_duration_minutes INTEGER DEFAULT 5
)
RETURNS TABLE (
    pid INTEGER,
    duration_minutes INTEGER,
    query_text TEXT,
    state TEXT,
    client_addr INET,
    application_name TEXT,
    recommendation TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        psa.pid,
        EXTRACT(MINUTES FROM (NOW() - psa.query_start))::INTEGER as duration_minutes,
        psa.query as query_text,
        psa.state,
        psa.client_addr,
        psa.application_name,
        CASE 
            WHEN EXTRACT(MINUTES FROM (NOW() - psa.query_start)) > 60 
            THEN 'CRITICAL - Consider terminating this query'
            WHEN EXTRACT(MINUTES FROM (NOW() - psa.query_start)) > 30 
            THEN 'WARNING - Monitor this query closely'
            ELSE 'INFO - Long-running but within acceptable limits'
        END as recommendation
    FROM pg_stat_activity psa
    WHERE psa.state = 'active'
    AND psa.query_start IS NOT NULL
    AND EXTRACT(MINUTES FROM (NOW() - psa.query_start)) >= min_duration_minutes
    AND psa.query NOT LIKE '%pg_stat_activity%'
    ORDER BY psa.query_start;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 5. CACHING STRATEGIES WITH REDIS INTEGRATION
-- =====================================================

-- Function to identify cacheable queries
CREATE OR REPLACE FUNCTION identify_cacheable_queries()
RETURNS TABLE (
    query_hash TEXT,
    query_text TEXT,
    calls BIGINT,
    mean_exec_time DOUBLE PRECISION,
    cache_benefit_score DECIMAL,
    cache_strategy TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        md5(pss.query) as query_hash,
        pss.query as query_text,
        pss.calls,
        pss.mean_exec_time,
        -- Cache benefit score based on frequency and execution time
        (pss.calls * pss.mean_exec_time / 1000.0)::DECIMAL as cache_benefit_score,
        CASE 
            WHEN pss.query ~* 'SELECT.*FROM.*WHERE.*=' AND pss.calls > 100 
            THEN 'QUERY_RESULT_CACHE'
            WHEN pss.query ~* 'COUNT\(\*\)' AND pss.calls > 50 
            THEN 'AGGREGATION_CACHE'
            WHEN pss.query ~* 'SELECT.*FROM.*ORDER BY.*LIMIT' 
            THEN 'PAGINATION_CACHE'
            ELSE 'GENERAL_CACHE'
        END as cache_strategy
    FROM pg_stat_statements pss
    WHERE pss.query ~* '^SELECT'
    AND pss.calls > 10
    AND pss.mean_exec_time > 100
    ORDER BY (pss.calls * pss.mean_exec_time) DESC
    LIMIT 20;
END;
$$ LANGUAGE plpgsql;

-- Function to generate cache configuration
CREATE OR REPLACE FUNCTION generate_cache_config(
    query_hash TEXT
)
RETURNS JSONB AS $$
DECLARE
    query_info RECORD;
    cache_config JSONB;
BEGIN
    SELECT * INTO query_info 
    FROM identify_cacheable_queries() 
    WHERE identify_cacheable_queries.query_hash = generate_cache_config.query_hash;
    
    IF query_info IS NULL THEN
        RETURN '{"error": "Query not found or not cacheable"}'::JSONB;
    END IF;
    
    cache_config := jsonb_build_object(
        'query_hash', query_info.query_hash,
        'cache_strategy', query_info.cache_strategy,
        'ttl_seconds', 
            CASE 
                WHEN query_info.cache_strategy = 'AGGREGATION_CACHE' THEN 3600
                WHEN query_info.cache_strategy = 'QUERY_RESULT_CACHE' THEN 1800
                WHEN query_info.cache_strategy = 'PAGINATION_CACHE' THEN 300
                ELSE 900
            END,
        'cache_key_pattern', 
            CASE 
                WHEN query_info.cache_strategy = 'QUERY_RESULT_CACHE' 
                THEN 'query_result:{tenant_id}:{table}:{conditions_hash}'
                WHEN query_info.cache_strategy = 'AGGREGATION_CACHE' 
                THEN 'aggregation:{tenant_id}:{table}:{function}:{conditions_hash}'
                ELSE 'general:{query_hash}:{params_hash}'
            END,
        'invalidation_triggers', 
            CASE 
                WHEN query_info.query_text ~* 'FROM\s+contacts' THEN '["contact_insert", "contact_update", "contact_delete"]'
                WHEN query_info.query_text ~* 'FROM\s+accounts' THEN '["account_insert", "account_update", "account_delete"]'
                WHEN query_info.query_text ~* 'FROM\s+deals' THEN '["deal_insert", "deal_update", "deal_delete"]'
                ELSE '["generic_data_change"]'
            END::JSONB,
        'estimated_benefit', query_info.cache_benefit_score
    );
    
    RETURN cache_config;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 6. DATABASE SCALING AND PARTITIONING STRATEGIES
-- =====================================================

-- Function to analyze partitioning candidates
CREATE OR REPLACE FUNCTION analyze_partitioning_candidates()
RETURNS TABLE (
    table_name TEXT,
    table_size TEXT,
    row_count BIGINT,
    partitioning_strategy TEXT,
    partition_column TEXT,
    estimated_benefit TEXT
) AS $$
DECLARE
    table_record RECORD;
    table_size_bytes BIGINT;
    row_count_estimate BIGINT;
BEGIN
    FOR table_record IN 
        SELECT schemaname, tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
    LOOP
        -- Get table statistics
        SELECT 
            pg_total_relation_size(table_record.schemaname||'.'||table_record.tablename),
            (SELECT reltuples FROM pg_class WHERE relname = table_record.tablename)
        INTO table_size_bytes, row_count_estimate;
        
        -- Only consider large tables for partitioning
        IF table_size_bytes > 1000000000 OR row_count_estimate > 1000000 THEN
            RETURN QUERY SELECT 
                table_record.tablename::TEXT,
                pg_size_pretty(table_size_bytes)::TEXT,
                row_count_estimate,
                CASE 
                    WHEN EXISTS (
                        SELECT 1 FROM information_schema.columns 
                        WHERE table_name = table_record.tablename 
                        AND column_name = 'created_at'
                    ) THEN 'RANGE_BY_DATE'
                    WHEN EXISTS (
                        SELECT 1 FROM information_schema.columns 
                        WHERE table_name = table_record.tablename 
                        AND column_name = 'tenant_id'
                    ) THEN 'HASH_BY_TENANT'
                    ELSE 'HASH_BY_ID'
                END::TEXT,
                CASE 
                    WHEN EXISTS (
                        SELECT 1 FROM information_schema.columns 
                        WHERE table_name = table_record.tablename 
                        AND column_name = 'created_at'
                    ) THEN 'created_at'
                    WHEN EXISTS (
                        SELECT 1 FROM information_schema.columns 
                        WHERE table_name = table_record.tablename 
                        AND column_name = 'tenant_id'
                    ) THEN 'tenant_id'
                    ELSE 'id'
                END::TEXT,
                CASE 
                    WHEN table_size_bytes > 10000000000 THEN 'HIGH - Significant performance improvement expected'
                    WHEN table_size_bytes > 5000000000 THEN 'MEDIUM - Moderate performance improvement expected'
                    ELSE 'LOW - Minor performance improvement expected'
                END::TEXT;
        END IF;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Function to generate partitioning DDL
CREATE OR REPLACE FUNCTION generate_partitioning_ddl(
    table_name TEXT,
    partition_strategy TEXT,
    partition_column TEXT
)
RETURNS TABLE (
    step_number INTEGER,
    ddl_statement TEXT,
    description TEXT
) AS $$
DECLARE
    step_counter INTEGER := 1;
BEGIN
    -- Step 1: Create partitioned table
    RETURN QUERY SELECT 
        step_counter,
        format('-- Step %s: Create new partitioned table
CREATE TABLE %s_partitioned (LIKE %s INCLUDING ALL) 
PARTITION BY %s (%s);',
            step_counter, table_name, table_name,
            CASE 
                WHEN partition_strategy = 'RANGE_BY_DATE' THEN 'RANGE'
                ELSE 'HASH'
            END,
            partition_column
        ),
        'Create the new partitioned table structure'::TEXT;
    
    step_counter := step_counter + 1;
    
    -- Step 2: Create initial partitions
    IF partition_strategy = 'RANGE_BY_DATE' THEN
        RETURN QUERY SELECT 
            step_counter,
            format('-- Step %s: Create monthly partitions for current and next year
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
BEGIN
    FOR i IN 0..23 LOOP
        start_date := date_trunc(''month'', CURRENT_DATE) + (i || '' months'')::INTERVAL;
        end_date := start_date + INTERVAL ''1 month'';
        partition_name := ''%s_'' || to_char(start_date, ''YYYY_MM'');
        
        EXECUTE format(''CREATE TABLE %%I PARTITION OF %s_partitioned 
                       FOR VALUES FROM (%%L) TO (%%L)'',
                       partition_name, start_date, end_date);
    END LOOP;
END $$;',
                step_counter, table_name, table_name
            ),
            'Create monthly partitions for date-based partitioning'::TEXT;
    ELSE
        RETURN QUERY SELECT 
            step_counter,
            format('-- Step %s: Create hash partitions
DO $$
BEGIN
    FOR i IN 0..15 LOOP
        EXECUTE format(''CREATE TABLE %s_p%%s PARTITION OF %s_partitioned 
                       FOR VALUES WITH (MODULUS 16, REMAINDER %%s)'',
                       i, i);
    END LOOP;
END $$;',
                step_counter, table_name, table_name
            ),
            'Create hash partitions for even data distribution'::TEXT;
    END IF;
    
    step_counter := step_counter + 1;
    
    -- Step 3: Migrate data
    RETURN QUERY SELECT 
        step_counter,
        format('-- Step %s: Migrate data to partitioned table
INSERT INTO %s_partitioned SELECT * FROM %s;',
            step_counter, table_name, table_name
        ),
        'Migrate existing data to the partitioned table'::TEXT;
    
    step_counter := step_counter + 1;
    
    -- Step 4: Swap tables
    RETURN QUERY SELECT 
        step_counter,
        format('-- Step %s: Swap tables (requires downtime)
BEGIN;
ALTER TABLE %s RENAME TO %s_old;
ALTER TABLE %s_partitioned RENAME TO %s;
COMMIT;',
            step_counter, table_name, table_name, table_name, table_name
        ),
        'Swap the old table with the new partitioned table'::TEXT;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. COMPREHENSIVE PERFORMANCE MONITORING
-- =====================================================

-- Function to generate performance report
CREATE OR REPLACE FUNCTION generate_performance_report()
RETURNS TEXT AS $$
DECLARE
    report TEXT;
    slow_query_count INTEGER;
    unused_index_count INTEGER;
    bloated_table_count INTEGER;
    connection_utilization DECIMAL;
BEGIN
    -- Get performance metrics
    SELECT COUNT(*) INTO slow_query_count 
    FROM analyze_slow_queries() WHERE optimization_priority IN ('CRITICAL', 'HIGH');
    
    SELECT COUNT(*) INTO unused_index_count 
    FROM analyze_index_efficiency() WHERE usage_category = 'UNUSED';
    
    SELECT COUNT(*) INTO bloated_table_count 
    FROM analyze_table_bloat() WHERE bloat_category IN ('HIGH', 'MODERATE');
    
    SELECT utilization_percent INTO connection_utilization 
    FROM analyze_connection_usage() WHERE metric_name = 'total_connections';
    
    -- Build report
    report := format('
DATABASE PERFORMANCE REPORT
===========================
Generated: %s

SUMMARY METRICS:
- Slow queries requiring optimization: %s
- Unused indexes: %s  
- Tables with significant bloat: %s
- Connection utilization: %s%%

RECOMMENDATIONS:
%s

DETAILED ANALYSIS:
- Run analyze_slow_queries() for query optimization opportunities
- Run analyze_index_efficiency() for index usage analysis
- Run analyze_table_bloat() for bloat assessment
- Run analyze_partitioning_candidates() for scaling opportunities

NEXT STEPS:
1. Address critical slow queries first
2. Remove unused indexes to reduce maintenance overhead
3. Schedule VACUUM operations for bloated tables
4. Consider connection pooling if utilization > 70%%
5. Implement caching for frequently accessed data
', 
        NOW(),
        slow_query_count,
        unused_index_count,
        bloated_table_count,
        connection_utilization,
        CASE 
            WHEN slow_query_count > 5 THEN '- URGENT: Multiple slow queries need immediate attention'
            WHEN unused_index_count > 10 THEN '- Consider removing unused indexes'
            WHEN bloated_table_count > 3 THEN '- Schedule maintenance for bloated tables'
            WHEN connection_utilization > 80 THEN '- Implement connection pooling'
            ELSE '- Performance appears to be within acceptable parameters'
        END
    );
    
    RETURN report;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 8. AUTOMATED OPTIMIZATION PROCEDURES
-- =====================================================

-- Function to run automated performance optimization
CREATE OR REPLACE FUNCTION run_automated_optimization(
    dry_run BOOLEAN DEFAULT TRUE
)
RETURNS TABLE (
    optimization_type TEXT,
    action_taken TEXT,
    result TEXT
) AS $$
DECLARE
    optimization_record RECORD;
    execution_result TEXT;
BEGIN
    -- Optimize slow queries
    FOR optimization_record IN 
        SELECT * FROM analyze_slow_queries() 
        WHERE optimization_priority = 'CRITICAL'
        LIMIT 5
    LOOP
        IF dry_run THEN
            RETURN QUERY SELECT 
                'SLOW_QUERY'::TEXT,
                'Would analyze and suggest optimizations for: ' || left(optimization_record.query_text, 100),
                'DRY_RUN'::TEXT;
        ELSE
            -- In practice, this would apply automatic optimizations
            RETURN QUERY SELECT 
                'SLOW_QUERY'::TEXT,
                'Analyzed query: ' || left(optimization_record.query_text, 100),
                'ANALYZED'::TEXT;
        END IF;
    END LOOP;
    
    -- Remove unused indexes
    FOR optimization_record IN 
        SELECT * FROM analyze_index_efficiency() 
        WHERE usage_category = 'UNUSED'
        AND index_name NOT LIKE '%_pkey'
        LIMIT 3
    LOOP
        IF dry_run THEN
            RETURN QUERY SELECT 
                'UNUSED_INDEX'::TEXT,
                'Would drop unused index: ' || optimization_record.index_name,
                'DRY_RUN'::TEXT;
        ELSE
            BEGIN
                EXECUTE 'DROP INDEX CONCURRENTLY ' || optimization_record.index_name;
                execution_result := 'DROPPED';
            EXCEPTION
                WHEN OTHERS THEN
                    execution_result := 'FAILED: ' || SQLERRM;
            END;
            
            RETURN QUERY SELECT 
                'UNUSED_INDEX'::TEXT,
                'Dropped unused index: ' || optimization_record.index_name,
                execution_result::TEXT;
        END IF;
    END LOOP;
    
    -- Update table statistics
    IF NOT dry_run THEN
        EXECUTE 'ANALYZE;';
        RETURN QUERY SELECT 
            'STATISTICS'::TEXT,
            'Updated table statistics',
            'COMPLETED'::TEXT;
    ELSE
        RETURN QUERY SELECT 
            'STATISTICS'::TEXT,
            'Would update table statistics',
            'DRY_RUN'::TEXT;
    END IF;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON FUNCTION analyze_slow_queries(INTEGER, INTEGER) IS 'Analyzes slow queries and provides optimization recommendations';
COMMENT ON FUNCTION analyze_index_efficiency() IS 'Analyzes index usage efficiency and provides recommendations';
COMMENT ON FUNCTION analyze_table_bloat() IS 'Analyzes table bloat and provides maintenance recommendations';
COMMENT ON FUNCTION suggest_missing_indexes() IS 'Suggests missing indexes based on table structure and common patterns';
COMMENT ON FUNCTION identify_cacheable_queries() IS 'Identifies queries that would benefit from caching';
COMMENT ON FUNCTION analyze_partitioning_candidates() IS 'Identifies tables that would benefit from partitioning';
COMMENT ON FUNCTION generate_performance_report() IS 'Generates comprehensive performance analysis report';
COMMENT ON FUNCTION run_automated_optimization(BOOLEAN) IS 'Runs automated performance optimization procedures';