package com.crm.platform.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_sessions_token_id", columnList = "token_id", unique = true),
    @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_user_sessions_expires_at", columnList = "expires_at"),
    @Index(name = "idx_user_sessions_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank
    @Column(name = "token_id", unique = true, nullable = false)
    private String tokenId;

    @NotBlank
    @Column(name = "refresh_token", nullable = false, length = 500)
    private String refreshToken;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @NotNull
    @Column(name = "refresh_expires_at", nullable = false)
    private LocalDateTime refreshExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public UserSession() {}

    public UserSession(UUID userId, String tokenId, String refreshToken, 
                      LocalDateTime expiresAt, LocalDateTime refreshExpiresAt) {
        this.userId = userId;
        this.tokenId = tokenId;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.lastAccessedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRefreshExpiresAt() { return refreshExpiresAt; }
    public void setRefreshExpiresAt(LocalDateTime refreshExpiresAt) { this.refreshExpiresAt = refreshExpiresAt; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRefreshExpired() {
        return LocalDateTime.now().isAfter(refreshExpiresAt);
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE && !isExpired();
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public enum SessionStatus {
        ACTIVE, EXPIRED, REVOKED, LOGGED_OUT
    }
}