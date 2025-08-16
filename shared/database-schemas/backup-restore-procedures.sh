#!/bin/bash

# =====================================================
# DATABASE BACKUP AND RESTORE PROCEDURES
# =====================================================
# This script provides comprehensive backup and restore
# procedures for CRM microservices databases

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/crm-platform}"
LOG_DIR="${LOG_DIR:-/var/log/crm-platform}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
COMPRESSION_LEVEL="${COMPRESSION_LEVEL:-6}"

# Database configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-}"

# Service databases
declare -A DATABASES=(
    ["auth"]="crm_auth_db"
    ["tenant"]="crm_tenant_db"
    ["contacts"]="crm_contacts_db"
    ["accounts"]="crm_accounts_db"
    ["deals"]="crm_deals_db"
    ["custom-objects"]="crm_custom_objects_db"
    ["leads"]="crm_leads_db"
    ["activities"]="crm_activities_db"
    ["analytics"]="crm_analytics_db"
    ["workflow"]="crm_workflow_db"
)

# Logging functions
log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] INFO: $*" | tee -a "${LOG_DIR}/backup-restore.log"
}

log_error() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $*" | tee -a "${LOG_DIR}/backup-restore.log" >&2
}

log_warn() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] WARN: $*" | tee -a "${LOG_DIR}/backup-restore.log"
}

# Utility functions
check_dependencies() {
    local deps=("pg_dump" "pg_restore" "psql" "gzip" "gunzip")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            log_error "Required dependency '$dep' not found"
            exit 1
        fi
    done
}

create_directories() {
    mkdir -p "$BACKUP_DIR" "$LOG_DIR"
    for service in "${!DATABASES[@]}"; do
        mkdir -p "$BACKUP_DIR/$service"
    done
}

get_timestamp() {
    date '+%Y%m%d_%H%M%S'
}

# Database connection functions
test_connection() {
    local database="$1"
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$database" -c "SELECT 1;" &>/dev/null; then
        return 0
    else
        return 1
    fi
}

wait_for_database() {
    local database="$1"
    local max_attempts=30
    local attempt=1
    
    log_info "Waiting for database $database to be available..."
    
    while [ $attempt -le $max_attempts ]; do
        if test_connection "$database"; then
            log_info "Database $database is available"
            return 0
        fi
        
        log_info "Attempt $attempt/$max_attempts: Database $database not ready, waiting..."
        sleep 2
        ((attempt++))
    done
    
    log_error "Database $database is not available after $max_attempts attempts"
    return 1
}

# Backup functions
backup_database() {
    local service="$1"
    local database="$2"
    local timestamp="$3"
    local backup_file="$BACKUP_DIR/$service/${service}_${timestamp}.sql"
    local compressed_file="${backup_file}.gz"
    
    log_info "Starting backup of $service database ($database)"
    
    # Test connection
    if ! wait_for_database "$database"; then
        log_error "Cannot connect to database $database"
        return 1
    fi
    
    # Create backup
    if PGPASSWORD="$DB_PASSWORD" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$database" \
        --verbose \
        --no-password \
        --format=custom \
        --compress="$COMPRESSION_LEVEL" \
        --file="$backup_file"; then
        
        log_info "Database backup completed: $backup_file"
        
        # Compress backup
        if gzip -f "$backup_file"; then
            log_info "Backup compressed: $compressed_file"
            
            # Verify backup integrity
            if verify_backup "$compressed_file"; then
                log_info "Backup verification successful for $service"
                return 0
            else
                log_error "Backup verification failed for $service"
                return 1
            fi
        else
            log_error "Failed to compress backup for $service"
            return 1
        fi
    else
        log_error "Failed to create backup for $service database"
        return 1
    fi
}

backup_schema_only() {
    local service="$1"
    local database="$2"
    local timestamp="$3"
    local backup_file="$BACKUP_DIR/$service/${service}_schema_${timestamp}.sql"
    
    log_info "Starting schema-only backup of $service database"
    
    if PGPASSWORD="$DB_PASSWORD" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$database" \
        --schema-only \
        --verbose \
        --no-password \
        --file="$backup_file"; then
        
        log_info "Schema backup completed: $backup_file"
        gzip -f "$backup_file"
        return 0
    else
        log_error "Failed to create schema backup for $service"
        return 1
    fi
}

backup_data_only() {
    local service="$1"
    local database="$2"
    local timestamp="$3"
    local backup_file="$BACKUP_DIR/$service/${service}_data_${timestamp}.sql"
    
    log_info "Starting data-only backup of $service database"
    
    if PGPASSWORD="$DB_PASSWORD" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$database" \
        --data-only \
        --verbose \
        --no-password \
        --file="$backup_file"; then
        
        log_info "Data backup completed: $backup_file"
        gzip -f "$backup_file"
        return 0
    else
        log_error "Failed to create data backup for $service"
        return 1
    fi
}

# Restore functions
restore_database() {
    local service="$1"
    local database="$2"
    local backup_file="$3"
    local create_db="${4:-false}"
    
    log_info "Starting restore of $service database from $backup_file"
    
    # Decompress if needed
    local restore_file="$backup_file"
    if [[ "$backup_file" == *.gz ]]; then
        restore_file="${backup_file%.gz}"
        if ! gunzip -c "$backup_file" > "$restore_file"; then
            log_error "Failed to decompress backup file"
            return 1
        fi
    fi
    
    # Create database if requested
    if [[ "$create_db" == "true" ]]; then
        log_info "Creating database $database"
        if ! PGPASSWORD="$DB_PASSWORD" createdb \
            -h "$DB_HOST" \
            -p "$DB_PORT" \
            -U "$DB_USER" \
            "$database"; then
            log_warn "Database $database might already exist or creation failed"
        fi
    fi
    
    # Test connection
    if ! wait_for_database "$database"; then
        log_error "Cannot connect to database $database for restore"
        return 1
    fi
    
    # Restore database
    if PGPASSWORD="$DB_PASSWORD" pg_restore \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$database" \
        --verbose \
        --no-password \
        --clean \
        --if-exists \
        "$restore_file"; then
        
        log_info "Database restore completed for $service"
        
        # Clean up decompressed file if it was created
        if [[ "$backup_file" == *.gz ]] && [[ -f "$restore_file" ]]; then
            rm -f "$restore_file"
        fi
        
        return 0
    else
        log_error "Failed to restore $service database"
        return 1
    fi
}

# Verification functions
verify_backup() {
    local backup_file="$1"
    
    log_info "Verifying backup integrity: $backup_file"
    
    # Check if file exists and is not empty
    if [[ ! -f "$backup_file" ]] || [[ ! -s "$backup_file" ]]; then
        log_error "Backup file is missing or empty"
        return 1
    fi
    
    # Test gzip integrity if compressed
    if [[ "$backup_file" == *.gz ]]; then
        if ! gzip -t "$backup_file"; then
            log_error "Backup file is corrupted (gzip test failed)"
            return 1
        fi
    fi
    
    # Additional verification could include:
    # - Testing restore to a temporary database
    # - Checking specific table counts
    # - Validating data integrity
    
    log_info "Backup verification passed"
    return 0
}

verify_restore() {
    local service="$1"
    local database="$2"
    
    log_info "Verifying restore for $service database"
    
    # Test basic connectivity
    if ! test_connection "$database"; then
        log_error "Cannot connect to restored database"
        return 1
    fi
    
    # Check if essential tables exist
    local table_count
    table_count=$(PGPASSWORD="$DB_PASSWORD" psql \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$database" \
        -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';")
    
    if [[ "$table_count" -lt 1 ]]; then
        log_error "Restored database has no tables"
        return 1
    fi
    
    log_info "Restore verification passed: $table_count tables found"
    return 0
}

# Cleanup functions
cleanup_old_backups() {
    local service="$1"
    local retention_days="$2"
    
    log_info "Cleaning up backups older than $retention_days days for $service"
    
    find "$BACKUP_DIR/$service" -name "*.gz" -type f -mtime +$retention_days -delete
    
    local deleted_count
    deleted_count=$(find "$BACKUP_DIR/$service" -name "*.gz" -type f -mtime +$retention_days | wc -l)
    
    if [[ "$deleted_count" -gt 0 ]]; then
        log_info "Deleted $deleted_count old backup files for $service"
    fi
}

# Main functions
backup_all_services() {
    local timestamp
    timestamp=$(get_timestamp)
    local failed_services=()
    
    log_info "Starting backup of all services at $timestamp"
    
    for service in "${!DATABASES[@]}"; do
        local database="${DATABASES[$service]}"
        
        if backup_database "$service" "$database" "$timestamp"; then
            log_info "Successfully backed up $service"
            cleanup_old_backups "$service" "$RETENTION_DAYS"
        else
            log_error "Failed to backup $service"
            failed_services+=("$service")
        fi
    done
    
    if [[ ${#failed_services[@]} -eq 0 ]]; then
        log_info "All service backups completed successfully"
        return 0
    else
        log_error "Failed to backup services: ${failed_services[*]}"
        return 1
    fi
}

backup_service() {
    local service="$1"
    local backup_type="${2:-full}"
    local timestamp
    timestamp=$(get_timestamp)
    
    if [[ ! -v "DATABASES[$service]" ]]; then
        log_error "Unknown service: $service"
        return 1
    fi
    
    local database="${DATABASES[$service]}"
    
    case "$backup_type" in
        "full")
            backup_database "$service" "$database" "$timestamp"
            ;;
        "schema")
            backup_schema_only "$service" "$database" "$timestamp"
            ;;
        "data")
            backup_data_only "$service" "$database" "$timestamp"
            ;;
        *)
            log_error "Unknown backup type: $backup_type"
            return 1
            ;;
    esac
}

restore_service() {
    local service="$1"
    local backup_file="$2"
    local create_db="${3:-false}"
    
    if [[ ! -v "DATABASES[$service]" ]]; then
        log_error "Unknown service: $service"
        return 1
    fi
    
    if [[ ! -f "$backup_file" ]]; then
        log_error "Backup file not found: $backup_file"
        return 1
    fi
    
    local database="${DATABASES[$service]}"
    
    if restore_database "$service" "$database" "$backup_file" "$create_db"; then
        verify_restore "$service" "$database"
    else
        return 1
    fi
}

# Migration-specific functions
pre_migration_backup() {
    local service="$1"
    local migration_version="$2"
    local timestamp
    timestamp=$(get_timestamp)
    
    log_info "Creating pre-migration backup for $service (migration $migration_version)"
    
    if [[ ! -v "DATABASES[$service]" ]]; then
        log_error "Unknown service: $service"
        return 1
    fi
    
    local database="${DATABASES[$service]}"
    local backup_file="$BACKUP_DIR/$service/${service}_pre_migration_${migration_version}_${timestamp}.sql"
    
    if PGPASSWORD="$DB_PASSWORD" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$database" \
        --verbose \
        --no-password \
        --format=custom \
        --compress="$COMPRESSION_LEVEL" \
        --file="$backup_file"; then
        
        gzip -f "$backup_file"
        log_info "Pre-migration backup created: ${backup_file}.gz"
        echo "${backup_file}.gz"
        return 0
    else
        log_error "Failed to create pre-migration backup"
        return 1
    fi
}

post_migration_verification() {
    local service="$1"
    local migration_version="$2"
    
    log_info "Running post-migration verification for $service (migration $migration_version)"
    
    if [[ ! -v "DATABASES[$service]" ]]; then
        log_error "Unknown service: $service"
        return 1
    fi
    
    local database="${DATABASES[$service]}"
    
    # Basic connectivity test
    if ! test_connection "$database"; then
        log_error "Post-migration connectivity test failed"
        return 1
    fi
    
    # Check migration history
    local migration_status
    migration_status=$(PGPASSWORD="$DB_PASSWORD" psql \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$database" \
        -t -c "SELECT success FROM flyway_schema_history WHERE version = '$migration_version' ORDER BY installed_rank DESC LIMIT 1;")
    
    if [[ "$migration_status" != *"t"* ]]; then
        log_error "Migration $migration_version was not successful"
        return 1
    fi
    
    log_info "Post-migration verification passed"
    return 0
}

# Help function
show_help() {
    cat << EOF
Database Backup and Restore Procedures for CRM Platform

Usage: $0 [COMMAND] [OPTIONS]

Commands:
    backup-all                  Backup all service databases
    backup SERVICE [TYPE]       Backup specific service (TYPE: full|schema|data)
    restore SERVICE FILE        Restore service from backup file
    verify FILE                 Verify backup file integrity
    cleanup SERVICE DAYS        Clean up old backups
    pre-migration SERVICE VER   Create pre-migration backup
    post-migration SERVICE VER  Run post-migration verification

Services: ${!DATABASES[*]}

Environment Variables:
    DB_HOST                     Database host (default: localhost)
    DB_PORT                     Database port (default: 5432)
    DB_USER                     Database user (default: postgres)
    DB_PASSWORD                 Database password
    BACKUP_DIR                  Backup directory (default: /var/backups/crm-platform)
    LOG_DIR                     Log directory (default: /var/log/crm-platform)
    RETENTION_DAYS              Backup retention days (default: 30)
    COMPRESSION_LEVEL           Compression level 1-9 (default: 6)

Examples:
    $0 backup-all
    $0 backup auth full
    $0 backup contacts schema
    $0 restore auth /path/to/backup.sql.gz
    $0 pre-migration deals V3
    $0 post-migration deals V3

EOF
}

# Main script logic
main() {
    check_dependencies
    create_directories
    
    case "${1:-}" in
        "backup-all")
            backup_all_services
            ;;
        "backup")
            if [[ $# -lt 2 ]]; then
                log_error "Service name required for backup command"
                show_help
                exit 1
            fi
            backup_service "$2" "${3:-full}"
            ;;
        "restore")
            if [[ $# -lt 3 ]]; then
                log_error "Service name and backup file required for restore command"
                show_help
                exit 1
            fi
            restore_service "$2" "$3" "${4:-false}"
            ;;
        "verify")
            if [[ $# -lt 2 ]]; then
                log_error "Backup file required for verify command"
                show_help
                exit 1
            fi
            verify_backup "$2"
            ;;
        "cleanup")
            if [[ $# -lt 3 ]]; then
                log_error "Service name and retention days required for cleanup command"
                show_help
                exit 1
            fi
            cleanup_old_backups "$2" "$3"
            ;;
        "pre-migration")
            if [[ $# -lt 3 ]]; then
                log_error "Service name and migration version required"
                show_help
                exit 1
            fi
            pre_migration_backup "$2" "$3"
            ;;
        "post-migration")
            if [[ $# -lt 3 ]]; then
                log_error "Service name and migration version required"
                show_help
                exit 1
            fi
            post_migration_verification "$2" "$3"
            ;;
        "help"|"-h"|"--help"|"")
            show_help
            ;;
        *)
            log_error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"