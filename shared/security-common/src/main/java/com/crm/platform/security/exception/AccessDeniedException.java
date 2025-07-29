package com.crm.platform.security.exception;

/**
 * Exception thrown when user lacks required permissions or roles
 */
public class AccessDeniedException extends SecurityException {
    
    public AccessDeniedException(String message) {
        super(message);
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}