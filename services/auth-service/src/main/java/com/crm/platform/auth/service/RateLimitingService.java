package com.crm.platform.auth.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    // Rate limiting configurations
    private static final int LOGIN_ATTEMPTS_PER_MINUTE = 5;
    private static final int LOGIN_ATTEMPTS_PER_HOUR = 20;

    @Autowired
    public RateLimitingService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String identifier, String operation) {
        String key = operation + ":" + identifier;
        Bucket bucket = bucketCache.computeIfAbsent(key, k -> createBucket(operation));
        return bucket.tryConsume(1);
    }

    private Bucket createBucket(String operation) {
        switch (operation) {
            case "login":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(LOGIN_ATTEMPTS_PER_MINUTE, Refill.intervally(LOGIN_ATTEMPTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(LOGIN_ATTEMPTS_PER_HOUR, Refill.intervally(LOGIN_ATTEMPTS_PER_HOUR, Duration.ofHours(1))))
                    .build();
            default:
                // Default rate limit: 100 requests per minute
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                    .build();
        }
    }

    public void resetRateLimit(String identifier, String operation) {
        String key = operation + ":" + identifier;
        bucketCache.remove(key);
    }
}