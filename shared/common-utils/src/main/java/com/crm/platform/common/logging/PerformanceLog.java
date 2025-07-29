package com.crm.platform.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for performance logging.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceLog {
    
    /**
     * Name of the operation.
     */
    String operation() default "";
    
    /**
     * Performance category.
     */
    PerformanceCategory category() default PerformanceCategory.GENERAL;
    
    /**
     * Threshold in milliseconds for slow operation warning.
     */
    long thresholdMs() default 1000;
    
    enum PerformanceCategory {
        DATABASE, CACHE, API_CALL, BUSINESS_LOGIC, SEARCH, REPORT, GENERAL
    }
}