package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public class SuspiciousActivityAlert {

    private AlertType type;
    private String message;

    @JsonProperty("risk_level")
    private RiskLevel riskLevel;

    private Map<String, Object> details;
    private LocalDateTime timestamp;

    @JsonProperty("action_required")
    private boolean actionRequired;

    @JsonProperty("recommended_actions")
    private String[] recommendedActions;

    // Constructors
    public SuspiciousActivityAlert() {
        this.timestamp = LocalDateTime.now();
    }

    public SuspiciousActivityAlert(AlertType type, String message, RiskLevel riskLevel, 
                                 Map<String, Object> details) {
        this.type = type;
        this.message = message;
        this.riskLevel = riskLevel;
        this.details = details;
        this.timestamp = LocalDateTime.now();
        this.actionRequired = determineActionRequired(riskLevel);
        this.recommendedActions = getRecommendedActions(type, riskLevel);
    }

    // Getters and Setters
    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { 
        this.riskLevel = riskLevel;
        this.actionRequired = determineActionRequired(riskLevel);
    }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isActionRequired() { return actionRequired; }
    public void setActionRequired(boolean actionRequired) { this.actionRequired = actionRequired; }

    public String[] getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(String[] recommendedActions) { this.recommendedActions = recommendedActions; }

    // Helper methods
    private boolean determineActionRequired(RiskLevel riskLevel) {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }

    private String[] getRecommendedActions(AlertType type, RiskLevel riskLevel) {
        switch (type) {
            case MULTIPLE_LOCATIONS:
                return new String[]{
                    "Verify recent login locations",
                    "Change password if unauthorized access suspected",
                    "Enable MFA if not already active",
                    "Review active sessions"
                };
            case MULTIPLE_DEVICES:
                return new String[]{
                    "Review active devices and sessions",
                    "Remove unrecognized devices",
                    "Update security settings"
                };
            case UNUSUAL_LOCATION:
                return new String[]{
                    "Confirm if you recently traveled",
                    "Verify the login was authorized",
                    "Consider enabling location-based alerts"
                };
            case NEW_DEVICE:
                return new String[]{
                    "Verify this is your device",
                    "Add device to trusted devices if legitimate",
                    "Remove device access if unauthorized"
                };
            case RAPID_LOGIN_ATTEMPTS:
                return new String[]{
                    "Change password immediately",
                    "Enable MFA",
                    "Check for compromised credentials"
                };
            case IMPOSSIBLE_TRAVEL:
                return new String[]{
                    "Verify recent travel",
                    "Change password immediately",
                    "Review all active sessions",
                    "Contact security team"
                };
            default:
                return new String[]{
                    "Review account activity",
                    "Update security settings",
                    "Contact support if needed"
                };
        }
    }

    // Enums
    public enum AlertType {
        MULTIPLE_LOCATIONS,
        MULTIPLE_DEVICES,
        UNUSUAL_LOCATION,
        NEW_DEVICE,
        RAPID_LOGIN_ATTEMPTS,
        IMPOSSIBLE_TRAVEL,
        SUSPICIOUS_USER_AGENT,
        BRUTE_FORCE_ATTEMPT,
        ACCOUNT_TAKEOVER_INDICATORS
    }

    public enum RiskLevel {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);

        private final int severity;

        RiskLevel(int severity) {
            this.severity = severity;
        }

        public int getSeverity() {
            return severity;
        }

        public boolean isHigherThan(RiskLevel other) {
            return this.severity > other.severity;
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", riskLevel, type, message);
    }
}