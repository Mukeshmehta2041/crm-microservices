package com.crm.platform.notification.dto;

import java.util.UUID;

public class AuthEvent extends BaseEvent {
    private String action; // LOGIN, LOGOUT, PASSWORD_RESET, ACCOUNT_LOCKED, FAILED_LOGIN
    private String ipAddress;
    private String userAgent;
    private String location;

    public AuthEvent() {
        super();
    }

    public AuthEvent(UUID tenantId, UUID userId, String action, String ipAddress) {
        super("authEvent", tenantId, userId, "auth-service");
        this.action = action;
        this.ipAddress = ipAddress;
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}