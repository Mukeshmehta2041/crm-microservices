package com.crm.platform.common.tracing;

import brave.sampler.Sampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for distributed tracing with Spring Cloud Sleuth and Zipkin.
 * Provides correlation IDs and trace propagation across microservices.
 */
@Configuration
public class TracingConfiguration {

    @Bean
    public Sampler alwaysSampler() {
        return Sampler.create(1.0f); // Sample 100% of traces in development
    }

    @Bean
    public CustomTraceFilter customTraceFilter() {
        return new CustomTraceFilter();
    }

    @Bean
    public BusinessTraceAspect businessTraceAspect() {
        return new BusinessTraceAspect();
    }
}