package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MfaVerificationRequest {
    
    @JsonProperty("mfa_token")
    private String mfaToken;
    
    @NotBlank(message = "MFA code is required")
    @Pattern(regexp = "\\d{6}|[A-Za-z0-9]{8}", message = "Code must be 6 digits or 8 character backup code")
    private String code;
    
    @Pattern(regexp = "totp|sms|email|backup", message = "Method must be totp, sms, email, or backup")
    private String method;
    
    @JsonProperty("remember_device")
    private boolean rememberDevice = false;

    public MfaVerificationRequest() {}

    public MfaVerificationRequest(String mfaToken, String code, String method, boolean rememberDevice) {
        this.mfaToken = mfaToken;
        this.code = code;
        this.method = method;
        this.rememberDevice = rememberDevice;
    }

    // Getters and Setters
    public String getMfaToken() { return mfaToken; }
    public void setMfaToken(String mfaToken) { this.mfaToken = mfaToken; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public boolean isRememberDevice() { return rememberDevice; }
    public void setRememberDevice(boolean rememberDevice) { this.rememberDevice = rememberDevice; }
}