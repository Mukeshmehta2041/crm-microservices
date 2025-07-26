#!/bin/bash

# Development deployment script

set -e

echo "Starting CRM Platform in development mode..."

# Change to docker directory
cd "$(dirname "$0")/../infra/docker"

# Stop any running containers
echo "Stopping existing containers..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

# Start infrastructure services first
echo "Starting infrastructure services..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres redis

# Wait for infrastructure to be ready
echo "Waiting for infrastructure services to be ready..."
sleep 20

# Start discovery server
echo "Starting discovery server..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d discovery-server
sleep 15

# Start application services
echo "Starting application services..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d auth-service tenant-service users-service

echo "CRM Platform development environment started successfully!"
echo "Services available at:"
echo "  - Discovery Server: http://localhost:8761"
echo "  - Auth Service: http://localhost:8081 (Debug: 5006)"
echo "  - Tenant Service: http://localhost:8082 (Debug: 5007)"
echo "  - Users Service: http://localhost:8083 (Debug: 5008)"
echo ""
echo "Debug ports for remote debugging:"
echo "  - Discovery Server: 5005"
echo "  - Auth Service: 5006"
echo "  - Tenant Service: 5007"
echo "  - Users Service: 5008"

# Show running containers
docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps