package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordResetConfirmRequest {
    
    @NotBlank(message = "Reset token is required")
    @JsonProperty("reset_token")
    private String resetToken;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    @JsonProperty("new_password")
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    @JsonProperty("confirm_password")
    private String confirmPassword;

    public PasswordResetConfirmRequest() {}

    public PasswordResetConfirmRequest(String resetToken, String newPassword, String confirmPassword) {
        this.resetToken = resetToken;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    // Getters and Setters
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}