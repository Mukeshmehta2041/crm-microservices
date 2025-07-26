#!/bin/bash

# Stop all CRM Platform services

set -e

echo "Stopping CRM Platform services..."

# Change to docker directory
cd "$(dirname "$0")/../infra/docker"

# Stop development environment
echo "Stopping development environment..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

# Stop production environment
echo "Stopping production environment..."
docker-compose down

# Remove orphaned containers
docker-compose down --remove-orphans

echo "All CRM Platform services stopped successfully!"

# Show remaining containers
echo "Remaining containers:"
docker ps --filter "name=crm-"