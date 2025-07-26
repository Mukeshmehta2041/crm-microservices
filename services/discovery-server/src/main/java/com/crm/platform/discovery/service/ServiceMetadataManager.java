package com.crm.platform.discovery.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing service metadata and providing load balancing information
 */
@Service
public class ServiceMetadataManager {

    private static final Logger logger = LoggerFactory.getLogger(ServiceMetadataManager.class);

    @Autowired
    private MeterRegistry meterRegistry;

    private final Map<String, ServiceMetadata> serviceMetadataCache = new ConcurrentHashMap<>();
    private final Counter serviceRegistrationCounter;
    private final Counter serviceDeregistrationCounter;
    private final Counter healthCheckCounter;

    public ServiceMetadataManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.serviceRegistrationCounter = Counter.builder("eureka.server.service.registrations")
                .description("Number of service registrations")
                .register(meterRegistry);
        this.serviceDeregistrationCounter = Counter.builder("eureka.server.service.deregistrations")
                .description("Number of service deregistrations")
                .register(meterRegistry);
        this.healthCheckCounter = Counter.builder("eureka.server.health.checks")
                .description("Number of health checks performed")
                .register(meterRegistry);
    }

    /**
     * Get service metadata for a specific service
     */
    public ServiceMetadata getServiceMetadata(String serviceName) {
        return serviceMetadataCache.get(serviceName.toLowerCase());
    }

    /**
     * Get all registered services with their metadata
     */
    public Map<String, ServiceMetadata> getAllServiceMetadata() {
        return new HashMap<>(serviceMetadataCache);
    }

    /**
     * Get load balancing information for a service
     */
    public LoadBalancingInfo getLoadBalancingInfo(String serviceName) {
        ServiceMetadata metadata = getServiceMetadata(serviceName);
        if (metadata == null) {
            return null;
        }

        return LoadBalancingInfo.builder()
                .serviceName(serviceName)
                .totalInstances(metadata.getTotalInstances())
                .healthyInstances(metadata.getHealthyInstances())
                .instances(metadata.getInstances())
                .loadBalancingStrategy("ROUND_ROBIN") // Default strategy
                .build();
    }

    /**
     * Scheduled task to update service metadata cache
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void updateServiceMetadata() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            if (serverContext == null) {
                logger.warn("Eureka server context not available");
                return;
            }

            PeerAwareInstanceRegistry registry = serverContext.getRegistry();
            if (registry == null) {
                logger.warn("Eureka registry not available");
                return;
            }

            // Get all applications from registry
            List<Application> applications = registry.getSortedApplications();
            
            // Clear old metadata
            serviceMetadataCache.clear();

            for (Application application : applications) {
                String serviceName = application.getName().toLowerCase();
                List<InstanceInfo> instances = application.getInstances();
                
                ServiceMetadata metadata = ServiceMetadata.builder()
                        .serviceName(serviceName)
                        .totalInstances(instances.size())
                        .healthyInstances(countHealthyInstances(instances))
                        .instances(convertToInstanceMetadata(instances))
                        .lastUpdated(System.currentTimeMillis())
                        .build();

                serviceMetadataCache.put(serviceName, metadata);
                
                // Update metrics
                meterRegistry.gauge("eureka.service.instances.total", 
                                  io.micrometer.core.instrument.Tags.of("service", serviceName), instances.size());
                meterRegistry.gauge("eureka.service.instances.healthy", 
                                  io.micrometer.core.instrument.Tags.of("service", serviceName), metadata.getHealthyInstances());
            }

            logger.debug("Updated metadata for {} services", serviceMetadataCache.size());
            
        } catch (Exception e) {
            logger.error("Error updating service metadata", e);
        }
    }

    /**
     * Handle service registration event
     */
    public void onServiceRegistration(String serviceName, InstanceInfo instanceInfo) {
        String instanceId = instanceInfo != null ? instanceInfo.getInstanceId() : "unknown";
        logger.info("Service registered: {} - Instance: {}", serviceName, instanceId);
        serviceRegistrationCounter.increment();
        
        // Update metadata immediately for new registrations
        updateServiceMetadata();
    }

    /**
     * Handle service deregistration event
     */
    public void onServiceDeregistration(String serviceName, String instanceId) {
        logger.info("Service deregistered: {} - Instance: {}", serviceName, instanceId);
        serviceDeregistrationCounter.increment();
        
        // Update metadata immediately for deregistrations
        updateServiceMetadata();
    }

    /**
     * Perform health check on service instances
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void performHealthChecks() {
        try {
            for (ServiceMetadata metadata : serviceMetadataCache.values()) {
                for (InstanceMetadata instance : metadata.getInstances()) {
                    // Increment health check counter
                    healthCheckCounter.increment();
                    
                    // Log health check (in a real implementation, you might perform actual health checks)
                    logger.debug("Health check for service: {} instance: {}", 
                               metadata.getServiceName(), instance.getInstanceId());
                }
            }
        } catch (Exception e) {
            logger.error("Error performing health checks", e);
        }
    }

    private int countHealthyInstances(List<InstanceInfo> instances) {
        return (int) instances.stream()
                .filter(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.UP)
                .count();
    }

    private List<InstanceMetadata> convertToInstanceMetadata(List<InstanceInfo> instances) {
        return instances.stream()
                .map(this::convertToInstanceMetadata)
                .collect(Collectors.toList());
    }

    private InstanceMetadata convertToInstanceMetadata(InstanceInfo instanceInfo) {
        return InstanceMetadata.builder()
                .instanceId(instanceInfo.getInstanceId())
                .hostName(instanceInfo.getHostName())
                .ipAddress(instanceInfo.getIPAddr())
                .port(instanceInfo.getPort())
                .securePort(instanceInfo.getSecurePort())
                .status(instanceInfo.getStatus().name())
                .healthCheckUrl(instanceInfo.getHealthCheckUrl())
                .statusPageUrl(instanceInfo.getStatusPageUrl())
                .homePageUrl(instanceInfo.getHomePageUrl())
                .metadata(new HashMap<>(instanceInfo.getMetadata()))
                .lastUpdatedTimestamp(instanceInfo.getLastUpdatedTimestamp())
                .build();
    }

    /**
     * Service metadata data class
     */
    public static class ServiceMetadata {
        private String serviceName;
        private int totalInstances;
        private int healthyInstances;
        private List<InstanceMetadata> instances;
        private long lastUpdated;

        // Builder pattern implementation
        public static ServiceMetadataBuilder builder() {
            return new ServiceMetadataBuilder();
        }

        // Getters
        public String getServiceName() { return serviceName; }
        public int getTotalInstances() { return totalInstances; }
        public int getHealthyInstances() { return healthyInstances; }
        public List<InstanceMetadata> getInstances() { return instances; }
        public long getLastUpdated() { return lastUpdated; }

        // Builder class
        public static class ServiceMetadataBuilder {
            private String serviceName;
            private int totalInstances;
            private int healthyInstances;
            private List<InstanceMetadata> instances;
            private long lastUpdated;

            public ServiceMetadataBuilder serviceName(String serviceName) {
                this.serviceName = serviceName;
                return this;
            }

            public ServiceMetadataBuilder totalInstances(int totalInstances) {
                this.totalInstances = totalInstances;
                return this;
            }

            public ServiceMetadataBuilder healthyInstances(int healthyInstances) {
                this.healthyInstances = healthyInstances;
                return this;
            }

            public ServiceMetadataBuilder instances(List<InstanceMetadata> instances) {
                this.instances = instances;
                return this;
            }

            public ServiceMetadataBuilder lastUpdated(long lastUpdated) {
                this.lastUpdated = lastUpdated;
                return this;
            }

            public ServiceMetadata build() {
                ServiceMetadata metadata = new ServiceMetadata();
                metadata.serviceName = this.serviceName;
                metadata.totalInstances = this.totalInstances;
                metadata.healthyInstances = this.healthyInstances;
                metadata.instances = this.instances;
                metadata.lastUpdated = this.lastUpdated;
                return metadata;
            }
        }
    }

    /**
     * Instance metadata data class
     */
    public static class InstanceMetadata {
        private String instanceId;
        private String hostName;
        private String ipAddress;
        private int port;
        private int securePort;
        private String status;
        private String healthCheckUrl;
        private String statusPageUrl;
        private String homePageUrl;
        private Map<String, String> metadata;
        private long lastUpdatedTimestamp;

        // Builder pattern implementation
        public static InstanceMetadataBuilder builder() {
            return new InstanceMetadataBuilder();
        }

        // Getters
        public String getInstanceId() { return instanceId; }
        public String getHostName() { return hostName; }
        public String getIpAddress() { return ipAddress; }
        public int getPort() { return port; }
        public int getSecurePort() { return securePort; }
        public String getStatus() { return status; }
        public String getHealthCheckUrl() { return healthCheckUrl; }
        public String getStatusPageUrl() { return statusPageUrl; }
        public String getHomePageUrl() { return homePageUrl; }
        public Map<String, String> getMetadata() { return metadata; }
        public long getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }

        // Builder class
        public static class InstanceMetadataBuilder {
            private String instanceId;
            private String hostName;
            private String ipAddress;
            private int port;
            private int securePort;
            private String status;
            private String healthCheckUrl;
            private String statusPageUrl;
            private String homePageUrl;
            private Map<String, String> metadata;
            private long lastUpdatedTimestamp;

            public InstanceMetadataBuilder instanceId(String instanceId) {
                this.instanceId = instanceId;
                return this;
            }

            public InstanceMetadataBuilder hostName(String hostName) {
                this.hostName = hostName;
                return this;
            }

            public InstanceMetadataBuilder ipAddress(String ipAddress) {
                this.ipAddress = ipAddress;
                return this;
            }

            public InstanceMetadataBuilder port(int port) {
                this.port = port;
                return this;
            }

            public InstanceMetadataBuilder securePort(int securePort) {
                this.securePort = securePort;
                return this;
            }

            public InstanceMetadataBuilder status(String status) {
                this.status = status;
                return this;
            }

            public InstanceMetadataBuilder healthCheckUrl(String healthCheckUrl) {
                this.healthCheckUrl = healthCheckUrl;
                return this;
            }

            public InstanceMetadataBuilder statusPageUrl(String statusPageUrl) {
                this.statusPageUrl = statusPageUrl;
                return this;
            }

            public InstanceMetadataBuilder homePageUrl(String homePageUrl) {
                this.homePageUrl = homePageUrl;
                return this;
            }

            public InstanceMetadataBuilder metadata(Map<String, String> metadata) {
                this.metadata = metadata;
                return this;
            }

            public InstanceMetadataBuilder lastUpdatedTimestamp(long lastUpdatedTimestamp) {
                this.lastUpdatedTimestamp = lastUpdatedTimestamp;
                return this;
            }

            public InstanceMetadata build() {
                InstanceMetadata instance = new InstanceMetadata();
                instance.instanceId = this.instanceId;
                instance.hostName = this.hostName;
                instance.ipAddress = this.ipAddress;
                instance.port = this.port;
                instance.securePort = this.securePort;
                instance.status = this.status;
                instance.healthCheckUrl = this.healthCheckUrl;
                instance.statusPageUrl = this.statusPageUrl;
                instance.homePageUrl = this.homePageUrl;
                instance.metadata = this.metadata;
                instance.lastUpdatedTimestamp = this.lastUpdatedTimestamp;
                return instance;
            }
        }
    }

    /**
     * Load balancing information data class
     */
    public static class LoadBalancingInfo {
        private String serviceName;
        private int totalInstances;
        private int healthyInstances;
        private List<InstanceMetadata> instances;
        private String loadBalancingStrategy;

        // Builder pattern implementation
        public static LoadBalancingInfoBuilder builder() {
            return new LoadBalancingInfoBuilder();
        }

        // Getters
        public String getServiceName() { return serviceName; }
        public int getTotalInstances() { return totalInstances; }
        public int getHealthyInstances() { return healthyInstances; }
        public List<InstanceMetadata> getInstances() { return instances; }
        public String getLoadBalancingStrategy() { return loadBalancingStrategy; }

        // Builder class
        public static class LoadBalancingInfoBuilder {
            private String serviceName;
            private int totalInstances;
            private int healthyInstances;
            private List<InstanceMetadata> instances;
            private String loadBalancingStrategy;

            public LoadBalancingInfoBuilder serviceName(String serviceName) {
                this.serviceName = serviceName;
                return this;
            }

            public LoadBalancingInfoBuilder totalInstances(int totalInstances) {
                this.totalInstances = totalInstances;
                return this;
            }

            public LoadBalancingInfoBuilder healthyInstances(int healthyInstances) {
                this.healthyInstances = healthyInstances;
                return this;
            }

            public LoadBalancingInfoBuilder instances(List<InstanceMetadata> instances) {
                this.instances = instances;
                return this;
            }

            public LoadBalancingInfoBuilder loadBalancingStrategy(String loadBalancingStrategy) {
                this.loadBalancingStrategy = loadBalancingStrategy;
                return this;
            }

            public LoadBalancingInfo build() {
                LoadBalancingInfo info = new LoadBalancingInfo();
                info.serviceName = this.serviceName;
                info.totalInstances = this.totalInstances;
                info.healthyInstances = this.healthyInstances;
                info.instances = this.instances;
                info.loadBalancingStrategy = this.loadBalancingStrategy;
                return info;
            }
        }
    }
}