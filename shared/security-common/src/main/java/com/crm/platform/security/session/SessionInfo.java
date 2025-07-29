package com.crm.platform.security.session;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Session information stored in Redis
 */
public class SessionInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private UUID userId;
    private UUID tenantId;
    private String sessionId;
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private SessionStatus status;
    private String refreshToken;
    
    public enum SessionStatus {
        ACTIVE, EXPIRED, LOGGED_OUT, REVOKED
    }
    
    public SessionInfo() {
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.status = SessionStatus.ACTIVE;
    }
    
    public SessionInfo(UUID userId, UUID tenantId, String sessionId, String deviceId,
                      String ipAddress, String userAgent, String refreshToken) {
        this();
        this.userId = userId;
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.refreshToken = refreshToken;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public SessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SessionStatus status) {
        this.status = status;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }
    
    public void invalidate() {
        this.status = SessionStatus.LOGGED_OUT;
    }
    
    public void revoke() {
        this.status = SessionStatus.REVOKED;
    }
    
    @Override
    public String toString() {
        return "SessionInfo{" +
                "userId=" + userId +
                ", tenantId=" + tenantId +
                ", sessionId='" + sessionId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                ", lastAccessedAt=" + lastAccessedAt +
                ", status=" + status +
                '}';
    }
}