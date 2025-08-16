# CRM Platform Monitoring and Observability

This document provides comprehensive information about the monitoring and observability implementation for the CRM microservices platform.

## Overview

The CRM platform implements a complete observability stack including:

- **Metrics Collection**: Micrometer with Prometheus for application and business metrics
- **Distributed Tracing**: Spring Cloud Sleuth with Zipkin for request tracing
- **Centralized Logging**: ELK Stack (Elasticsearch, Logstash, Kibana) for structured logging
- **Alerting**: Prometheus Alertmanager for intelligent alert routing
- **Dashboards**: Grafana dashboards for visualization and monitoring

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CRM Microservices                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ Auth Service│ │Contacts Svc │ │  Deals Svc  │    ...    │
│  │             │ │             │ │             │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
│         │               │               │                   │
│         └───────────────┼───────────────┘                   │
│                         │                                   │
└─────────────────────────┼───────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
  │ Prometheus  │ │   Zipkin    │ │  Logstash   │
  │  (Metrics)  │ │ (Tracing)   │ │ (Logging)   │
  └─────────────┘ └─────────────┘ └─────────────┘
          │               │               │
          ▼               ▼               ▼
  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
  │   Grafana   │ │   Grafana   │ │   Kibana    │
  │(Dashboards) │ │ (Tracing)   │ │(Log Search) │
  └─────────────┘ └─────────────┘ └─────────────┘
          │
          ▼
  ┌─────────────┐
  │Alertmanager │
  │  (Alerts)   │
  └─────────────┘
```

## Components

### 1. Metrics Collection

#### Application Metrics
- **HTTP Request Metrics**: Response times, error rates, request counts
- **JVM Metrics**: Memory usage, garbage collection, thread pools
- **Database Metrics**: Connection pool usage, query performance
- **Cache Metrics**: Hit rates, evictions, memory usage

#### Business Metrics
- **User Activity**: Login rates, active users, session duration
- **CRM Operations**: Contacts created, deals converted, lead scoring
- **Revenue Metrics**: Pipeline value, conversion rates, revenue trends
- **Performance KPIs**: Search response times, report generation

#### Custom Annotations
```java
@Timed(value = "business.operation", description = "Business operation timing")
@Monitored("operation-name")
@BusinessLog(operation = "create-contact", domain = "contacts", action = CREATE)
```

### 2. Distributed Tracing

#### Features
- **Automatic Instrumentation**: HTTP requests, database calls, message queues
- **Custom Spans**: Business operations with context
- **Correlation IDs**: Request tracking across services
- **Performance Analysis**: Bottleneck identification

#### Trace Context
- Correlation ID for request tracking
- Tenant ID for multi-tenant tracing
- User ID for user activity correlation
- Business operation context

### 3. Structured Logging

#### Log Types
- **Application Logs**: Standard application events
- **Business Logs**: Business operation tracking
- **Security Logs**: Authentication, authorization events
- **Performance Logs**: Slow operations, resource usage
- **Audit Logs**: Data changes, user actions

#### Log Format
```json
{
  "timestamp": "2024-01-24T10:30:00.000Z",
  "level": "INFO",
  "logger": "BUSINESS_OPERATIONS",
  "message": "Contact created successfully",
  "correlationId": "abc123def456",
  "tenantId": "tenant-001",
  "userId": "user-123",
  "business": {
    "operation": "create-contact",
    "domain": "contacts",
    "action": "CREATE",
    "duration": 150,
    "status": "SUCCESS"
  }
}
```

### 4. Health Checks

#### Comprehensive Health Indicators
- **Database Connectivity**: Connection validation
- **Memory Usage**: JVM heap and non-heap memory
- **Disk Space**: Available storage
- **External Dependencies**: Service availability

#### Health Endpoint
```
GET /actuator/health
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "Available",
        "validationQuery": "Passed"
      }
    },
    "memory": {
      "status": "UP",
      "details": {
        "usage_percent": "65.2%"
      }
    }
  }
}
```

## Dashboards

### 1. CRM Platform Overview
- Service health status
- API request rates and response times
- Error rates and availability
- Active users and business KPIs

### 2. Business Metrics Dashboard
- Daily active users
- Contact creation rates
- Deal conversion funnel
- Revenue trends and pipeline value

### 3. Security Metrics Dashboard
- Authentication events
- Failed login attempts
- JWT token metrics
- Security alerts and suspicious activity

### 4. Performance Dashboard
- Database query performance
- Cache hit rates
- Memory and CPU usage
- Slow operation detection

## Alerting

### Alert Categories

#### Critical Alerts
- Service downtime
- High error rates (>5%)
- Database connectivity issues
- Memory usage >90%

#### Warning Alerts
- High response times (>2s 95th percentile)
- Low cache hit rates (<80%)
- High CPU usage (>80%)
- Disk space low (>85%)

#### Security Alerts
- Suspicious login activity (>50 failed attempts/sec)
- High unauthorized access rates
- Account lockouts
- JWT token anomalies

#### Business Alerts
- Low deal conversion rates (<10%)
- Unusual user activity patterns
- API rate limit violations
- Data synchronization failures

### Alert Routing
- **Critical**: Immediate notification to ops team via email/Slack
- **Warning**: Notification to dev team
- **Security**: Immediate notification to security team
- **Business**: Notification to business stakeholders

## Setup and Configuration

### Quick Start
```bash
# Run the monitoring setup script
./scripts/setup-monitoring.sh

# Start all monitoring services
docker-compose -f infra/docker/docker-compose.yml up -d
```

### Access URLs
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Alertmanager**: http://localhost:9093
- **Kibana**: http://localhost:5601
- **Zipkin**: http://localhost:9411

### Configuration Files
- **Prometheus**: `infra/docker/config/prometheus/prometheus.yml`
- **Alert Rules**: `infra/docker/config/prometheus/alerts.yml`
- **Alertmanager**: `infra/docker/config/alertmanager/alertmanager.yml`
- **Logstash**: `infra/docker/config/logstash/pipeline/logstash.conf`

## Usage Examples

### Adding Custom Metrics
```java
@Service
public class ContactService {
    
    @Autowired
    private BusinessMetricsCollector metricsCollector;
    
    @Timed("contact.creation")
    @Monitored("create-contact")
    @BusinessLog(operation = "create-contact", domain = "contacts", action = CREATE)
    public Contact createContact(ContactRequest request) {
        Timer.Sample sample = metricsCollector.startContactSearchTimer();
        
        try {
            Contact contact = // ... create contact logic
            metricsCollector.incrementContactsCreated();
            return contact;
        } finally {
            metricsCollector.recordContactSearchTime(sample);
        }
    }
}
```

### Adding Custom Traces
```java
@Traced(operationName = "complex-business-operation", 
        domain = "deals", 
        includeParameters = true)
public DealResult processComplexDeal(DealRequest request) {
    // Business logic with automatic tracing
    return result;
}
```

### Security Logging
```java
@SecurityLog(operation = "admin-action", 
             type = ADMIN_ACTION, 
             riskLevel = HIGH)
public void performAdminAction(AdminRequest request) {
    // High-risk admin operation with security logging
}
```

## Best Practices

### Metrics
1. Use consistent naming conventions for metrics
2. Add appropriate tags for filtering and grouping
3. Monitor both technical and business metrics
4. Set up alerts for critical thresholds

### Tracing
1. Add business context to spans
2. Use correlation IDs consistently
3. Avoid tracing sensitive data
4. Monitor trace sampling rates

### Logging
1. Use structured logging with JSON format
2. Include correlation IDs in all logs
3. Log at appropriate levels
4. Avoid logging sensitive information

### Alerting
1. Set meaningful alert thresholds
2. Avoid alert fatigue with proper grouping
3. Include runbook links in alerts
4. Test alert routing regularly

## Troubleshooting

### Common Issues

#### High Memory Usage
1. Check JVM heap settings
2. Review garbage collection metrics
3. Analyze memory leaks with heap dumps
4. Monitor cache usage patterns

#### Slow Database Queries
1. Review query execution plans
2. Check database connection pool usage
3. Monitor slow query logs
4. Analyze database performance metrics

#### High Error Rates
1. Check application logs for exceptions
2. Review distributed traces for failures
3. Monitor external service dependencies
4. Analyze error patterns and trends

### Monitoring Health
- Regularly review dashboard accuracy
- Validate alert thresholds
- Test monitoring during incidents
- Update dashboards based on operational needs

## Security Considerations

### Data Privacy
- Mask sensitive data in logs and traces
- Use secure communication for metrics
- Implement proper access controls
- Regular security audits of monitoring data

### Access Control
- Role-based access to monitoring tools
- Secure API endpoints for metrics
- Encrypted communication channels
- Regular credential rotation

## Maintenance

### Regular Tasks
- Update monitoring tool versions
- Review and tune alert thresholds
- Clean up old metrics and logs
- Validate dashboard accuracy

### Capacity Planning
- Monitor storage usage for metrics/logs
- Plan for metric retention policies
- Scale monitoring infrastructure
- Optimize query performance

This comprehensive monitoring and observability implementation provides full visibility into the CRM platform's health, performance, and business metrics, enabling proactive issue resolution and data-driven decision making.