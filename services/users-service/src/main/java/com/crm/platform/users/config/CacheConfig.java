package com.crm.platform.users.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Cache configuration for the users service
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine cache manager - high performance local cache
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "users", "roles", "permissions", "teams", "profiles", "userRoles"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .expireAfterAccess(Duration.ofMinutes(2))
            .recordStats());
        return cacheManager;
    }

    /**
     * Redis cache manager - used when Redis is available
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(
                    org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(java.time.Duration.ofMinutes(30))
                        .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                            .fromSerializer(new org.springframework.data.redis.serializer.StringRedisSerializer()))
                        .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                            .fromSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer()))
                )
                .build();
    }

    /**
     * Fallback cache manager - used when neither Caffeine nor Redis is available
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(
            "users",
            "roles", 
            "permissions",
            "teams",
            "profiles"
        );
    }
}