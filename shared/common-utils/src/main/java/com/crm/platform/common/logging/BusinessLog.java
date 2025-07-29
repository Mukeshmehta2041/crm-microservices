package com.crm.platform.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for business operation logging.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessLog {
    
    /**
     * Name of the business operation.
     */
    String operation() default "";
    
    /**
     * Business domain (e.g., "contacts", "deals", "leads").
     */
    String domain() default "";
    
    /**
     * Type of business action.
     */
    BusinessAction action() default BusinessAction.OTHER;
    
    enum BusinessAction {
        CREATE, READ, UPDATE, DELETE, SEARCH, CONVERT, PROCESS, OTHER
    }
}