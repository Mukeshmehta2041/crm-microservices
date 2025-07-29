#!/bin/bash

# Environment Deployment Script for CRM Platform
# This script deploys the CRM platform to different environments

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Configuration
ENVIRONMENT=${1:-dev}
DOCKER_REGISTRY=${DOCKER_REGISTRY:-ghcr.io}
IMAGE_TAG=${IMAGE_TAG:-latest}
COMPOSE_PROJECT_NAME="crm-platform-${ENVIRONMENT}"

# Environment-specific configurations
case $ENVIRONMENT in
    "dev"|"development")
        COMPOSE_FILE="docker-compose.yml:docker-compose.dev.yml"
        DB_NAME="crm_dev"
        REPLICAS=1
        MEMORY_LIMIT="512m"
        ;;
    "staging")
        COMPOSE_FILE="docker-compose.yml:docker-compose.staging.yml"
        DB_NAME="crm_staging"
        REPLICAS=2
        MEMORY_LIMIT="1g"
        ;;
    "prod"|"production")
        COMPOSE_FILE="docker-compose.yml:docker-compose.prod.yml"
        DB_NAME="crm_production"
        REPLICAS=3
        MEMORY_LIMIT="2g"
        ;;
    *)
        print_error "Unknown environment: $ENVIRONMENT"
        print_info "Supported environments: dev, staging, prod"
        exit 1
        ;;
esac

# Functions
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi
    print_success "Docker found"
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed"
        exit 1
    fi
    print_success "Docker Compose found"
    
    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running"
        exit 1
    fi
    print_success "Docker daemon is running"
    
    # Check environment files
    ENV_FILE="infra/config/environments/${ENVIRONMENT}.env"
    if [ ! -f "$ENV_FILE" ]; then
        print_error "Environment file not found: $ENV_FILE"
        exit 1
    fi
    print_success "Environment configuration found"
}

setup_environment() {
    print_header "Setting Up Environment"
    
    # Create necessary directories
    mkdir -p logs
    mkdir -p data/postgres
    mkdir -p data/redis
    
    # Set environment variables
    export COMPOSE_PROJECT_NAME
    export DOCKER_REGISTRY
    export IMAGE_TAG
    export DB_NAME
    export REPLICAS
    export MEMORY_LIMIT
    
    # Load environment-specific variables
    if [ -f "infra/config/environments/${ENVIRONMENT}.env" ]; then
        set -a
        source "infra/config/environments/${ENVIRONMENT}.env"
        set +a
        print_success "Environment variables loaded"
    fi
    
    print_info "Environment: $ENVIRONMENT"
    print_info "Registry: $DOCKER_REGISTRY"
    print_info "Image Tag: $IMAGE_TAG"
    print_info "Database: $DB_NAME"
    print_info "Replicas: $REPLICAS"
}

pull_images() {
    print_header "Pulling Docker Images"
    
    services=("discovery-server" "api-gateway" "auth-service" "tenant-service" "users-service")
    
    for service in "${services[@]}"; do
        image="${DOCKER_REGISTRY}/${GITHUB_REPOSITORY:-crm-platform}/${service}:${IMAGE_TAG}"
        print_info "Pulling $image..."
        
        if docker pull "$image"; then
            print_success "Pulled $service image"
        else
            print_warning "Failed to pull $service image, will try to build locally"
        fi
    done
}

start_infrastructure() {
    print_header "Starting Infrastructure Services"
    
    # Start infrastructure services first
    print_info "Starting PostgreSQL..."
    docker-compose -f infra/docker/docker-compose.yml up -d postgres
    
    print_info "Starting Redis..."
    docker-compose -f infra/docker/docker-compose.yml up -d redis
    
    # Wait for services to be ready
    print_info "Waiting for PostgreSQL to be ready..."
    timeout=60
    while ! docker-compose -f infra/docker/docker-compose.yml exec -T postgres pg_isready -U postgres; do
        sleep 2
        timeout=$((timeout - 2))
        if [ $timeout -le 0 ]; then
            print_error "PostgreSQL failed to start within 60 seconds"
            exit 1
        fi
    done
    print_success "PostgreSQL is ready"
    
    print_info "Waiting for Redis to be ready..."
    timeout=30
    while ! docker-compose -f infra/docker/docker-compose.yml exec -T redis redis-cli ping; do
        sleep 2
        timeout=$((timeout - 2))
        if [ $timeout -le 0 ]; then
            print_error "Redis failed to start within 30 seconds"
            exit 1
        fi
    done
    print_success "Redis is ready"
}

run_migrations() {
    print_header "Running Database Migrations"
    
    services=("auth-service" "tenant-service" "users-service")
    
    for service in "${services[@]}"; do
        print_info "Running migrations for $service..."
        
        # Run Flyway migrations
        if [ -d "services/$service/src/main/resources/db/migration" ]; then
            docker run --rm \
                --network "${COMPOSE_PROJECT_NAME}_default" \
                -v "$(pwd)/services/$service/src/main/resources/db/migration:/flyway/sql" \
                flyway/flyway:latest \
                -url="jdbc:postgresql://postgres:5432/${DB_NAME}" \
                -user="$DB_USER" \
                -password="$DB_PASSWORD" \
                -schemas="$service" \
                migrate
            
            print_success "Migrations completed for $service"
        else
            print_warning "No migrations found for $service"
        fi
    done
}

deploy_services() {
    print_header "Deploying Application Services"
    
    # Start discovery server first
    print_info "Starting Discovery Server..."
    docker-compose -f infra/docker/docker-compose.yml up -d discovery-server
    
    # Wait for discovery server
    print_info "Waiting for Discovery Server to be ready..."
    timeout=120
    while ! curl -f http://localhost:8761/actuator/health &> /dev/null; do
        sleep 5
        timeout=$((timeout - 5))
        if [ $timeout -le 0 ]; then
            print_error "Discovery Server failed to start within 120 seconds"
            exit 1
        fi
    done
    print_success "Discovery Server is ready"
    
    # Start other services
    services=("auth-service" "tenant-service" "users-service" "api-gateway")
    
    for service in "${services[@]}"; do
        print_info "Starting $service..."
        docker-compose -f infra/docker/docker-compose.yml up -d "$service"
        
        # Wait a bit between services to avoid overwhelming the system
        sleep 10
    done
    
    print_success "All services started"
}

verify_deployment() {
    print_header "Verifying Deployment"
    
    # Check service health
    services=("discovery-server:8761" "api-gateway:8080" "auth-service:8081" "tenant-service:8082" "users-service:8083")
    
    for service_port in "${services[@]}"; do
        service=$(echo "$service_port" | cut -d':' -f1)
        port=$(echo "$service_port" | cut -d':' -f2)
        
        print_info "Checking health of $service on port $port..."
        
        timeout=60
        while ! curl -f "http://localhost:$port/actuator/health" &> /dev/null; do
            sleep 5
            timeout=$((timeout - 5))
            if [ $timeout -le 0 ]; then
                print_warning "$service health check failed"
                break
            fi
        done
        
        if [ $timeout -gt 0 ]; then
            print_success "$service is healthy"
        fi
    done
    
    # Check service registration with discovery server
    print_info "Checking service registration..."
    if curl -s http://localhost:8761/eureka/apps | grep -q "application"; then
        print_success "Services are registered with discovery server"
    else
        print_warning "Some services may not be registered"
    fi
}

setup_monitoring() {
    print_header "Setting Up Monitoring"
    
    if [ "$ENVIRONMENT" != "dev" ]; then
        print_info "Starting monitoring services..."
        
        # Start Prometheus
        docker-compose -f infra/docker/docker-compose.yml up -d prometheus
        
        # Start Grafana
        docker-compose -f infra/docker/docker-compose.yml up -d grafana
        
        # Start ELK stack for logging
        docker-compose -f infra/docker/docker-compose.yml up -d elasticsearch logstash kibana
        
        print_success "Monitoring services started"
        print_info "Grafana: http://localhost:3000 (admin/admin)"
        print_info "Prometheus: http://localhost:9090"
        print_info "Kibana: http://localhost:5601"
    else
        print_info "Monitoring setup skipped for development environment"
    fi
}

create_backup() {
    if [ "$ENVIRONMENT" = "prod" ]; then
        print_header "Creating Backup"
        
        BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
        mkdir -p "$BACKUP_DIR"
        
        # Backup database
        print_info "Creating database backup..."
        docker-compose -f infra/docker/docker-compose.yml exec -T postgres \
            pg_dump -U "$DB_USER" "$DB_NAME" > "$BACKUP_DIR/database.sql"
        
        # Backup configuration
        print_info "Backing up configuration..."
        cp -r infra/config "$BACKUP_DIR/"
        
        print_success "Backup created in $BACKUP_DIR"
    fi
}

rollback_deployment() {
    print_error "Deployment failed, initiating rollback..."
    
    # Stop all services
    docker-compose -f infra/docker/docker-compose.yml down
    
    # Restore from backup if available
    if [ "$ENVIRONMENT" = "prod" ] && [ -d "backups" ]; then
        LATEST_BACKUP=$(ls -t backups/ | head -n1)
        if [ -n "$LATEST_BACKUP" ]; then
            print_info "Restoring from backup: $LATEST_BACKUP"
            # Add restore logic here
        fi
    fi
    
    print_error "Rollback completed"
    exit 1
}

show_deployment_info() {
    print_header "Deployment Information"
    
    echo "Environment: $ENVIRONMENT"
    echo "Docker Registry: $DOCKER_REGISTRY"
    echo "Image Tag: $IMAGE_TAG"
    echo "Database: $DB_NAME"
    echo ""
    echo "Service URLs:"
    echo "  Discovery Server: http://localhost:8761"
    echo "  API Gateway: http://localhost:8080"
    echo "  Auth Service: http://localhost:8081"
    echo "  Tenant Service: http://localhost:8082"
    echo "  Users Service: http://localhost:8083"
    echo ""
    
    if [ "$ENVIRONMENT" != "dev" ]; then
        echo "Monitoring URLs:"
        echo "  Grafana: http://localhost:3000"
        echo "  Prometheus: http://localhost:9090"
        echo "  Kibana: http://localhost:5601"
        echo ""
    fi
    
    echo "Useful Commands:"
    echo "  View logs: docker-compose -f infra/docker/docker-compose.yml logs -f [service]"
    echo "  Scale service: docker-compose -f infra/docker/docker-compose.yml up -d --scale [service]=[replicas]"
    echo "  Stop all: docker-compose -f infra/docker/docker-compose.yml down"
    echo "  Health check: curl http://localhost:8080/actuator/health"
}

show_help() {
    echo "CRM Platform Deployment Script"
    echo ""
    echo "Usage: $0 [ENVIRONMENT] [OPTIONS]"
    echo ""
    echo "Environments:"
    echo "  dev, development    Deploy to development environment"
    echo "  staging            Deploy to staging environment"
    echo "  prod, production   Deploy to production environment"
    echo ""
    echo "Options:"
    echo "  -h, --help         Show this help message"
    echo "  --rollback         Rollback the deployment"
    echo "  --stop             Stop all services"
    echo "  --restart          Restart all services"
    echo "  --logs             Show logs for all services"
    echo ""
    echo "Environment Variables:"
    echo "  DOCKER_REGISTRY    Docker registry URL (default: ghcr.io)"
    echo "  IMAGE_TAG          Docker image tag (default: latest)"
    echo "  DB_USER            Database username"
    echo "  DB_PASSWORD        Database password"
    echo ""
    echo "Examples:"
    echo "  $0 dev             Deploy to development"
    echo "  $0 staging         Deploy to staging"
    echo "  $0 prod            Deploy to production"
    echo "  $0 dev --stop      Stop development environment"
}

# Parse command line arguments
case "${2:-}" in
    --rollback)
        rollback_deployment
        exit 0
        ;;
    --stop)
        print_info "Stopping all services..."
        docker-compose -f infra/docker/docker-compose.yml down
        print_success "All services stopped"
        exit 0
        ;;
    --restart)
        print_info "Restarting all services..."
        docker-compose -f infra/docker/docker-compose.yml restart
        print_success "All services restarted"
        exit 0
        ;;
    --logs)
        docker-compose -f infra/docker/docker-compose.yml logs -f
        exit 0
        ;;
    -h|--help)
        show_help
        exit 0
        ;;
esac

# Main deployment process
main() {
    print_header "CRM Platform Deployment - $ENVIRONMENT"
    
    # Set up error handling
    trap rollback_deployment ERR
    
    check_prerequisites
    setup_environment
    create_backup
    pull_images
    start_infrastructure
    run_migrations
    deploy_services
    verify_deployment
    setup_monitoring
    show_deployment_info
    
    print_success "Deployment to $ENVIRONMENT completed successfully!"
}

# Run main function
main