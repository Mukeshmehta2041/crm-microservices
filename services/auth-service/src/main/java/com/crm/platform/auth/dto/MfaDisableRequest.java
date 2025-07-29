package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaDisableRequest {
    
    @NotBlank(message = "Current password is required")
    private String password;
    
    @NotBlank(message = "MFA code is required")
    @Pattern(regexp = "\\d{6}|[A-Za-z0-9]{8}", message = "Code must be 6 digits or 8 character backup code")
    private String code;

    public MfaDisableRequest() {}

    public MfaDisableRequest(String password, String code) {
        this.password = password;
        this.code = code;
    }

    // Getters and Setters
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}