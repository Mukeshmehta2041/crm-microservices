package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for email verification status
 */
public class EmailVerificationStatusResponse {
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("is_verified")
    private boolean isVerified;
    
    @JsonProperty("verified_at")
    private LocalDateTime verifiedAt;
    
    @JsonProperty("pending_verifications")
    private List<PendingVerification> pendingVerifications;
    
    @JsonProperty("can_resend")
    private boolean canResend;
    
    @JsonProperty("next_resend_at")
    private LocalDateTime nextResendAt;
    
    @JsonProperty("verification_required")
    private boolean verificationRequired;

    public EmailVerificationStatusResponse() {}

    public EmailVerificationStatusResponse(String email, boolean isVerified, LocalDateTime verifiedAt,
                                         List<PendingVerification> pendingVerifications, boolean canResend,
                                         LocalDateTime nextResendAt, boolean verificationRequired) {
        this.email = email;
        this.isVerified = isVerified;
        this.verifiedAt = verifiedAt;
        this.pendingVerifications = pendingVerifications;
        this.canResend = canResend;
        this.nextResendAt = nextResendAt;
        this.verificationRequired = verificationRequired;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public List<PendingVerification> getPendingVerifications() { return pendingVerifications; }
    public void setPendingVerifications(List<PendingVerification> pendingVerifications) { this.pendingVerifications = pendingVerifications; }

    public boolean isCanResend() { return canResend; }
    public void setCanResend(boolean canResend) { this.canResend = canResend; }

    public LocalDateTime getNextResendAt() { return nextResendAt; }
    public void setNextResendAt(LocalDateTime nextResendAt) { this.nextResendAt = nextResendAt; }

    public boolean isVerificationRequired() { return verificationRequired; }
    public void setVerificationRequired(boolean verificationRequired) { this.verificationRequired = verificationRequired; }

    public static class PendingVerification {
        @JsonProperty("verification_type")
        private String verificationType;
        
        @JsonProperty("expires_at")
        private LocalDateTime expiresAt;
        
        @JsonProperty("attempts_remaining")
        private Integer attemptsRemaining;

        public PendingVerification() {}

        public PendingVerification(String verificationType, LocalDateTime expiresAt, Integer attemptsRemaining) {
            this.verificationType = verificationType;
            this.expiresAt = expiresAt;
            this.attemptsRemaining = attemptsRemaining;
        }

        // Getters and Setters
        public String getVerificationType() { return verificationType; }
        public void setVerificationType(String verificationType) { this.verificationType = verificationType; }

        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

        public Integer getAttemptsRemaining() { return attemptsRemaining; }
        public void setAttemptsRemaining(Integer attemptsRemaining) { this.attemptsRemaining = attemptsRemaining; }
    }
}