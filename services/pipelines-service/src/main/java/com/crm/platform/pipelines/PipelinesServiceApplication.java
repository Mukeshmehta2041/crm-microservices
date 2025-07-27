package com.crm.platform.pipelines;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableEurekaClient
@EnableJpaAuditing
@EnableKafka
@EnableCaching
public class PipelinesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelinesServiceApplication.class, args);
    }
}