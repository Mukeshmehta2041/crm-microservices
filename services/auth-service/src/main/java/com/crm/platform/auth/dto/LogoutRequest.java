package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogoutRequest {
    
    @JsonProperty("logout_all_sessions")
    private boolean logoutAllSessions = false;

    public LogoutRequest() {}

    public LogoutRequest(boolean logoutAllSessions) {
        this.logoutAllSessions = logoutAllSessions;
    }

    // Getters and Setters
    public boolean isLogoutAllSessions() { return logoutAllSessions; }
    public void setLogoutAllSessions(boolean logoutAllSessions) { this.logoutAllSessions = logoutAllSessions; }
}