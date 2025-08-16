package com.crm.platform.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request DTO for updating custom fields
 */
public class CustomFieldsRequest {
    
    @NotNull(message = "Custom fields cannot be null")
    @JsonProperty("custom_fields")
    private Map<String, Object> customFields;

    public CustomFieldsRequest() {}

    public CustomFieldsRequest(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    // Getters and Setters
    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }
}