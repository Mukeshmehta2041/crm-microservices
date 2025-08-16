package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MfaSetupRequest {
    
    @NotBlank(message = "MFA method is required")
    @Pattern(regexp = "totp|sms|email", message = "Method must be totp, sms, or email")
    private String method;
    
    @JsonProperty("phone_number")
    private String phoneNumber;

    public MfaSetupRequest() {}

    public MfaSetupRequest(String method, String phoneNumber) {
        this.method = method;
        this.phoneNumber = phoneNumber;
    }

    // Getters and Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}