package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for email verification operations
 */
public class EmailVerificationResponse {
    
    @JsonProperty("verification_id")
    private UUID verificationId;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("is_verified")
    private boolean isVerified;
    
    @JsonProperty("verified_at")
    private LocalDateTime verifiedAt;
    
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @JsonProperty("verification_type")
    private String verificationType;
    
    @JsonProperty("attempts_remaining")
    private Integer attemptsRemaining;
    
    @JsonProperty("message")
    private String message;

    public EmailVerificationResponse() {}

    public EmailVerificationResponse(UUID verificationId, String email, boolean isVerified, 
                                   LocalDateTime verifiedAt, LocalDateTime expiresAt, 
                                   String verificationType, Integer attemptsRemaining, String message) {
        this.verificationId = verificationId;
        this.email = email;
        this.isVerified = isVerified;
        this.verifiedAt = verifiedAt;
        this.expiresAt = expiresAt;
        this.verificationType = verificationType;
        this.attemptsRemaining = attemptsRemaining;
        this.message = message;
    }

    // Getters and Setters
    public UUID getVerificationId() { return verificationId; }
    public void setVerificationId(UUID verificationId) { this.verificationId = verificationId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getVerificationType() { return verificationType; }
    public void setVerificationType(String verificationType) { this.verificationType = verificationType; }

    public Integer getAttemptsRemaining() { return attemptsRemaining; }
    public void setAttemptsRemaining(Integer attemptsRemaining) { this.attemptsRemaining = attemptsRemaining; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}