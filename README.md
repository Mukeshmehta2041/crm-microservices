# CRM Microservices Platform

A comprehensive CRM microservices platform built with Java Spring Boot, featuring multi-tenant architecture, service discovery, API gateway, and comprehensive business services for managing customer relationships.

## Architecture Overview

The platform follows a microservices architecture with the following components:

### Infrastructure Services
- **Discovery Server** (Port 8761) - Netflix Eureka for service discovery
- **API Gateway** (Port 8080) - Spring Cloud Gateway for routing and security
- **Auth Service** (Port 8081) - JWT-based authentication and authorization
- **Tenant Service** (Port 8082) - Multi-tenant management and configuration
- **Users Service** (Port 8083) - User profile and role management

### Business Services
- **Contacts Service** (Port 8084) - Contact management and relationships
- **Deals Service** (Port 8085) - Sales opportunity management
- **Leads Service** (Port 8086) - Lead capture and qualification
- **Accounts Service** (Port 8087) - Account hierarchy and management
- **Activities Service** (Port 8088) - Task and event management
- **Pipelines Service** (Port 8089) - Sales pipeline configuration

### Advanced Services
- **Custom Objects Service** - Dynamic entity management
- **Analytics Service** - Real-time dashboards and reporting
- **Workflow Service** - Business process automation

## Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.2.1** - Application framework
- **Spring Cloud 2023.0.0** - Microservices framework
- **PostgreSQL 15** - Primary database
- **Redis 7** - Caching and session storage
- **Apache Kafka** - Event streaming
- **RabbitMQ** - Message queuing
- **Elasticsearch** - Search and analytics
- **Docker & Docker Compose** - Containerization
- **Prometheus & Grafana** - Monitoring and observability
- **Zipkin** - Distributed tracing

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker and Docker Compose
- Git

## Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd crm-microservices-platform
```

### 2. Build the Project
```bash
mvn clean install
```

### 3. Start Infrastructure Services
```bash
# Start all infrastructure services (databases, message brokers, monitoring)
docker-compose up -d postgres redis kafka rabbitmq elasticsearch prometheus grafana zipkin

# Wait for services to be healthy
docker-compose ps
```

### 4. Start Application Services
```bash
# Development environment
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Or start individual services
mvn spring-boot:run -pl services/discovery-server
mvn spring-boot:run -pl services/api-gateway
mvn spring-boot:run -pl services/auth-service
```

### 5. Verify Services
- Discovery Server: http://localhost:8761
- API Gateway: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Zipkin: http://localhost:9411

## Development

### Project Structure
```
crm-microservices-platform/
├── shared/                     # Shared libraries
│   ├── common-utils/          # Common utilities and DTOs
│   ├── security-common/       # JWT and security utilities
│   └── testing-common/        # Testing utilities and base classes
├── services/                  # Microservices
│   ├── discovery-server/      # Eureka server
│   ├── api-gateway/          # Spring Cloud Gateway
│   ├── auth-service/         # Authentication service
│   ├── tenant-service/       # Multi-tenant management
│   ├── users-service/        # User management
│   ├── contacts-service/     # Contact management
│   ├── deals-service/        # Deal management
│   ├── leads-service/        # Lead management
│   ├── accounts-service/     # Account management
│   ├── activities-service/   # Activity management
│   └── pipelines-service/    # Pipeline management
├── docker/                   # Docker configurations
│   ├── postgres/            # PostgreSQL initialization
│   └── prometheus/          # Prometheus configuration
├── docs/                    # Documentation
└── .github/workflows/       # CI/CD pipelines
```

### Running Tests
```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Code coverage
mvn jacoco:report
```

### Code Quality
```bash
# Checkstyle
mvn checkstyle:check

# SpotBugs
mvn spotbugs:check

# OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check
```

## Configuration

### Environment Variables
- `SPRING_PROFILES_ACTIVE` - Active Spring profile (dev, docker, prod)
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` - Eureka server URL
- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

### Database Configuration
Each service has its own database:
- `auth_db` - Authentication service
- `tenant_db` - Tenant service
- `users_db` - Users service
- `contacts_db` - Contacts service
- `deals_db` - Deals service
- `leads_db` - Leads service
- `accounts_db` - Accounts service
- `activities_db` - Activities service
- `pipelines_db` - Pipelines service

## API Documentation

API documentation is available via Swagger UI for each service:
- API Gateway: http://localhost:8080/swagger-ui.html
- Auth Service: http://localhost:8081/swagger-ui.html
- Contacts Service: http://localhost:8084/swagger-ui.html

## Monitoring and Observability

### Metrics
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000

### Distributed Tracing
- **Zipkin**: http://localhost:9411

### Health Checks
Each service exposes health check endpoints:
- `/actuator/health` - Service health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## Security

### Authentication
- JWT-based authentication with access and refresh tokens
- OAuth2 authorization code flow support
- Role-based access control (RBAC)

### Multi-Tenancy
- Tenant isolation at database and application levels
- Tenant context propagation across services
- Tenant-specific configurations and feature flags

## Deployment

### Docker Deployment
```bash
# Production deployment
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### CI/CD Pipeline
The project includes GitHub Actions workflows for:
- Automated testing and code quality checks
- Security scanning with OWASP dependency check
- Docker image building and pushing
- Automated deployment to staging and production

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions, please open an issue in the GitHub repository.