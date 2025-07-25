# Comprehensive CRM Platform Deployment Guide

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Infrastructure Setup](#infrastructure-setup)
4. [Database Deployment](#database-deployment)
5. [Application Deployment](#application-deployment)
6. [Configuration Management](#configuration-management)
7. [Security Configuration](#security-configuration)
8. [Monitoring and Logging](#monitoring-and-logging)
9. [Testing and Validation](#testing-and-validation)
10. [Production Deployment](#production-deployment)
11. [Maintenance and Updates](#maintenance-and-updates)
12. [Troubleshooting](#troubleshooting)
13. [Rollback Procedures](#rollback-procedures)
14. [Appendices](#appendices)

---

## Overview

### Deployment Architecture
This guide covers the complete deployment process for the Comprehensive CRM Platform, including infrastructure provisioning, application deployment, and operational procedures for production environments.

### Supported Deployment Models
- **Cloud-Native Kubernetes**: Primary deployment model using Kubernetes
- **Docker Compose**: Development and small-scale deployments
- **Hybrid Cloud**: Multi-cloud and on-premises integration
- **Edge Deployment**: Regional and edge computing scenarios

### Deployment Environments
- **Development**: Local development and testing
- **Staging**: Pre-production testing and validation
- **Production**: Live production environment
- **Disaster Recovery**: Backup production environment

---

## Prerequisites

### System Requirements

#### Minimum Hardware Requirements
```yaml
Development Environment:
  CPU: 4 cores
  Memory: 16 GB RAM
  Storage: 100 GB SSD
  Network: 1 Gbps

Staging Environment:
  CPU: 8 cores
  Memory: 32 GB RAM
  Storage: 500 GB SSD
  Network: 10 Gbps

Production Environment:
  CPU: 16+ cores per node
  Memory: 64+ GB RAM per node
  Storage: 1+ TB NVMe SSD
  Network: 10+ Gbps
  Nodes: 3+ for high availability
```

#### Software Prerequisites
```bash
# Required Software Versions
Kubernetes: v1.28+
Docker: v24.0+
Helm: v3.12+
kubectl: v1.28+
Terraform: v1.5+
PostgreSQL: v15+
Redis: v7.0+
Elasticsearch: v8.8+

# Development Tools
Node.js: v18+
Python: v3.11+
Go: v1.21+
Java: v17+
```

### Cloud Provider Requirements

#### AWS Requirements
```yaml
AWS Services:
  - EKS (Elastic Kubernetes Service)
  - RDS (Relational Database Service)
  - ElastiCache (Redis)
  - S3 (Simple Storage Service)
  - ALB (Application Load Balancer)
  - Route 53 (DNS)
  - CloudWatch (Monitoring)
  - IAM (Identity and Access Management)

Required Permissions:
  - EKS cluster management
  - RDS instance management
  - S3 bucket management
  - IAM role and policy management
  - VPC and networking management
  - CloudWatch logs and metrics
```

#### Azure Requirements
```yaml
Azure Services:
  - AKS (Azure Kubernetes Service)
  - Azure Database for PostgreSQL
  - Azure Cache for Redis
  - Azure Blob Storage
  - Azure Load Balancer
  - Azure DNS
  - Azure Monitor
  - Azure Active Directory

Required Permissions:
  - Contributor role on resource group
  - User Access Administrator
  - Network Contributor
  - Storage Account Contributor
```

### Network Requirements
```yaml
Network Configuration:
  Ingress Ports:
    - 80 (HTTP)
    - 443 (HTTPS)
    - 22 (SSH - management only)
  
  Internal Communication:
    - 5432 (PostgreSQL)
    - 6379 (Redis)
    - 9200 (Elasticsearch)
    - 9092 (Kafka)
    - 3000-8080 (Application services)
  
  Outbound Access:
    - 443 (HTTPS for external APIs)
    - 25/587 (SMTP for email)
    - 53 (DNS)
    - Container registries
    - Package repositories
```

---

## Infrastructure Setup

### Terraform Infrastructure as Code

#### AWS Infrastructure
```hcl
# terraform/aws/main.tf
terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.20"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.10"
    }
  }
  
  backend "s3" {
    bucket = "crm-terraform-state"
    key    = "infrastructure/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Environment = var.environment
      Project     = "crm-platform"
      ManagedBy   = "terraform"
    }
  }
}

# VPC Configuration
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  
  name = "${var.project_name}-${var.environment}-vpc"
  cidr = var.vpc_cidr
  
  azs             = var.availability_zones
  private_subnets = var.private_subnet_cidrs
  public_subnets  = var.public_subnet_cidrs
  
  enable_nat_gateway = true
  enable_vpn_gateway = false
  enable_dns_hostnames = true
  enable_dns_support = true
  
  tags = {
    "kubernetes.io/cluster/${var.cluster_name}" = "shared"
  }
  
  public_subnet_tags = {
    "kubernetes.io/cluster/${var.cluster_name}" = "shared"
    "kubernetes.io/role/elb" = "1"
  }
  
  private_subnet_tags = {
    "kubernetes.io/cluster/${var.cluster_name}" = "shared"
    "kubernetes.io/role/internal-elb" = "1"
  }
}

# EKS Cluster
module "eks" {
  source = "terraform-aws-modules/eks/aws"
  
  cluster_name    = var.cluster_name
  cluster_version = var.kubernetes_version
  
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets
  
  cluster_endpoint_private_access = true
  cluster_endpoint_public_access  = true
  
  cluster_addons = {
    coredns = {
      resolve_conflicts = "OVERWRITE"
    }
    kube-proxy = {}
    vpc-cni = {
      resolve_conflicts = "OVERWRITE"
    }
    aws-ebs-csi-driver = {
      resolve_conflicts = "OVERWRITE"
    }
  }
  
  eks_managed_node_groups = {
    system = {
      name = "system-nodes"
      
      instance_types = ["t3.medium"]
      capacity_type  = "ON_DEMAND"
      
      min_size     = 2
      max_size     = 5
      desired_size = 3
      
      labels = {
        role = "system"
      }
      
      taints = {
        system = {
          key    = "node-role"
          value  = "system"
          effect = "NO_SCHEDULE"
        }
      }
    }
    
    application = {
      name = "application-nodes"
      
      instance_types = ["c5.xlarge"]
      capacity_type  = "ON_DEMAND"
      
      min_size     = 3
      max_size     = 20
      desired_size = 5
      
      labels = {
        role = "application"
      }
    }
    
    spot = {
      name = "spot-nodes"
      
      instance_types = ["c5.large", "c5.xlarge", "c5.2xlarge"]
      capacity_type  = "SPOT"
      
      min_size     = 0
      max_size     = 10
      desired_size = 2
      
      labels = {
        role = "spot"
      }
      
      taints = {
        spot = {
          key    = "node-type"
          value  = "spot"
          effect = "NO_SCHEDULE"
        }
      }
    }
  }
}

# RDS PostgreSQL
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet-group"
  subnet_ids = module.vpc.private_subnets
  
  tags = {
    Name = "${var.project_name}-${var.environment}-db-subnet-group"
  }
}

resource "aws_security_group" "rds" {
  name_prefix = "${var.project_name}-${var.environment}-rds-"
  vpc_id      = module.vpc.vpc_id
  
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-${var.environment}-db"
  
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = var.db_instance_class
  
  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true
  
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = var.environment != "production"
  deletion_protection = var.environment == "production"
  
  performance_insights_enabled = true
  monitoring_interval         = 60
  
  tags = {
    Name = "${var.project_name}-${var.environment}-db"
  }
}

# ElastiCache Redis
resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-cache-subnet-group"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "redis" {
  name_prefix = "${var.project_name}-${var.environment}-redis-"
  vpc_id      = module.vpc.vpc_id
  
  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }
}

resource "aws_elasticache_replication_group" "main" {
  replication_group_id       = "${var.project_name}-${var.environment}-redis"
  description                = "Redis cluster for CRM platform"
  
  node_type            = var.redis_node_type
  port                 = 6379
  parameter_group_name = "default.redis7"
  
  num_cache_clusters = var.redis_num_cache_nodes
  
  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  automatic_failover_enabled = var.redis_num_cache_nodes > 1
  multi_az_enabled          = var.redis_num_cache_nodes > 1
  
  tags = {
    Name = "${var.project_name}-${var.environment}-redis"
  }
}

# S3 Buckets
resource "aws_s3_bucket" "app_storage" {
  bucket = "${var.project_name}-${var.environment}-app-storage"
}

resource "aws_s3_bucket_versioning" "app_storage" {
  bucket = aws_s3_bucket.app_storage.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_encryption" "app_storage" {
  bucket = aws_s3_bucket.app_storage.id
  
  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
}

resource "aws_s3_bucket_public_access_block" "app_storage" {
  bucket = aws_s3_bucket.app_storage.id
  
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
```

#### Variables Configuration
```hcl
# terraform/aws/variables.tf
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "production"], var.environment)
    error_message = "Environment must be dev, staging, or production."
  }
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "crm-platform"
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
  default     = "crm-platform-cluster"
}

variable "kubernetes_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.28"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.r6g.large"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 100
}

variable "db_max_allocated_storage" {
  description = "RDS max allocated storage in GB"
  type        = number
  default     = 1000
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "crm_platform"
}

variable "db_username" {
  description = "Database username"
  type        = string
  default     = "crm_admin"
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "redis_node_type" {
  description = "Redis node type"
  type        = string
  default     = "cache.r6g.large"
}

variable "redis_num_cache_nodes" {
  description = "Number of Redis cache nodes"
  type        = number
  default     = 2
}
```

### Infrastructure Deployment Commands
```bash
#!/bin/bash
# deploy-infrastructure.sh

set -e

ENVIRONMENT=${1:-dev}
AWS_REGION=${2:-us-east-1}

echo "Deploying infrastructure for environment: $ENVIRONMENT"

# Initialize Terraform
cd terraform/aws
terraform init

# Plan infrastructure changes
terraform plan \
  -var="environment=$ENVIRONMENT" \
  -var="aws_region=$AWS_REGION" \
  -out=tfplan

# Apply infrastructure changes
terraform apply tfplan

# Update kubeconfig
aws eks update-kubeconfig \
  --region $AWS_REGION \
  --name crm-platform-$ENVIRONMENT-cluster

# Verify cluster access
kubectl cluster-info
kubectl get nodes

echo "Infrastructure deployment completed successfully!"
```

---

## Database Deployment

### Database Schema Deployment

#### Database Migration Scripts
```bash
#!/bin/bash
# scripts/deploy-database.sh

set -e

ENVIRONMENT=${1:-dev}
DB_HOST=${2}
DB_NAME=${3:-crm_platform}
DB_USER=${4:-crm_admin}
DB_PASSWORD=${5}

echo "Deploying database schema for environment: $ENVIRONMENT"

# Database connection parameters
export PGHOST=$DB_HOST
export PGDATABASE=$DB_NAME
export PGUSER=$DB_USER
export PGPASSWORD=$DB_PASSWORD

# Create databases for each service
databases=(
  "crm_contacts_db"
  "crm_deals_db"
  "crm_leads_db"
  "crm_accounts_db"
  "crm_activities_db"
  "crm_pipelines_db"
  "crm_analytics_db"
  "crm_ai_insights_db"
  "crm_custom_objects_db"
  "crm_workflows_db"
  "crm_marketplace_db"
  "crm_integrations_db"
  "crm_auth_db"
  "crm_users_db"
  "crm_tenants_db"
  "crm_notifications_db"
  "crm_files_db"
  "crm_audit_db"
  "crm_search_db"
  "crm_reports_db"
  "crm_dashboards_db"
  "crm_communication_db"
  "crm_social_db"
  "crm_collaboration_db"
)

# Create databases
for db in "${databases[@]}"; do
  echo "Creating database: $db"
  createdb $db || echo "Database $db already exists"
done

# Run migrations for each service
for db in "${databases[@]}"; do
  echo "Running migrations for database: $db"
  export PGDATABASE=$db
  
  # Run schema migrations
  if [ -d "database/migrations/$db" ]; then
    for migration in database/migrations/$db/*.sql; do
      if [ -f "$migration" ]; then
        echo "Applying migration: $migration"
        psql -f "$migration"
      fi
    done
  fi
  
  # Run seed data (for non-production environments)
  if [ "$ENVIRONMENT" != "production" ] && [ -d "database/seeds/$db" ]; then
    for seed in database/seeds/$db/*.sql; do
      if [ -f "$seed" ]; then
        echo "Applying seed data: $seed"
        psql -f "$seed"
      fi
    done
  fi
done

echo "Database deployment completed successfully!"
```

#### Database Migration Framework
```sql
-- database/migrations/framework/001_create_migration_table.sql
-- Migration tracking table
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(20) PRIMARY KEY,
    description TEXT NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    applied_by VARCHAR(100) NOT NULL,
    checksum VARCHAR(64) NOT NULL
);

-- Migration management functions
CREATE OR REPLACE FUNCTION apply_migration(
    migration_version VARCHAR(20),
    migration_description TEXT,
    migration_checksum VARCHAR(64)
)
RETURNS BOOLEAN AS $$
DECLARE
    existing_checksum VARCHAR(64);
BEGIN
    -- Check if migration already applied
    SELECT checksum INTO existing_checksum 
    FROM schema_migrations 
    WHERE version = migration_version;
    
    IF existing_checksum IS NOT NULL THEN
        IF existing_checksum != migration_checksum THEN
            RAISE EXCEPTION 'Migration % checksum mismatch. Expected: %, Found: %', 
                migration_version, migration_checksum, existing_checksum;
        END IF;
        RAISE NOTICE 'Migration % already applied', migration_version;
        RETURN FALSE;
    END IF;
    
    -- Record migration
    INSERT INTO schema_migrations (version, description, applied_by, checksum) 
    VALUES (migration_version, migration_description, current_user, migration_checksum);
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Get current schema version
CREATE OR REPLACE FUNCTION get_current_schema_version()
RETURNS VARCHAR(20) AS $$
BEGIN
    RETURN (
        SELECT version 
        FROM schema_migrations 
        ORDER BY applied_at DESC 
        LIMIT 1
    );
END;
$$ LANGUAGE plpgsql;
```

### Database Configuration

#### PostgreSQL Configuration
```yaml
# kubernetes/database/postgresql-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgresql-config
  namespace: crm-platform
data:
  postgresql.conf: |
    # Connection Settings
    max_connections = 200
    shared_buffers = 256MB
    effective_cache_size = 1GB
    work_mem = 4MB
    maintenance_work_mem = 64MB
    
    # WAL Settings
    wal_level = replica
    max_wal_size = 1GB
    min_wal_size = 80MB
    checkpoint_completion_target = 0.9
    wal_buffers = 16MB
    
    # Query Planner
    random_page_cost = 1.1
    effective_io_concurrency = 200
    
    # Logging
    log_destination = 'csvlog'
    logging_collector = on
    log_directory = 'pg_log'
    log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
    log_min_duration_statement = 1000
    log_checkpoints = on
    log_connections = on
    log_disconnections = on
    log_lock_waits = on
    log_temp_files = 0
    
    # Autovacuum
    autovacuum = on
    autovacuum_max_workers = 3
    autovacuum_naptime = 1min
    autovacuum_vacuum_threshold = 50
    autovacuum_analyze_threshold = 50
    autovacuum_vacuum_scale_factor = 0.2
    autovacuum_analyze_scale_factor = 0.1
    
    # Security
    ssl = on
    ssl_cert_file = '/etc/ssl/certs/server.crt'
    ssl_key_file = '/etc/ssl/private/server.key'
    ssl_ca_file = '/etc/ssl/certs/ca.crt'
    
  pg_hba.conf: |
    # TYPE  DATABASE        USER            ADDRESS                 METHOD
    local   all             all                                     trust
    host    all             all             127.0.0.1/32            md5
    host    all             all             ::1/128                 md5
    host    all             all             10.0.0.0/16             md5
    hostssl all             all             0.0.0.0/0               md5
```

---

## Application Deployment

### Kubernetes Deployment

#### Namespace Configuration
```yaml
# kubernetes/namespaces/crm-namespaces.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: crm-core
  labels:
    name: crm-core
    tier: core
---
apiVersion: v1
kind: Namespace
metadata:
  name: crm-advanced
  labels:
    name: crm-advanced
    tier: advanced
---
apiVersion: v1
kind: Namespace
metadata:
  name: crm-platform
  labels:
    name: crm-platform
    tier: platform
---
apiVersion: v1
kind: Namespace
metadata:
  name: crm-system
  labels:
    name: crm-system
    tier: system
---
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
  labels:
    name: monitoring
    tier: monitoring
```

#### Service Deployment Example
```yaml
# kubernetes/services/contacts-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: contacts-service
  namespace: crm-core
  labels:
    app: contacts-service
    version: v1.0.0
spec:
  replicas: 3
  selector:
    matchLabels:
      app: contacts-service
  template:
    metadata:
      labels:
        app: contacts-service
        version: v1.0.0
    spec:
      serviceAccountName: contacts-service
      containers:
      - name: contacts-service
        image: crm-platform/contacts-service:v1.0.0
        ports:
        - containerPort: 3000
          name: http
        env:
        - name: NODE_ENV
          value: "production"
        - name: PORT
          value: "3000"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: contacts-db-url
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: redis-credentials
              key: redis-url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: config
        configMap:
          name: contacts-service-config
      - name: logs
        emptyDir: {}
      imagePullSecrets:
      - name: registry-credentials
---
apiVersion: v1
kind: Service
metadata:
  name: contacts-service
  namespace: crm-core
  labels:
    app: contacts-service
spec:
  selector:
    app: contacts-service
  ports:
  - port: 80
    targetPort: 3000
    protocol: TCP
    name: http
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: contacts-service-hpa
  namespace: crm-core
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: contacts-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
```

### Helm Charts

#### Helm Chart Structure
```
helm/
├── crm-platform/
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── values-dev.yaml
│   ├── values-staging.yaml
│   ├── values-production.yaml
│   └── templates/
│       ├── _helpers.tpl
│       ├── configmap.yaml
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── ingress.yaml
│       ├── hpa.yaml
│       ├── pdb.yaml
│       ├── serviceaccount.yaml
│       ├── rbac.yaml
│       └── secrets.yaml
└── charts/
    ├── postgresql/
    ├── redis/
    ├── elasticsearch/
    └── monitoring/
```

#### Main Helm Chart
```yaml
# helm/crm-platform/Chart.yaml
apiVersion: v2
name: crm-platform
description: Comprehensive CRM Platform Helm Chart
type: application
version: 1.0.0
appVersion: "1.0.0"

dependencies:
- name: postgresql
  version: "12.8.2"
  repository: "https://charts.bitnami.com/bitnami"
  condition: postgresql.enabled
- name: redis
  version: "17.11.3"
  repository: "https://charts.bitnami.com/bitnami"
  condition: redis.enabled
- name: elasticsearch
  version: "19.10.0"
  repository: "https://charts.bitnami.com/bitnami"
  condition: elasticsearch.enabled

maintainers:
- name: CRM Platform Team
  email: platform-team@company.com
```

#### Values Configuration
```yaml
# helm/crm-platform/values.yaml
global:
  imageRegistry: ""
  imagePullSecrets: []
  storageClass: ""

replicaCount: 3

image:
  registry: docker.io
  repository: crm-platform
  tag: "1.0.0"
  pullPolicy: IfNotPresent

nameOverride: ""
fullnameOverride: ""

serviceAccount:
  create: true
  annotations: {}
  name: ""

podAnnotations: {}

podSecurityContext:
  fsGroup: 2000

securityContext:
  capabilities:
    drop:
    - ALL
  readOnlyRootFilesystem: true
  runAsNonRoot: true
  runAsUser: 1000

service:
  type: ClusterIP
  port: 80
  targetPort: 3000

ingress:
  enabled: true
  className: "nginx"
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
  hosts:
  - host: api.crm-platform.com
    paths:
    - path: /
      pathType: Prefix
  tls:
  - secretName: crm-platform-tls
    hosts:
    - api.crm-platform.com

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

nodeSelector: {}

tolerations: []

affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: app.kubernetes.io/name
            operator: In
            values:
            - crm-platform
        topologyKey: kubernetes.io/hostname

# Service-specific configurations
services:
  contacts:
    enabled: true
    replicaCount: 3
    image:
      repository: crm-platform/contacts-service
      tag: "1.0.0"
    resources:
      limits:
        cpu: 500m
        memory: 512Mi
      requests:
        cpu: 250m
        memory: 256Mi
  
  deals:
    enabled: true
    replicaCount: 3
    image:
      repository: crm-platform/deals-service
      tag: "1.0.0"
    resources:
      limits:
        cpu: 500m
        memory: 512Mi
      requests:
        cpu: 250m
        memory: 256Mi

# Database configurations
postgresql:
  enabled: false
  auth:
    postgresPassword: ""
    username: crm_admin
    password: ""
    database: crm_platform

redis:
  enabled: false
  auth:
    enabled: true
    password: ""

elasticsearch:
  enabled: false
  auth:
    enabled: true
    username: elastic
    password: ""

# External database configurations
externalDatabase:
  host: ""
  port: 5432
  username: crm_admin
  password: ""
  database: crm_platform

externalRedis:
  host: ""
  port: 6379
  password: ""

externalElasticsearch:
  host: ""
  port: 9200
  username: elastic
  password: ""

# Application configuration
config:
  logLevel: "info"
  jwtSecret: ""
  encryptionKey: ""
  
  # Email configuration
  email:
    provider: "smtp"
    host: ""
    port: 587
    username: ""
    password: ""
    from: "noreply@crm-platform.com"
  
  # File storage configuration
  storage:
    provider: "s3"
    bucket: ""
    region: "us-east-1"
    accessKey: ""
    secretKey: ""

# Monitoring and observability
monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
    interval: 30s
    scrapeTimeout: 10s
  
  grafana:
    enabled: true
    dashboards:
      enabled: true
  
  prometheus:
    enabled: true
    rules:
      enabled: true

# Security configurations
security:
  networkPolicies:
    enabled: true
  
  podSecurityPolicy:
    enabled: true
  
  rbac:
    create: true
```

### Deployment Scripts

#### Helm Deployment Script
```bash
#!/bin/bash
# scripts/deploy-helm.sh

set -e

ENVIRONMENT=${1:-dev}
NAMESPACE=${2:-crm-platform}
RELEASE_NAME=${3:-crm-platform}
CHART_PATH=${4:-./helm/crm-platform}

echo "Deploying CRM Platform to environment: $ENVIRONMENT"

# Create namespace if it doesn't exist
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Add required Helm repositories
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo add cert-manager https://charts.jetstack.io
helm repo update

# Install cert-manager if not already installed
if ! helm list -n cert-manager | grep -q cert-manager; then
  echo "Installing cert-manager..."
  kubectl create namespace cert-manager --dry-run=client -o yaml | kubectl apply -f -
  helm install cert-manager cert-manager/cert-manager \
    --namespace cert-manager \
    --set installCRDs=true
fi

# Install ingress-nginx if not already installed
if ! helm list -n ingress-nginx | grep -q ingress-nginx; then
  echo "Installing ingress-nginx..."
  kubectl create namespace ingress-nginx --dry-run=client -o yaml | kubectl apply -f -
  helm install ingress-nginx ingress-nginx/ingress-nginx \
    --namespace ingress-nginx
fi

# Deploy the CRM platform
echo "Deploying CRM Platform..."
helm upgrade --install $RELEASE_NAME $CHART_PATH \
  --namespace $NAMESPACE \
  --values $CHART_PATH/values.yaml \
  --values $CHART_PATH/values-$ENVIRONMENT.yaml \
  --timeout 10m \
  --wait

# Verify deployment
echo "Verifying deployment..."
kubectl get pods -n $NAMESPACE
kubectl get services -n $NAMESPACE
kubectl get ingress -n $NAMESPACE

echo "Deployment completed successfully!"
```

---

*This deployment guide provides comprehensive instructions for deploying the CRM platform across different environments, from infrastructure provisioning to application deployment and configuration management.*