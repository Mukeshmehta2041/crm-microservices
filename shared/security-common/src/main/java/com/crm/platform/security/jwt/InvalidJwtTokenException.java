package com.crm.platform.security.jwt;

import com.crm.platform.common.exception.CrmBusinessException;

/**
 * Exception thrown when JWT token is invalid
 */
public class InvalidJwtTokenException extends CrmBusinessException {
    
    public InvalidJwtTokenException(String message) {
        super("INVALID_JWT_TOKEN", message);
    }
    
    public InvalidJwtTokenException(String message, Throwable cause) {
        super("INVALID_JWT_TOKEN", message, cause);
    }
}