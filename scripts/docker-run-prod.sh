#!/bin/bash

# Production deployment script

set -e

echo "Starting CRM Platform in production mode..."

# Change to docker directory
cd "$(dirname "$0")/../infra/docker"

# Stop any running containers
echo "Stopping existing containers..."
docker-compose down

# Start infrastructure services first
echo "Starting infrastructure services..."
docker-compose up -d postgres redis kafka zookeeper elasticsearch

# Wait for infrastructure to be ready
echo "Waiting for infrastructure services to be ready..."
sleep 30

# Start application services
echo "Starting application services..."
docker-compose up -d discovery-server
sleep 20

docker-compose up -d auth-service tenant-service users-service

# Start monitoring services
echo "Starting monitoring services..."
docker-compose up -d prometheus grafana zipkin

echo "CRM Platform started successfully!"
echo "Services available at:"
echo "  - Discovery Server: http://localhost:8761"
echo "  - Auth Service: http://localhost:8081"
echo "  - Tenant Service: http://localhost:8082"
echo "  - Users Service: http://localhost:8083"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Prometheus: http://localhost:9090"
echo "  - Zipkin: http://localhost:9411"

# Show running containers
docker-compose ps