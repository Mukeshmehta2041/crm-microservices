package com.programming.auth.entity;

import com.programming.common.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "user_sessions", 
       uniqueConstraints = {
           @UniqueConstraint(name = "user_sessions_session_id_unique", columnNames = {"sessionId"})
       },
       indexes = {
           @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
           @Index(name = "idx_user_sessions_session_id", columnList = "sessionId"),
           @Index(name = "idx_user_sessions_expires_at", columnList = "expiresAt"),
           @Index(name = "idx_user_sessions_last_activity_at", columnList = "lastActivityAt"),
           @Index(name = "idx_user_sessions_tenant_active", columnList = "tenant_id, isActive")
       })
public class UserSession extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @NotNull
    @Column(name = "ip_address", nullable = false)
    private InetAddress ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_info", columnDefinition = "jsonb")
    private Map<String, Object> deviceInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "location_info", columnDefinition = "jsonb")
    private Map<String, Object> locationInfo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false, insertable = false)
    private User createdByUser;

    // Constructors
    public UserSession() {
        super();
        this.lastActivityAt = Instant.now();
    }

    public UserSession(String sessionId, User user, InetAddress ipAddress, Instant expiresAt) {
        super(user.getTenantId());
        this.sessionId = sessionId;
        this.user = user;
        this.tenant = user.getTenant();
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
        this.lastActivityAt = Instant.now();
    }

    // Business methods
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void updateLastActivity() {
        this.lastActivityAt = Instant.now();
    }

    public void invalidate() {
        this.isActive = false;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, Object> getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(Map<String, Object> deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public Map<String, Object> getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(Map<String, Object> locationInfo) {
        this.locationInfo = locationInfo;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.tenant = user.getTenant();
            setTenantId(user.getTenantId());
        }
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }
}