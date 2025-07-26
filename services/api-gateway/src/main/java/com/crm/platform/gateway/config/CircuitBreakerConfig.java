package com.crm.platform.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration using Resilience4j
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * Default circuit breaker configuration
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .slowCallRateThreshold(50)
                        .slowCallDurationThreshold(Duration.ofSeconds(2))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .minimumNumberOfCalls(5)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .build());
    }

    /**
     * Circuit breaker configuration for auth service
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> authServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowSize(20)
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .failureRateThreshold(60)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .slowCallRateThreshold(60)
                        .slowCallDurationThreshold(Duration.ofSeconds(3))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .minimumNumberOfCalls(10)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build()), "auth-service-cb");
    }

    /**
     * Circuit breaker configuration for business services
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> businessServiceCustomizer() {
        return factory -> {
            // Configure for tenant service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(45))
                            .slowCallRateThreshold(50)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .minimumNumberOfCalls(7)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(7))
                            .build()), "tenant-service-cb");

            // Configure for users service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(45))
                            .slowCallRateThreshold(50)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .minimumNumberOfCalls(7)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(7))
                            .build()), "users-service-cb");

            // Configure for contacts service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallRateThreshold(50)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .minimumNumberOfCalls(7)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()), "contacts-service-cb");

            // Configure for deals service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallRateThreshold(50)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .minimumNumberOfCalls(7)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()), "deals-service-cb");

            // Configure for leads service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallRateThreshold(50)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .minimumNumberOfCalls(7)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()), "leads-service-cb");

            // Configure for accounts service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallRateThreshold(50)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .minimumNumberOfCalls(7)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()), "accounts-service-cb");

            // Configure for activities service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallRateThreshold(50)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .minimumNumberOfCalls(7)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()), "activities-service-cb");

            // Configure for pipelines service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallRateThreshold(50)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .minimumNumberOfCalls(7)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()), "pipelines-service-cb");
        };
    }
}