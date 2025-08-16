package com.crm.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ResendVerificationRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @JsonProperty("tenant_id")
    private UUID tenantId;

    public ResendVerificationRequest() {}

    public ResendVerificationRequest(String email, UUID tenantId) {
        this.email = email;
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}