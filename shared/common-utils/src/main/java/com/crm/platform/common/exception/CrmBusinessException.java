package com.crm.platform.common.exception;

import java.util.Map;

/**
 * Base exception for all business exceptions in the CRM platform
 */
public abstract class CrmBusinessException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> details;
    
    public CrmBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Map.of();
    }
    
    public CrmBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = Map.of();
    }
    
    public CrmBusinessException(String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? details : Map.of();
    }
    
    public CrmBusinessException(String errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details != null ? details : Map.of();
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
}