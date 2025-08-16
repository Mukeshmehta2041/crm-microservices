package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class RegistrationResponse {
    
    @JsonProperty("user_id")
    private UUID userId;
    
    private String email;
    
    @JsonProperty("email_verification_required")
    private boolean emailVerificationRequired;
    
    @JsonProperty("verification_email_sent")
    private boolean verificationEmailSent;
    
    @JsonProperty("tenant_id")
    private UUID tenantId;
    
    @JsonProperty("created_at")
    private Instant createdAt;

    public RegistrationResponse() {}

    public RegistrationResponse(UUID userId, String email, boolean emailVerificationRequired, 
                              boolean verificationEmailSent, UUID tenantId, Instant createdAt) {
        this.userId = userId;
        this.email = email;
        this.emailVerificationRequired = emailVerificationRequired;
        this.verificationEmailSent = verificationEmailSent;
        this.tenantId = tenantId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerificationRequired() { return emailVerificationRequired; }
    public void setEmailVerificationRequired(boolean emailVerificationRequired) { this.emailVerificationRequired = emailVerificationRequired; }

    public boolean isVerificationEmailSent() { return verificationEmailSent; }
    public void setVerificationEmailSent(boolean verificationEmailSent) { this.verificationEmailSent = verificationEmailSent; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}