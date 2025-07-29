package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    
    private String code;
    private String message;
    private String field;
    private Object value;
    private String constraint;
    private Map<String, Object> context;

    public ApiError() {}

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiError(String code, String message, String field, Object value, String constraint, Map<String, Object> context) {
        this.code = code;
        this.message = message;
        this.field = field;
        this.value = value;
        this.constraint = constraint;
        this.context = context;
    }

    // Static factory methods for common error types
    public static ApiError validationError(String field, String message, Object value) {
        return new ApiError("VALIDATION_ERROR", message, field, value, null, null);
    }

    public static ApiError requiredField(String field) {
        return new ApiError("REQUIRED_FIELD", field + " is required", field, null, "Required field", null);
    }

    public static ApiError invalidFormat(String field, String message, Object value) {
        return new ApiError("INVALID_FORMAT", message, field, value, "Invalid format", null);
    }

    public static ApiError notFound(String resource, Object id) {
        return new ApiError("NOT_FOUND", resource + " not found", "id", id, null, null);
    }

    public static ApiError unauthorized(String message) {
        return new ApiError("UNAUTHORIZED", message != null ? message : "Authentication required", null, null, null, null);
    }

    public static ApiError forbidden(String message) {
        return new ApiError("FORBIDDEN", message != null ? message : "Access denied", null, null, null, null);
    }

    public static ApiError conflict(String field, String message, Object value) {
        return new ApiError("CONFLICT", message, field, value, "Conflict", null);
    }

    public static ApiError internalError(String message) {
        return new ApiError("INTERNAL_ERROR", message != null ? message : "Internal server error", null, null, null, null);
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public String getConstraint() { return constraint; }
    public void setConstraint(String constraint) { this.constraint = constraint; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}