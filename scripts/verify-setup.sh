#!/bin/bash

# Verification script for CRM Platform services

set -e

echo "Verifying CRM Platform setup..."

# Function to check if service is responding
check_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo -n "Checking $service_name... "
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            echo "✓ OK"
            return 0
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "✗ FAILED (timeout after $max_attempts attempts)"
    return 1
}

# Check infrastructure services
echo "=== Infrastructure Services ==="
check_service "PostgreSQL" "http://localhost:5432" || echo "  Note: PostgreSQL check via HTTP not available, check with: docker-compose exec postgres pg_isready"
check_service "Redis" "http://localhost:6379" || echo "  Note: Redis check via HTTP not available, check with: docker-compose exec redis redis-cli ping"

# Check application services
echo ""
echo "=== Application Services ==="
check_service "Discovery Server" "http://localhost:8761/actuator/health"
check_service "Auth Service" "http://localhost:8081/actuator/health"
check_service "Tenant Service" "http://localhost:8082/actuator/health"
check_service "Users Service" "http://localhost:8083/actuator/health"

# Check service registration with Eureka
echo ""
echo "=== Service Registration ==="
echo "Checking Eureka service registry..."
if curl -s "http://localhost:8761/eureka/apps" | grep -q "application"; then
    echo "✓ Services registered with Eureka"
    echo "Registered services:"
    curl -s "http://localhost:8761/eureka/apps" | grep -o '<name>[^<]*</name>' | sed 's/<name>//g' | sed 's/<\/name>//g' | sort | uniq | sed 's/^/  - /'
else
    echo "✗ No services registered with Eureka yet (may take a few minutes)"
fi

echo ""
echo "=== Container Status ==="
docker-compose ps

echo ""
echo "=== Quick Access URLs ==="
echo "Discovery Server: http://localhost:8761"
echo "Auth Service Health: http://localhost:8081/actuator/health"
echo "Tenant Service Health: http://localhost:8082/actuator/health"
echo "Users Service Health: http://localhost:8083/actuator/health"

echo ""
echo "Verification complete!"