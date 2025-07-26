package com.crm.platform.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_audit_log", indexes = {
    @Index(name = "idx_audit_log_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_log_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_audit_log_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_log_ip_address", columnList = "ip_address")
})
@EntityListeners(AuditingEntityListener.class)
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @NotBlank
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @NotBlank
    @Column(name = "event_description", nullable = false, length = 500)
    private String eventDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditEventStatus status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;

    @NotNull
    @CreatedDate
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Constructors
    public SecurityAuditLog() {}

    public SecurityAuditLog(UUID userId, UUID tenantId, String eventType, 
                           String eventDescription, AuditEventStatus status) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.eventDescription = eventDescription;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }

    public AuditEventStatus getStatus() { return status; }
    public void setStatus(AuditEventStatus status) { this.status = status; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public enum AuditEventStatus {
        SUCCESS, FAILURE, WARNING
    }

    // Common event types as constants
    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String EVENT_LOGOUT = "LOGOUT";
    public static final String EVENT_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String EVENT_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String EVENT_ACCOUNT_UNLOCKED = "ACCOUNT_UNLOCKED";
    public static final String EVENT_TOKEN_REFRESH = "TOKEN_REFRESH";
    public static final String EVENT_SESSION_EXPIRED = "SESSION_EXPIRED";
    public static final String EVENT_BRUTE_FORCE_ATTEMPT = "BRUTE_FORCE_ATTEMPT";
}