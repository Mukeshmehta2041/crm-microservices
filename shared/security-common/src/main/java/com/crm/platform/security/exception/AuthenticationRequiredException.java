package com.crm.platform.security.exception;

/**
 * Exception thrown when authentication is required but not provided
 */
public class AuthenticationRequiredException extends SecurityException {
    
    public AuthenticationRequiredException(String message) {
        super(message);
    }
    
    public AuthenticationRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}