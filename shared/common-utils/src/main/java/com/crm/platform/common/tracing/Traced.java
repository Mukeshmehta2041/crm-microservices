package com.crm.platform.common.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for adding business context to distributed traces.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    
    /**
     * Name of the business operation.
     */
    String operationName() default "";
    
    /**
     * Business domain (e.g., "contacts", "deals", "leads").
     */
    String domain() default "";
    
    /**
     * Whether to include method parameters in the trace.
     */
    boolean includeParameters() default false;
    
    /**
     * Whether to include result information in the trace.
     */
    boolean includeResult() default false;
}