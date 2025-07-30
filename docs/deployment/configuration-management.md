# Configuration Management Guide

This guide covers comprehensive configuration management for the CRM Platform across different environments.

## Table of Contents

1. [Configuration Overview](#configuration-overview)
2. [Environment-Specific Configuration](#environment-specific-configuration)
3. [Security Configuration](#security-configuration)
4. [Database Configuration](#database-configuration)
5. [Caching Configuration](#caching-configuration)
6. [Logging Configuration](#logging-configuration)
7. [Monitoring Configuration](#monitoring-configuration)
8. [Performance Tuning](#performance-tuning)
9. [Configuration Validation](#configuration-validation)
10. [Best Practices](#best-practices)

## Configuration Overview

The CRM Platform uses Spring Boot's configuration system with support for:

- **Property Files** - `.properties` and `.yml` files
- **Environment Variables** - System environment variables
- **Command Line Arguments** - JVM and application arguments
- **External Configuration** - Config servers and external sources
- **Profiles** - Environment-specific configurations

### Configuration Hierarchy

Configuration is loaded in the following order (higher priority overrides lower):

1. Command line arguments
2. System environment variables
3. External configuration (Config Server)
4. Profile-specific properties (`application-{profile}.yml`)
5. Default properties (`application.yml`)

### Configuration Structure

```
config/
├── application.yml                 # Default configuration
├── application-local.yml          # Local development
├── application-docker.yml         # Docker environment
├── application-staging.yml        # Staging environment
├── application-production.yml     # Production environment
├── logback-spring.xml             # Logging configuration
└── security/
    ├── keystore.p12              # SSL certificates
    └── truststore.jks            # Trusted certificates
```

## Environment-Specific Configuration

### Default Configuration (application.yml)

```yaml
# Application Configuration
spring:
  application:
    name: crm-platform
  profiles:
    active: local
  
  # Database Configuration
  datasource:
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      validation-timeout: 5000
      leak-detection-threshold: 60000
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
  # Redis Configuration
  redis:
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
  
  # Security Configuration
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081/auth/realms/crm-platform

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /api/v1
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
  http2:
    enabled: true

# Management Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
  health:
    redis:
      enabled: true
    db:
      enabled: true

# Application-Specific Configuration
crm:
  security:
    jwt:
      expiration: 900 # 15 minutes
      refresh-expiration: 604800 # 7 days
    rate-limiting:
      enabled: true
      default-limit: 100
      window-size: 60
    cors:
      allowed-origins: "*"
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: "*"
      allow-credentials: true
  
  email:
    enabled: true
    templates:
      path: classpath:/templates/email/
  
  tenant:
    default-tenant: system
    isolation-level: SCHEMA
  
  audit:
    enabled: true
    retention-days: 90

# Logging Configuration
logging:
  level:
    com.crm.platform: INFO
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Local Development (application-local.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/crm_platform
    username: crm_user
    password: crm_password
  
  redis:
    host: localhost
    port: 6379
    password: ""
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update

server:
  port: 8081

crm:
  security:
    jwt:
      secret: local-development-secret-key-change-in-production
    cors:
      allowed-origins: "http://localhost:3000,http://localhost:8080"
    rate-limiting:
      enabled: false
  
  email:
    smtp:
      host: localhost
      port: 1025
      username: ""
      password: ""
      from: noreply@crm-platform.local
  
  audit:
    enabled: false

logging:
  level:
    com.crm.platform: DEBUG
    org.springframework.web: DEBUG
  file:
    name: logs/crm-platform-local.log
```

### Docker Environment (application-docker.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:crm_platform}
    username: ${DB_USERNAME:crm_user}
    password: ${DB_PASSWORD:crm_password}
  
  redis:
    host: ${REDIS_HOST:redis}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:""}

server:
  port: ${SERVER_PORT:8081}

crm:
  security:
    jwt:
      secret: ${JWT_SECRET:docker-secret-key}
      expiration: ${JWT_EXPIRATION:900}
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800}
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS:*}
    rate-limiting:
      enabled: ${RATE_LIMIT_ENABLED:true}
  
  email:
    smtp:
      host: ${SMTP_HOST:mailhog}
      port: ${SMTP_PORT:1025}
      username: ${SMTP_USERNAME:""}
      password: ${SMTP_PASSWORD:""}
      from: ${SMTP_FROM:noreply@crm-platform.local}

logging:
  level:
    com.crm.platform: ${LOG_LEVEL:INFO}
  file:
    name: ${LOG_FILE_PATH:/var/log/crm-platform}/crm-platform.log
```

### Staging Environment (application-staging.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 15
      minimum-idle: 3
      connection-timeout: 30000
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}
    ssl: true
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

server:
  port: ${SERVER_PORT:8081}
  ssl:
    enabled: true
    key-store: classpath:security/keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12

crm:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: ${JWT_EXPIRATION:900}
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800}
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS}
    rate-limiting:
      enabled: true
      default-limit: 100
      burst-capacity: 200
  
  email:
    smtp:
      host: ${SMTP_HOST}
      port: ${SMTP_PORT:587}
      username: ${SMTP_USERNAME}
      password: ${SMTP_PASSWORD}
      from: ${SMTP_FROM}
      ssl:
        enabled: true
  
  tenant:
    isolation-level: SCHEMA
  
  audit:
    enabled: true
    retention-days: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: never

logging:
  level:
    com.crm.platform: INFO
    org.springframework.security: WARN
    org.hibernate: WARN
  file:
    name: ${LOG_FILE_PATH:/var/log/crm-platform}/crm-platform-staging.log
    max-size: 100MB
    max-history: 30
```

### Production Environment (application-production.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
  
  redis:
    cluster:
      nodes: ${REDIS_CLUSTER_NODES}
    password: ${REDIS_PASSWORD}
    ssl: true
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 2
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory

server:
  port: ${SERVER_PORT:8081}
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    trust-store: ${SSL_TRUSTSTORE_PATH}
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD}
  tomcat:
    max-threads: 200
    min-spare-threads: 10
    max-connections: 8192
    accept-count: 100
    connection-timeout: 20000

crm:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: ${JWT_EXPIRATION:900}
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800}
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS}
    rate-limiting:
      enabled: true
      default-limit: 1000
      burst-capacity: 2000
      redis-key-prefix: "rate_limit:"
  
  email:
    smtp:
      host: ${SMTP_HOST}
      port: ${SMTP_PORT:587}
      username: ${SMTP_USERNAME}
      password: ${SMTP_PASSWORD}
      from: ${SMTP_FROM}
      ssl:
        enabled: true
      connection-timeout: 5000
      timeout: 10000
  
  tenant:
    isolation-level: SCHEMA
    cache:
      enabled: true
      ttl: 3600
  
  audit:
    enabled: true
    retention-days: 365
    async: true
    batch-size: 100

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: never
      cache:
        time-to-live: 10s
  metrics:
    export:
      prometheus:
        enabled: true
        step: 30s
    distribution:
      percentiles-histogram:
        http.server.requests: true
      sla:
        http.server.requests: 50ms,100ms,200ms,500ms

logging:
  level:
    com.crm.platform: WARN
    org.springframework: WARN
    org.hibernate: WARN
    org.apache.tomcat: WARN
  file:
    name: ${LOG_FILE_PATH:/var/log/crm-platform}/crm-platform-production.log
    max-size: 500MB
    max-history: 90
    total-size-cap: 10GB
  logback:
    rollingpolicy:
      clean-history-on-start: true
```

## Security Configuration

### JWT Configuration

```yaml
crm:
  security:
    jwt:
      secret: ${JWT_SECRET} # Must be at least 256 bits
      expiration: 900 # 15 minutes
      refresh-expiration: 604800 # 7 days
      issuer: crm-platform
      audience: crm-platform-api
      algorithm: HS256
      
    oauth2:
      enabled: true
      providers:
        google:
          client-id: ${GOOGLE_CLIENT_ID}
          client-secret: ${GOOGLE_CLIENT_SECRET}
          redirect-uri: ${BASE_URL}/auth/oauth2/callback/google
        microsoft:
          client-id: ${MICROSOFT_CLIENT_ID}
          client-secret: ${MICROSOFT_CLIENT_SECRET}
          redirect-uri: ${BASE_URL}/auth/oauth2/callback/microsoft
    
    password:
      min-length: 8
      require-uppercase: true
      require-lowercase: true
      require-numbers: true
      require-special-chars: true
      history-count: 5
      expiry-days: 90
    
    session:
      timeout: 1800 # 30 minutes
      max-concurrent: 5
      prevent-fixation: true
    
    mfa:
      enabled: true
      issuer: CRM Platform
      window-size: 3
      backup-codes-count: 10
```

### CORS Configuration

```yaml
crm:
  security:
    cors:
      allowed-origins: 
        - https://app.crm-platform.com
        - https://admin.crm-platform.com
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers:
        - Authorization
        - Content-Type
        - X-Tenant-ID
        - X-Request-ID
      exposed-headers:
        - X-Total-Count
        - X-Page-Count
        - X-Rate-Limit-Remaining
      allow-credentials: true
      max-age: 3600
```

### Rate Limiting Configuration

```yaml
crm:
  security:
    rate-limiting:
      enabled: true
      redis-key-prefix: "rate_limit:"
      default-limit: 100
      window-size: 60 # seconds
      burst-capacity: 200
      
      endpoints:
        "/auth/login":
          limit: 5
          window: 60
          burst: 10
        "/auth/register":
          limit: 3
          window: 300
          burst: 5
        "/auth/password/reset":
          limit: 3
          window: 300
          burst: 5
        "/users/**":
          limit: 1000
          window: 60
          burst: 1500
      
      ip-whitelist:
        - 10.0.0.0/8
        - 172.16.0.0/12
        - 192.168.0.0/16
      
      captcha:
        enabled: true
        threshold: 5 # Failed attempts before CAPTCHA
        provider: recaptcha
        site-key: ${RECAPTCHA_SITE_KEY}
        secret-key: ${RECAPTCHA_SECRET_KEY}
```

## Database Configuration

### Connection Pool Configuration

```yaml
spring:
  datasource:
    hikari:
      # Pool sizing
      maximum-pool-size: 30
      minimum-idle: 10
      
      # Connection management
      connection-timeout: 30000 # 30 seconds
      idle-timeout: 600000 # 10 minutes
      max-lifetime: 1800000 # 30 minutes
      
      # Leak detection
      leak-detection-threshold: 60000 # 1 minute
      
      # Connection validation
      validation-timeout: 5000
      connection-test-query: SELECT 1
      
      # Pool name for monitoring
      pool-name: CRMPlatformPool
      
      # Additional properties
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false
```

### JPA/Hibernate Configuration

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
        implicit-strategy: org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl
    
    properties:
      hibernate:
        # SQL and formatting
        show_sql: false
        format_sql: true
        use_sql_comments: true
        
        # Batch processing
        jdbc:
          batch_size: 50
          batch_versioned_data: true
        order_inserts: true
        order_updates: true
        
        # Second-level cache
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        
        # Statistics
        generate_statistics: true
        
        # Connection handling
        connection:
          provider_disables_autocommit: true
        
        # Query optimization
        query:
          plan_cache_max_size: 2048
          plan_parameter_metadata_max_size: 128
```

### Migration Configuration (Flyway)

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    clean-disabled: true
    out-of-order: false
    ignore-missing-migrations: false
    ignore-ignored-migrations: false
    ignore-pending-migrations: false
    ignore-future-migrations: false
    mixed: false
    group: false
    installed-by: crm-platform
    connect-retries: 10
    connect-retries-interval: 60
```

## Caching Configuration

### Redis Configuration

```yaml
spring:
  redis:
    # Connection settings
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:""}
    database: 0
    ssl: ${REDIS_SSL:false}
    
    # Timeout settings
    timeout: 3000ms
    connect-timeout: 2000ms
    
    # Connection pool (Lettuce)
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 2
        max-wait: -1ms
      shutdown-timeout: 100ms
    
    # Cluster configuration (if using Redis Cluster)
    cluster:
      nodes: ${REDIS_CLUSTER_NODES:""}
      max-redirects: 3
    
    # Sentinel configuration (if using Redis Sentinel)
    sentinel:
      master: ${REDIS_SENTINEL_MASTER:""}
      nodes: ${REDIS_SENTINEL_NODES:""}

# Cache configuration
crm:
  cache:
    redis:
      key-prefix: "crm:"
      default-ttl: 3600 # 1 hour
      
      # Cache-specific TTL settings
      caches:
        users:
          ttl: 1800 # 30 minutes
        roles:
          ttl: 7200 # 2 hours
        permissions:
          ttl: 7200 # 2 hours
        tenants:
          ttl: 3600 # 1 hour
        sessions:
          ttl: 1800 # 30 minutes
        rate-limits:
          ttl: 60 # 1 minute
```

### Application Cache Configuration

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000 # 1 hour in milliseconds
      cache-null-values: false
      key-prefix: "crm:cache:"
      use-key-prefix: true
    
    cache-names:
      - users
      - roles
      - permissions
      - tenants
      - configurations
      - rate-limits

# Caffeine local cache (for frequently accessed data)
crm:
  cache:
    local:
      enabled: true
      caffeine:
        spec: "maximumSize=1000,expireAfterWrite=10m"
      
      caches:
        user-permissions:
          spec: "maximumSize=5000,expireAfterWrite=5m"
        tenant-config:
          spec: "maximumSize=100,expireAfterWrite=30m"
        rate-limit-config:
          spec: "maximumSize=1000,expireAfterWrite=1m"
```

## Logging Configuration

### Logback Configuration (logback-spring.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE_PATH:-./logs}/crm-platform.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE_PATH:-./logs}/crm-platform.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
            <cleanHistoryOnStart>true</cleanHistoryOnStart>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Audit Log Appender -->
    <appender name="AUDIT" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE_PATH:-./logs}/audit.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE_PATH:-./logs}/audit.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>500MB</maxFileSize>
            <maxHistory>365</maxHistory>
            <totalSizeCap>50GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Security Log Appender -->
    <appender name="SECURITY" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE_PATH:-./logs}/security.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE_PATH:-./logs}/security.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>200MB</maxFileSize>
            <maxHistory>90</maxHistory>
            <totalSizeCap>20GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Async Appenders for Performance -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>1024</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>false</includeCallerData>
    </appender>
    
    <appender name="ASYNC_AUDIT" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="AUDIT"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>false</includeCallerData>
    </appender>
    
    <!-- Logger Configurations -->
    <logger name="com.crm.platform" level="${LOG_LEVEL:-INFO}" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
    </logger>
    
    <logger name="com.crm.platform.audit" level="INFO" additivity="false">
        <appender-ref ref="ASYNC_AUDIT"/>
    </logger>
    
    <logger name="com.crm.platform.security" level="INFO" additivity="false">
        <appender-ref ref="SECURITY"/>
    </logger>
    
    <!-- Third-party loggers -->
    <logger name="org.springframework.security" level="WARN"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE"/>
    <logger name="com.zaxxer.hikari" level="WARN"/>
    <logger name="org.apache.tomcat" level="WARN"/>
    
    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
    </root>
    
    <!-- Profile-specific configurations -->
    <springProfile name="local">
        <logger name="com.crm.platform" level="DEBUG"/>
        <logger name="org.springframework.web" level="DEBUG"/>
    </springProfile>
    
    <springProfile name="production">
        <logger name="com.crm.platform" level="WARN"/>
        <root level="WARN">
            <appender-ref ref="ASYNC_FILE"/>
        </root>
    </springProfile>
</configuration>
```

### Application Logging Configuration

```yaml
logging:
  level:
    com.crm.platform: ${LOG_LEVEL:INFO}
    org.springframework.security: WARN
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    com.zaxxer.hikari: WARN
    org.apache.tomcat: WARN
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n"
  
  file:
    name: ${LOG_FILE_PATH:/var/log/crm-platform}/crm-platform.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 10GB

# Custom logging configuration
crm:
  logging:
    audit:
      enabled: true
      file: ${LOG_FILE_PATH:/var/log/crm-platform}/audit.log
      max-size: 500MB
      max-history: 365
      async: true
    
    security:
      enabled: true
      file: ${LOG_FILE_PATH:/var/log/crm-platform}/security.log
      max-size: 200MB
      max-history: 90
      include-request-details: true
    
    performance:
      enabled: true
      slow-query-threshold: 1000 # milliseconds
      log-sql-parameters: false
```

## Configuration Validation

### Validation Rules

```yaml
# Configuration validation
crm:
  config:
    validation:
      enabled: true
      fail-fast: true
      
      rules:
        # Database validation
        database:
          connection-timeout-max: 60000
          pool-size-min: 5
          pool-size-max: 100
        
        # Security validation
        security:
          jwt-secret-min-length: 32
          password-min-length: 8
          session-timeout-max: 7200
        
        # Performance validation
        performance:
          cache-ttl-max: 86400
          batch-size-max: 1000
          thread-pool-max: 500
```

### Configuration Health Checks

```java
@Component
@ConfigurationProperties(prefix = "crm.config.validation")
public class ConfigurationValidator {
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        validateDatabaseConfiguration();
        validateSecurityConfiguration();
        validateCacheConfiguration();
        validateLoggingConfiguration();
    }
    
    private void validateDatabaseConfiguration() {
        // Validate database connection settings
        // Check pool size limits
        // Verify connection timeout values
    }
    
    private void validateSecurityConfiguration() {
        // Validate JWT secret strength
        // Check password policy settings
        // Verify CORS configuration
    }
}
```

## Best Practices

### Security Best Practices

1. **Never store secrets in configuration files**
   - Use environment variables or secret management systems
   - Rotate secrets regularly
   - Use different secrets for each environment

2. **Encrypt sensitive configuration**
   - Use Spring Cloud Config encryption
   - Implement configuration encryption at rest
   - Use secure communication channels

3. **Validate configuration on startup**
   - Implement configuration validation
   - Fail fast on invalid configuration
   - Log configuration validation results

### Performance Best Practices

1. **Optimize connection pools**
   - Size pools based on actual load
   - Monitor pool utilization
   - Configure appropriate timeouts

2. **Configure caching appropriately**
   - Set reasonable TTL values
   - Monitor cache hit rates
   - Use appropriate cache eviction policies

3. **Tune JVM settings**
   - Set appropriate heap sizes
   - Configure garbage collection
   - Monitor JVM metrics

### Operational Best Practices

1. **Use configuration profiles**
   - Separate configuration by environment
   - Use profile-specific overrides
   - Validate profile-specific settings

2. **Implement configuration monitoring**
   - Monitor configuration changes
   - Track configuration drift
   - Alert on configuration issues

3. **Document configuration changes**
   - Maintain configuration change logs
   - Document configuration dependencies
   - Version configuration files

---

*This configuration management guide provides comprehensive settings for all environments. Always validate configuration changes in non-production environments first.*