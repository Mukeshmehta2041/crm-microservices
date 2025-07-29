package com.crm.platform.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${analytics.cache.ttl:300}")
    private long cacheTtlSeconds;

    private static final String CACHE_PREFIX = "analytics:";
    private static final String QUERY_CACHE_PREFIX = CACHE_PREFIX + "query:";
    private static final String DASHBOARD_CACHE_PREFIX = CACHE_PREFIX + "dashboard:";
    private static final String WIDGET_CACHE_PREFIX = CACHE_PREFIX + "widget:";

    public void cacheQueryResult(String queryHash, List<Map<String, Object>> result) {
        try {
            String key = QUERY_CACHE_PREFIX + queryHash;
            String value = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(cacheTtlSeconds));
            log.debug("Cached query result for hash: {}", queryHash);
        } catch (JsonProcessingException e) {
            log.error("Error caching query result: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<List<Map<String, Object>>> getCachedQueryResult(String queryHash) {
        try {
            String key = QUERY_CACHE_PREFIX + queryHash;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                List<Map<String, Object>> result = objectMapper.readValue(value, List.class);
                log.debug("Retrieved cached query result for hash: {}", queryHash);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached query result: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void cacheDashboardData(Long dashboardId, String organizationId, Object data) {
        try {
            String key = DASHBOARD_CACHE_PREFIX + organizationId + ":" + dashboardId;
            String value = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(cacheTtlSeconds));
            log.debug("Cached dashboard data for dashboard: {} in org: {}", dashboardId, organizationId);
        } catch (JsonProcessingException e) {
            log.error("Error caching dashboard data: {}", e.getMessage(), e);
        }
    }

    public Optional<Object> getCachedDashboardData(Long dashboardId, String organizationId) {
        try {
            String key = DASHBOARD_CACHE_PREFIX + organizationId + ":" + dashboardId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                Object result = objectMapper.readValue(value, Object.class);
                log.debug("Retrieved cached dashboard data for dashboard: {} in org: {}", dashboardId, organizationId);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached dashboard data: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void cacheWidgetData(Long widgetId, Object data) {
        try {
            String key = WIDGET_CACHE_PREFIX + widgetId;
            String value = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(cacheTtlSeconds));
            log.debug("Cached widget data for widget: {}", widgetId);
        } catch (JsonProcessingException e) {
            log.error("Error caching widget data: {}", e.getMessage(), e);
        }
    }

    public Optional<Object> getCachedWidgetData(Long widgetId) {
        try {
            String key = WIDGET_CACHE_PREFIX + widgetId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                Object result = objectMapper.readValue(value, Object.class);
                log.debug("Retrieved cached widget data for widget: {}", widgetId);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached widget data: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void invalidateQueryCache(String queryHash) {
        String key = QUERY_CACHE_PREFIX + queryHash;
        redisTemplate.delete(key);
        log.debug("Invalidated query cache for hash: {}", queryHash);
    }

    public void invalidateDashboardCache(Long dashboardId, String organizationId) {
        String key = DASHBOARD_CACHE_PREFIX + organizationId + ":" + dashboardId;
        redisTemplate.delete(key);
        log.debug("Invalidated dashboard cache for dashboard: {} in org: {}", dashboardId, organizationId);
    }

    public void invalidateWidgetCache(Long widgetId) {
        String key = WIDGET_CACHE_PREFIX + widgetId;
        redisTemplate.delete(key);
        log.debug("Invalidated widget cache for widget: {}", widgetId);
    }

    public void invalidateAllCacheForOrganization(String organizationId) {
        String pattern = CACHE_PREFIX + "*" + organizationId + "*";
        redisTemplate.delete(redisTemplate.keys(pattern));
        log.debug("Invalidated all cache for organization: {}", organizationId);
    }

    public String generateQueryHash(String query, Map<String, Object> parameters) {
        try {
            String combined = query + (parameters != null ? objectMapper.writeValueAsString(parameters) : "");
            return String.valueOf(combined.hashCode());
        } catch (JsonProcessingException e) {
            log.error("Error generating query hash: {}", e.getMessage(), e);
            return String.valueOf(query.hashCode());
        }
    }

    public boolean isCacheEnabled() {
        return cacheTtlSeconds > 0;
    }

    public long getCacheTtl() {
        return cacheTtlSeconds;
    }
}