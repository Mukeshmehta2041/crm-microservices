package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaVerifySetupRequest {
    
    @NotBlank(message = "MFA method is required")
    @Pattern(regexp = "totp|sms|email", message = "Method must be totp, sms, or email")
    private String method;
    
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
    private String code;

    public MfaVerifySetupRequest() {}

    public MfaVerifySetupRequest(String method, String code) {
        this.method = method;
        this.code = code;
    }

    // Getters and Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}