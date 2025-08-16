# CRM Platform Deployment Guide

This guide provides comprehensive instructions for deploying the CRM Platform microservices across different environments.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Environment Setup](#environment-setup)
4. [Local Development](#local-development)
5. [Docker Deployment](#docker-deployment)
6. [Kubernetes Deployment](#kubernetes-deployment)
7. [Production Deployment](#production-deployment)
8. [Configuration Management](#configuration-management)
9. [Monitoring and Observability](#monitoring-and-observability)
10. [Backup and Disaster Recovery](#backup-and-disaster-recovery)
11. [Health Checks](#health-checks)
12. [Troubleshooting](#troubleshooting)

## Overview

The CRM Platform consists of multiple microservices:

- **Authentication Service** (Port 8081) - Handles authentication, authorization, and security
- **User Management Service** (Port 8082) - Manages users, roles, and teams
- **Tenant Service** (Port 8083) - Handles multi-tenant operations
- **API Gateway** (Port 8080) - Routes requests and handles cross-cutting concerns

### Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Load Balancer │    │   API Gateway   │    │   Monitoring    │
│   (nginx/ALB)   │────│   (Port 8080)   │────│   (Prometheus)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
        │ Auth Service │ │ User Service│ │Tenant Svc  │
        │ (Port 8081)  │ │(Port 8082)  │ │(Port 8083) │
        └──────────────┘ └─────────────┘ └────────────┘
                │               │               │
        ┌───────▼───────────────▼───────────────▼──────┐
        │           PostgreSQL Database                │
        │         (Separate schemas per service)       │
        └─────────────────────────────────────────────┘
```

## Prerequisites

### System Requirements

**Minimum Requirements:**
- CPU: 4 cores
- RAM: 8GB
- Storage: 50GB SSD
- Network: 1Gbps

**Recommended Requirements:**
- CPU: 8 cores
- RAM: 16GB
- Storage: 100GB SSD
- Network: 10Gbps

### Software Dependencies

- **Java 17+** - Runtime for Spring Boot applications
- **PostgreSQL 13+** - Primary database
- **Redis 6+** - Caching and session storage
- **Docker 20.10+** - Containerization
- **Docker Compose 2.0+** - Local orchestration
- **Kubernetes 1.21+** - Production orchestration (optional)
- **nginx 1.20+** - Load balancing and reverse proxy

### Development Tools

- **Maven 3.8+** - Build tool
- **Git 2.30+** - Version control
- **curl** - API testing
- **jq** - JSON processing

## Environment Setup

### Environment Variables

Create environment-specific configuration files:

**`.env.local`** (Local Development):
```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crm_platform
DB_USERNAME=crm_user
DB_PASSWORD=crm_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production
JWT_EXPIRATION=900
JWT_REFRESH_EXPIRATION=604800

# Service Ports
AUTH_SERVICE_PORT=8081
USER_SERVICE_PORT=8082
TENANT_SERVICE_PORT=8083
API_GATEWAY_PORT=8080

# Logging
LOG_LEVEL=INFO
LOG_FILE_PATH=./logs

# Security
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
RATE_LIMIT_ENABLED=true
CAPTCHA_ENABLED=false

# Email Configuration (for development)
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM=noreply@crm-platform.local
```

**`.env.staging`** (Staging Environment):
```bash
# Database Configuration
DB_HOST=staging-db.crm-platform.com
DB_PORT=5432
DB_NAME=crm_platform_staging
DB_USERNAME=crm_staging_user
DB_PASSWORD=${DB_PASSWORD_STAGING}

# Redis Configuration
REDIS_HOST=staging-redis.crm-platform.com
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD_STAGING}

# JWT Configuration
JWT_SECRET=${JWT_SECRET_STAGING}
JWT_EXPIRATION=900
JWT_REFRESH_EXPIRATION=604800

# Service Configuration
AUTH_SERVICE_PORT=8081
USER_SERVICE_PORT=8082
TENANT_SERVICE_PORT=8083
API_GATEWAY_PORT=8080

# Logging
LOG_LEVEL=INFO
LOG_FILE_PATH=/var/log/crm-platform

# Security
CORS_ALLOWED_ORIGINS=https://staging.crm-platform.com
RATE_LIMIT_ENABLED=true
CAPTCHA_ENABLED=true

# Email Configuration
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=${SENDGRID_API_KEY}
SMTP_FROM=noreply@crm-platform.com

# Monitoring
METRICS_ENABLED=true
HEALTH_CHECK_ENABLED=true
```

**`.env.production`** (Production Environment):
```bash
# Database Configuration
DB_HOST=prod-db-cluster.crm-platform.com
DB_PORT=5432
DB_NAME=crm_platform
DB_USERNAME=crm_prod_user
DB_PASSWORD=${DB_PASSWORD_PROD}

# Redis Configuration
REDIS_HOST=prod-redis-cluster.crm-platform.com
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD_PROD}

# JWT Configuration
JWT_SECRET=${JWT_SECRET_PROD}
JWT_EXPIRATION=900
JWT_REFRESH_EXPIRATION=604800

# Service Configuration
AUTH_SERVICE_PORT=8081
USER_SERVICE_PORT=8082
TENANT_SERVICE_PORT=8083
API_GATEWAY_PORT=8080

# Logging
LOG_LEVEL=WARN
LOG_FILE_PATH=/var/log/crm-platform

# Security
CORS_ALLOWED_ORIGINS=https://app.crm-platform.com
RATE_LIMIT_ENABLED=true
CAPTCHA_ENABLED=true

# Email Configuration
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=${SENDGRID_API_KEY}
SMTP_FROM=noreply@crm-platform.com

# Monitoring
METRICS_ENABLED=true
HEALTH_CHECK_ENABLED=true
TRACING_ENABLED=true

# Performance
JVM_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC
```

## Local Development

### Quick Start

1. **Clone the repository:**
```bash
git clone https://github.com/your-org/crm-platform.git
cd crm-platform
```

2. **Start infrastructure services:**
```bash
docker-compose -f docker-compose.infrastructure.yml up -d
```

3. **Set up the database:**
```bash
# Create database and user
psql -h localhost -U postgres -c \"CREATE DATABASE crm_platform;\"
psql -h localhost -U postgres -c \"CREATE USER crm_user WITH PASSWORD 'crm_password';\"
psql -h localhost -U postgres -c \"GRANT ALL PRIVILEGES ON DATABASE crm_platform TO crm_user;\"

# Run migrations
cd services/auth-service && mvn flyway:migrate
cd ../user-service && mvn flyway:migrate
cd ../tenant-service && mvn flyway:migrate
```

4. **Build and run services:**
```bash
# Build all services
mvn clean install

# Start services (in separate terminals)
cd services/auth-service && mvn spring-boot:run
cd services/user-service && mvn spring-boot:run  
cd services/tenant-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

5. **Verify deployment:**
```bash
# Check service health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8080/actuator/health

# Test API
curl -X POST http://localhost:8080/api/v1/auth/login \\
  -H \"Content-Type: application/json\" \\
  -d '{\"usernameOrEmail\":\"admin@example.com\",\"password\":\"admin123\"}'
```

### Development Tools Setup

**IDE Configuration (IntelliJ IDEA):**
1. Import as Maven project
2. Set Project SDK to Java 17
3. Enable annotation processing
4. Install Spring Boot plugin
5. Configure code style (Google Java Style)

**VS Code Configuration:**
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Configure settings.json:
```json
{
  \"java.home\": \"/path/to/java17\",
  \"spring-boot.ls.java.home\": \"/path/to/java17\",
  \"java.format.settings.url\": \"https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml\"
}
```

## Docker Deployment

### Docker Compose Setup

**docker-compose.yml** (Complete stack):
```yaml
version: '3.8'

services:
  # Infrastructure Services
  postgres:
    image: postgres:13
    environment:
      POSTGRES_DB: crm_platform
      POSTGRES_USER: crm_user
      POSTGRES_PASSWORD: crm_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    ports:
      - \"5432:5432\"
    healthcheck:
      test: [\"CMD-SHELL\", \"pg_isready -U crm_user -d crm_platform\"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:6-alpine
    ports:
      - \"6379:6379\"
    volumes:
      - redis_data:/data
    healthcheck:
      test: [\"CMD\", \"redis-cli\", \"ping\"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Application Services
  auth-service:
    build:
      context: ./services/auth-service
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - REDIS_HOST=redis
    ports:
      - \"8081:8081\"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: [\"CMD\", \"curl\", \"-f\", \"http://localhost:8081/actuator/health\"]
      interval: 30s
      timeout: 10s
      retries: 3

  user-service:
    build:
      context: ./services/user-service
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - REDIS_HOST=redis
    ports:
      - \"8082:8082\"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: [\"CMD\", \"curl\", \"-f\", \"http://localhost:8082/actuator/health\"]
      interval: 30s
      timeout: 10s
      retries: 3

  tenant-service:
    build:
      context: ./services/tenant-service
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - REDIS_HOST=redis
    ports:
      - \"8083:8083\"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: [\"CMD\", \"curl\", \"-f\", \"http://localhost:8083/actuator/health\"]
      interval: 30s
      timeout: 10s
      retries: 3

  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - AUTH_SERVICE_URL=http://auth-service:8081
      - USER_SERVICE_URL=http://user-service:8082
      - TENANT_SERVICE_URL=http://tenant-service:8083
    ports:
      - \"8080:8080\"
    depends_on:
      auth-service:
        condition: service_healthy
      user-service:
        condition: service_healthy
      tenant-service:
        condition: service_healthy
    healthcheck:
      test: [\"CMD\", \"curl\", \"-f\", \"http://localhost:8080/actuator/health\"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Monitoring
  prometheus:
    image: prom/prometheus:latest
    ports:
      - \"9090:9090\"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus

  grafana:
    image: grafana/grafana:latest
    ports:
      - \"3000:3000\"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources

volumes:
  postgres_data:
  redis_data:
  prometheus_data:
  grafana_data:
```

### Docker Commands

**Build and Deploy:**
```bash
# Build all images
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f auth-service
docker-compose logs -f user-service

# Scale services
docker-compose up -d --scale auth-service=3 --scale user-service=2

# Stop all services
docker-compose down

# Clean up
docker-compose down -v --remove-orphans
docker system prune -a
```

**Individual Service Management:**
```bash
# Restart specific service
docker-compose restart auth-service

# Update service
docker-compose build auth-service
docker-compose up -d auth-service

# Execute commands in container
docker-compose exec auth-service bash
docker-compose exec postgres psql -U crm_user -d crm_platform
```

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (1.21+)
- kubectl configured
- Helm 3.0+ (optional)
- Ingress controller (nginx/traefik)

### Namespace Setup

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: crm-platform
  labels:
    name: crm-platform
```

### ConfigMaps and Secrets

**ConfigMap:**
```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: crm-platform-config
  namespace: crm-platform
data:
  DB_HOST: \"postgres-service\"
  DB_PORT: \"5432\"
  DB_NAME: \"crm_platform\"
  REDIS_HOST: \"redis-service\"
  REDIS_PORT: \"6379\"
  JWT_EXPIRATION: \"900\"
  JWT_REFRESH_EXPIRATION: \"604800\"
  LOG_LEVEL: \"INFO\"
  CORS_ALLOWED_ORIGINS: \"https://app.crm-platform.com\"
  RATE_LIMIT_ENABLED: \"true\"
  CAPTCHA_ENABLED: \"true\"
  METRICS_ENABLED: \"true\"
  HEALTH_CHECK_ENABLED: \"true\"
```

**Secrets:**
```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: crm-platform-secrets
  namespace: crm-platform
type: Opaque
data:
  DB_USERNAME: Y3JtX3VzZXI=  # base64 encoded
  DB_PASSWORD: Y3JtX3Bhc3N3b3Jk  # base64 encoded
  JWT_SECRET: eW91ci1zdXBlci1zZWNyZXQtand0LWtleQ==
  REDIS_PASSWORD: cmVkaXNfcGFzc3dvcmQ=
  SENDGRID_API_KEY: c2VuZGdyaWRfYXBpX2tleQ==
```

### Database Deployment

```yaml
# postgres-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: crm-platform
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:13
        env:
        - name: POSTGRES_DB
          valueFrom:
            configMapKeyRef:
              name: crm-platform-config
              key: DB_NAME
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: crm-platform-secrets
              key: DB_USERNAME
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: crm-platform-secrets
              key: DB_PASSWORD
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: \"1Gi\"
            cpu: \"500m\"
          limits:
            memory: \"2Gi\"
            cpu: \"1000m\"
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - $(POSTGRES_USER)
            - -d
            - $(POSTGRES_DB)
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - $(POSTGRES_USER)
            - -d
            - $(POSTGRES_DB)
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: crm-platform
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: crm-platform
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```

### Application Services

**Auth Service:**
```yaml
# auth-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: crm-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: crm-platform/auth-service:latest
        envFrom:
        - configMapRef:
            name: crm-platform-config
        - secretRef:
            name: crm-platform-secrets
        ports:
        - containerPort: 8081
        resources:
          requests:
            memory: \"512Mi\"
            cpu: \"250m\"
          limits:
            memory: \"1Gi\"
            cpu: \"500m\"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
          failureThreshold: 30
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: crm-platform
spec:
  selector:
    app: auth-service
  ports:
  - port: 8081
    targetPort: 8081
  type: ClusterIP
```

### Ingress Configuration

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: crm-platform-ingress
  namespace: crm-platform
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: \"true\"
    nginx.ingress.kubernetes.io/use-regex: \"true\"
    nginx.ingress.kubernetes.io/rate-limit: \"100\"
    nginx.ingress.kubernetes.io/rate-limit-window: \"1m\"
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - api.crm-platform.com
    secretName: crm-platform-tls
  rules:
  - host: api.crm-platform.com
    http:
      paths:
      - path: /api/v1/auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 8081
      - path: /api/v1/users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8082
      - path: /api/v1/tenants
        pathType: Prefix
        backend:
          service:
            name: tenant-service
            port:
              number: 8083
```

### Deployment Commands

```bash
# Apply configurations
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f redis-deployment.yaml
kubectl apply -f auth-service-deployment.yaml
kubectl apply -f user-service-deployment.yaml
kubectl apply -f tenant-service-deployment.yaml
kubectl apply -f ingress.yaml

# Check deployment status
kubectl get pods -n crm-platform
kubectl get services -n crm-platform
kubectl get ingress -n crm-platform

# View logs
kubectl logs -f deployment/auth-service -n crm-platform
kubectl logs -f deployment/user-service -n crm-platform

# Scale services
kubectl scale deployment auth-service --replicas=5 -n crm-platform

# Rolling update
kubectl set image deployment/auth-service auth-service=crm-platform/auth-service:v2.0.0 -n crm-platform
kubectl rollout status deployment/auth-service -n crm-platform

# Rollback
kubectl rollout undo deployment/auth-service -n crm-platform
```

## Production Deployment

### Infrastructure Requirements

**Load Balancer Configuration (AWS ALB):**
```yaml
# alb-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: crm-platform-alb
  namespace: crm-platform
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-TLS-1-2-2017-01
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:region:account:certificate/cert-id
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
    alb.ingress.kubernetes.io/healthcheck-interval-seconds: '30'
    alb.ingress.kubernetes.io/healthy-threshold-count: '2'
    alb.ingress.kubernetes.io/unhealthy-threshold-count: '3'
spec:
  rules:
  - host: api.crm-platform.com
    http:
      paths:
      - path: /*
        pathType: ImplementationSpecific
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
```

**Database Configuration (AWS RDS):**
```bash
# RDS PostgreSQL setup
aws rds create-db-instance \\
  --db-instance-identifier crm-platform-prod \\
  --db-instance-class db.r5.xlarge \\
  --engine postgres \\
  --engine-version 13.7 \\
  --allocated-storage 100 \\
  --storage-type gp2 \\
  --storage-encrypted \\
  --master-username crmadmin \\
  --master-user-password ${DB_MASTER_PASSWORD} \\
  --vpc-security-group-ids sg-xxxxxxxxx \\
  --db-subnet-group-name crm-platform-subnet-group \\
  --backup-retention-period 7 \\
  --multi-az \\
  --auto-minor-version-upgrade \\
  --deletion-protection
```

**Redis Configuration (AWS ElastiCache):**
```bash
# ElastiCache Redis setup
aws elasticache create-replication-group \\
  --replication-group-id crm-platform-redis \\
  --description \"CRM Platform Redis Cluster\" \\
  --num-cache-clusters 3 \\
  --cache-node-type cache.r5.large \\
  --engine redis \\
  --engine-version 6.2 \\
  --security-group-ids sg-xxxxxxxxx \\
  --subnet-group-name crm-platform-cache-subnet \\
  --at-rest-encryption-enabled \\
  --transit-encryption-enabled \\
  --auth-token ${REDIS_AUTH_TOKEN}
```

### Security Configuration

**Network Policies:**
```yaml
# network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: crm-platform-network-policy
  namespace: crm-platform
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    - podSelector:
        matchLabels:
          app: auth-service
    - podSelector:
        matchLabels:
          app: user-service
    - podSelector:
        matchLabels:
          app: tenant-service
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
  - to: []
    ports:
    - protocol: TCP
      port: 443
    - protocol: TCP
      port: 80
```

**Pod Security Policy:**
```yaml
# pod-security-policy.yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: crm-platform-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

### Monitoring Setup

**Prometheus Configuration:**
```yaml
# prometheus-config.yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - \"crm-platform-rules.yml\"

scrape_configs:
  - job_name: 'crm-platform-services'
    kubernetes_sd_configs:
    - role: pod
      namespaces:
        names:
        - crm-platform
    relabel_configs:
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
      action: keep
      regex: true
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
      action: replace
      target_label: __metrics_path__
      regex: (.+)
    - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
      action: replace
      regex: ([^:]+)(?::\\d+)?;(\\d+)
      replacement: $1:$2
      target_label: __address__

alerting:
  alertmanagers:
  - static_configs:
    - targets:
      - alertmanager:9093
```

**Alert Rules:**
```yaml
# crm-platform-rules.yml
groups:
- name: crm-platform-alerts
  rules:
  - alert: ServiceDown
    expr: up{job=\"crm-platform-services\"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: \"Service {{ $labels.instance }} is down\"
      description: \"Service {{ $labels.instance }} has been down for more than 1 minute.\"

  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~\"5..\"}[5m]) > 0.1
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: \"High error rate on {{ $labels.instance }}\"
      description: \"Error rate is {{ $value }} errors per second.\"

  - alert: HighMemoryUsage
    expr: (container_memory_usage_bytes / container_spec_memory_limit_bytes) > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: \"High memory usage on {{ $labels.instance }}\"
      description: \"Memory usage is {{ $value | humanizePercentage }}.\"

  - alert: DatabaseConnectionsHigh
    expr: pg_stat_activity_count > 80
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: \"High database connections\"
      description: \"Database has {{ $value }} active connections.\"
```

---

*This deployment guide provides comprehensive instructions for deploying the CRM Platform across different environments. For specific environment configurations, see the individual configuration files in the `/config` directory.*