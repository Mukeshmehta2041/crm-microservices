#!/bin/bash

# Docker Registry Management Script for CRM Microservices Platform
# This script manages Docker image building, tagging, and pushing to registry

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REGISTRY_URL="${DOCKER_REGISTRY_URL:-localhost:5000}"
PROJECT_NAME="crm-platform"
BUILD_TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
VERSION="${BUILD_VERSION:-latest}"

# Services to build
SERVICES=(
    "discovery-server"
    "auth-service"
    "tenant-service"
    "users-service"
    "api-gateway"
    "contacts-service"
    "deals-service"
    "leads-service"
    "accounts-service"
    "activities-service"
    "pipelines-service"
    "custom-objects-service"
    "analytics-service"
    "workflow-service"
)

# Function to display usage
usage() {
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  build [service]     Build Docker images (all services if no service specified)"
    echo "  push [service]      Push Docker images to registry"
    echo "  pull [service]      Pull Docker images from registry"
    echo "  tag [service]       Tag Docker images"
    echo "  clean              Clean up local Docker images"
    echo "  list               List all built images"
    echo "  setup-registry     Set up local Docker registry"
    echo ""
    echo "Options:"
    echo "  --registry URL     Docker registry URL (default: ${REGISTRY_URL})"
    echo "  --version VERSION  Image version tag (default: ${VERSION})"
    echo "  --no-cache         Build without using cache"
    echo "  --parallel         Build images in parallel"
    echo ""
    echo "Environment Variables:"
    echo "  DOCKER_REGISTRY_URL    Docker registry URL"
    echo "  BUILD_VERSION          Version tag for images"
    echo ""
    echo "Examples:"
    echo "  $0 build                           # Build all services"
    echo "  $0 build auth-service              # Build specific service"
    echo "  $0 push --version v1.0.0          # Push all images with version tag"
    echo "  $0 build --parallel --no-cache    # Build all services in parallel without cache"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Error: Docker is not running${NC}"
        exit 1
    fi
}

# Function to validate service name
validate_service() {
    local service=$1
    for valid_service in "${SERVICES[@]}"; do
        if [[ "$valid_service" == "$service" ]]; then
            return 0
        fi
    done
    echo -e "${RED}Error: Invalid service name '$service'${NC}"
    echo -e "${YELLOW}Valid services: ${SERVICES[*]}${NC}"
    exit 1
}

# Function to build a single service
build_service() {
    local service=$1
    local no_cache=$2
    local image_name="${PROJECT_NAME}/${service}"
    local full_image_name="${REGISTRY_URL}/${image_name}"
    
    echo -e "${BLUE}Building ${service}...${NC}"
    
    local build_args=""
    if [[ "$no_cache" == "true" ]]; then
        build_args="--no-cache"
    fi
    
    # Build the image
    docker build ${build_args} \
        -t "${image_name}:latest" \
        -t "${image_name}:${VERSION}" \
        -t "${image_name}:${GIT_COMMIT}" \
        -t "${full_image_name}:latest" \
        -t "${full_image_name}:${VERSION}" \
        -t "${full_image_name}:${GIT_COMMIT}" \
        --label "build.timestamp=${BUILD_TIMESTAMP}" \
        --label "build.version=${VERSION}" \
        --label "build.commit=${GIT_COMMIT}" \
        --label "service.name=${service}" \
        -f "services/${service}/Dockerfile" \
        .
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ Successfully built ${service}${NC}"
        
        # Display image size
        local image_size=$(docker images "${image_name}:latest" --format "table {{.Size}}" | tail -n 1)
        echo -e "${BLUE}   Image size: ${image_size}${NC}"
        
        return 0
    else
        echo -e "${RED}❌ Failed to build ${service}${NC}"
        return 1
    fi
}

# Function to build all services
build_all_services() {
    local no_cache=$1
    local parallel=$2
    
    echo -e "${BLUE}Building all services...${NC}"
    echo -e "${BLUE}Registry: ${REGISTRY_URL}${NC}"
    echo -e "${BLUE}Version: ${VERSION}${NC}"
    echo -e "${BLUE}Commit: ${GIT_COMMIT}${NC}"
    echo ""
    
    local failed_services=()
    
    if [[ "$parallel" == "true" ]]; then
        echo -e "${YELLOW}Building services in parallel...${NC}"
        
        # Build services in parallel
        local pids=()
        for service in "${SERVICES[@]}"; do
            build_service "$service" "$no_cache" &
            pids+=($!)
        done
        
        # Wait for all builds to complete
        for pid in "${pids[@]}"; do
            if ! wait $pid; then
                failed_services+=("unknown")
            fi
        done
    else
        # Build services sequentially
        for service in "${SERVICES[@]}"; do
            if ! build_service "$service" "$no_cache"; then
                failed_services+=("$service")
            fi
        done
    fi
    
    # Report results
    echo ""
    echo -e "${BLUE}=== Build Summary ===${NC}"
    echo -e "${GREEN}Successful builds: $((${#SERVICES[@]} - ${#failed_services[@]}))${NC}"
    echo -e "${RED}Failed builds: ${#failed_services[@]}${NC}"
    
    if [[ ${#failed_services[@]} -gt 0 ]]; then
        echo -e "${RED}Failed services:${NC}"
        for service in "${failed_services[@]}"; do
            echo -e "${RED}  - ${service}${NC}"
        done
        return 1
    fi
    
    return 0
}

# Function to push images to registry
push_service() {
    local service=$1
    local image_name="${PROJECT_NAME}/${service}"
    local full_image_name="${REGISTRY_URL}/${image_name}"
    
    echo -e "${BLUE}Pushing ${service} to registry...${NC}"
    
    # Push all tags
    docker push "${full_image_name}:latest"
    docker push "${full_image_name}:${VERSION}"
    docker push "${full_image_name}:${GIT_COMMIT}"
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ Successfully pushed ${service}${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to push ${service}${NC}"
        return 1
    fi
}

# Function to push all services
push_all_services() {
    echo -e "${BLUE}Pushing all services to registry...${NC}"
    
    local failed_services=()
    
    for service in "${SERVICES[@]}"; do
        if ! push_service "$service"; then
            failed_services+=("$service")
        fi
    done
    
    # Report results
    echo ""
    echo -e "${BLUE}=== Push Summary ===${NC}"
    echo -e "${GREEN}Successful pushes: $((${#SERVICES[@]} - ${#failed_services[@]}))${NC}"
    echo -e "${RED}Failed pushes: ${#failed_services[@]}${NC}"
    
    if [[ ${#failed_services[@]} -gt 0 ]]; then
        echo -e "${RED}Failed services:${NC}"
        for service in "${failed_services[@]}"; do
            echo -e "${RED}  - ${service}${NC}"
        done
        return 1
    fi
    
    return 0
}

# Function to pull images from registry
pull_service() {
    local service=$1
    local image_name="${PROJECT_NAME}/${service}"
    local full_image_name="${REGISTRY_URL}/${image_name}"
    
    echo -e "${BLUE}Pulling ${service} from registry...${NC}"
    
    docker pull "${full_image_name}:${VERSION}"
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ Successfully pulled ${service}${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to pull ${service}${NC}"
        return 1
    fi
}

# Function to list all images
list_images() {
    echo -e "${BLUE}=== CRM Platform Images ===${NC}"
    echo ""
    
    for service in "${SERVICES[@]}"; do
        local image_name="${PROJECT_NAME}/${service}"
        echo -e "${YELLOW}${service}:${NC}"
        docker images "${image_name}" --format "table {{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | head -n 10
        echo ""
    done
}

# Function to clean up images
clean_images() {
    echo -e "${BLUE}Cleaning up Docker images...${NC}"
    
    # Remove dangling images
    echo -e "${YELLOW}Removing dangling images...${NC}"
    docker image prune -f
    
    # Remove old images (keep latest 3 versions)
    for service in "${SERVICES[@]}"; do
        local image_name="${PROJECT_NAME}/${service}"
        echo -e "${YELLOW}Cleaning up ${service} images...${NC}"
        
        # Get image IDs sorted by creation date (oldest first)
        local old_images=$(docker images "${image_name}" --format "{{.ID}}" | tail -n +4)
        
        if [[ -n "$old_images" ]]; then
            echo "$old_images" | xargs docker rmi -f 2>/dev/null || true
        fi
    done
    
    echo -e "${GREEN}✅ Cleanup completed${NC}"
}

# Function to set up local Docker registry
setup_registry() {
    echo -e "${BLUE}Setting up local Docker registry...${NC}"
    
    # Check if registry is already running
    if docker ps | grep -q "registry:2"; then
        echo -e "${YELLOW}Registry is already running${NC}"
        return 0
    fi
    
    # Start local registry
    docker run -d \
        -p 5000:5000 \
        --restart=always \
        --name registry \
        -v registry_data:/var/lib/registry \
        registry:2
    
    echo -e "${GREEN}✅ Local registry started on localhost:5000${NC}"
    
    # Create registry UI (optional)
    docker run -d \
        -p 8080:8080 \
        --restart=always \
        --name registry-ui \
        -e REGISTRY_URL=http://registry:5000 \
        --link registry \
        joxit/docker-registry-ui:static
    
    echo -e "${GREEN}✅ Registry UI available at http://localhost:8080${NC}"
}

# Main function
main() {
    local command=""
    local service=""
    local no_cache="false"
    local parallel="false"
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            build|push|pull|tag|clean|list|setup-registry)
                command=$1
                shift
                ;;
            --registry)
                REGISTRY_URL=$2
                shift 2
                ;;
            --version)
                VERSION=$2
                shift 2
                ;;
            --no-cache)
                no_cache="true"
                shift
                ;;
            --parallel)
                parallel="true"
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                if [[ -z "$service" && "$command" != "clean" && "$command" != "list" && "$command" != "setup-registry" ]]; then
                    service=$1
                fi
                shift
                ;;
        esac
    done
    
    # Check if command is provided
    if [[ -z "$command" ]]; then
        echo -e "${RED}Error: No command specified${NC}"
        usage
        exit 1
    fi
    
    # Check Docker
    check_docker
    
    # Execute command
    case $command in
        build)
            if [[ -n "$service" ]]; then
                validate_service "$service"
                build_service "$service" "$no_cache"
            else
                build_all_services "$no_cache" "$parallel"
            fi
            ;;
        push)
            if [[ -n "$service" ]]; then
                validate_service "$service"
                push_service "$service"
            else
                push_all_services
            fi
            ;;
        pull)
            if [[ -n "$service" ]]; then
                validate_service "$service"
                pull_service "$service"
            else
                for svc in "${SERVICES[@]}"; do
                    pull_service "$svc"
                done
            fi
            ;;
        clean)
            clean_images
            ;;
        list)
            list_images
            ;;
        setup-registry)
            setup_registry
            ;;
        *)
            echo -e "${RED}Error: Unknown command '$command'${NC}"
            usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"