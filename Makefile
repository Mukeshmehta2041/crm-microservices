# CRM Microservices Platform Makefile

.PHONY: help build test clean start stop restart logs status

# Default target
help:
	@echo "CRM Microservices Platform - Available commands:"
	@echo ""
	@echo "  build          - Build all services"
	@echo "  test           - Run all tests"
	@echo "  test-unit      - Run unit tests only"
	@echo "  test-integration - Run integration tests only"
	@echo "  clean          - Clean build artifacts"
	@echo "  start          - Start all services in development mode"
	@echo "  start-infra    - Start infrastructure services only"
	@echo "  stop           - Stop all services"
	@echo "  restart        - Restart all services"
	@echo "  logs           - Show logs for all services"
	@echo "  status         - Show status of all services"
	@echo "  quality        - Run code quality checks"
	@echo "  security       - Run security scans"
	@echo ""

# Build all services
build:
	@echo "Building all services..."
	mvn clean install -DskipTests

# Run all tests
test:
	@echo "Running all tests..."
	mvn clean test verify

# Run unit tests only
test-unit:
	@echo "Running unit tests..."
	mvn clean test

# Run integration tests only
test-integration:
	@echo "Running integration tests..."
	mvn clean verify -DskipUnitTests

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	docker-compose down -v
	docker system prune -f

# Start all services in development mode
start:
	@echo "Starting all services in development mode..."
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
	@echo "Services starting... Use 'make logs' to see logs or 'make status' to check status"

# Start infrastructure services only
start-infra:
	@echo "Starting infrastructure services..."
	docker-compose up -d postgres redis kafka rabbitmq elasticsearch prometheus grafana zipkin
	@echo "Infrastructure services started"

# Stop all services
stop:
	@echo "Stopping all services..."
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

# Restart all services
restart: stop start

# Show logs for all services
logs:
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f

# Show status of all services
status:
	@echo "Service Status:"
	@echo "==============="
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps

# Run code quality checks
quality:
	@echo "Running code quality checks..."
	mvn checkstyle:check
	mvn spotbugs:check

# Run security scans
security:
	@echo "Running security scans..."
	mvn org.owasp:dependency-check-maven:check

# Development helpers
dev-setup:
	@echo "Setting up development environment..."
	@echo "1. Building project..."
	make build
	@echo "2. Starting infrastructure..."
	make start-infra
	@echo "3. Waiting for services to be ready..."
	sleep 30
	@echo "Development environment ready!"

# Production deployment
deploy-prod:
	@echo "Deploying to production..."
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Health check
health:
	@echo "Checking service health..."
	@curl -f http://localhost:8761/actuator/health || echo "Discovery Server: DOWN"
	@curl -f http://localhost:8080/actuator/health || echo "API Gateway: DOWN"
	@curl -f http://localhost:8081/actuator/health || echo "Auth Service: DOWN"
	@curl -f http://localhost:8082/actuator/health || echo "Tenant Service: DOWN"
	@curl -f http://localhost:8083/actuator/health || echo "Users Service: DOWN"