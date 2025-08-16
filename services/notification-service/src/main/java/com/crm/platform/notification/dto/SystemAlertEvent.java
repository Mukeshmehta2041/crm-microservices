package com.crm.platform.notification.dto;

import java.util.UUID;

public class SystemAlertEvent extends BaseEvent {
    private String alertType; // ERROR, WARNING, INFO
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String message;
    private String component;

    public SystemAlertEvent() {
        super();
    }

    public SystemAlertEvent(UUID tenantId, String alertType, String severity, String message, String component) {
        super("systemAlert", tenantId, null, component);
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.component = component;
    }

    // Getters and Setters
    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }
}