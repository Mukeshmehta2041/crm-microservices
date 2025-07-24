# Infrastructure and DevOps

This document outlines the infrastructure architecture, CI/CD pipelines, monitoring strategies, and operational procedures for the CRM platform's Java microservices architecture.

---

**Status**: In Progress  
**Last Updated**: January 2025  
**Version**: 1.0

## Table of Contents

1. [CI/CD Pipeline Architecture](#cicd-pipeline-architecture)
2. [Observability and Monitoring](#observability-and-monitoring)
3. [Environment Management](#environment-management)
4. [Scalability and Reliability](#scalability-and-reliability)

---

## CI/CD Pipeline Architecture

### Overview

The CI/CD pipeline is designed to support multiple Java microservices with automated testing, security scanning, and deployment strategies. The pipeline ensures code quality, security compliance, and reliable deployments across all environments.

### Build Pipeline Stages

#### Stage 1: Source Control Integration
```yaml
trigger:
  branches:
    include:
      - main
      - develop
      - feature/*
      - hotfix/*
  paths:
    exclude:
      - docs/*
      - README.md

variables:
  JAVA_VERSION: '17'
  MAVEN_VERSION: '3.9.0'
  DOCKER_REGISTRY: 'crm-registry.azurecr.io'
```

#### Stage 2: Code Quality and Security
```yaml
jobs:
  - job: CodeQuality
    steps:
      - task: SonarCloudPrepare@1
        inputs:
          SonarCloud: 'SonarCloud'
          organization: 'crm-platform'
          scannerMode: 'Other'
      
      - task: Maven@3
        inputs:
          mavenPomFile: 'pom.xml'
          goals: 'clean compile test-compile'
          options: '-Dmaven.test.skip=true'
      
      - task: SonarCloudAnalyze@1
      - task: SonarCloudPublish@1
      
      - task: WhiteSource@21
        inputs:
          cwd: '$(System.DefaultWorkingDirectory)'
          projectName: 'CRM-Platform'
```

#### Stage 3: Unit and Integration Testing
```yaml
jobs:
  - job: UnitTests
    strategy:
      matrix:
        contacts-service:
          SERVICE_NAME: 'contacts-service'
        deals-service:
          SERVICE_NAME: 'deals-service'
        leads-service:
          SERVICE_NAME: 'leads-service'
        accounts-service:
          SERVICE_NAME: 'accounts-service'
    steps:
      - task: Maven@3
        inputs:
          mavenPomFile: '$(SERVICE_NAME)/pom.xml'
          goals: 'test'
          publishJUnitResults: true
          testResultsFiles: '**/surefire-reports/TEST-*.xml'
          codeCoverageToolOption: 'JaCoCo'
          codeCoverageClassFilter: '+:com.crm.**,-:com.crm.**.dto.**'
          codeCoverageFailIfEmpty: true

  - job: IntegrationTests
    dependsOn: UnitTests
    steps:
      - task: DockerCompose@0
        inputs:
          action: 'Run services'
          dockerComposeFile: 'docker-compose.test.yml'
          buildImages: true
      
      - task: Maven@3
        inputs:
          mavenPomFile: 'integration-tests/pom.xml'
          goals: 'verify'
          options: '-Dspring.profiles.active=integration'
      
      - task: DockerCompose@0
        inputs:
          action: 'Down'
          dockerComposeFile: 'docker-compose.test.yml'
```

#### Stage 4: Container Build and Registry
```yaml
jobs:
  - job: ContainerBuild
    dependsOn: IntegrationTests
    strategy:
      matrix:
        contacts-service:
          SERVICE_NAME: 'contacts-service'
          DOCKERFILE_PATH: 'contacts-service/Dockerfile'
        deals-service:
          SERVICE_NAME: 'deals-service'
          DOCKERFILE_PATH: 'deals-service/Dockerfile'
        # Additional services...
    steps:
      - task: Maven@3
        inputs:
          mavenPomFile: '$(SERVICE_NAME)/pom.xml'
          goals: 'package'
          options: '-DskipTests'
      
      - task: Docker@2
        inputs:
          containerRegistry: '$(DOCKER_REGISTRY)'
          repository: '$(SERVICE_NAME)'
          command: 'buildAndPush'
          Dockerfile: '$(DOCKERFILE_PATH)'
          tags: |
            $(Build.BuildNumber)
            latest
      
      - task: HelmDeploy@0
        inputs:
          command: 'package'
          chartPath: 'helm/$(SERVICE_NAME)'
          chartVersion: '$(Build.BuildNumber)'
```

### Automated Testing Integration

#### Testing Strategy per Pipeline Stage

| Stage | Test Type | Tools | Coverage Requirement |
|-------|-----------|-------|---------------------|
| Pre-commit | Unit Tests | JUnit 5, Mockito | 80% line coverage |
| Build | Integration Tests | TestContainers, WireMock | 70% integration paths |
| Pre-deployment | Contract Tests | Pact, Spring Cloud Contract | 100% API contracts |
| Post-deployment | Smoke Tests | RestAssured, Cucumber | Critical user journeys |
| Production | Canary Tests | Custom health checks | Business KPIs |

#### Test Automation Configuration
```yaml
# Maven Surefire Plugin Configuration
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>3.0.0-M9</version>
  <configuration>
    <includes>
      <include>**/*Test.java</include>
      <include>**/*Tests.java</include>
    </includes>
    <excludes>
      <exclude>**/*IntegrationTest.java</exclude>
    </excludes>
    <systemPropertyVariables>
      <spring.profiles.active>test</spring.profiles.active>
    </systemPropertyVariables>
  </configuration>
</plugin>

# Failsafe Plugin for Integration Tests
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <version>3.0.0-M9</version>
  <configuration>
    <includes>
      <include>**/*IntegrationTest.java</include>
      <include>**/*IT.java</include>
    </includes>
  </configuration>
</plugin>
```

### Deployment Strategies

#### Blue/Green Deployment
```yaml
# Blue/Green Deployment Configuration
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: contacts-service
spec:
  replicas: 5
  strategy:
    blueGreen:
      activeService: contacts-service-active
      previewService: contacts-service-preview
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: contacts-service-preview
      postPromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: contacts-service-active
  selector:
    matchLabels:
      app: contacts-service
  template:
    metadata:
      labels:
        app: contacts-service
    spec:
      containers:
      - name: contacts-service
        image: crm-registry.azurecr.io/contacts-service:{{.Values.image.tag}}
        ports:
        - containerPort: 8080
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
```

#### Rolling Deployment
```yaml
# Rolling Deployment Configuration
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deals-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: deals-service
  template:
    metadata:
      labels:
        app: deals-service
    spec:
      containers:
      - name: deals-service
        image: crm-registry.azurecr.io/deals-service:{{.Values.image.tag}}
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
```

#### Canary Deployment
```yaml
# Canary Deployment with Istio
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: leads-service
spec:
  replicas: 10
  strategy:
    canary:
      canaryService: leads-service-canary
      stableService: leads-service-stable
      trafficRouting:
        istio:
          virtualService:
            name: leads-service-vs
            routes:
            - primary
      steps:
      - setWeight: 10
      - pause: {duration: 2m}
      - setWeight: 20
      - pause: {duration: 5m}
      - setWeight: 50
      - pause: {duration: 10m}
      - setWeight: 100
  selector:
    matchLabels:
      app: leads-service
```

### Artifact Management and Container Registry

#### Container Registry Structure
```
crm-registry.azurecr.io/
├── contacts-service/
│   ├── latest
│   ├── v1.0.0
│   ├── v1.0.1
│   └── develop-abc123
├── deals-service/
│   ├── latest
│   ├── v1.0.0
│   └── develop-def456
├── shared-libraries/
│   ├── crm-common/
│   └── crm-security/
└── infrastructure/
    ├── api-gateway/
    └── service-mesh/
```

#### Artifact Lifecycle Management
```yaml
# Registry Cleanup Policy
retention_policy:
  untagged_manifests:
    enabled: true
    days: 7
  tagged_manifests:
    enabled: true
    rules:
      - tag_pattern: "develop-*"
        days: 14
      - tag_pattern: "feature-*"
        days: 7
      - tag_pattern: "v*"
        days: 365
      - tag_pattern: "latest"
        days: -1  # Never delete
```

#### Maven Artifact Repository
```xml
<!-- Nexus Repository Configuration -->
<distributionManagement>
  <repository>
    <id>crm-releases</id>
    <name>CRM Releases</name>
    <url>https://nexus.crm-platform.com/repository/maven-releases/</url>
  </repository>
  <snapshotRepository>
    <id>crm-snapshots</id>
    <name>CRM Snapshots</name>
    <url>https://nexus.crm-platform.com/repository/maven-snapshots/</url>
  </snapshotRepository>
</distributionManagement>

<repositories>
  <repository>
    <id>crm-public</id>
    <name>CRM Public Repository</name>
    <url>https://nexus.crm-platform.com/repository/maven-public/</url>
  </repository>
</repositories>
```

#### Security Scanning Integration
```yaml
# Container Security Scanning
- task: AquaSecurityScanner@4
  inputs:
    image: '$(DOCKER_REGISTRY)/$(SERVICE_NAME):$(Build.BuildNumber)'
    scanType: 'local'
    register: true
    hideBase: false
    showNegligible: false
    
# Dependency Vulnerability Scanning
- task: OwaspZap@1
  inputs:
    aggressiveMode: false
    threshold: 'Medium'
    scanType: 'targetedScan'
    url: 'http://$(SERVICE_NAME):8080'
```
## Observa
bility and Monitoring

### Overview

The observability stack provides comprehensive monitoring, logging, and tracing capabilities across all microservices. The implementation follows the three pillars of observability: metrics, logs, and traces.

### Logging Standards and Centralized Aggregation

#### Logging Framework Configuration
```xml
<!-- Logback Configuration for Spring Boot -->
<configuration>
    <springProfile name="!local">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
    </springProfile>
    
    <springProfile name="local">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
    </springProfile>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
    
    <!-- Service-specific loggers -->
    <logger name="com.crm.contacts" level="DEBUG"/>
    <logger name="com.crm.deals" level="DEBUG"/>
    <logger name="com.crm.security" level="INFO"/>
    <logger name="org.springframework.security" level="DEBUG"/>
</configuration>
```

#### Structured Logging Standards
```java
// Logging Best Practices Example
@Service
@Slf4j
public class ContactService {
    
    public Contact createContact(CreateContactRequest request) {
        MDC.put("operation", "create_contact");
        MDC.put("userId", SecurityContextHolder.getContext().getAuthentication().getName());
        MDC.put("tenantId", TenantContext.getCurrentTenant());
        
        try {
            log.info("Creating contact with email: {}", request.getEmail());
            
            Contact contact = contactRepository.save(mapToEntity(request));
            
            log.info("Contact created successfully with id: {}", contact.getId());
            
            // Structured event logging for analytics
            log.info("event=contact_created contact_id={} tenant_id={} user_id={}", 
                contact.getId(), 
                TenantContext.getCurrentTenant(),
                SecurityContextHolder.getContext().getAuthentication().getName());
                
            return contact;
            
        } catch (Exception e) {
            log.error("Failed to create contact", e);
            throw new ContactCreationException("Contact creation failed", e);
        } finally {
            MDC.clear();
        }
    }
}
```

#### Centralized Log Aggregation with ELK Stack
```yaml
# Elasticsearch Configuration
apiVersion: elasticsearch.k8s.elastic.co/v1
kind: Elasticsearch
metadata:
  name: crm-elasticsearch
spec:
  version: 8.5.0
  nodeSets:
  - name: default
    count: 3
    config:
      node.store.allow_mmap: false
      xpack.security.enabled: true
      xpack.security.transport.ssl.enabled: true
    podTemplate:
      spec:
        containers:
        - name: elasticsearch
          resources:
            requests:
              memory: 2Gi
              cpu: 1
            limits:
              memory: 4Gi
              cpu: 2
          env:
          - name: ES_JAVA_OPTS
            value: "-Xms2g -Xmx2g"

---
# Kibana Configuration
apiVersion: kibana.k8s.elastic.co/v1
kind: Kibana
metadata:
  name: crm-kibana
spec:
  version: 8.5.0
  count: 1
  elasticsearchRef:
    name: crm-elasticsearch
  config:
    server.publicBaseUrl: "https://kibana.crm-platform.com"
    xpack.security.enabled: true

---
# Logstash Configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: logstash-config
data:
  logstash.conf: |
    input {
      beats {
        port => 5044
      }
    }
    
    filter {
      if [kubernetes][container][name] =~ /.*-service$/ {
        json {
          source => "message"
        }
        
        mutate {
          add_field => { "service_name" => "%{[kubernetes][container][name]}" }
          add_field => { "namespace" => "%{[kubernetes][namespace]}" }
        }
        
        if [level] == "ERROR" {
          mutate {
            add_tag => [ "error" ]
          }
        }
      }
    }
    
    output {
      elasticsearch {
        hosts => ["crm-elasticsearch-es-http:9200"]
        index => "crm-logs-%{+YYYY.MM.dd}"
        user => "${ELASTICSEARCH_USERNAME}"
        password => "${ELASTICSEARCH_PASSWORD}"
      }
    }
```

### Metrics Collection and Alerting

#### Prometheus Configuration
```yaml
# Prometheus Configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    rule_files:
      - "/etc/prometheus/rules/*.yml"
    
    alerting:
      alertmanagers:
        - static_configs:
            - targets:
              - alertmanager:9093
    
    scrape_configs:
      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
          - role: pod
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
            regex: ([^:]+)(?::\d+)?;(\d+)
            replacement: $1:$2
            target_label: __address__
      
      - job_name: 'crm-services'
        static_configs:
          - targets: 
            - 'contacts-service:8080'
            - 'deals-service:8080'
            - 'leads-service:8080'
            - 'accounts-service:8080'
        metrics_path: '/actuator/prometheus'
        scrape_interval: 10s

---
# Service Monitor for CRM Services
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: crm-services
spec:
  selector:
    matchLabels:
      monitoring: enabled
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

#### Custom Metrics Implementation
```java
// Custom Metrics Configuration
@Configuration
@EnablePrometheusEndpoint
public class MetricsConfiguration {
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "application", "crm-platform",
            "service", "${spring.application.name}",
            "environment", "${spring.profiles.active}"
        );
    }
}

// Business Metrics Example
@Component
@Slf4j
public class BusinessMetrics {
    
    private final Counter contactsCreated;
    private final Counter dealsWon;
    private final Timer contactSearchTime;
    private final Gauge activeUsers;
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.contactsCreated = Counter.builder("crm.contacts.created")
            .description("Number of contacts created")
            .tag("type", "business")
            .register(meterRegistry);
            
        this.dealsWon = Counter.builder("crm.deals.won")
            .description("Number of deals won")
            .tag("type", "business")
            .register(meterRegistry);
            
        this.contactSearchTime = Timer.builder("crm.contacts.search.duration")
            .description("Time taken to search contacts")
            .register(meterRegistry);
            
        this.activeUsers = Gauge.builder("crm.users.active")
            .description("Number of active users")
            .register(meterRegistry, this, BusinessMetrics::getActiveUserCount);
    }
    
    public void incrementContactsCreated() {
        contactsCreated.increment();
    }
    
    public void recordContactSearchTime(Duration duration) {
        contactSearchTime.record(duration);
    }
    
    private double getActiveUserCount() {
        // Implementation to get active user count
        return 0.0;
    }
}
```

#### Alerting Rules Configuration
```yaml
# Prometheus Alerting Rules
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-rules
data:
  crm-alerts.yml: |
    groups:
    - name: crm-platform-alerts
      rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} for service {{ $labels.service }}"
      
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }}s for {{ $labels.service }}"
      
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service is down"
          description: "Service {{ $labels.instance }} is down"
      
      - alert: HighMemoryUsage
        expr: (container_memory_usage_bytes / container_spec_memory_limit_bytes) > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value | humanizePercentage }} for {{ $labels.container }}"
      
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value | humanizePercentage }} for {{ $labels.service }}"
```

### Distributed Tracing Implementation

#### Jaeger Configuration
```yaml
# Jaeger All-in-One Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jaeger
  template:
    metadata:
      labels:
        app: jaeger
    spec:
      containers:
      - name: jaeger
        image: jaegertracing/all-in-one:1.41
        ports:
        - containerPort: 16686
        - containerPort: 14268
        env:
        - name: COLLECTOR_ZIPKIN_HOST_PORT
          value: ":9411"
        - name: SPAN_STORAGE_TYPE
          value: "elasticsearch"
        - name: ES_SERVER_URLS
          value: "http://crm-elasticsearch-es-http:9200"
        - name: ES_USERNAME
          value: "elastic"
        - name: ES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: crm-elasticsearch-es-elastic-user
              key: elastic

---
# Jaeger Service
apiVersion: v1
kind: Service
metadata:
  name: jaeger
spec:
  selector:
    app: jaeger
  ports:
  - name: ui
    port: 16686
    targetPort: 16686
  - name: collector
    port: 14268
    targetPort: 14268
```

#### Spring Boot Tracing Configuration
```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://jaeger:9411/api/v2/spans

spring:
  sleuth:
    zipkin:
      base-url: http://jaeger:9411
    sampler:
      probability: 1.0
    web:
      skip-pattern: /actuator.*|/health.*
```

#### Custom Tracing Implementation
```java
// Custom Tracing Configuration
@Configuration
public class TracingConfiguration {
    
    @Bean
    public Sender sender() {
        return OkHttpSender.create("http://jaeger:9411/api/v2/spans");
    }
    
    @Bean
    public AsyncReporter<Span> spanReporter() {
        return AsyncReporter.create(sender());
    }
    
    @Bean
    public Tracing tracing() {
        return Tracing.newBuilder()
            .localServiceName("crm-platform")
            .spanReporter(spanReporter())
            .sampler(Sampler.create(1.0f))
            .build();
    }
}

// Custom Span Annotations
@Service
@Slf4j
public class ContactService {
    
    private final Tracer tracer;
    
    public ContactService(Tracing tracing) {
        this.tracer = tracing.tracer();
    }
    
    @NewSpan("contact-search")
    public List<Contact> searchContacts(@SpanTag("query") String query) {
        Span span = tracer.nextSpan()
            .name("contact-search")
            .tag("query.length", String.valueOf(query.length()))
            .tag("tenant.id", TenantContext.getCurrentTenant())
            .start();
            
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Search implementation
            List<Contact> results = performSearch(query);
            
            span.tag("results.count", String.valueOf(results.size()));
            
            return results;
        } finally {
            span.end();
        }
    }
}
```

### Monitoring Dashboards and SLA Definitions

#### Grafana Dashboard Configuration
```json
{
  "dashboard": {
    "title": "CRM Platform Overview",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) by (service) / sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Active Users",
        "type": "singlestat",
        "targets": [
          {
            "expr": "sum(crm_users_active)",
            "legendFormat": "Active Users"
          }
        ]
      }
    ]
  }
}
```

#### SLA Definitions and Monitoring
```yaml
# SLA Configuration
sla_definitions:
  availability:
    target: 99.9%
    measurement_window: "30d"
    error_budget: 0.1%
    
  response_time:
    p95_target: "500ms"
    p99_target: "2s"
    measurement_window: "24h"
    
  error_rate:
    target: "<0.1%"
    measurement_window: "1h"
    
  throughput:
    min_rps: 1000
    measurement_window: "5m"

# SLI Queries
sli_queries:
  availability:
    good_events: 'sum(rate(http_requests_total{status!~"5.."}[5m]))'
    total_events: 'sum(rate(http_requests_total[5m]))'
    
  response_time:
    query: 'histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))'
    threshold: 0.5
    
  error_rate:
    good_events: 'sum(rate(http_requests_total{status!~"5.."}[5m]))'
    total_events: 'sum(rate(http_requests_total[5m]))'
```

#### Health Check Implementation
```java
// Custom Health Indicators
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "Available")
                    .withDetail("connection_pool", getConnectionPoolStatus())
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "Unavailable")
                .withException(e)
                .build();
        }
        return Health.down().withDetail("database", "Connection validation failed").build();
    }
}

@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {
    
    private final RestTemplate restTemplate;
    
    @Override
    public Health health() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "https://api.external-service.com/health", String.class);
                
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("external_service", "Available")
                    .withDetail("response_time", measureResponseTime())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("external_service", "Unavailable")
                .withException(e)
                .build();
        }
        return Health.down().build();
    }
}
```## Envir
onment Management

### Overview

The CRM platform maintains four distinct environments with progressive promotion strategies. Each environment serves specific purposes in the development lifecycle and has tailored configurations for security, performance, and functionality.

### Environment Configurations

#### Development Environment
```yaml
# Development Environment Configuration
environment: development
namespace: crm-dev

database:
  host: postgres-dev.crm-platform.com
  name: crm_dev
  pool_size: 5
  ssl_mode: disable

redis:
  host: redis-dev.crm-platform.com
  port: 6379
  database: 0

security:
  jwt_expiration: 24h
  cors_origins: 
    - "http://localhost:3000"
    - "http://localhost:8080"
  oauth2:
    client_id: crm-dev-client
    redirect_uri: http://localhost:3000/auth/callback

logging:
  level: DEBUG
  pattern: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

features:
  ai_insights: true
  analytics: true
  marketplace: false
  social_crm: true

resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"

replicas: 1
```

#### Testing Environment
```yaml
# Testing Environment Configuration
environment: testing
namespace: crm-test

database:
  host: postgres-test.crm-platform.com
  name: crm_test
  pool_size: 10
  ssl_mode: require
  connection_timeout: 30s

redis:
  host: redis-test.crm-platform.com
  port: 6379
  database: 1
  sentinel_enabled: true

security:
  jwt_expiration: 8h
  cors_origins: 
    - "https://test.crm-platform.com"
  oauth2:
    client_id: crm-test-client
    redirect_uri: https://test.crm-platform.com/auth/callback

logging:
  level: INFO
  format: json
  
monitoring:
  metrics_enabled: true
  tracing_sample_rate: 0.1

features:
  ai_insights: true
  analytics: true
  marketplace: true
  social_crm: true

resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"

replicas: 2

testing:
  data_seeding: true
  mock_external_services: true
  performance_testing: true
```

#### Staging Environment
```yaml
# Staging Environment Configuration
environment: staging
namespace: crm-staging

database:
  host: postgres-staging.crm-platform.com
  name: crm_staging
  pool_size: 20
  ssl_mode: require
  connection_timeout: 10s
  read_replicas: 2

redis:
  cluster_enabled: true
  nodes:
    - redis-staging-1.crm-platform.com:6379
    - redis-staging-2.crm-platform.com:6379
    - redis-staging-3.crm-platform.com:6379

security:
  jwt_expiration: 4h
  cors_origins: 
    - "https://staging.crm-platform.com"
  oauth2:
    client_id: crm-staging-client
    redirect_uri: https://staging.crm-platform.com/auth/callback
  rate_limiting:
    enabled: true
    requests_per_minute: 1000

logging:
  level: INFO
  format: json
  
monitoring:
  metrics_enabled: true
  tracing_sample_rate: 0.05
  alerting_enabled: true

features:
  ai_insights: true
  analytics: true
  marketplace: true
  social_crm: true
  gdpr_compliance: true

resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1500m"

replicas: 3

autoscaling:
  enabled: true
  min_replicas: 3
  max_replicas: 10
  target_cpu_utilization: 70
```

#### Production Environment
```yaml
# Production Environment Configuration
environment: production
namespace: crm-prod

database:
  host: postgres-prod.crm-platform.com
  name: crm_production
  pool_size: 50
  ssl_mode: require
  connection_timeout: 5s
  read_replicas: 3
  backup_enabled: true
  backup_schedule: "0 2 * * *"

redis:
  cluster_enabled: true
  high_availability: true
  nodes:
    - redis-prod-1.crm-platform.com:6379
    - redis-prod-2.crm-platform.com:6379
    - redis-prod-3.crm-platform.com:6379
    - redis-prod-4.crm-platform.com:6379
    - redis-prod-5.crm-platform.com:6379
    - redis-prod-6.crm-platform.com:6379

security:
  jwt_expiration: 2h
  cors_origins: 
    - "https://app.crm-platform.com"
    - "https://api.crm-platform.com"
  oauth2:
    client_id: crm-prod-client
    redirect_uri: https://app.crm-platform.com/auth/callback
  rate_limiting:
    enabled: true
    requests_per_minute: 5000
    burst_limit: 10000
  waf_enabled: true

logging:
  level: WARN
  format: json
  retention_days: 90
  
monitoring:
  metrics_enabled: true
  tracing_sample_rate: 0.01
  alerting_enabled: true
  sla_monitoring: true

features:
  ai_insights: true
  analytics: true
  marketplace: true
  social_crm: true
  gdpr_compliance: true
  advanced_security: true

resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "4Gi"
    cpu: "2000m"

replicas: 5

autoscaling:
  enabled: true
  min_replicas: 5
  max_replicas: 50
  target_cpu_utilization: 60
  target_memory_utilization: 70

disaster_recovery:
  enabled: true
  backup_regions: ["us-west-2", "eu-west-1"]
  rto: "4h"
  rpo: "1h"
```

### Environment-Specific Configuration Management

#### ConfigMap Strategy
```yaml
# Base ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: crm-base-config
data:
  application.yml: |
    spring:
      application:
        name: ${SERVICE_NAME:crm-service}
      profiles:
        active: ${SPRING_PROFILES_ACTIVE:default}
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: when-authorized
    
    logging:
      pattern:
        console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

---
# Environment-Specific ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: crm-prod-config
data:
  application-prod.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
        hikari:
          maximum-pool-size: ${DB_POOL_SIZE:50}
          connection-timeout: ${DB_CONNECTION_TIMEOUT:5000}
      
      redis:
        cluster:
          nodes: ${REDIS_CLUSTER_NODES}
        password: ${REDIS_PASSWORD}
    
    security:
      jwt:
        expiration: ${JWT_EXPIRATION:7200}
      oauth2:
        client-id: ${OAUTH2_CLIENT_ID}
        client-secret: ${OAUTH2_CLIENT_SECRET}
    
    logging:
      level:
        com.crm: ${LOG_LEVEL:WARN}
        org.springframework.security: INFO
```

#### Secret Management
```yaml
# Sealed Secrets for Production
apiVersion: bitnami.com/v1alpha1
kind: SealedSecret
metadata:
  name: crm-prod-secrets
  namespace: crm-prod
spec:
  encryptedData:
    DB_PASSWORD: AgBy3i4OJSWK+PiTySYZZA9rO43cGHuJRK1W...
    REDIS_PASSWORD: AgAKAoiQm7QDhii4J5Eqt9K8eFHyacAvK2SA...
    JWT_SECRET: AgAghpFrAqd5Ac2JQMKK+wuC7BgTrp9Wn1...
    OAUTH2_CLIENT_SECRET: AgAB5iFjLm8eDhii4J5Eqt9K8eFHyacAv...

---
# External Secrets Operator Configuration
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: vault-backend
spec:
  provider:
    vault:
      server: "https://vault.crm-platform.com"
      path: "secret"
      version: "v2"
      auth:
        kubernetes:
          mountPath: "kubernetes"
          role: "crm-platform"

---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: crm-database-secret
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: database-credentials
    creationPolicy: Owner
  data:
  - secretKey: username
    remoteRef:
      key: database/crm-prod
      property: username
  - secretKey: password
    remoteRef:
      key: database/crm-prod
      property: password
```

### Database Migration and Seeding Strategies

#### Flyway Migration Configuration
```xml
<!-- Flyway Maven Plugin Configuration -->
<plugin>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-maven-plugin</artifactId>
    <version>9.8.1</version>
    <configuration>
        <url>jdbc:postgresql://${db.host}:5432/${db.name}</url>
        <user>${db.username}</user>
        <password>${db.password}</password>
        <schemas>
            <schema>crm</schema>
        </schemas>
        <locations>
            <location>classpath:db/migration</location>
            <location>classpath:db/migration/${spring.profiles.active}</location>
        </locations>
        <placeholders>
            <environment>${spring.profiles.active}</environment>
        </placeholders>
    </configuration>
</plugin>
```

#### Migration Scripts Structure
```
src/main/resources/db/migration/
├── V1__Create_base_schema.sql
├── V2__Create_contacts_table.sql
├── V3__Create_deals_table.sql
├── V4__Create_leads_table.sql
├── V5__Create_accounts_table.sql
├── V6__Create_activities_table.sql
├── V7__Create_pipelines_table.sql
├── V8__Add_indexes.sql
├── V9__Add_audit_triggers.sql
└── development/
    ├── R__Insert_dev_data.sql
    └── R__Create_test_users.sql
└── testing/
    ├── R__Insert_test_data.sql
    └── R__Create_performance_data.sql
```

#### Environment-Specific Data Seeding
```java
// Development Data Seeder
@Component
@Profile("development")
@Slf4j
public class DevelopmentDataSeeder implements ApplicationRunner {
    
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    
    @Override
    public void run(ApplicationArguments args) {
        if (contactRepository.count() == 0) {
            seedDevelopmentData();
        }
    }
    
    private void seedDevelopmentData() {
        log.info("Seeding development data...");
        
        // Create test users
        User adminUser = createUser("admin@crm.com", "Admin", "User", Role.ADMIN);
        User salesUser = createUser("sales@crm.com", "Sales", "Rep", Role.SALES);
        
        // Create test contacts
        List<Contact> contacts = IntStream.range(1, 101)
            .mapToObj(i -> createContact("contact" + i + "@example.com", "Contact " + i))
            .collect(Collectors.toList());
        contactRepository.saveAll(contacts);
        
        // Create test deals
        List<Deal> deals = contacts.stream()
            .limit(50)
            .map(contact -> createDeal("Deal for " + contact.getName(), contact, salesUser))
            .collect(Collectors.toList());
        dealRepository.saveAll(deals);
        
        log.info("Development data seeding completed");
    }
}

// Testing Data Seeder
@Component
@Profile("testing")
public class TestingDataSeeder implements ApplicationRunner {
    
    @Override
    public void run(ApplicationArguments args) {
        seedPerformanceTestData();
        seedIntegrationTestData();
    }
    
    private void seedPerformanceTestData() {
        // Create large datasets for performance testing
        // 10,000 contacts, 5,000 deals, 100 users
    }
    
    private void seedIntegrationTestData() {
        // Create specific test scenarios for integration tests
    }
}
```

### Environment Promotion and Rollback Procedures

#### Promotion Pipeline
```yaml
# Environment Promotion Pipeline
name: Environment Promotion

trigger:
  branches:
    include:
      - main
      - release/*

stages:
- stage: PromoteToDev
  displayName: 'Promote to Development'
  jobs:
  - deployment: DeployToDev
    environment: 'crm-development'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: HelmDeploy@0
            inputs:
              command: 'upgrade'
              chartType: 'FilePath'
              chartPath: 'helm/crm-platform'
              releaseName: 'crm-dev'
              namespace: 'crm-dev'
              valueFile: 'helm/values-dev.yaml'
          
          - task: Bash@3
            displayName: 'Run Smoke Tests'
            inputs:
              script: |
                ./scripts/smoke-tests.sh https://dev.crm-platform.com

- stage: PromoteToTest
  displayName: 'Promote to Testing'
  dependsOn: PromoteToDev
  condition: succeeded()
  jobs:
  - deployment: DeployToTest
    environment: 'crm-testing'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: HelmDeploy@0
            inputs:
              command: 'upgrade'
              chartType: 'FilePath'
              chartPath: 'helm/crm-platform'
              releaseName: 'crm-test'
              namespace: 'crm-test'
              valueFile: 'helm/values-test.yaml'
          
          - task: Bash@3
            displayName: 'Run Integration Tests'
            inputs:
              script: |
                ./scripts/integration-tests.sh https://test.crm-platform.com
          
          - task: Bash@3
            displayName: 'Run Performance Tests'
            inputs:
              script: |
                ./scripts/performance-tests.sh https://test.crm-platform.com

- stage: PromoteToStaging
  displayName: 'Promote to Staging'
  dependsOn: PromoteToTest
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))
  jobs:
  - deployment: DeployToStaging
    environment: 'crm-staging'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: HelmDeploy@0
            inputs:
              command: 'upgrade'
              chartType: 'FilePath'
              chartPath: 'helm/crm-platform'
              releaseName: 'crm-staging'
              namespace: 'crm-staging'
              valueFile: 'helm/values-staging.yaml'
          
          - task: Bash@3
            displayName: 'Run End-to-End Tests'
            inputs:
              script: |
                ./scripts/e2e-tests.sh https://staging.crm-platform.com

- stage: PromoteToProduction
  displayName: 'Promote to Production'
  dependsOn: PromoteToStaging
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))
  jobs:
  - deployment: DeployToProduction
    environment: 'crm-production'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: HelmDeploy@0
            inputs:
              command: 'upgrade'
              chartType: 'FilePath'
              chartPath: 'helm/crm-platform'
              releaseName: 'crm-prod'
              namespace: 'crm-prod'
              valueFile: 'helm/values-prod.yaml'
              install: true
              waitForExecution: true
              arguments: '--timeout 600s'
          
          - task: Bash@3
            displayName: 'Verify Production Deployment'
            inputs:
              script: |
                ./scripts/production-verification.sh https://app.crm-platform.com
```

#### Rollback Procedures
```bash
#!/bin/bash
# rollback.sh - Environment Rollback Script

set -e

ENVIRONMENT=$1
REVISION=$2

if [ -z "$ENVIRONMENT" ] || [ -z "$REVISION" ]; then
    echo "Usage: $0 <environment> <revision>"
    echo "Example: $0 production 5"
    exit 1
fi

echo "Rolling back $ENVIRONMENT to revision $REVISION..."

# Rollback using Helm
helm rollback crm-$ENVIRONMENT $REVISION --namespace crm-$ENVIRONMENT

# Wait for rollback to complete
kubectl rollout status deployment/contacts-service -n crm-$ENVIRONMENT
kubectl rollout status deployment/deals-service -n crm-$ENVIRONMENT
kubectl rollout status deployment/leads-service -n crm-$ENVIRONMENT
kubectl rollout status deployment/accounts-service -n crm-$ENVIRONMENT

# Verify rollback
echo "Verifying rollback..."
./scripts/health-check.sh https://$ENVIRONMENT.crm-platform.com

# Database rollback (if needed)
if [ "$3" == "--with-database" ]; then
    echo "Rolling back database migrations..."
    flyway -url=jdbc:postgresql://postgres-$ENVIRONMENT.crm-platform.com:5432/crm_$ENVIRONMENT \
           -user=$DB_USERNAME \
           -password=$DB_PASSWORD \
           -target=$REVISION \
           migrate
fi

echo "Rollback completed successfully"
```

#### Automated Rollback Triggers
```yaml
# Automated Rollback Configuration
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: success-rate
spec:
  args:
  - name: service-name
  metrics:
  - name: success-rate
    interval: 30s
    count: 10
    successCondition: result[0] >= 0.95
    failureLimit: 3
    provider:
      prometheus:
        address: http://prometheus:9090
        query: |
          sum(rate(http_requests_total{service="{{args.service-name}}",status!~"5.."}[2m])) /
          sum(rate(http_requests_total{service="{{args.service-name}}"}[2m]))

---
# Automatic Rollback on Failure
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: contacts-service
spec:
  strategy:
    blueGreen:
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: contacts-service
      postPromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: contacts-service
```## Scal
ability and Reliability

### Overview

The CRM platform is designed for enterprise-scale operations with automatic scaling, high availability, and comprehensive disaster recovery capabilities. The architecture supports horizontal scaling across multiple regions with intelligent load distribution.

### Auto-scaling Policies for Microservices

#### Horizontal Pod Autoscaler (HPA) Configuration
```yaml
# Contacts Service HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: contacts-service-hpa
  namespace: crm-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: contacts-service
  minReplicas: 3
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 60
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
      - type: Pods
        value: 5
        periodSeconds: 60
      selectPolicy: Max

---
# Deals Service HPA (High Traffic)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: deals-service-hpa
  namespace: crm-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: deals-service
  minReplicas: 5
  maxReplicas: 100
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 60
  - type: External
    external:
      metric:
        name: queue_depth
        selector:
          matchLabels:
            queue: deals-processing
      target:
        type: AverageValue
        averageValue: "10"

---
# Analytics Service HPA (CPU Intensive)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: analytics-service-hpa
  namespace: crm-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: analytics-service
  minReplicas: 2
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
```

#### Vertical Pod Autoscaler (VPA) Configuration
```yaml
# VPA for AI Insights Service
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: ai-insights-service-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-insights-service
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: ai-insights-service
      minAllowed:
        cpu: 100m
        memory: 512Mi
      maxAllowed:
        cpu: 4
        memory: 8Gi
      controlledResources: ["cpu", "memory"]
```

#### Custom Metrics for Scaling
```java
// Custom Metrics for Business Logic Scaling
@Component
@Slf4j
public class ScalingMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Gauge queueDepth;
    private final Gauge activeConnections;
    private final Gauge processingLatency;
    
    public ScalingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.queueDepth = Gauge.builder("crm.queue.depth")
            .description("Current queue depth for processing")
            .register(meterRegistry, this, ScalingMetrics::getQueueDepth);
            
        this.activeConnections = Gauge.builder("crm.connections.active")
            .description("Number of active database connections")
            .register(meterRegistry, this, ScalingMetrics::getActiveConnections);
            
        this.processingLatency = Gauge.builder("crm.processing.latency")
            .description("Average processing latency in milliseconds")
            .register(meterRegistry, this, ScalingMetrics::getProcessingLatency);
    }
    
    private double getQueueDepth() {
        // Implementation to get current queue depth
        return 0.0;
    }
    
    private double getActiveConnections() {
        // Implementation to get active connection count
        return 0.0;
    }
    
    private double getProcessingLatency() {
        // Implementation to get processing latency
        return 0.0;
    }
}
```

### Load Balancing Strategies and Health Checks

#### Ingress Controller Configuration
```yaml
# NGINX Ingress with Advanced Load Balancing
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: crm-platform-ingress
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    nginx.ingress.kubernetes.io/load-balance: "ewma"
    nginx.ingress.kubernetes.io/upstream-hash-by: "$request_uri"
    nginx.ingress.kubernetes.io/rate-limit: "1000"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.crm-platform.com
    secretName: crm-platform-tls
  rules:
  - host: api.crm-platform.com
    http:
      paths:
      - path: /api/v1/contacts(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: contacts-service
            port:
              number: 8080
      - path: /api/v1/deals(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: deals-service
            port:
              number: 8080
      - path: /api/v1/leads(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: leads-service
            port:
              number: 8080

---
# Service Configuration with Session Affinity
apiVersion: v1
kind: Service
metadata:
  name: contacts-service
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
    service.beta.kubernetes.io/aws-load-balancer-backend-protocol: "http"
spec:
  type: LoadBalancer
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 3600
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
  selector:
    app: contacts-service
```

#### Istio Service Mesh Load Balancing
```yaml
# Destination Rule for Advanced Load Balancing
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: contacts-service-dr
spec:
  host: contacts-service
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN
    connectionPool:
      tcp:
        maxConnections: 100
        connectTimeout: 30s
      http:
        http1MaxPendingRequests: 50
        http2MaxRequests: 100
        maxRequestsPerConnection: 10
        maxRetries: 3
        consecutiveGatewayErrors: 5
        interval: 30s
        baseEjectionTime: 30s
    circuitBreaker:
      consecutiveGatewayErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2

---
# Virtual Service for Traffic Splitting
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: contacts-service-vs
spec:
  hosts:
  - contacts-service
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: contacts-service
        subset: v2
      weight: 100
  - route:
    - destination:
        host: contacts-service
        subset: v1
      weight: 90
    - destination:
        host: contacts-service
        subset: v2
      weight: 10
    fault:
      delay:
        percentage:
          value: 0.1
        fixedDelay: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
```

#### Health Check Implementation
```java
// Comprehensive Health Check Implementation
@RestController
@RequestMapping("/actuator/health")
@Slf4j
public class HealthController {
    
    private final DatabaseHealthIndicator databaseHealth;
    private final RedisHealthIndicator redisHealth;
    private final ExternalServiceHealthIndicator externalServiceHealth;
    
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", Instant.now());
        status.put("service", "contacts-service");
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        boolean isReady = true;
        
        // Check database connectivity
        Health dbHealth = databaseHealth.health();
        response.put("database", dbHealth.getStatus().getCode());
        if (dbHealth.getStatus() != Status.UP) {
            isReady = false;
        }
        
        // Check Redis connectivity
        Health redisHealthStatus = redisHealth.health();
        response.put("redis", redisHealthStatus.getStatus().getCode());
        if (redisHealthStatus.getStatus() != Status.UP) {
            isReady = false;
        }
        
        // Check external services
        Health externalHealth = externalServiceHealth.health();
        response.put("external_services", externalHealth.getStatus().getCode());
        if (externalHealth.getStatus() != Status.UP) {
            isReady = false;
        }
        
        response.put("status", isReady ? "UP" : "DOWN");
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.status(isReady ? 200 : 503).body(response);
    }
    
    @GetMapping("/startup")
    public ResponseEntity<Map<String, Object>> startup() {
        Map<String, Object> status = new HashMap<>();
        
        // Check if application has fully started
        boolean isStarted = checkApplicationStartup();
        
        status.put("status", isStarted ? "UP" : "DOWN");
        status.put("timestamp", Instant.now());
        status.put("startup_time", getStartupTime());
        
        return ResponseEntity.status(isStarted ? 200 : 503).body(status);
    }
}

// Kubernetes Probe Configuration
@Configuration
public class ProbeConfiguration {
    
    @Bean
    public HealthContributor startupProbe() {
        return new CompositeHealthContributor(Map.of(
            "database-migration", new DatabaseMigrationHealthIndicator(),
            "cache-warmup", new CacheWarmupHealthIndicator(),
            "external-dependencies", new ExternalDependenciesHealthIndicator()
        ));
    }
}
```

### Disaster Recovery Procedures and Backup Strategies

#### Multi-Region Disaster Recovery
```yaml
# Primary Region Configuration (us-east-1)
apiVersion: v1
kind: ConfigMap
metadata:
  name: disaster-recovery-config
data:
  primary_region: "us-east-1"
  backup_regions: "us-west-2,eu-west-1"
  rto_target: "4h"
  rpo_target: "1h"
  
  database_backup:
    schedule: "0 */6 * * *"  # Every 6 hours
    retention: "30d"
    cross_region_replication: "true"
    
  file_storage_backup:
    schedule: "0 2 * * *"    # Daily at 2 AM
    retention: "90d"
    cross_region_replication: "true"

---
# Database Backup CronJob
apiVersion: batch/v1
kind: CronJob
metadata:
  name: database-backup
spec:
  schedule: "0 */6 * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: postgres-backup
            image: postgres:15
            env:
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-credentials
                  key: password
            command:
            - /bin/bash
            - -c
            - |
              TIMESTAMP=$(date +%Y%m%d_%H%M%S)
              BACKUP_FILE="crm_backup_${TIMESTAMP}.sql"
              
              # Create backup
              pg_dump -h postgres-primary.crm-prod.svc.cluster.local \
                      -U postgres \
                      -d crm_production \
                      --verbose \
                      --no-owner \
                      --no-privileges > /tmp/${BACKUP_FILE}
              
              # Compress backup
              gzip /tmp/${BACKUP_FILE}
              
              # Upload to S3 with cross-region replication
              aws s3 cp /tmp/${BACKUP_FILE}.gz \
                  s3://crm-backups-primary/database/${BACKUP_FILE}.gz
              
              # Verify backup integrity
              gunzip -t /tmp/${BACKUP_FILE}.gz
              
              echo "Backup completed: ${BACKUP_FILE}.gz"
            volumeMounts:
            - name: backup-storage
              mountPath: /tmp
          volumes:
          - name: backup-storage
            emptyDir: {}
          restartPolicy: OnFailure

---
# Cross-Region Replication Configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: s3-replication-config
data:
  replication.json: |
    {
      "Role": "arn:aws:iam::123456789012:role/replication-role",
      "Rules": [
        {
          "ID": "ReplicateToWest",
          "Status": "Enabled",
          "Prefix": "",
          "Destination": {
            "Bucket": "arn:aws:s3:::crm-backups-west",
            "StorageClass": "STANDARD_IA"
          }
        },
        {
          "ID": "ReplicateToEurope",
          "Status": "Enabled",
          "Prefix": "",
          "Destination": {
            "Bucket": "arn:aws:s3:::crm-backups-eu",
            "StorageClass": "STANDARD_IA"
          }
        }
      ]
    }
```

#### Automated Disaster Recovery Procedures
```bash
#!/bin/bash
# disaster-recovery.sh - Automated DR Failover Script

set -e

DR_REGION=${1:-us-west-2}
RECOVERY_POINT=${2:-latest}
DRY_RUN=${3:-false}

echo "Starting disaster recovery to region: $DR_REGION"
echo "Recovery point: $RECOVERY_POINT"
echo "Dry run: $DRY_RUN"

# Step 1: Validate DR region readiness
echo "Validating DR region readiness..."
kubectl config use-context crm-$DR_REGION

if ! kubectl get nodes | grep -q Ready; then
    echo "ERROR: DR region nodes are not ready"
    exit 1
fi

# Step 2: Restore database from backup
echo "Restoring database from backup..."
LATEST_BACKUP=$(aws s3 ls s3://crm-backups-$DR_REGION/database/ | sort | tail -n 1 | awk '{print $4}')

if [ "$DRY_RUN" = "false" ]; then
    # Download and restore backup
    aws s3 cp s3://crm-backups-$DR_REGION/database/$LATEST_BACKUP /tmp/
    gunzip /tmp/$LATEST_BACKUP
    
    # Create new database instance
    kubectl apply -f k8s/postgres-dr.yaml
    
    # Wait for database to be ready
    kubectl wait --for=condition=ready pod -l app=postgres-dr --timeout=300s
    
    # Restore data
    kubectl exec -i postgres-dr-0 -- psql -U postgres -d crm_production < /tmp/${LATEST_BACKUP%.gz}
fi

# Step 3: Update DNS to point to DR region
echo "Updating DNS records..."
if [ "$DRY_RUN" = "false" ]; then
    aws route53 change-resource-record-sets \
        --hosted-zone-id Z123456789 \
        --change-batch file://dns-failover-$DR_REGION.json
fi

# Step 4: Deploy applications to DR region
echo "Deploying applications to DR region..."
if [ "$DRY_RUN" = "false" ]; then
    helm upgrade --install crm-platform ./helm/crm-platform \
        --namespace crm-prod \
        --values helm/values-dr-$DR_REGION.yaml \
        --wait --timeout 600s
fi

# Step 5: Verify application health
echo "Verifying application health..."
sleep 60  # Wait for applications to start

HEALTH_CHECK_URL="https://api-$DR_REGION.crm-platform.com/actuator/health"
for i in {1..10}; do
    if curl -f $HEALTH_CHECK_URL; then
        echo "Health check passed"
        break
    fi
    echo "Health check failed, retrying in 30 seconds..."
    sleep 30
done

# Step 6: Notify stakeholders
echo "Sending notifications..."
if [ "$DRY_RUN" = "false" ]; then
    aws sns publish \
        --topic-arn arn:aws:sns:us-east-1:123456789012:crm-alerts \
        --message "Disaster recovery completed successfully to region $DR_REGION"
fi

echo "Disaster recovery procedure completed"
```

### Capacity Planning Guidelines and Performance Benchmarks

#### Resource Allocation Guidelines
```yaml
# Resource Allocation Matrix
resource_allocation:
  contacts_service:
    small_deployment:  # < 10k users
      replicas: 2
      cpu_request: "250m"
      cpu_limit: "500m"
      memory_request: "512Mi"
      memory_limit: "1Gi"
      
    medium_deployment:  # 10k - 100k users
      replicas: 5
      cpu_request: "500m"
      cpu_limit: "1000m"
      memory_request: "1Gi"
      memory_limit: "2Gi"
      
    large_deployment:  # > 100k users
      replicas: 10
      cpu_request: "1000m"
      cpu_limit: "2000m"
      memory_request: "2Gi"
      memory_limit: "4Gi"

  deals_service:
    small_deployment:
      replicas: 3
      cpu_request: "500m"
      cpu_limit: "1000m"
      memory_request: "1Gi"
      memory_limit: "2Gi"
      
    medium_deployment:
      replicas: 8
      cpu_request: "1000m"
      cpu_limit: "2000m"
      memory_request: "2Gi"
      memory_limit: "4Gi"
      
    large_deployment:
      replicas: 20
      cpu_request: "2000m"
      cpu_limit: "4000m"
      memory_request: "4Gi"
      memory_limit: "8Gi"

  analytics_service:
    small_deployment:
      replicas: 1
      cpu_request: "1000m"
      cpu_limit: "2000m"
      memory_request: "2Gi"
      memory_limit: "4Gi"
      
    medium_deployment:
      replicas: 3
      cpu_request: "2000m"
      cpu_limit: "4000m"
      memory_request: "4Gi"
      memory_limit: "8Gi"
      
    large_deployment:
      replicas: 8
      cpu_request: "4000m"
      cpu_limit: "8000m"
      memory_request: "8Gi"
      memory_limit: "16Gi"
```

#### Performance Benchmarks
```yaml
# Performance Benchmarks and SLAs
performance_benchmarks:
  api_response_times:
    contacts:
      get_single: "< 100ms (p95)"
      get_list: "< 200ms (p95)"
      create: "< 300ms (p95)"
      update: "< 250ms (p95)"
      delete: "< 150ms (p95)"
      search: "< 500ms (p95)"
      
    deals:
      get_single: "< 150ms (p95)"
      get_list: "< 300ms (p95)"
      create: "< 400ms (p95)"
      update: "< 350ms (p95)"
      pipeline_view: "< 800ms (p95)"
      
    analytics:
      dashboard_load: "< 2s (p95)"
      report_generation: "< 10s (p95)"
      real_time_metrics: "< 500ms (p95)"

  throughput_targets:
    contacts_service:
      reads_per_second: 5000
      writes_per_second: 1000
      concurrent_users: 10000
      
    deals_service:
      reads_per_second: 3000
      writes_per_second: 800
      concurrent_users: 5000
      
    analytics_service:
      queries_per_second: 500
      concurrent_reports: 100

  database_performance:
    connection_pool_size: 50
    query_timeout: "30s"
    transaction_timeout: "60s"
    max_connections: 200
    
  cache_performance:
    redis_hit_ratio: "> 95%"
    cache_response_time: "< 5ms (p95)"
    cache_memory_usage: "< 80%"
```

#### Capacity Planning Automation
```java
// Capacity Planning Service
@Service
@Slf4j
public class CapacityPlanningService {
    
    private final MeterRegistry meterRegistry;
    private final KubernetesClient kubernetesClient;
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void analyzeCapacity() {
        Map<String, ServiceMetrics> serviceMetrics = collectServiceMetrics();
        
        for (Map.Entry<String, ServiceMetrics> entry : serviceMetrics.entrySet()) {
            String serviceName = entry.getKey();
            ServiceMetrics metrics = entry.getValue();
            
            CapacityRecommendation recommendation = calculateRecommendation(metrics);
            
            if (recommendation.requiresScaling()) {
                log.info("Scaling recommendation for {}: {}", serviceName, recommendation);
                
                if (recommendation.isUrgent()) {
                    triggerAutoScaling(serviceName, recommendation);
                } else {
                    scheduleScaling(serviceName, recommendation);
                }
            }
        }
    }
    
    private CapacityRecommendation calculateRecommendation(ServiceMetrics metrics) {
        // CPU utilization analysis
        if (metrics.getCpuUtilization() > 0.8) {
            return CapacityRecommendation.scaleUp("High CPU utilization", true);
        }
        
        // Memory utilization analysis
        if (metrics.getMemoryUtilization() > 0.85) {
            return CapacityRecommendation.scaleUp("High memory utilization", true);
        }
        
        // Response time analysis
        if (metrics.getP95ResponseTime() > Duration.ofMillis(500)) {
            return CapacityRecommendation.scaleUp("High response time", false);
        }
        
        // Queue depth analysis
        if (metrics.getQueueDepth() > 100) {
            return CapacityRecommendation.scaleUp("High queue depth", true);
        }
        
        // Scale down if resources are underutilized
        if (metrics.getCpuUtilization() < 0.3 && 
            metrics.getMemoryUtilization() < 0.4 && 
            metrics.getCurrentReplicas() > metrics.getMinReplicas()) {
            return CapacityRecommendation.scaleDown("Low resource utilization", false);
        }
        
        return CapacityRecommendation.noAction();
    }
    
    private void triggerAutoScaling(String serviceName, CapacityRecommendation recommendation) {
        // Implement auto-scaling logic
        log.info("Triggering auto-scaling for service: {}", serviceName);
    }
}
```

---

## Summary

This Infrastructure and DevOps documentation provides a comprehensive foundation for deploying, monitoring, and maintaining the CRM platform at enterprise scale. The architecture supports:

- **Automated CI/CD pipelines** with comprehensive testing and security scanning
- **Full observability** with metrics, logging, and distributed tracing
- **Multi-environment management** with automated promotion and rollback capabilities
- **Enterprise-grade scalability** with auto-scaling and load balancing
- **High availability** with disaster recovery and backup strategies
- **Performance optimization** with capacity planning and monitoring

The implementation follows cloud-native best practices and provides the operational foundation needed for a production-ready CRM platform serving thousands of concurrent users across multiple regions.