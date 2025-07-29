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
    
    // Token endpoint rate limits
    private static final int TOKEN_REQUESTS_PER_MINUTE = 10;
    private static final int TOKEN_REQUESTS_PER_HOUR = 100;
    private static final int REFRESH_TOKEN_REQUESTS_PER_MINUTE = 5;
    private static final int REFRESH_TOKEN_REQUESTS_PER_HOUR = 50;
    private static final int REVOKE_TOKEN_REQUESTS_PER_MINUTE = 20;
    private static final int INTROSPECT_REQUESTS_PER_MINUTE = 30;

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
            case "oauth2_token":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(TOKEN_REQUESTS_PER_MINUTE, Refill.intervally(TOKEN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(TOKEN_REQUESTS_PER_HOUR, Refill.intervally(TOKEN_REQUESTS_PER_HOUR, Duration.ofHours(1))))
                    .build();
            case "refresh_token":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(REFRESH_TOKEN_REQUESTS_PER_MINUTE, Refill.intervally(REFRESH_TOKEN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(REFRESH_TOKEN_REQUESTS_PER_HOUR, Refill.intervally(REFRESH_TOKEN_REQUESTS_PER_HOUR, Duration.ofHours(1))))
                    .build();
            case "revoke_token":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(REVOKE_TOKEN_REQUESTS_PER_MINUTE, Refill.intervally(REVOKE_TOKEN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .build();
            case "token_introspect":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(INTROSPECT_REQUESTS_PER_MINUTE, Refill.intervally(INTROSPECT_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
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

    /**
     * Check rate limit for OAuth2 token requests
     */
    public boolean isTokenRequestAllowed(String clientId) {
        return isAllowed(clientId, "oauth2_token");
    }

    /**
     * Check rate limit for refresh token requests
     */
    public boolean isRefreshTokenAllowed(String identifier) {
        return isAllowed(identifier, "refresh_token");
    }

    /**
     * Check rate limit for token revocation requests
     */
    public boolean isRevokeTokenAllowed(String identifier) {
        return isAllowed(identifier, "revoke_token");
    }

    /**
     * Check rate limit for token introspection requests
     */
    public boolean isTokenIntrospectionAllowed(String identifier) {
        return isAllowed(identifier, "token_introspect");
    }

    /**
     * Get remaining tokens for a specific operation
     */
    public long getRemainingTokens(String identifier, String operation) {
        String key = operation + ":" + identifier;
        Bucket bucket = bucketCache.get(key);
        if (bucket != null) {
            return bucket.getAvailableTokens();
        }
        return createBucket(operation).getAvailableTokens();
    }

    /**
     * Get rate limit information for monitoring
     */
    public RateLimitInfo getRateLimitInfo(String identifier, String operation) {
        String key = operation + ":" + identifier;
        Bucket bucket = bucketCache.computeIfAbsent(key, k -> createBucket(operation));
        
        return new RateLimitInfo(
            bucket.getAvailableTokens(),
            getMaxTokensForOperation(operation),
            System.currentTimeMillis()
        );
    }

    private long getMaxTokensForOperation(String operation) {
        switch (operation) {
            case "login": return LOGIN_ATTEMPTS_PER_MINUTE;
            case "oauth2_token": return TOKEN_REQUESTS_PER_MINUTE;
            case "refresh_token": return REFRESH_TOKEN_REQUESTS_PER_MINUTE;
            case "revoke_token": return REVOKE_TOKEN_REQUESTS_PER_MINUTE;
            case "token_introspect": return INTROSPECT_REQUESTS_PER_MINUTE;
            default: return 100;
        }
    }

    /**
     * Rate limit information holder
     */
    public static class RateLimitInfo {
        private final long remainingTokens;
        private final long maxTokens;
        private final long timestamp;

        public RateLimitInfo(long remainingTokens, long maxTokens, long timestamp) {
            this.remainingTokens = remainingTokens;
            this.maxTokens = maxTokens;
            this.timestamp = timestamp;
        }

        public long getRemainingTokens() { return remainingTokens; }
        public long getMaxTokens() { return maxTokens; }
        public long getTimestamp() { return timestamp; }
        public boolean isLimitExceeded() { return remainingTokens <= 0; }
    }
}