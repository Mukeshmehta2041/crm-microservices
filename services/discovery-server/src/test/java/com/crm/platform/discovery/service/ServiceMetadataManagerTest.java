package com.crm.platform.discovery.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ServiceMetadataManager
 */
class ServiceMetadataManagerTest {

    private ServiceMetadataManager serviceMetadataManager;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        serviceMetadataManager = new ServiceMetadataManager(meterRegistry);
    }

    @Test
    void shouldReturnNullForNonExistentService() {
        // When
        ServiceMetadataManager.ServiceMetadata metadata = 
            serviceMetadataManager.getServiceMetadata("non-existent-service");

        // Then
        assertThat(metadata).isNull();
    }

    @Test
    void shouldReturnEmptyMapWhenNoServicesRegistered() {
        // When
        var allMetadata = serviceMetadataManager.getAllServiceMetadata();

        // Then
        assertThat(allMetadata).isEmpty();
    }

    @Test
    void shouldReturnNullLoadBalancingInfoForNonExistentService() {
        // When
        ServiceMetadataManager.LoadBalancingInfo info = 
            serviceMetadataManager.getLoadBalancingInfo("non-existent-service");

        // Then
        assertThat(info).isNull();
    }

    @Test
    void shouldHandleServiceRegistrationEvent() {
        // Given
        String serviceName = "test-service";
        String instanceId = "test-instance-1";

        // When/Then - should not throw any exceptions
        serviceMetadataManager.onServiceRegistration(serviceName, null);
    }

    @Test
    void shouldHandleServiceDeregistrationEvent() {
        // Given
        String serviceName = "test-service";
        String instanceId = "test-instance-1";

        // When/Then - should not throw any exceptions
        serviceMetadataManager.onServiceDeregistration(serviceName, instanceId);
    }

    @Test
    void shouldPerformHealthChecksWithoutErrors() {
        // When/Then - should not throw any exceptions
        serviceMetadataManager.performHealthChecks();
    }

    @Test
    void shouldUpdateServiceMetadataWithoutErrors() {
        // When/Then - should not throw any exceptions
        serviceMetadataManager.updateServiceMetadata();
    }

    @Test
    void shouldBuildServiceMetadataCorrectly() {
        // Given
        String serviceName = "test-service";
        int totalInstances = 3;
        int healthyInstances = 2;
        long lastUpdated = System.currentTimeMillis();

        // When
        ServiceMetadataManager.ServiceMetadata metadata = 
            ServiceMetadataManager.ServiceMetadata.builder()
                .serviceName(serviceName)
                .totalInstances(totalInstances)
                .healthyInstances(healthyInstances)
                .lastUpdated(lastUpdated)
                .build();

        // Then
        assertThat(metadata.getServiceName()).isEqualTo(serviceName);
        assertThat(metadata.getTotalInstances()).isEqualTo(totalInstances);
        assertThat(metadata.getHealthyInstances()).isEqualTo(healthyInstances);
        assertThat(metadata.getLastUpdated()).isEqualTo(lastUpdated);
    }

    @Test
    void shouldBuildInstanceMetadataCorrectly() {
        // Given
        String instanceId = "test-instance-1";
        String hostName = "localhost";
        String ipAddress = "127.0.0.1";
        int port = 8080;
        String status = "UP";

        // When
        ServiceMetadataManager.InstanceMetadata instance = 
            ServiceMetadataManager.InstanceMetadata.builder()
                .instanceId(instanceId)
                .hostName(hostName)
                .ipAddress(ipAddress)
                .port(port)
                .status(status)
                .build();

        // Then
        assertThat(instance.getInstanceId()).isEqualTo(instanceId);
        assertThat(instance.getHostName()).isEqualTo(hostName);
        assertThat(instance.getIpAddress()).isEqualTo(ipAddress);
        assertThat(instance.getPort()).isEqualTo(port);
        assertThat(instance.getStatus()).isEqualTo(status);
    }

    @Test
    void shouldBuildLoadBalancingInfoCorrectly() {
        // Given
        String serviceName = "test-service";
        int totalInstances = 3;
        int healthyInstances = 2;
        String strategy = "ROUND_ROBIN";

        // When
        ServiceMetadataManager.LoadBalancingInfo info = 
            ServiceMetadataManager.LoadBalancingInfo.builder()
                .serviceName(serviceName)
                .totalInstances(totalInstances)
                .healthyInstances(healthyInstances)
                .loadBalancingStrategy(strategy)
                .build();

        // Then
        assertThat(info.getServiceName()).isEqualTo(serviceName);
        assertThat(info.getTotalInstances()).isEqualTo(totalInstances);
        assertThat(info.getHealthyInstances()).isEqualTo(healthyInstances);
        assertThat(info.getLoadBalancingStrategy()).isEqualTo(strategy);
    }
}