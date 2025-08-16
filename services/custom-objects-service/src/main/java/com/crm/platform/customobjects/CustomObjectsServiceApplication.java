package com.crm.platform.customobjects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Custom Objects Service Application
 * 
 * This service provides dynamic entity management capabilities for the CRM platform,
 * allowing users to create custom objects with custom fields, relationships, and validation rules.
 */
@SpringBootApplication
@EnableEurekaClient
@EnableJpaAuditing
@EnableCaching
@EnableKafka
public class CustomObjectsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomObjectsServiceApplication.class, args);
    }
}