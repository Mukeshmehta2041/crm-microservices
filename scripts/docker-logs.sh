#!/bin/bash

# View logs for CRM Platform services

SERVICE=${1:-"all"}

cd "$(dirname "$0")/../infra/docker"

case $SERVICE in
  "all")
    echo "Showing logs for all services..."
    docker-compose logs -f
    ;;
  "discovery")
    echo "Showing logs for discovery server..."
    docker-compose logs -f discovery-server
    ;;
  "auth")
    echo "Showing logs for auth service..."
    docker-compose logs -f auth-service
    ;;
  "tenant")
    echo "Showing logs for tenant service..."
    docker-compose logs -f tenant-service
    ;;
  "users")
    echo "Showing logs for users service..."
    docker-compose logs -f users-service
    ;;
  "postgres")
    echo "Showing logs for postgres..."
    docker-compose logs -f postgres
    ;;
  "redis")
    echo "Showing logs for redis..."
    docker-compose logs -f redis
    ;;
  *)
    echo "Usage: $0 [all|discovery|auth|tenant|users|postgres|redis]"
    echo "Default: all"
    ;;
esac