package com.crm.platform.gateway.config;

import com.crm.platform.gateway.filter.AuthenticationGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Routes Configuration
 */
@Configuration
public class GatewayRoutesConfig {

    @Autowired
    private AuthenticationGatewayFilterFactory authenticationGatewayFilterFactory;

    @Autowired
    private RedisRateLimiter defaultRedisRateLimiter;

    @Autowired
    private KeyResolver userKeyResolver;

    /**
     * Configure routes for all microservices
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("auth-service-cb")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                                .addRequestHeader("X-Gateway-Timestamp", "#{T(java.time.Instant).now().toString()}"))
                        .uri("lb://auth-service"))
                
                // Tenant Service Routes
                .route("tenant-service", r -> r
                        .path("/api/v1/tenants/**")
                        .filters(f -> f
                                .filter(authenticationGatewayFilterFactory.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("tenant-service-cb")
                                        .setFallbackUri("forward:/fallback/tenant"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}"))
                        .uri("lb://tenant-service"))
                
                // Users Service Routes
                .route("users-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(authenticationGatewayFilterFactory.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("users-service-cb")
                                        .setFallbackUri("forward:/fallback/users"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}"))
                        .uri("lb://users-service"))
                
                // Contacts Service Routes (for future implementation)
                .route("contacts-service", r -> r
                        .path("/api/v1/contacts/**")
                        .filters(f -> f
                                .filter(authenticationGatewayFilterFactory.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("contacts-service-cb")
                                        .setFallbackUri("forward:/fallback/contacts"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}"))
                        .uri("lb://contacts-service"))
                
                // Deals Service Routes (for future implementation)
                .route("deals-service", r -> r
                        .path("/api/v1/deals/**")
                        .filters(f -> f
                                .filter(authenticationGatewayFilterFactory.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("deals-service-cb")
                                        .setFallbackUri("forward:/fallback/deals"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}"))
                        .uri("lb://deals-service"))
                
                // Leads Service Routes (for future implementation)
                .route("leads-service", r -> r
                        .path("/api/v1/leads/**")
                        .filters(f -> f
                                .filter(authenticationGatewayFilterFactory.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("leads-service-cb")
                                        .setFallbackUri("forward:/fallback/leads"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}"))
                        .uri("lb://leads-service"))
                
                // Accounts Service Routes (for future implementation)
                .route("accounts-service", r -> r
                        .path("/api/v1/accounts/**")
                        .filters(f -> f
                                .filter(authenticationGatewayFilterFactory.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("accounts-service-cb")
                                        .setFallbackUri("forward:/fallback/accounts"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}"))
                        .uri("lb://accounts-service"))
                
                // Activities Service Routes (for future implementation)
                .route("activities-service", r -> r
                        .path("/api/v1/activities/**")
                        .filters(f -> f
                                .filter(authenticationGatewayFilterFactory.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("activities-service-cb")
                                        .setFallbackUri("forward:/fallback/activities"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}"))
                        .uri("lb://activities-service"))
                
                // Pipelines Service Routes (for future implementation)
                .route("pipelines-service", r -> r
                        .path("/api/v1/pipelines/**")
                        .filters(f -> f
                                .filter(authenticationGatewayFilterFactory.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("pipelines-service-cb")
                                        .setFallbackUri("forward:/fallback/pipelines"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRedisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}"))
                        .uri("lb://pipelines-service"))
                
                // Health Check Route
                .route("health-check", r -> r
                        .path("/health/**")
                        .uri("lb://discovery-server"))
                
                .build();
    }
}