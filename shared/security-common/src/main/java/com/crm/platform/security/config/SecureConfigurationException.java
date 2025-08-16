package com.crm.platform.security.config;

/**
 * Exception thrown when secure configuration operations fail
 */
public class SecureConfigurationException extends RuntimeException {
    
    public SecureConfigurationException(String message) {
        super(message);
    }
    
    public SecureConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}