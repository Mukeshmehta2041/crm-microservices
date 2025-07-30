package com.crm.platform.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a permission
 */
public class CreatePermissionRequest {
    
    @NotBlank(message = "Resource is required")
    @Size(min = 2, max = 100, message = "Resource must be between 2 and 100 characters")
    @JsonProperty("resource")
    private String resource;
    
    @NotBlank(message = "Action is required")
    @Size(min = 2, max = 50, message = "Action must be between 2 and 50 characters")
    @JsonProperty("action")
    private String action;
    
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    @JsonProperty("description")
    private String description;
    
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("priority")
    private Integer priority = 0;
    
    @JsonProperty("constraints")
    private String constraints;

    public CreatePermissionRequest() {}

    public CreatePermissionRequest(String resource, String action, String description, String category) {
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.category = category;
    }

    // Getters and Setters
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }
}