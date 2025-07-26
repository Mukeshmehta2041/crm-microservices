package com.crm.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * API Gateway Application
 * 
 * This service acts as the entry point for all client requests,
 * providing routing, authentication, rate limiting, and monitoring.
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
    "com.crm.platform.gateway",
    "com.crm.platform.security",
    "com.crm.platform.common"
})
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}