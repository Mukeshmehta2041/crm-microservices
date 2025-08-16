package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionValidationRequest {
    
    @NotBlank(message = "Session ID is required")
    @JsonProperty("session_id")
    private String sessionId;

    public SessionValidationRequest() {}

    public SessionValidationRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}