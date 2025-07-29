package com.crm.platform.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for security operation logging.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecurityLog {
    
    /**
     * Name of the security operation.
     */
    String operation() default "";
    
    /**
     * Type of security operation.
     */
    SecurityType type() default SecurityType.OTHER;
    
    /**
     * Risk level of the operation.
     */
    RiskLevel riskLevel() default RiskLevel.MEDIUM;
    
    enum SecurityType {
        AUTHENTICATION, AUTHORIZATION, DATA_ACCESS, ADMIN_ACTION, OTHER
    }
    
    enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}