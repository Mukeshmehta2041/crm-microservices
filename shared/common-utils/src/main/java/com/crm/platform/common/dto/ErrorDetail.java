package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standardized error detail for API responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("field")
    private String field;
    
    @JsonProperty("value")
    private String value;
    
    @JsonProperty("constraint")
    private String constraint;
    
    public ErrorDetail() {}
    
    public ErrorDetail(String code, String message, String field, String value, String constraint) {
        this.code = code;
        this.message = message;
        this.field = field;
        this.value = value;
        this.constraint = constraint;
    }
    
    // Getters and setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getConstraint() {
        return constraint;
    }
    
    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }
}