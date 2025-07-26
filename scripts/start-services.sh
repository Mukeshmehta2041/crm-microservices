#!/bin/bash

echo "Starting CRM Microservices Platform..."

# Function to check if a port is in use
check_port() {
    lsof -i :$1 > /dev/null 2>&1
    return $?
}

# Function to start a service
start_service() {
    local service_name=$1
    local port=$2
    
    echo "Starting $service_name on port $port..."
    
    if check_port $port; then
        echo "$service_name is already running on port $port"
        return 0
    fi
    
    # Start the service in background
    mvn spring-boot:run -pl services/$service_name > logs/$service_name.log 2>&1 &
    local pid=$!
    
    echo "Started $service_name with PID $pid"
    echo $pid > pids/$service_name.pid
    
    # Wait a bit for service to start
    sleep 5
    
    # Check if service is running
    if check_port $port; then
        echo "$service_name started successfully on port $port"
        return 0
    else
        echo "Failed to start $service_name"
        return 1
    fi
}

# Create directories for logs and pids
mkdir -p logs pids

# Start services in order
echo "=== Starting Discovery Server ==="
start_service "discovery-server" 8761

echo "=== Starting Auth Service ==="
start_service "auth-service" 8081

echo "=== Starting Tenant Service ==="
start_service "tenant-service" 8082

echo "=== Starting Users Service ==="
start_service "users-service" 8083

echo "All services started!"
echo "Discovery Server: http://localhost:8761"
echo "Auth Service: http://localhost:8081"
echo "Tenant Service: http://localhost:8082"
echo "Users Service: http://localhost:8083"