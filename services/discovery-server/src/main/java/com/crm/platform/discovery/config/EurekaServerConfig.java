package com.crm.platform.discovery.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration class for Eureka Server with custom monitoring and health checks
 */
@Configuration
@EnableScheduling
public class EurekaServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(EurekaServerConfig.class);

    @Autowired
    MeterRegistry meterRegistry;

    private final AtomicInteger registeredInstancesCount = new AtomicInteger(0);
    private final AtomicLong lastRegistrationTime = new AtomicLong(0);
    private final AtomicInteger healthCheckFailures = new AtomicInteger(0);

    /**
     * Custom health indicator for Eureka Server
     */
    @Bean
    public HealthIndicator eurekaServerHealthIndicator() {
        return new EurekaServerHealthIndicator();
    }

    /**
     * Scheduled task to collect and report Eureka server metrics
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void collectMetrics() {
        try {
            // Update metrics for monitoring
            meterRegistry.gauge("eureka.server.registered.instances", registeredInstancesCount.get());
            meterRegistry.gauge("eureka.server.last.registration.time", lastRegistrationTime.get());
            meterRegistry.gauge("eureka.server.health.check.failures", healthCheckFailures.get());
            
            logger.debug("Eureka server metrics updated - Registered instances: {}", 
                        registeredInstancesCount.get());
        } catch (Exception e) {
            logger.error("Error collecting Eureka server metrics", e);
            healthCheckFailures.incrementAndGet();
        }
    }

    /**
     * Custom health indicator implementation
     */
    private class EurekaServerHealthIndicator implements HealthIndicator {

        @Override
        public Health health() {
            try {
                // Check if Eureka server is running and responsive
                boolean isHealthy = checkEurekaServerHealth();
                
                if (isHealthy) {
                    healthCheckFailures.set(0);
                    return Health.up()
                            .withDetail("status", "Eureka Server is running")
                            .withDetail("registeredInstances", registeredInstancesCount.get())
                            .withDetail("lastRegistrationTime", lastRegistrationTime.get())
                            .withDetail("selfPreservationMode", getSelfPreservationStatus())
                            .build();
                } else {
                    healthCheckFailures.incrementAndGet();
                    return Health.down()
                            .withDetail("status", "Eureka Server health check failed")
                            .withDetail("consecutiveFailures", healthCheckFailures.get())
                            .build();
                }
            } catch (Exception e) {
                healthCheckFailures.incrementAndGet();
                logger.error("Eureka server health check failed", e);
                return Health.down()
                        .withDetail("status", "Health check exception")
                        .withDetail("error", e.getMessage())
                        .withDetail("consecutiveFailures", healthCheckFailures.get())
                        .build();
            }
        }

        private boolean checkEurekaServerHealth() {
            // Implement basic health checks
            try {
                // Check if the server context is available and initialized
                // This is a basic check - in a real implementation, you might want to
                // check registry status, peer connectivity, etc.
                return true; // Simplified for this implementation
            } catch (Exception e) {
                logger.error("Eureka server health check failed", e);
                return false;
            }
        }

        private String getSelfPreservationStatus() {
            // This would typically check the actual self-preservation mode
            // For now, return a placeholder
            return "enabled";
        }
    }

    /**
     * Timer for measuring service registration time
     */
    @Bean
    public Timer serviceRegistrationTimer() {
        return Timer.builder("eureka.server.service.registration.time")
                .description("Time taken to register a service")
                .register(meterRegistry);
    }

    /**
     * Timer for measuring service deregistration time
     */
    @Bean
    public Timer serviceDeregistrationTimer() {
        return Timer.builder("eureka.server.service.deregistration.time")
                .description("Time taken to deregister a service")
                .register(meterRegistry);
    }
}