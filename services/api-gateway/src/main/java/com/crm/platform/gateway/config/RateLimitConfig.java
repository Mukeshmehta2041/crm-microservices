package com.crm.platform.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting Configuration
 */
@Configuration
public class RateLimitConfig {

    /**
     * Default rate limiter with Redis backend
     * 10 requests per second with burst capacity of 20
     */
    @Bean
    @Primary
    public RedisRateLimiter defaultRedisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Higher rate limit for authenticated users
     */
    @Bean("authenticatedUserRateLimiter")
    public RedisRateLimiter authenticatedUserRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }

    /**
     * Lower rate limit for public endpoints
     */
    @Bean("publicEndpointRateLimiter")
    public RedisRateLimiter publicEndpointRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /**
     * Key resolver based on user ID from JWT token
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from request headers (set by auth filter)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            
            // Fallback to IP-based rate limiting
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * Key resolver based on tenant ID
     */
    @Bean("tenantKeyResolver")
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
            if (tenantId != null) {
                return Mono.just("tenant:" + tenantId);
            }
            
            // Fallback to IP-based rate limiting
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * Key resolver based on IP address
     */
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("ip:" + getClientIp(exchange));
    }

    private String getClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        // Check for X-Forwarded-For header (proxy/load balancer)
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check for X-Real-IP header
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fallback to remote address
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }
}