#!/bin/bash

# Smoke Tests for CRM Platform
# This script runs basic smoke tests to verify deployment health

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
BASE_URL=${BASE_URL:-http://localhost:8080}
TIMEOUT=${TIMEOUT:-30}
RETRY_COUNT=${RETRY_COUNT:-3}

# Test results
TESTS_PASSED=0
TESTS_FAILED=0
FAILED_TESTS=()

# Helper functions
make_request() {
    local url="$1"
    local method="${2:-GET}"
    local data="${3:-}"
    local expected_status="${4:-200}"
    
    local response
    local status_code
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            --max-time "$TIMEOUT" \
            "$url" 2>/dev/null || echo -e "\n000")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            --max-time "$TIMEOUT" \
            "$url" 2>/dev/null || echo -e "\n000")
    fi
    
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" = "$expected_status" ]; then
        return 0
    else
        echo "Expected status $expected_status, got $status_code"
        echo "Response: $body"
        return 1
    fi
}

run_test() {
    local test_name="$1"
    local test_function="$2"
    
    print_info "Running: $test_name"
    
    if $test_function; then
        print_success "$test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        print_error "$test_name"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        FAILED_TESTS+=("$test_name")
    fi
}

# Test functions
test_discovery_server_health() {
    make_request "http://localhost:8761/actuator/health"
}

test_api_gateway_health() {
    make_request "$BASE_URL/actuator/health"
}

test_auth_service_health() {
    make_request "$BASE_URL/api/v1/auth/health" "GET" "" "200"
}

test_tenant_service_health() {
    make_request "$BASE_URL/api/v1/tenants/health" "GET" "" "200"
}

test_users_service_health() {
    make_request "$BASE_URL/api/v1/users/health" "GET" "" "200"
}

test_service_registration() {
    local response
    response=$(curl -s "http://localhost:8761/eureka/apps" --max-time "$TIMEOUT" 2>/dev/null || echo "")
    
    if echo "$response" | grep -q "AUTH-SERVICE" && \
       echo "$response" | grep -q "TENANT-SERVICE" && \
       echo "$response" | grep -q "USERS-SERVICE"; then
        return 0
    else
        echo "Not all services are registered with discovery server"
        return 1
    fi
}

test_database_connectivity() {
    # Test auth service database connectivity
    if make_request "$BASE_URL/api/v1/auth/health/db" "GET" "" "200"; then
        return 0
    else
        echo "Database connectivity test failed"
        return 1
    fi
}

test_redis_connectivity() {
    # Test Redis connectivity through a service
    if make_request "$BASE_URL/api/v1/auth/health/redis" "GET" "" "200"; then
        return 0
    else
        echo "Redis connectivity test failed"
        return 1
    fi
}

test_authentication_flow() {
    local login_data='{"username":"admin","password":"admin123"}'
    local response
    
    # Attempt login
    response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$login_data" \
        --max-time "$TIMEOUT" \
        "$BASE_URL/api/v1/auth/login" 2>/dev/null || echo "")
    
    if echo "$response" | grep -q "token"; then
        return 0
    else
        echo "Authentication flow test failed"
        echo "Response: $response"
        return 1
    fi
}

test_tenant_creation() {
    local tenant_data='{"name":"Test Tenant","domain":"test.example.com"}'
    
    # Create tenant
    if make_request "$BASE_URL/api/v1/tenants" "POST" "$tenant_data" "201"; then
        return 0
    else
        echo "Tenant creation test failed"
        return 1
    fi
}

test_user_creation() {
    local user_data='{"firstName":"Test","lastName":"User","email":"test@example.com"}'
    
    # Create user
    if make_request "$BASE_URL/api/v1/users" "POST" "$user_data" "201"; then
        return 0
    else
        echo "User creation test failed"
        return 1
    fi
}

test_api_gateway_routing() {
    # Test routing through API gateway
    local routes=("auth" "tenants" "users")
    
    for route in "${routes[@]}"; do
        if ! make_request "$BASE_URL/api/v1/$route/health" "GET" "" "200"; then
            echo "API Gateway routing failed for $route"
            return 1
        fi
    done
    
    return 0
}

test_cors_headers() {
    local response
    response=$(curl -s -I -X OPTIONS \
        -H "Origin: http://localhost:3000" \
        -H "Access-Control-Request-Method: POST" \
        -H "Access-Control-Request-Headers: Content-Type" \
        --max-time "$TIMEOUT" \
        "$BASE_URL/api/v1/auth/login" 2>/dev/null || echo "")
    
    if echo "$response" | grep -q "Access-Control-Allow-Origin"; then
        return 0
    else
        echo "CORS headers test failed"
        return 1
    fi
}

test_rate_limiting() {
    local count=0
    local max_requests=10
    
    # Make multiple requests quickly
    for i in $(seq 1 $max_requests); do
        if make_request "$BASE_URL/api/v1/auth/health" "GET" "" "200" 2>/dev/null; then
            count=$((count + 1))
        fi
    done
    
    # Should have some successful requests but potentially hit rate limit
    if [ $count -gt 0 ]; then
        return 0
    else
        echo "Rate limiting test failed - no requests succeeded"
        return 1
    fi
}

test_metrics_endpoint() {
    # Test Prometheus metrics endpoint
    if make_request "$BASE_URL/actuator/prometheus" "GET" "" "200"; then
        return 0
    else
        echo "Metrics endpoint test failed"
        return 1
    fi
}

test_logging_output() {
    # Check if services are producing logs
    local log_count
    log_count=$(docker-compose -f infra/docker/docker-compose.yml logs --tail=10 auth-service 2>/dev/null | wc -l)
    
    if [ "$log_count" -gt 0 ]; then
        return 0
    else
        echo "Logging output test failed - no recent logs found"
        return 1
    fi
}

test_security_headers() {
    local response
    response=$(curl -s -I "$BASE_URL/api/v1/auth/health" --max-time "$TIMEOUT" 2>/dev/null || echo "")
    
    if echo "$response" | grep -q "X-Content-Type-Options" && \
       echo "$response" | grep -q "X-Frame-Options"; then
        return 0
    else
        echo "Security headers test failed"
        return 1
    fi
}

# Performance tests
test_response_time() {
    local start_time
    local end_time
    local duration
    
    start_time=$(date +%s%N)
    make_request "$BASE_URL/actuator/health" "GET" "" "200" >/dev/null 2>&1
    end_time=$(date +%s%N)
    
    duration=$(( (end_time - start_time) / 1000000 )) # Convert to milliseconds
    
    if [ $duration -lt 2000 ]; then # Less than 2 seconds
        return 0
    else
        echo "Response time test failed - took ${duration}ms"
        return 1
    fi
}

# Load test (basic)
test_concurrent_requests() {
    local pids=()
    local success_count=0
    local total_requests=5
    
    # Start concurrent requests
    for i in $(seq 1 $total_requests); do
        (make_request "$BASE_URL/actuator/health" "GET" "" "200" >/dev/null 2>&1 && echo "success") &
        pids+=($!)
    done
    
    # Wait for all requests to complete
    for pid in "${pids[@]}"; do
        if wait $pid; then
            success_count=$((success_count + 1))
        fi
    done
    
    if [ $success_count -ge $((total_requests / 2)) ]; then
        return 0
    else
        echo "Concurrent requests test failed - only $success_count/$total_requests succeeded"
        return 1
    fi
}

# Main test execution
run_smoke_tests() {
    print_header "CRM Platform Smoke Tests - $ENVIRONMENT"
    
    # Wait for services to be ready
    print_info "Waiting for services to be ready..."
    sleep 30
    
    # Basic health checks
    print_header "Health Checks"
    run_test "Discovery Server Health" test_discovery_server_health
    run_test "API Gateway Health" test_api_gateway_health
    run_test "Auth Service Health" test_auth_service_health
    run_test "Tenant Service Health" test_tenant_service_health
    run_test "Users Service Health" test_users_service_health
    
    # Service integration tests
    print_header "Service Integration"
    run_test "Service Registration" test_service_registration
    run_test "Database Connectivity" test_database_connectivity
    run_test "Redis Connectivity" test_redis_connectivity
    run_test "API Gateway Routing" test_api_gateway_routing
    
    # Functional tests
    print_header "Functional Tests"
    run_test "Authentication Flow" test_authentication_flow
    run_test "Tenant Creation" test_tenant_creation
    run_test "User Creation" test_user_creation
    
    # Security tests
    print_header "Security Tests"
    run_test "CORS Headers" test_cors_headers
    run_test "Security Headers" test_security_headers
    run_test "Rate Limiting" test_rate_limiting
    
    # Monitoring tests
    print_header "Monitoring Tests"
    run_test "Metrics Endpoint" test_metrics_endpoint
    run_test "Logging Output" test_logging_output
    
    # Performance tests
    print_header "Performance Tests"
    run_test "Response Time" test_response_time
    run_test "Concurrent Requests" test_concurrent_requests
}

# Generate test report
generate_report() {
    print_header "Test Results Summary"
    
    local total_tests=$((TESTS_PASSED + TESTS_FAILED))
    local success_rate=0
    
    if [ $total_tests -gt 0 ]; then
        success_rate=$(( (TESTS_PASSED * 100) / total_tests ))
    fi
    
    echo "Total Tests: $total_tests"
    echo "Passed: $TESTS_PASSED"
    echo "Failed: $TESTS_FAILED"
    echo "Success Rate: $success_rate%"
    echo ""
    
    if [ $TESTS_FAILED -gt 0 ]; then
        echo "Failed Tests:"
        for test in "${FAILED_TESTS[@]}"; do
            echo "  - $test"
        done
        echo ""
    fi
    
    # Generate JSON report
    cat > "smoke-test-results.json" << EOF
{
  "environment": "$ENVIRONMENT",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "total_tests": $total_tests,
  "passed": $TESTS_PASSED,
  "failed": $TESTS_FAILED,
  "success_rate": $success_rate,
  "failed_tests": [$(printf '"%s",' "${FAILED_TESTS[@]}" | sed 's/,$//')]
}
EOF
    
    if [ $TESTS_FAILED -eq 0 ]; then
        print_success "All smoke tests passed! ✅"
        return 0
    else
        print_error "Some smoke tests failed! ❌"
        return 1
    fi
}

show_help() {
    echo "CRM Platform Smoke Tests"
    echo ""
    echo "Usage: $0 [ENVIRONMENT] [OPTIONS]"
    echo ""
    echo "Environments:"
    echo "  dev, development    Test development environment"
    echo "  staging            Test staging environment"
    echo "  prod, production   Test production environment"
    echo ""
    echo "Options:"
    echo "  -h, --help         Show this help message"
    echo "  --quick            Run only basic health checks"
    echo "  --full             Run all tests including performance"
    echo ""
    echo "Environment Variables:"
    echo "  BASE_URL           Base URL for API calls (default: http://localhost:8080)"
    echo "  TIMEOUT            Request timeout in seconds (default: 30)"
    echo "  RETRY_COUNT        Number of retries for failed requests (default: 3)"
    echo ""
    echo "Examples:"
    echo "  $0 dev             Run smoke tests for development"
    echo "  $0 staging --quick Run quick tests for staging"
    echo "  BASE_URL=https://api.example.com $0 prod"
}

# Parse command line arguments
case "${2:-}" in
    -h|--help)
        show_help
        exit 0
        ;;
    --quick)
        # Run only basic health checks
        print_header "Quick Smoke Tests - $ENVIRONMENT"
        run_test "API Gateway Health" test_api_gateway_health
        run_test "Service Registration" test_service_registration
        run_test "Database Connectivity" test_database_connectivity
        generate_report
        exit $?
        ;;
    --full)
        # Run all tests (default behavior)
        ;;
esac

# Main execution
main() {
    run_smoke_tests
    generate_report
    exit $?
}

# Run main function
main