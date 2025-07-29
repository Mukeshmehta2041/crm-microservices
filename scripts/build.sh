#!/bin/bash

# CRM Platform Build Script
# This script provides comprehensive build and quality checks for the CRM platform

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
SKIP_TESTS=${SKIP_TESTS:-false}
SKIP_QUALITY_CHECKS=${SKIP_QUALITY_CHECKS:-false}
PROFILE=${PROFILE:-dev}
CLEAN=${CLEAN:-true}

# Functions
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

check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check Java version
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            print_success "Java $JAVA_VERSION found"
        else
            print_error "Java 17 or higher required. Found Java $JAVA_VERSION"
            exit 1
        fi
    else
        print_error "Java not found. Please install Java 17 or higher"
        exit 1
    fi
    
    # Check Maven wrapper
    if [ -f "./mvnw" ]; then
        print_success "Maven wrapper found"
    else
        print_error "Maven wrapper not found. Run 'mvn wrapper:wrapper' first"
        exit 1
    fi
    
    # Check Docker (optional)
    if command -v docker &> /dev/null; then
        print_success "Docker found"
    else
        print_warning "Docker not found. Docker builds will be skipped"
    fi
}

clean_build() {
    if [ "$CLEAN" = "true" ]; then
        print_header "Cleaning Previous Build"
        ./mvnw clean -q
        print_success "Clean completed"
    fi
}

compile_code() {
    print_header "Compiling Code"
    ./mvnw compile -P$PROFILE -q
    print_success "Compilation completed"
}

run_tests() {
    if [ "$SKIP_TESTS" = "false" ]; then
        print_header "Running Tests"
        
        # Unit tests
        print_info "Running unit tests..."
        ./mvnw test -P$PROFILE
        print_success "Unit tests completed"
        
        # Integration tests
        print_info "Running integration tests..."
        ./mvnw verify -P$PROFILE -DskipUnitTests=true
        print_success "Integration tests completed"
        
        # Generate test report
        print_info "Generating test coverage report..."
        ./mvnw jacoco:report -q
        print_success "Test coverage report generated"
    else
        print_warning "Tests skipped"
    fi
}

run_quality_checks() {
    if [ "$SKIP_QUALITY_CHECKS" = "false" ]; then
        print_header "Running Quality Checks"
        
        # Code formatting check
        print_info "Checking code formatting..."
        if ./mvnw spotless:check -q; then
            print_success "Code formatting check passed"
        else
            print_warning "Code formatting issues found. Run 'mvn spotless:apply' to fix"
        fi
        
        # Checkstyle
        print_info "Running Checkstyle..."
        ./mvnw checkstyle:check -q
        print_success "Checkstyle check passed"
        
        # PMD
        print_info "Running PMD..."
        ./mvnw pmd:check -q
        print_success "PMD check passed"
        
        # SpotBugs
        print_info "Running SpotBugs..."
        ./mvnw spotbugs:check -q
        print_success "SpotBugs check passed"
        
        # Dependency check
        print_info "Running dependency vulnerability check..."
        ./mvnw dependency-check:check -q
        print_success "Dependency check passed"
    else
        print_warning "Quality checks skipped"
    fi
}

package_artifacts() {
    print_header "Packaging Artifacts"
    ./mvnw package -P$PROFILE -DskipTests=$SKIP_TESTS -q
    print_success "Packaging completed"
}

build_docker_images() {
    if command -v docker &> /dev/null; then
        print_header "Building Docker Images"
        
        # Build all service images
        services=("discovery-server" "api-gateway" "auth-service" "tenant-service" "users-service")
        
        for service in "${services[@]}"; do
            if [ -d "services/$service" ]; then
                print_info "Building Docker image for $service..."
                cd "services/$service"
                docker build -t "crm-platform/$service:latest" .
                cd ../..
                print_success "Docker image built for $service"
            fi
        done
    else
        print_warning "Docker not available. Skipping Docker image builds"
    fi
}

generate_reports() {
    print_header "Generating Reports"
    
    # Create reports directory
    mkdir -p target/reports
    
    # Copy test reports
    if [ -d "target/site/jacoco" ]; then
        cp -r target/site/jacoco target/reports/
        print_success "Test coverage report copied"
    fi
    
    # Copy quality reports
    find . -name "checkstyle-result.xml" -exec cp {} target/reports/ \; 2>/dev/null || true
    find . -name "pmd.xml" -exec cp {} target/reports/ \; 2>/dev/null || true
    find . -name "spotbugsXml.xml" -exec cp {} target/reports/ \; 2>/dev/null || true
    
    print_success "Reports generated in target/reports/"
}

print_summary() {
    print_header "Build Summary"
    
    echo "Profile: $PROFILE"
    echo "Tests: $([ "$SKIP_TESTS" = "false" ] && echo "Enabled" || echo "Skipped")"
    echo "Quality Checks: $([ "$SKIP_QUALITY_CHECKS" = "false" ] && echo "Enabled" || echo "Skipped")"
    echo "Docker: $(command -v docker &> /dev/null && echo "Available" || echo "Not Available")"
    
    if [ -d "target/reports" ]; then
        echo "Reports: target/reports/"
    fi
    
    print_success "Build completed successfully!"
}

show_help() {
    echo "CRM Platform Build Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -p, --profile PROFILE   Maven profile to use (default: dev)"
    echo "  -s, --skip-tests        Skip running tests"
    echo "  -q, --skip-quality      Skip quality checks"
    echo "  -n, --no-clean          Skip clean before build"
    echo "  -d, --docker-only       Only build Docker images"
    echo "  -f, --format            Format code and exit"
    echo ""
    echo "Environment Variables:"
    echo "  SKIP_TESTS              Skip tests (true/false)"
    echo "  SKIP_QUALITY_CHECKS     Skip quality checks (true/false)"
    echo "  PROFILE                 Maven profile to use"
    echo "  CLEAN                   Clean before build (true/false)"
    echo ""
    echo "Examples:"
    echo "  $0                      Full build with all checks"
    echo "  $0 -s                   Build without tests"
    echo "  $0 -p prod              Build with production profile"
    echo "  $0 -q -s                Quick build (no tests, no quality checks)"
    echo "  $0 -f                   Format code only"
    echo "  $0 -d                   Build Docker images only"
}

format_code() {
    print_header "Formatting Code"
    ./mvnw spotless:apply -q
    print_success "Code formatting completed"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -q|--skip-quality)
            SKIP_QUALITY_CHECKS=true
            shift
            ;;
        -n|--no-clean)
            CLEAN=false
            shift
            ;;
        -d|--docker-only)
            check_prerequisites
            build_docker_images
            exit 0
            ;;
        -f|--format)
            format_code
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Main build process
main() {
    print_header "CRM Platform Build Started"
    
    check_prerequisites
    clean_build
    compile_code
    run_tests
    run_quality_checks
    package_artifacts
    build_docker_images
    generate_reports
    print_summary
}

# Run main function
main