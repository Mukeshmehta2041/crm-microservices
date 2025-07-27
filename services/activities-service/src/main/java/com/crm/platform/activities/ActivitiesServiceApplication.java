package com.crm.platform.activities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableEurekaClient
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class ActivitiesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivitiesServiceApplication.class, args);
    }
}