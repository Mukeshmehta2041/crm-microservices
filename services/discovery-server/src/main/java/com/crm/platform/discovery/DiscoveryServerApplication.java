package com.crm.platform.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Discovery Server Application using Netflix Eureka
 * 
 * This service provides service discovery and registration capabilities
 * for the CRM microservices platform. It enables services to register
 * themselves and discover other services dynamically.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}