package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailVerificationRequest {
    
    @NotBlank(message = "Verification token is required")
    @JsonProperty("verification_token")
    private String verificationToken;

    public EmailVerificationRequest() {}

    public EmailVerificationRequest(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    // Getters and Setters
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
}