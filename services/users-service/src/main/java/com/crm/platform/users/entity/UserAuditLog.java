package com.crm.platform.users.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking user changes and audit trail
 */
@Entity
@Table(name = "user_audit_logs", indexes = {
    @Index(name = "idx_user_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_user_audit_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_user_audit_action", columnList = "action"),
    @Index(name = "idx_user_audit_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Column(name = "action", nullable = false, length = 100)
    private String action; // CREATE, UPDATE, DELETE, LOGIN, LOGOUT, etc.

    @Column(name = "entity_type", length = 50)
    private String entityType = "USER";

    @Column(name = "field_name", length = 100)
    private String fieldName; // Field that was changed (for UPDATE actions)

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "performed_by")
    private UUID performedBy; // User who performed the action

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private AuditSeverity severity = AuditSeverity.INFO;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData; // JSON for additional context

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public UserAuditLog() {}

    public UserAuditLog(UUID userId, UUID tenantId, String action, String description, UUID performedBy) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.action = action;
        this.description = description;
        this.performedBy = performedBy;
    }

    public UserAuditLog(UUID userId, UUID tenantId, String action, String fieldName, 
                       String oldValue, String newValue, UUID performedBy) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.action = action;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.performedBy = performedBy;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public UUID getPerformedBy() { return performedBy; }
    public void setPerformedBy(UUID performedBy) { this.performedBy = performedBy; }

    public AuditSeverity getSeverity() { return severity; }
    public void setSeverity(AuditSeverity severity) { this.severity = severity; }

    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum AuditSeverity {
        LOW, INFO, MEDIUM, HIGH, CRITICAL
    }

    // Common audit actions
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String ACTION_STATUS_CHANGE = "STATUS_CHANGE";
    public static final String ACTION_ROLE_CHANGE = "ROLE_CHANGE";
    public static final String ACTION_PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String ACTION_PRIVACY_CHANGE = "PRIVACY_CHANGE";
    public static final String ACTION_CONSENT_CHANGE = "CONSENT_CHANGE";
    public static final String ACTION_DATA_EXPORT = "DATA_EXPORT";
    public static final String ACTION_DATA_DELETION = "DATA_DELETION";
}