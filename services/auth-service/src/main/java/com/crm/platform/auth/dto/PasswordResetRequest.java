package com.crm.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    // Constructors
    public PasswordResetRequest() {}

    public PasswordResetRequest(String email) {
        this.email = email;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}