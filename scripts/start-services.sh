#!/bin/bash

# Quick start script for CRM Platform services

set -e

MODE=${1:-"dev"}

echo "Starting CRM Platform in $MODE mode..."

case $MODE in
  "dev"|"development")
    echo "Starting development environment with hot reload..."
    # Use root docker-compose.override.yml for easy development
    docker-compose up -d postgres redis
    sleep 10
    docker-compose up -d discovery-server
    sleep 15
    docker-compose up -d auth-service tenant-service users-service
    ;;
  "prod"|"production")
    echo "Starting production environment..."
    ./scripts/docker-run-prod.sh
    ;;
  *)
    echo "Usage: $0 [dev|prod]"
    echo "  dev  - Start development environment (default)"
    echo "  prod - Start production environment"
    exit 1
    ;;
esac

echo ""
echo "CRM Platform started successfully!"
echo "Check service status with: docker-compose ps"
echo "View logs with: docker-compose logs -f [service-name]"