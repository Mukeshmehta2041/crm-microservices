package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaBackupCodesRegenerateRequest {
    
    @NotBlank(message = "Current password is required")
    private String password;
    
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    public MfaBackupCodesRegenerateRequest() {}

    public MfaBackupCodesRegenerateRequest(String password) {
        this.password = password;
    }

    // Getters and Setters
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}