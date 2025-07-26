package com.crm.platform.discovery.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EurekaServerConfig
 */
class EurekaServerConfigTest {

    private EurekaServerConfig eurekaServerConfig;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        eurekaServerConfig = new EurekaServerConfig();
        // Manually inject the meter registry for testing
        eurekaServerConfig.meterRegistry = meterRegistry;
    }

    @Test
    void shouldCreateEurekaServerHealthIndicator() {
        // When
        HealthIndicator healthIndicator = eurekaServerConfig.eurekaServerHealthIndicator();

        // Then
        assertThat(healthIndicator).isNotNull();
    }

    @Test
    void shouldReturnHealthyStatusWhenEurekaServerIsRunning() {
        // Given
        HealthIndicator healthIndicator = eurekaServerConfig.eurekaServerHealthIndicator();

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.UP);
        assertThat(health.getDetails()).containsKey("status");
        assertThat(health.getDetails()).containsKey("registeredInstances");
        assertThat(health.getDetails()).containsKey("lastRegistrationTime");
        assertThat(health.getDetails()).containsKey("selfPreservationMode");
    }

    @Test
    void shouldCreateServiceRegistrationTimer() {
        // When
        var timer = eurekaServerConfig.serviceRegistrationTimer();

        // Then
        assertThat(timer).isNotNull();
        assertThat(timer.getId().getName()).isEqualTo("eureka.server.service.registration.time");
    }

    @Test
    void shouldCreateServiceDeregistrationTimer() {
        // When
        var timer = eurekaServerConfig.serviceDeregistrationTimer();

        // Then
        assertThat(timer).isNotNull();
        assertThat(timer.getId().getName()).isEqualTo("eureka.server.service.deregistration.time");
    }

    @Test
    void shouldCollectMetricsWithoutErrors() {
        // When/Then - should not throw any exceptions
        eurekaServerConfig.collectMetrics();
        
        // Verify metrics are registered
        assertThat(meterRegistry.find("eureka.server.registered.instances").gauge()).isNotNull();
        assertThat(meterRegistry.find("eureka.server.last.registration.time").gauge()).isNotNull();
        assertThat(meterRegistry.find("eureka.server.health.check.failures").gauge()).isNotNull();
    }
}