package com.crm.platform.security.aspect;

import com.crm.platform.security.annotation.RequirePermission;
import com.crm.platform.security.annotation.RequireRole;
import com.crm.platform.security.context.SecurityContext;
import com.crm.platform.security.context.SecurityContextHolder;
import com.crm.platform.security.exception.AccessDeniedException;
import com.crm.platform.security.exception.AuthenticationRequiredException;
import com.crm.platform.security.rbac.Permission;
import com.crm.platform.security.rbac.Role;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect for handling authorization annotations
 */
@Aspect
@Component
public class AuthorizationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationAspect.class);
    
    @Before("@annotation(com.crm.platform.security.annotation.RequirePermission) || " +
            "@within(com.crm.platform.security.annotation.RequirePermission)")
    public void checkPermissions(JoinPoint joinPoint) {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            throw new AuthenticationRequiredException("Authentication required");
        }
        
        RequirePermission annotation = getPermissionAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }
        
        // Check if user has all required permissions
        if (annotation.value().length > 0) {
            for (Permission permission : annotation.value()) {
                if (!context.hasPermission(permission)) {
                    String message = annotation.message().isEmpty() 
                        ? "Access denied: missing permission " + permission.getCode()
                        : annotation.message();
                    
                    logger.warn("Access denied for user {} (tenant {}): missing permission {}",
                               context.getUserId(), context.getTenantId(), permission.getCode());
                    
                    throw new AccessDeniedException(message);
                }
            }
        }
        
        // Check if user has any of the alternative permissions
        if (annotation.anyOf().length > 0) {
            boolean hasAnyPermission = false;
            for (Permission permission : annotation.anyOf()) {
                if (context.hasPermission(permission)) {
                    hasAnyPermission = true;
                    break;
                }
            }
            
            if (!hasAnyPermission) {
                String message = annotation.message().isEmpty()
                    ? "Access denied: missing required permissions"
                    : annotation.message();
                
                logger.warn("Access denied for user {} (tenant {}): missing any of required permissions",
                           context.getUserId(), context.getTenantId());
                
                throw new AccessDeniedException(message);
            }
        }
        
        logger.debug("Permission check passed for user {} (tenant {})",
                    context.getUserId(), context.getTenantId());
    }
    
    @Before("@annotation(com.crm.platform.security.annotation.RequireRole) || " +
            "@within(com.crm.platform.security.annotation.RequireRole)")
    public void checkRoles(JoinPoint joinPoint) {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            throw new AuthenticationRequiredException("Authentication required");
        }
        
        RequireRole annotation = getRoleAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }
        
        // Check if user has all required roles
        if (annotation.value().length > 0) {
            for (Role role : annotation.value()) {
                if (!context.hasRole(role)) {
                    String message = annotation.message().isEmpty()
                        ? "Access denied: missing role " + role.getCode()
                        : annotation.message();
                    
                    logger.warn("Access denied for user {} (tenant {}): missing role {}",
                               context.getUserId(), context.getTenantId(), role.getCode());
                    
                    throw new AccessDeniedException(message);
                }
            }
        }
        
        // Check if user has any of the alternative roles
        if (annotation.anyOf().length > 0) {
            boolean hasAnyRole = false;
            for (Role role : annotation.anyOf()) {
                if (context.hasRole(role)) {
                    hasAnyRole = true;
                    break;
                }
            }
            
            if (!hasAnyRole) {
                String message = annotation.message().isEmpty()
                    ? "Access denied: missing required roles"
                    : annotation.message();
                
                logger.warn("Access denied for user {} (tenant {}): missing any of required roles",
                           context.getUserId(), context.getTenantId());
                
                throw new AccessDeniedException(message);
            }
        }
        
        logger.debug("Role check passed for user {} (tenant {})",
                    context.getUserId(), context.getTenantId());
    }
    
    private RequirePermission getPermissionAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Check method-level annotation first
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation != null) {
            return annotation;
        }
        
        // Check class-level annotation
        return method.getDeclaringClass().getAnnotation(RequirePermission.class);
    }
    
    private RequireRole getRoleAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Check method-level annotation first
        RequireRole annotation = method.getAnnotation(RequireRole.class);
        if (annotation != null) {
            return annotation;
        }
        
        // Check class-level annotation
        return method.getDeclaringClass().getAnnotation(RequireRole.class);
    }
}