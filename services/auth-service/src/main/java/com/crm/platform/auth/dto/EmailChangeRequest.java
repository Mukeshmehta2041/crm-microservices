package com.crm.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for email change verification
 */
public class EmailChangeRequest {
    
    @NotBlank(message = "New email is required")
    @Email(message = "Valid email is required")
    @JsonProperty("new_email")
    private String newEmail;
    
    @NotBlank(message = "Current password is required")
    @JsonProperty("current_password")
    private String currentPassword;

    public EmailChangeRequest() {}

    public EmailChangeRequest(String newEmail, String currentPassword) {
        this.newEmail = newEmail;
        this.currentPassword = currentPassword;
    }

    // Getters and Setters
    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}