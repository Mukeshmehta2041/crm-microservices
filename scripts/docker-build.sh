#!/bin/bash

# Build script for CRM Platform Docker images

set -e

echo "Building CRM Platform Docker images..."

# Change to project root
cd "$(dirname "$0")/.."

# Build production images
echo "Building production images..."
docker build -f services/discovery-server/Dockerfile -t crm-discovery-server:latest .
docker build -f services/auth-service/Dockerfile -t crm-auth-service:latest .
docker build -f services/tenant-service/Dockerfile -t crm-tenant-service:latest .
docker build -f services/users-service/Dockerfile -t crm-users-service:latest .

echo "Building development images..."
docker build -f services/discovery-server/Dockerfile.dev -t crm-discovery-server:dev .
docker build -f services/auth-service/Dockerfile.dev -t crm-auth-service:dev .
docker build -f services/tenant-service/Dockerfile.dev -t crm-tenant-service:dev .
docker build -f services/users-service/Dockerfile.dev -t crm-users-service:dev .

echo "Docker images built successfully!"
docker images | grep crm-