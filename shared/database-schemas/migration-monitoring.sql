-- =====================================================
-- MIGRATION MONITORING AND ALERTING SETUP
-- =====================================================
-- This file contains monitoring views, functions, and
-- alerting procedures for database migrations

-- =====================================================
-- 1. MIGRATION MONITORING VIEWS
-- =====================================================

-- View for migration history and status
CREATE OR REPLACE VIEW migration_history AS
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success,
    CASE 
        WHEN success THEN 'SUCCESS'
        ELSE 'FAILED'
    END as status,
    CASE 
        WHEN execution_time > 300000 THEN 'SLOW'
        WHEN execution_time > 60000 THEN 'MODERATE'
        ELSE 'FAST'
    END as performance_category
FROM flyway_schema_history
ORDER BY installed_rank DESC;

-- View for migration performance metrics
CREATE OR REPLACE VIEW migration_performance AS
SELECT 
    version,
    description,
    execution_time,
    installed_on,
    LAG(execution_time) OVER (ORDER BY installed_rank) as previous_execution_time,
    CASE 
        WHEN LAG(execution_time) OVER (ORDER BY installed_rank) IS NOT NULL 
        THEN ((execution_time - LAG(execution_time) OVER (ORDER BY installed_rank))::DECIMAL / 
              LAG(execution_time) OVER (ORDER BY installed_rank) * 100)
        ELSE NULL 
    END as performance_change_percent,
    RANK() OVER (ORDER BY execution_time DESC) as slowest_rank
FROM flyway_schema_history
WHERE success = true
ORDER BY execution_time DESC;

-- View for failed migrations
CREATE OR REPLACE VIEW failed_migrations AS
SELECT 
    installed_rank,
    version,
    description,
    script,
    installed_by,
    installed_on,
    execution_time,
    'Migration failed during execution' as failure_reason
FROM flyway_schema_history
WHERE success = false
ORDER BY installed_on DESC;

-- View for migration trends
CREATE OR REPLACE VIEW migration_trends AS
SELECT 
    DATE(installed_on) as migration_date,
    COUNT(*) as total_migrations,
    COUNT(*) FILTER (WHERE success = true) as successful_migrations,
    COUNT(*) FILTER (WHERE success = false) as failed_migrations,
    AVG(execution_time) as avg_execution_time,
    MAX(execution_time) as max_execution_time,
    MIN(execution_time) as min_execution_time
FROM flyway_schema_history
GROUP BY DATE(installed_on)
ORDER BY migration_date DESC;

-- =====================================================
-- 2. SYSTEM HEALTH MONITORING VIEWS
-- =====================================================

-- View for database size and growth
CREATE OR REPLACE VIEW database_size_monitoring AS
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_total_relation_size(schemaname||'.'||tablename) as table_size_bytes,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as data_size,
    pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename)) as index_size,
    (SELECT reltuples::BIGINT FROM pg_class WHERE relname = tablename) as estimated_rows
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- View for index usage statistics
CREATE OR REPLACE VIEW index_usage_monitoring AS
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    CASE 
        WHEN idx_scan = 0 THEN 'UNUSED'
        WHEN idx_scan < 100 THEN 'LOW_USAGE'
        WHEN idx_scan < 1000 THEN 'MODERATE_USAGE'
        ELSE 'HIGH_USAGE'
    END as usage_category,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- View for query performance monitoring
CREATE OR REPLACE VIEW query_performance_monitoring AS
SELECT 
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    min_exec_time,
    stddev_exec_time,
    rows,
    CASE 
        WHEN mean_exec_time > 5000 THEN 'VERY_SLOW'
        WHEN mean_exec_time > 1000 THEN 'SLOW'
        WHEN mean_exec_time > 100 THEN 'MODERATE'
        ELSE 'FAST'
    END as performance_category
FROM pg_stat_statements
WHERE calls > 10
ORDER BY mean_exec_time DESC
LIMIT 50;

-- =====================================================
-- 3. ALERTING FUNCTIONS
-- =====================================================

-- Function to check for failed migrations
CREATE OR REPLACE FUNCTION check_failed_migrations()
RETURNS TABLE (
    alert_level TEXT,
    alert_message TEXT,
    details JSONB
) AS $$
DECLARE
    failed_count INTEGER;
    recent_failures RECORD;
BEGIN
    -- Count failed migrations in last 24 hours
    SELECT COUNT(*) INTO failed_count
    FROM flyway_schema_history
    WHERE success = false 
    AND installed_on > NOW() - INTERVAL '24 hours';
    
    IF failed_count > 0 THEN
        -- Get details of recent failures
        FOR recent_failures IN 
            SELECT version, description, installed_on, installed_by
            FROM flyway_schema_history
            WHERE success = false 
            AND installed_on > NOW() - INTERVAL '24 hours'
            ORDER BY installed_on DESC
        LOOP
            RETURN QUERY SELECT 
                'CRITICAL'::TEXT,
                format('Migration failure detected: %s - %s', 
                    recent_failures.version, recent_failures.description)::TEXT,
                jsonb_build_object(
                    'version', recent_failures.version,
                    'description', recent_failures.description,
                    'installed_on', recent_failures.installed_on,
                    'installed_by', recent_failures.installed_by,
                    'total_failures_24h', failed_count
                );
        END LOOP;
    END IF;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Function to check for slow migrations
CREATE OR REPLACE FUNCTION check_slow_migrations(
    threshold_ms INTEGER DEFAULT 300000
)
RETURNS TABLE (
    alert_level TEXT,
    alert_message TEXT,
    details JSONB
) AS $$
DECLARE
    slow_migration RECORD;
BEGIN
    FOR slow_migration IN 
        SELECT version, description, execution_time, installed_on
        FROM flyway_schema_history
        WHERE execution_time > threshold_ms
        AND installed_on > NOW() - INTERVAL '24 hours'
        ORDER BY execution_time DESC
    LOOP
        RETURN QUERY SELECT 
            'WARNING'::TEXT,
            format('Slow migration detected: %s took %s ms', 
                slow_migration.version, slow_migration.execution_time)::TEXT,
            jsonb_build_object(
                'version', slow_migration.version,
                'description', slow_migration.description,
                'execution_time_ms', slow_migration.execution_time,
                'execution_time_formatted', 
                    CASE 
                        WHEN slow_migration.execution_time > 3600000 THEN 
                            (slow_migration.execution_time / 3600000) || ' hours'
                        WHEN slow_migration.execution_time > 60000 THEN 
                            (slow_migration.execution_time / 60000) || ' minutes'
                        ELSE 
                            (slow_migration.execution_time / 1000) || ' seconds'
                    END,
                'installed_on', slow_migration.installed_on,
                'threshold_ms', threshold_ms
            );
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Function to check database size growth
CREATE OR REPLACE FUNCTION check_database_growth(
    growth_threshold_percent DECIMAL DEFAULT 20.0
)
RETURNS TABLE (
    alert_level TEXT,
    alert_message TEXT,
    details JSONB
) AS $$
DECLARE
    current_size BIGINT;
    previous_size BIGINT;
    growth_percent DECIMAL;
BEGIN
    -- This is a simplified version - in practice, you'd store historical size data
    SELECT pg_database_size(current_database()) INTO current_size;
    
    -- For demonstration, we'll use a mock previous size
    -- In reality, this would come from a monitoring table
    previous_size := current_size * 0.9; -- Simulate 10% growth
    
    IF previous_size > 0 THEN
        growth_percent := ((current_size - previous_size)::DECIMAL / previous_size) * 100;
        
        IF growth_percent > growth_threshold_percent THEN
            RETURN QUERY SELECT 
                'WARNING'::TEXT,
                format('Database size increased by %.2f%% (threshold: %.2f%%)', 
                    growth_percent, growth_threshold_percent)::TEXT,
                jsonb_build_object(
                    'current_size_bytes', current_size,
                    'current_size_formatted', pg_size_pretty(current_size),
                    'previous_size_bytes', previous_size,
                    'previous_size_formatted', pg_size_pretty(previous_size),
                    'growth_percent', growth_percent,
                    'threshold_percent', growth_threshold_percent
                );
        END IF;
    END IF;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Function to check for unused indexes
CREATE OR REPLACE FUNCTION check_unused_indexes()
RETURNS TABLE (
    alert_level TEXT,
    alert_message TEXT,
    details JSONB
) AS $$
DECLARE
    unused_index RECORD;
    unused_count INTEGER;
BEGIN
    -- Count unused indexes
    SELECT COUNT(*) INTO unused_count
    FROM pg_stat_user_indexes
    WHERE idx_scan = 0
    AND schemaname = 'public';
    
    IF unused_count > 0 THEN
        FOR unused_index IN 
            SELECT schemaname, tablename, indexname, 
                   pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
                   pg_relation_size(indexrelid) as index_size_bytes
            FROM pg_stat_user_indexes
            WHERE idx_scan = 0
            AND schemaname = 'public'
            ORDER BY pg_relation_size(indexrelid) DESC
            LIMIT 10
        LOOP
            RETURN QUERY SELECT 
                'INFO'::TEXT,
                format('Unused index detected: %s.%s (%s)', 
                    unused_index.tablename, unused_index.indexname, unused_index.index_size)::TEXT,
                jsonb_build_object(
                    'schema', unused_index.schemaname,
                    'table', unused_index.tablename,
                    'index', unused_index.indexname,
                    'size_bytes', unused_index.index_size_bytes,
                    'size_formatted', unused_index.index_size,
                    'total_unused_indexes', unused_count
                );
        END LOOP;
    END IF;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 4. COMPREHENSIVE MONITORING FUNCTION
-- =====================================================

-- Function to run all monitoring checks
CREATE OR REPLACE FUNCTION run_migration_monitoring()
RETURNS TABLE (
    check_name TEXT,
    alert_level TEXT,
    alert_message TEXT,
    details JSONB,
    check_timestamp TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    -- Check for failed migrations
    RETURN QUERY 
    SELECT 
        'failed_migrations'::TEXT,
        cfm.alert_level,
        cfm.alert_message,
        cfm.details,
        NOW()
    FROM check_failed_migrations() cfm;
    
    -- Check for slow migrations
    RETURN QUERY 
    SELECT 
        'slow_migrations'::TEXT,
        csm.alert_level,
        csm.alert_message,
        csm.details,
        NOW()
    FROM check_slow_migrations() csm;
    
    -- Check database growth
    RETURN QUERY 
    SELECT 
        'database_growth'::TEXT,
        cdg.alert_level,
        cdg.alert_message,
        cdg.details,
        NOW()
    FROM check_database_growth() cdg;
    
    -- Check for unused indexes
    RETURN QUERY 
    SELECT 
        'unused_indexes'::TEXT,
        cui.alert_level,
        cui.alert_message,
        cui.details,
        NOW()
    FROM check_unused_indexes() cui;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 5. MONITORING DATA COLLECTION
-- =====================================================

-- Table to store monitoring history
CREATE TABLE IF NOT EXISTS migration_monitoring_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    check_name VARCHAR(100) NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    alert_message TEXT NOT NULL,
    details JSONB DEFAULT '{}',
    check_timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(255),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_by VARCHAR(255),
    resolved_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT valid_alert_level CHECK (alert_level IN ('INFO', 'WARNING', 'CRITICAL'))
);

-- Indexes for monitoring log
CREATE INDEX IF NOT EXISTS idx_monitoring_log_timestamp ON migration_monitoring_log(check_timestamp);
CREATE INDEX IF NOT EXISTS idx_monitoring_log_level ON migration_monitoring_log(alert_level);
CREATE INDEX IF NOT EXISTS idx_monitoring_log_check_name ON migration_monitoring_log(check_name);
CREATE INDEX IF NOT EXISTS idx_monitoring_log_unresolved ON migration_monitoring_log(resolved) WHERE resolved = false;

-- Function to log monitoring results
CREATE OR REPLACE FUNCTION log_monitoring_results()
RETURNS INTEGER AS $$
DECLARE
    result_count INTEGER := 0;
    monitoring_result RECORD;
BEGIN
    FOR monitoring_result IN 
        SELECT * FROM run_migration_monitoring()
    LOOP
        INSERT INTO migration_monitoring_log (
            check_name, alert_level, alert_message, details, check_timestamp
        ) VALUES (
            monitoring_result.check_name,
            monitoring_result.alert_level,
            monitoring_result.alert_message,
            monitoring_result.details,
            monitoring_result.check_timestamp
        );
        
        result_count := result_count + 1;
    END LOOP;
    
    RETURN result_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 6. ALERTING INTEGRATION
-- =====================================================

-- Function to format alerts for external systems
CREATE OR REPLACE FUNCTION format_alert_for_webhook(
    alert_record RECORD
) RETURNS JSONB AS $$
BEGIN
    RETURN jsonb_build_object(
        'service', current_database(),
        'timestamp', alert_record.check_timestamp,
        'level', alert_record.alert_level,
        'check', alert_record.check_name,
        'message', alert_record.alert_message,
        'details', alert_record.details,
        'environment', current_setting('app.environment', true),
        'hostname', current_setting('app.hostname', true)
    );
END;
$$ LANGUAGE plpgsql;

-- Function to send alerts (placeholder for webhook integration)
CREATE OR REPLACE FUNCTION send_alert_notification(
    alert_data JSONB
) RETURNS BOOLEAN AS $$
BEGIN
    -- This is a placeholder function
    -- In a real implementation, this would:
    -- 1. Send HTTP requests to webhook endpoints
    -- 2. Integrate with monitoring systems (Prometheus, Grafana, etc.)
    -- 3. Send notifications to Slack, email, etc.
    
    RAISE NOTICE 'Alert notification: %', alert_data;
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. AUTOMATED MONITORING PROCEDURES
-- =====================================================

-- Function to run automated monitoring and alerting
CREATE OR REPLACE FUNCTION run_automated_monitoring()
RETURNS TEXT AS $$
DECLARE
    alert_record RECORD;
    alert_count INTEGER := 0;
    critical_count INTEGER := 0;
    warning_count INTEGER := 0;
    info_count INTEGER := 0;
    alert_data JSONB;
BEGIN
    -- Run monitoring checks and log results
    PERFORM log_monitoring_results();
    
    -- Process unacknowledged alerts from the last hour
    FOR alert_record IN 
        SELECT * FROM migration_monitoring_log
        WHERE check_timestamp > NOW() - INTERVAL '1 hour'
        AND acknowledged = false
        ORDER BY 
            CASE alert_level 
                WHEN 'CRITICAL' THEN 1
                WHEN 'WARNING' THEN 2
                WHEN 'INFO' THEN 3
            END,
            check_timestamp DESC
    LOOP
        alert_count := alert_count + 1;
        
        CASE alert_record.alert_level
            WHEN 'CRITICAL' THEN critical_count := critical_count + 1;
            WHEN 'WARNING' THEN warning_count := warning_count + 1;
            WHEN 'INFO' THEN info_count := info_count + 1;
        END CASE;
        
        -- Format and send alert
        alert_data := format_alert_for_webhook(alert_record);
        PERFORM send_alert_notification(alert_data);
    END LOOP;
    
    RETURN format('Monitoring completed: %s alerts (%s critical, %s warning, %s info)',
        alert_count, critical_count, warning_count, info_count);
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 8. MONITORING DASHBOARD VIEWS
-- =====================================================

-- View for monitoring dashboard summary
CREATE OR REPLACE VIEW monitoring_dashboard_summary AS
SELECT 
    COUNT(*) as total_alerts,
    COUNT(*) FILTER (WHERE alert_level = 'CRITICAL') as critical_alerts,
    COUNT(*) FILTER (WHERE alert_level = 'WARNING') as warning_alerts,
    COUNT(*) FILTER (WHERE alert_level = 'INFO') as info_alerts,
    COUNT(*) FILTER (WHERE acknowledged = false) as unacknowledged_alerts,
    COUNT(*) FILTER (WHERE resolved = false) as unresolved_alerts,
    MAX(check_timestamp) as last_check_time,
    COUNT(DISTINCT check_name) as active_checks
FROM migration_monitoring_log
WHERE check_timestamp > NOW() - INTERVAL '24 hours';

-- View for recent alerts
CREATE OR REPLACE VIEW recent_alerts AS
SELECT 
    id,
    check_name,
    alert_level,
    alert_message,
    check_timestamp,
    acknowledged,
    resolved,
    EXTRACT(EPOCH FROM (NOW() - check_timestamp)) / 60 as minutes_ago
FROM migration_monitoring_log
WHERE check_timestamp > NOW() - INTERVAL '24 hours'
ORDER BY check_timestamp DESC
LIMIT 100;

-- =====================================================
-- 9. MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to clean up old monitoring logs
CREATE OR REPLACE FUNCTION cleanup_monitoring_logs(
    retention_days INTEGER DEFAULT 30
) RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM migration_monitoring_log
    WHERE check_timestamp < NOW() - (retention_days || ' days')::INTERVAL
    AND resolved = true;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to acknowledge alerts
CREATE OR REPLACE FUNCTION acknowledge_alert(
    alert_id UUID,
    acknowledged_by_user VARCHAR(255)
) RETURNS BOOLEAN AS $$
BEGIN
    UPDATE migration_monitoring_log
    SET acknowledged = true,
        acknowledged_by = acknowledged_by_user,
        acknowledged_at = NOW()
    WHERE id = alert_id
    AND acknowledged = false;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to resolve alerts
CREATE OR REPLACE FUNCTION resolve_alert(
    alert_id UUID,
    resolved_by_user VARCHAR(255)
) RETURNS BOOLEAN AS $$
BEGIN
    UPDATE migration_monitoring_log
    SET resolved = true,
        resolved_by = resolved_by_user,
        resolved_at = NOW(),
        acknowledged = true,
        acknowledged_by = COALESCE(acknowledged_by, resolved_by_user),
        acknowledged_at = COALESCE(acknowledged_at, NOW())
    WHERE id = alert_id
    AND resolved = false;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON VIEW migration_history IS 'Complete history of database migrations with status and performance metrics';
COMMENT ON VIEW migration_performance IS 'Performance analysis of database migrations';
COMMENT ON VIEW failed_migrations IS 'List of failed database migrations requiring attention';
COMMENT ON FUNCTION run_migration_monitoring() IS 'Comprehensive monitoring function that runs all migration health checks';
COMMENT ON FUNCTION log_monitoring_results() IS 'Logs monitoring results to the monitoring log table';
COMMENT ON FUNCTION run_automated_monitoring() IS 'Automated monitoring procedure for scheduled execution';
COMMENT ON TABLE migration_monitoring_log IS 'Log table for storing migration monitoring alerts and their resolution status';