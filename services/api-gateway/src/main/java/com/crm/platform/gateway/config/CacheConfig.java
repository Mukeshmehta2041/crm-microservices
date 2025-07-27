package com.crm.platform.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Cache configuration for API Gateway
 * Configures Caffeine cache for Spring Cloud LoadBalancer
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager for LoadBalancer
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(256)
                .expireAfterWrite(Duration.ofSeconds(35))
                .recordStats());
        return cacheManager;
    }
}