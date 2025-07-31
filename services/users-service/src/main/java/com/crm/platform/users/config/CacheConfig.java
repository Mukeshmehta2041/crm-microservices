package com.crm.platform.users.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Cache configuration for the users service
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Redis cache manager - used when Redis is available
     */
    @Bean
    @Primary
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
     * Fallback cache manager - used when Redis is not available
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple", matchIfMissing = true)
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