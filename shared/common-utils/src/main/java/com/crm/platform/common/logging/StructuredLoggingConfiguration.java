package com.crm.platform.common.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for structured logging with JSON format and correlation IDs.
 */
@Configuration
public class StructuredLoggingConfiguration {

    @Bean
    public LogstashEncoder logstashEncoder() {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setIncludeContext(true);
        encoder.setIncludeMdc(true);
        encoder.setCustomFields("{\"service\":\"${spring.application.name:-unknown}\"}");
        return encoder;
    }

    @Bean
    public BusinessLoggerAspect businessLoggerAspect() {
        return new BusinessLoggerAspect();
    }

    @Bean
    public SecurityLoggerAspect securityLoggerAspect() {
        return new SecurityLoggerAspect();
    }

    @Bean
    public PerformanceLoggerAspect performanceLoggerAspect() {
        return new PerformanceLoggerAspect();
    }
}