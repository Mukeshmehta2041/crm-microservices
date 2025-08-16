package com.crm.platform.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify API version for controllers and methods
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {
    
    /**
     * API version number (e.g., "1", "2", "1.1")
     */
    String value();
    
    /**
     * Whether this version is deprecated
     */
    boolean deprecated() default false;
    
    /**
     * Deprecation message
     */
    String deprecationMessage() default "";
    
    /**
     * Sunset date for deprecated versions (ISO 8601 format)
     */
    String sunsetDate() default "";
}