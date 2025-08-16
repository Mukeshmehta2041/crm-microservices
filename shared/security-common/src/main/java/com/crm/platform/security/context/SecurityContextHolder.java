package com.crm.platform.security.context;

/**
 * Thread-local holder for security context information
 */
public class SecurityContextHolder {
    
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();
    
    public static void setContext(SecurityContext context) {
        contextHolder.set(context);
    }
    
    public static SecurityContext getContext() {
        return contextHolder.get();
    }
    
    public static void clearContext() {
        contextHolder.remove();
    }
    
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
}