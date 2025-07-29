package com.crm.platform.security.annotation;

import com.crm.platform.security.rbac.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require specific permissions for method execution
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * Required permissions (user must have ALL specified permissions)
     */
    Permission[] value() default {};
    
    /**
     * Alternative permissions (user must have ANY of the specified permissions)
     */
    Permission[] anyOf() default {};
    
    /**
     * Whether to check tenant context (default: true)
     */
    boolean checkTenant() default true;
    
    /**
     * Custom error message when authorization fails
     */
    String message() default "";
}