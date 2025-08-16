# Redis Caching Integration for CRM Platform

## Overview
This document provides comprehensive guidance for implementing Redis caching strategies across all CRM microservices to improve database performance and reduce query load.

## Caching Architecture

### 1. Cache Layers
```
Application Layer
    ↓
L1 Cache (Application Memory)
    ↓
L2 Cache (Redis Cluster)
    ↓
Database Layer (PostgreSQL)
```

### 2. Cache Types

#### Query Result Cache
- **Purpose**: Cache frequently executed SELECT queries
- **TTL**: 5-30 minutes depending on data volatility
- **Key Pattern**: `query:{service}:{tenant_id}:{query_hash}:{params_hash}`
- **Use Cases**: Contact lists, account searches, deal pipelines

#### Aggregation Cache
- **Purpose**: Cache expensive aggregation queries (COUNT, SUM, AVG)
- **TTL**: 1-6 hours depending on business requirements
- **Key Pattern**: `agg:{service}:{tenant_id}:{table}:{function}:{conditions_hash}`
- **Use Cases**: Dashboard metrics, reporting data, analytics

#### Session Cache
- **Purpose**: Store user session data and authentication tokens
- **TTL**: 8 hours (configurable)
- **Key Pattern**: `session:{user_id}:{session_id}`
- **Use Cases**: User authentication, session management

#### Configuration Cache
- **Purpose**: Cache tenant configurations and feature flags
- **TTL**: 1 hour (with manual invalidation)
- **Key Pattern**: `config:{tenant_id}:{config_type}`
- **Use Cases**: Tenant settings, feature flags, UI configurations

#### Lookup Cache
- **Purpose**: Cache reference data and lookup tables
- **TTL**: 24 hours
- **Key Pattern**: `lookup:{service}:{table}:{key}`
- **Use Cases**: Country codes, industry types, status values

## Implementation Strategy

### 1. Cache-Aside Pattern
```java
@Service
public class ContactService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ContactRepository contactRepository;
    
    public List<Contact> getContactsByTenant(UUID tenantId, int page, int size) {
        String cacheKey = String.format("contacts:tenant:%s:page:%d:size:%d", 
            tenantId, page, size);
        
        // Try cache first
        List<Contact> cachedContacts = (List<Contact>) redisTemplate.opsForValue()
            .get(cacheKey);
        
        if (cachedContacts != null) {
            return cachedContacts;
        }
        
        // Cache miss - fetch from database
        List<Contact> contacts = contactRepository.findByTenantId(tenantId, 
            PageRequest.of(page, size));
        
        // Store in cache with TTL
        redisTemplate.opsForValue().set(cacheKey, contacts, 
            Duration.ofMinutes(15));
        
        return contacts;
    }
}
```

### 2. Write-Through Pattern
```java
@Service
public class AccountService {
    
    public Account updateAccount(Account account) {
        // Update database
        Account updatedAccount = accountRepository.save(account);
        
        // Update cache
        String cacheKey = String.format("account:%s", account.getId());
        redisTemplate.opsForValue().set(cacheKey, updatedAccount, 
            Duration.ofMinutes(30));
        
        // Invalidate related caches
        invalidateRelatedCaches(account.getTenantId(), "accounts");
        
        return updatedAccount;
    }
}
```

### 3. Write-Behind Pattern
```java
@Component
public class CacheWriteBehindProcessor {
    
    @EventListener
    public void handleDataChange(DataChangeEvent event) {
        // Queue cache updates for batch processing
        cacheUpdateQueue.offer(new CacheUpdateTask(
            event.getEntityType(),
            event.getEntityId(),
            event.getOperation()
        ));
    }
    
    @Scheduled(fixedDelay = 5000)
    public void processCacheUpdates() {
        List<CacheUpdateTask> tasks = new ArrayList<>();
        cacheUpdateQueue.drainTo(tasks, 100);
        
        for (CacheUpdateTask task : tasks) {
            processCacheUpdate(task);
        }
    }
}
```

## Cache Configuration

### 1. Redis Cluster Configuration
```yaml
spring:
  redis:
    cluster:
      nodes:
        - redis-node-1:6379
        - redis-node-2:6379
        - redis-node-3:6379
      max-redirects: 3
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms
```

### 2. Cache Serialization
```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use JSON serialization
        Jackson2JsonRedisSerializer<Object> serializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        
        return template;
    }
}
```

### 3. Cache Eviction Policies
```java
@Component
public class CacheEvictionService {
    
    // Time-based eviction
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void evictExpiredCaches() {
        Set<String> keys = redisTemplate.keys("*:expired:*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    
    // Event-based eviction
    @EventListener
    public void handleEntityUpdate(EntityUpdateEvent event) {
        String pattern = String.format("%s:tenant:%s:*", 
            event.getEntityType(), event.getTenantId());
        
        Set<String> keysToEvict = redisTemplate.keys(pattern);
        if (!keysToEvict.isEmpty()) {
            redisTemplate.delete(keysToEvict);
        }
    }
}
```

## Service-Specific Caching Strategies

### 1. Auth Service
```java
// Session caching
@Cacheable(value = "user-sessions", key = "#sessionId")
public UserSession getSession(String sessionId) {
    return sessionRepository.findByTokenId(sessionId);
}

// User profile caching
@Cacheable(value = "user-profiles", key = "#userId")
public UserProfile getUserProfile(UUID userId) {
    return userRepository.findById(userId);
}
```

### 2. Contacts Service
```java
// Contact search results
@Cacheable(value = "contact-search", 
           key = "#tenantId + ':' + #searchCriteria.hashCode()")
public List<Contact> searchContacts(UUID tenantId, 
                                   ContactSearchCriteria searchCriteria) {
    return contactRepository.search(tenantId, searchCriteria);
}

// Contact activities
@Cacheable(value = "contact-activities", key = "#contactId")
public List<Activity> getContactActivities(UUID contactId) {
    return activityRepository.findByContactId(contactId);
}
```

### 3. Deals Service
```java
// Pipeline data
@Cacheable(value = "pipeline-data", key = "#tenantId + ':' + #pipelineId")
public PipelineData getPipelineData(UUID tenantId, UUID pipelineId) {
    return pipelineRepository.getPipelineWithDeals(tenantId, pipelineId);
}

// Deal forecasting
@Cacheable(value = "deal-forecasts", key = "#tenantId + ':' + #period")
public ForecastData getDealForecasts(UUID tenantId, String period) {
    return forecastService.calculateForecasts(tenantId, period);
}
```

### 4. Analytics Service
```java
// Dashboard metrics
@Cacheable(value = "dashboard-metrics", 
           key = "#tenantId + ':' + #dateRange", 
           unless = "#result.isEmpty()")
public DashboardMetrics getDashboardMetrics(UUID tenantId, 
                                           DateRange dateRange) {
    return metricsCalculator.calculate(tenantId, dateRange);
}
```

## Cache Monitoring and Metrics

### 1. Cache Hit Rate Monitoring
```java
@Component
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @EventListener
    public void handleCacheHit(CacheHitEvent event) {
        meterRegistry.counter("cache.hits", 
            "service", event.getService(),
            "cache", event.getCacheName()).increment();
    }
    
    @EventListener
    public void handleCacheMiss(CacheMissEvent event) {
        meterRegistry.counter("cache.misses",
            "service", event.getService(),
            "cache", event.getCacheName()).increment();
    }
}
```

### 2. Cache Performance Metrics
```java
@Component
public class CachePerformanceMonitor {
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void collectCacheMetrics() {
        RedisInfo info = redisTemplate.getConnectionFactory()
            .getConnection().info();
        
        // Collect memory usage
        long usedMemory = info.getUsedMemory();
        long maxMemory = info.getMaxMemory();
        
        meterRegistry.gauge("redis.memory.used", usedMemory);
        meterRegistry.gauge("redis.memory.max", maxMemory);
        meterRegistry.gauge("redis.memory.usage.ratio", 
            (double) usedMemory / maxMemory);
        
        // Collect connection metrics
        meterRegistry.gauge("redis.connections.active", 
            info.getConnectedClients());
    }
}
```

## Cache Invalidation Strategies

### 1. Time-Based Invalidation
```java
// Short TTL for frequently changing data
@Cacheable(value = "active-deals", key = "#tenantId")
@CacheEvict(value = "active-deals", key = "#tenantId", 
            condition = "#result.size() > 1000")
public List<Deal> getActiveDeals(UUID tenantId) {
    return dealRepository.findActiveByTenantId(tenantId);
}
```

### 2. Event-Based Invalidation
```java
@EventListener
public void handleContactUpdate(ContactUpdatedEvent event) {
    // Invalidate contact-specific caches
    cacheManager.getCache("contacts").evict(event.getContactId());
    
    // Invalidate related caches
    String pattern = String.format("contact-search:%s:*", 
        event.getTenantId());
    evictCachesByPattern(pattern);
}
```

### 3. Manual Invalidation
```java
@RestController
public class CacheManagementController {
    
    @PostMapping("/admin/cache/invalidate")
    public ResponseEntity<String> invalidateCache(
            @RequestParam String cacheName,
            @RequestParam(required = false) String key) {
        
        if (key != null) {
            cacheManager.getCache(cacheName).evict(key);
        } else {
            cacheManager.getCache(cacheName).clear();
        }
        
        return ResponseEntity.ok("Cache invalidated successfully");
    }
}
```

## Best Practices

### 1. Cache Key Design
- Use consistent naming conventions
- Include tenant ID for multi-tenant isolation
- Use meaningful prefixes for different cache types
- Avoid overly long keys (Redis limit: 512MB)

### 2. TTL Management
- Set appropriate TTLs based on data volatility
- Use shorter TTLs for frequently changing data
- Implement cache warming for critical data
- Monitor and adjust TTLs based on usage patterns

### 3. Memory Management
- Monitor Redis memory usage
- Implement cache size limits
- Use appropriate eviction policies (LRU, LFU)
- Regular cleanup of expired keys

### 4. Error Handling
- Implement cache fallback mechanisms
- Handle Redis connection failures gracefully
- Log cache errors for monitoring
- Use circuit breakers for cache operations

### 5. Security Considerations
- Encrypt sensitive cached data
- Implement proper access controls
- Use secure Redis configurations
- Regular security audits of cached data

## Performance Tuning

### 1. Connection Pool Optimization
```properties
# Lettuce connection pool settings
spring.redis.lettuce.pool.max-active=20
spring.redis.lettuce.pool.max-idle=10
spring.redis.lettuce.pool.min-idle=5
spring.redis.lettuce.pool.max-wait=2000ms
spring.redis.lettuce.pool.time-between-eviction-runs=30s
```

### 2. Serialization Optimization
- Use efficient serialization formats (Protocol Buffers, Avro)
- Compress large cached objects
- Implement custom serializers for complex objects
- Monitor serialization/deserialization performance

### 3. Network Optimization
- Use Redis pipelining for batch operations
- Implement connection pooling
- Configure appropriate timeouts
- Use Redis Cluster for horizontal scaling

## Monitoring and Alerting

### 1. Key Metrics to Monitor
- Cache hit/miss ratios
- Memory usage and growth
- Connection pool utilization
- Query response times
- Cache eviction rates

### 2. Alerting Thresholds
- Cache hit ratio < 80%
- Memory usage > 85%
- Connection pool exhaustion
- High cache eviction rates
- Slow cache operations (> 10ms)

### 3. Dashboard Visualization
- Real-time cache performance metrics
- Historical trends and patterns
- Service-specific cache usage
- Cost-benefit analysis of caching

This comprehensive Redis caching integration will significantly improve database performance by reducing query load and improving response times across all CRM microservices.