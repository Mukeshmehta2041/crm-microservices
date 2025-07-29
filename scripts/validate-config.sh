#!/bin/bash

# Configuration Validation Script for CRM Microservices Platform
# This script validates environment configurations and secrets

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
CONFIG_DIR="infra/config"
ENVIRONMENTS=("development" "staging" "production")
REQUIRED_SECRETS=(
    "DB_PASSWORD"
    "JWT_SECRET"
    "REDIS_PASSWORD"
)

# Function to display usage
usage() {
    echo "Usage: $0 [ENVIRONMENT] [OPTIONS]"
    echo ""
    echo "Environments:"
    echo "  development    Validate development configuration"
    echo "  staging        Validate staging configuration"
    echo "  production     Validate production configuration"
    echo "  all           Validate all environments"
    echo ""
    echo "Options:"
    echo "  --check-secrets    Check if all required secrets are set"
    echo "  --check-services   Check if all services can connect to dependencies"
    echo "  --generate-docs    Generate configuration documentation"
    echo "  --fix-permissions  Fix file permissions for configuration files"
    echo ""
    echo "Examples:"
    echo "  $0 development                    # Validate development config"
    echo "  $0 production --check-secrets    # Validate production config and secrets"
    echo "  $0 all --check-services          # Validate all configs and test connections"
}

# Function to validate environment file
validate_env_file() {
    local env=$1
    local env_file="${CONFIG_DIR}/environments/${env}.env"
    
    echo -e "${BLUE}Validating ${env} environment configuration...${NC}"
    
    if [[ ! -f "$env_file" ]]; then
        echo -e "${RED}❌ Environment file not found: $env_file${NC}"
        return 1
    fi
    
    local errors=0
    local warnings=0
    
    # Check for required variables
    local required_vars=(
        "ENVIRONMENT"
        "SPRING_PROFILES_ACTIVE"
        "DB_HOST"
        "DB_PORT"
        "DB_NAME"
        "DB_USERNAME"
        "REDIS_HOST"
        "REDIS_PORT"
        "JWT_SECRET"
        "EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE"
    )
    
    echo -e "${YELLOW}Checking required variables...${NC}"
    for var in "${required_vars[@]}"; do
        if ! grep -q "^${var}=" "$env_file"; then
            echo -e "${RED}  ❌ Missing required variable: $var${NC}"
            ((errors++))
        else
            echo -e "${GREEN}  ✅ Found: $var${NC}"
        fi
    done
    
    # Check for empty values
    echo -e "${YELLOW}Checking for empty values...${NC}"
    while IFS= read -r line; do
        if [[ "$line" =~ ^[A-Z_]+=$ ]]; then
            local var_name=$(echo "$line" | cut -d'=' -f1)
            echo -e "${YELLOW}  ⚠️  Empty value for: $var_name${NC}"
            ((warnings++))
        fi
    done < "$env_file"
    
    # Check for sensitive data in non-production environments
    if [[ "$env" != "production" ]]; then
        echo -e "${YELLOW}Checking for development-appropriate values...${NC}"
        if grep -q "change-in-production" "$env_file"; then
            echo -e "${GREEN}  ✅ Found development placeholder values${NC}"
        else
            echo -e "${YELLOW}  ⚠️  No development placeholders found${NC}"
            ((warnings++))
        fi
    fi
    
    # Validate URL formats
    echo -e "${YELLOW}Validating URL formats...${NC}"
    local url_vars=("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE" "ZIPKIN_BASE_URL" "CORS_ALLOWED_ORIGINS")
    for var in "${url_vars[@]}"; do
        local value=$(grep "^${var}=" "$env_file" | cut -d'=' -f2- | tr -d '"')
        if [[ -n "$value" && ! "$value" =~ ^https?:// ]]; then
            echo -e "${RED}  ❌ Invalid URL format for $var: $value${NC}"
            ((errors++))
        fi
    done
    
    # Validate port numbers
    echo -e "${YELLOW}Validating port numbers...${NC}"
    local port_vars=("DB_PORT" "REDIS_PORT" "RABBITMQ_PORT" "ELASTICSEARCH_PORT")
    for var in "${port_vars[@]}"; do
        local value=$(grep "^${var}=" "$env_file" | cut -d'=' -f2 | tr -d '"')
        if [[ -n "$value" && ! "$value" =~ ^[0-9]+$ ]] || [[ "$value" -lt 1 || "$value" -gt 65535 ]]; then
            echo -e "${RED}  ❌ Invalid port number for $var: $value${NC}"
            ((errors++))
        fi
    done
    
    # Check JVM options
    echo -e "${YELLOW}Validating JVM options...${NC}"
    local jvm_opts=$(grep "^JVM_OPTS=" "$env_file" | cut -d'=' -f2- | tr -d '"')
    if [[ -n "$jvm_opts" ]]; then
        if [[ "$jvm_opts" =~ -Xms && "$jvm_opts" =~ -Xmx ]]; then
            echo -e "${GREEN}  ✅ JVM memory options configured${NC}"
        else
            echo -e "${YELLOW}  ⚠️  JVM memory options not found${NC}"
            ((warnings++))
        fi
    fi
    
    # Environment-specific validations
    case "$env" in
        "production")
            echo -e "${YELLOW}Validating production-specific settings...${NC}"
            
            # Check security settings
            local security_vars=("SECURITY_REQUIRE_HTTPS" "SECURITY_HSTS_ENABLED" "SECURITY_CSRF_ENABLED")
            for var in "${security_vars[@]}"; do
                local value=$(grep "^${var}=" "$env_file" | cut -d'=' -f2 | tr -d '"')
                if [[ "$value" != "true" ]]; then
                    echo -e "${RED}  ❌ Security setting should be enabled in production: $var${NC}"
                    ((errors++))
                fi
            done
            
            # Check debug settings
            if grep -q "DEBUG_ENABLED=true" "$env_file"; then
                echo -e "${RED}  ❌ Debug should be disabled in production${NC}"
                ((errors++))
            fi
            
            if grep -q "SWAGGER_ENABLED=true" "$env_file"; then
                echo -e "${RED}  ❌ Swagger should be disabled in production${NC}"
                ((errors++))
            fi
            ;;
            
        "development")
            echo -e "${YELLOW}Validating development-specific settings...${NC}"
            
            # Check if development features are enabled
            if ! grep -q "DEBUG_ENABLED=true" "$env_file"; then
                echo -e "${YELLOW}  ⚠️  Debug not enabled in development${NC}"
                ((warnings++))
            fi
            ;;
    esac
    
    # Summary
    echo ""
    echo -e "${BLUE}=== Validation Summary for $env ===${NC}"
    echo -e "${GREEN}✅ Passed checks: $(($(wc -l < "$env_file") - errors - warnings))${NC}"
    echo -e "${YELLOW}⚠️  Warnings: $warnings${NC}"
    echo -e "${RED}❌ Errors: $errors${NC}"
    
    if [[ $errors -gt 0 ]]; then
        return 1
    elif [[ $warnings -gt 0 ]]; then
        return 2
    else
        return 0
    fi
}

# Function to check secrets
check_secrets() {
    local env=$1
    local secrets_file="${CONFIG_DIR}/secrets/secrets.env"
    
    echo -e "${BLUE}Checking secrets for ${env} environment...${NC}"
    
    if [[ ! -f "$secrets_file" ]]; then
        echo -e "${RED}❌ Secrets file not found: $secrets_file${NC}"
        echo -e "${YELLOW}💡 Copy secrets.template.env to secrets.env and fill in values${NC}"
        return 1
    fi
    
    local missing_secrets=()
    
    for secret in "${REQUIRED_SECRETS[@]}"; do
        if ! grep -q "^${secret}=" "$secrets_file" || grep -q "^${secret}=$" "$secrets_file" || grep -q "your_" "$secrets_file"; then
            missing_secrets+=("$secret")
        fi
    done
    
    if [[ ${#missing_secrets[@]} -gt 0 ]]; then
        echo -e "${RED}❌ Missing or template values found for secrets:${NC}"
        for secret in "${missing_secrets[@]}"; do
            echo -e "${RED}  - $secret${NC}"
        done
        return 1
    else
        echo -e "${GREEN}✅ All required secrets are configured${NC}"
        return 0
    fi
}

# Function to test service connections
test_service_connections() {
    local env=$1
    local env_file="${CONFIG_DIR}/environments/${env}.env"
    
    echo -e "${BLUE}Testing service connections for ${env} environment...${NC}"
    
    # Source environment variables
    set -a
    source "$env_file"
    if [[ -f "${CONFIG_DIR}/secrets/secrets.env" ]]; then
        source "${CONFIG_DIR}/secrets/secrets.env"
    fi
    set +a
    
    local connection_errors=0
    
    # Test database connection
    echo -e "${YELLOW}Testing database connection...${NC}"
    if command -v pg_isready &> /dev/null; then
        if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" &> /dev/null; then
            echo -e "${GREEN}  ✅ Database connection successful${NC}"
        else
            echo -e "${RED}  ❌ Database connection failed${NC}"
            ((connection_errors++))
        fi
    else
        echo -e "${YELLOW}  ⚠️  pg_isready not available, skipping database test${NC}"
    fi
    
    # Test Redis connection
    echo -e "${YELLOW}Testing Redis connection...${NC}"
    if command -v redis-cli &> /dev/null; then
        if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping &> /dev/null; then
            echo -e "${GREEN}  ✅ Redis connection successful${NC}"
        else
            echo -e "${RED}  ❌ Redis connection failed${NC}"
            ((connection_errors++))
        fi
    else
        echo -e "${YELLOW}  ⚠️  redis-cli not available, skipping Redis test${NC}"
    fi
    
    # Test HTTP endpoints
    echo -e "${YELLOW}Testing HTTP endpoints...${NC}"
    local endpoints=("$EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE")
    
    for endpoint in "${endpoints[@]}"; do
        if [[ -n "$endpoint" ]]; then
            if curl -s --connect-timeout 5 "$endpoint" &> /dev/null; then
                echo -e "${GREEN}  ✅ Endpoint reachable: $endpoint${NC}"
            else
                echo -e "${RED}  ❌ Endpoint unreachable: $endpoint${NC}"
                ((connection_errors++))
            fi
        fi
    done
    
    if [[ $connection_errors -gt 0 ]]; then
        echo -e "${RED}❌ $connection_errors connection test(s) failed${NC}"
        return 1
    else
        echo -e "${GREEN}✅ All connection tests passed${NC}"
        return 0
    fi
}

# Function to generate configuration documentation
generate_docs() {
    local docs_file="${CONFIG_DIR}/configuration-reference.md"
    
    echo -e "${BLUE}Generating configuration documentation...${NC}"
    
    cat > "$docs_file" << 'EOF'
# CRM Microservices Platform Configuration Reference

This document provides a comprehensive reference for all configuration options available in the CRM microservices platform.

## Environment Files

The platform uses environment-specific configuration files located in `infra/config/environments/`:

- `development.env` - Development environment configuration
- `staging.env` - Staging environment configuration  
- `production.env` - Production environment configuration

## Secrets Management

Sensitive configuration values are stored in `infra/config/secrets/secrets.env`. Use the template file `secrets.template.env` as a starting point.

## Configuration Categories

### Database Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| DB_HOST | Database host | localhost | Yes |
| DB_PORT | Database port | 5432 | Yes |
| DB_NAME | Database name | crm_platform | Yes |
| DB_USERNAME | Database username | crm_user | Yes |
| DB_PASSWORD | Database password | - | Yes |
| DB_POOL_SIZE | Connection pool size | 10 | No |

### Redis Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| REDIS_HOST | Redis host | localhost | Yes |
| REDIS_PORT | Redis port | 6379 | Yes |
| REDIS_PASSWORD | Redis password | - | No |
| REDIS_DATABASE | Redis database number | 0 | No |

### Security Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| JWT_SECRET | JWT signing secret | - | Yes |
| JWT_EXPIRATION | JWT token expiration (seconds) | 3600 | No |
| CORS_ALLOWED_ORIGINS | Allowed CORS origins | * | No |

### Monitoring Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| METRICS_ENABLED | Enable metrics collection | true | No |
| TRACING_ENABLED | Enable distributed tracing | true | No |
| ZIPKIN_BASE_URL | Zipkin server URL | - | No |

## Environment-Specific Notes

### Development
- Debug features are enabled
- Swagger UI is available
- Relaxed security settings
- Local file storage

### Staging
- Production-like configuration
- Swagger UI enabled for testing
- Enhanced logging
- Cloud storage integration

### Production
- Maximum security settings
- Debug features disabled
- Swagger UI disabled
- Full monitoring and alerting
- Encrypted storage

## Validation

Use the configuration validation script to check your configuration:

```bash
./scripts/validate-config.sh [environment]
```

## Security Best Practices

1. Never commit secrets to version control
2. Use strong, unique passwords for all services
3. Enable HTTPS in staging and production
4. Regularly rotate secrets and certificates
5. Use environment-specific JWT secrets
6. Enable audit logging in production

EOF

    echo -e "${GREEN}✅ Configuration documentation generated: $docs_file${NC}"
}

# Function to fix file permissions
fix_permissions() {
    echo -e "${BLUE}Fixing configuration file permissions...${NC}"
    
    # Set restrictive permissions on secrets
    if [[ -f "${CONFIG_DIR}/secrets/secrets.env" ]]; then
        chmod 600 "${CONFIG_DIR}/secrets/secrets.env"
        echo -e "${GREEN}✅ Set permissions 600 on secrets.env${NC}"
    fi
    
    # Set readable permissions on environment files
    for env in "${ENVIRONMENTS[@]}"; do
        local env_file="${CONFIG_DIR}/environments/${env}.env"
        if [[ -f "$env_file" ]]; then
            chmod 644 "$env_file"
            echo -e "${GREEN}✅ Set permissions 644 on ${env}.env${NC}"
        fi
    done
    
    # Set executable permissions on scripts
    chmod +x scripts/*.sh
    echo -e "${GREEN}✅ Set executable permissions on scripts${NC}"
}

# Main function
main() {
    local environment=""
    local check_secrets=false
    local check_services=false
    local generate_docs=false
    local fix_permissions=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            development|staging|production|all)
                environment=$1
                shift
                ;;
            --check-secrets)
                check_secrets=true
                shift
                ;;
            --check-services)
                check_services=true
                shift
                ;;
            --generate-docs)
                generate_docs=true
                shift
                ;;
            --fix-permissions)
                fix_permissions=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                echo -e "${RED}Error: Unknown option '$1'${NC}"
                usage
                exit 1
                ;;
        esac
    done
    
    # Handle special actions
    if [[ "$generate_docs" == "true" ]]; then
        generate_docs
        exit 0
    fi
    
    if [[ "$fix_permissions" == "true" ]]; then
        fix_permissions
        exit 0
    fi
    
    # Check if environment is provided
    if [[ -z "$environment" ]]; then
        echo -e "${RED}Error: No environment specified${NC}"
        usage
        exit 1
    fi
    
    # Validate environments
    local environments_to_check=()
    if [[ "$environment" == "all" ]]; then
        environments_to_check=("${ENVIRONMENTS[@]}")
    else
        environments_to_check=("$environment")
    fi
    
    local total_errors=0
    local total_warnings=0
    
    for env in "${environments_to_check[@]}"; do
        echo ""
        echo -e "${BLUE}=== Validating $env Environment ===${NC}"
        
        # Validate environment file
        validate_env_file "$env"
        local result=$?
        
        if [[ $result -eq 1 ]]; then
            ((total_errors++))
        elif [[ $result -eq 2 ]]; then
            ((total_warnings++))
        fi
        
        # Check secrets if requested
        if [[ "$check_secrets" == "true" ]]; then
            echo ""
            check_secrets "$env"
            if [[ $? -ne 0 ]]; then
                ((total_errors++))
            fi
        fi
        
        # Test service connections if requested
        if [[ "$check_services" == "true" ]]; then
            echo ""
            test_service_connections "$env"
            if [[ $? -ne 0 ]]; then
                ((total_errors++))
            fi
        fi
    done
    
    # Final summary
    echo ""
    echo -e "${BLUE}=== Final Summary ===${NC}"
    echo -e "${GREEN}Environments validated: ${#environments_to_check[@]}${NC}"
    echo -e "${YELLOW}Total warnings: $total_warnings${NC}"
    echo -e "${RED}Total errors: $total_errors${NC}"
    
    if [[ $total_errors -gt 0 ]]; then
        echo -e "${RED}❌ Configuration validation failed${NC}"
        exit 1
    elif [[ $total_warnings -gt 0 ]]; then
        echo -e "${YELLOW}⚠️  Configuration validation completed with warnings${NC}"
        exit 2
    else
        echo -e "${GREEN}✅ Configuration validation passed${NC}"
        exit 0
    fi
}

# Run main function
main "$@"