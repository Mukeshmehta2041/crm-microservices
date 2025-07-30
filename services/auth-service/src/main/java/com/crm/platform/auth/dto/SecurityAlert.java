package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for security alerts
 */
public class SecurityAlert {

    @JsonProperty("alert_id")
    private String alertId;

    @JsonProperty("alert_type")
    private AlertType alertType;

    @JsonProperty("severity")
    private Severity severity;

    @JsonProperty("status")
    private AlertStatus status;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("source")
    private String source;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("tenant_id")
    private UUID tenantId;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("resolved_at")
    private LocalDateTime resolvedAt;

    @JsonProperty("resolved_by")
    private UUID resolvedBy;

    @JsonProperty("resolution_notes")
    private String resolutionNotes;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("related_events_count")
    private int relatedEventsCount;

    @JsonProperty("risk_score")
    private double riskScore;

    @JsonProperty("auto_resolved")
    private boolean autoResolved;

    // Constructors
    public SecurityAlert() {
        this.alertId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.status = AlertStatus.OPEN;
    }

    public SecurityAlert(AlertType alertType, Severity severity, String title, String description) {
        this();
        this.alertType = alertType;
        this.severity = severity;
        this.title = title;
        this.description = description;
    }

    // Enums
    public enum AlertType {
        BRUTE_FORCE_ATTACK,
        SUSPICIOUS_LOGIN,
        ACCOUNT_LOCKOUT,
        MULTIPLE_FAILED_LOGINS,
        UNUSUAL_LOCATION,
        NEW_DEVICE_LOGIN,
        PRIVILEGE_ESCALATION,
        DATA_BREACH_ATTEMPT,
        RATE_LIMIT_EXCEEDED,
        IP_BLOCKED,
        CAPTCHA_FAILED,
        MFA_BYPASS_ATTEMPT,
        SESSION_HIJACKING,
        CREDENTIAL_STUFFING,
        DDOS_ATTACK,
        MALICIOUS_USER_AGENT,
        SECURITY_POLICY_VIOLATION,
        COMPLIANCE_VIOLATION,
        SYSTEM_ANOMALY,
        CONFIGURATION_CHANGE
    }

    public enum Severity {
        LOW(1),
        MEDIUM(2), 
        HIGH(3),
        CRITICAL(4);

        private final int level;

        Severity(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    public enum AlertStatus {
        OPEN,
        INVESTIGATING,
        RESOLVED,
        FALSE_POSITIVE,
        SUPPRESSED
    }

    // Getters and Setters
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { 
        this.status = status; 
        this.updatedAt = LocalDateTime.now();
        if (status == AlertStatus.RESOLVED || status == AlertStatus.FALSE_POSITIVE) {
            this.resolvedAt = LocalDateTime.now();
        }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public int getRelatedEventsCount() { return relatedEventsCount; }
    public void setRelatedEventsCount(int relatedEventsCount) { this.relatedEventsCount = relatedEventsCount; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public boolean isAutoResolved() { return autoResolved; }
    public void setAutoResolved(boolean autoResolved) { this.autoResolved = autoResolved; }

    // Utility methods
    public boolean isOpen() {
        return status == AlertStatus.OPEN || status == AlertStatus.INVESTIGATING;
    }

    public boolean isResolved() {
        return status == AlertStatus.RESOLVED || status == AlertStatus.FALSE_POSITIVE;
    }

    public boolean isHighPriority() {
        return severity == Severity.HIGH || severity == Severity.CRITICAL;
    }

    public long getAgeInMinutes() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }

    public void resolve(UUID resolvedBy, String notes) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFalsePositive(UUID resolvedBy, String notes) {
        this.status = AlertStatus.FALSE_POSITIVE;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}