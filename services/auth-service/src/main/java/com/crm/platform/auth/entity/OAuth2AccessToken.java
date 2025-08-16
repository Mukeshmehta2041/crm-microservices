package com.crm.platform.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth2_access_tokens", indexes = {
    @Index(name = "idx_oauth2_access_tokens_token", columnList = "access_token", unique = true),
    @Index(name = "idx_oauth2_access_tokens_refresh_token", columnList = "refresh_token", unique = true),
    @Index(name = "idx_oauth2_access_tokens_client_id", columnList = "client_id"),
    @Index(name = "idx_oauth2_access_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_oauth2_access_tokens_expires_at", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
public class OAuth2AccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "access_token", unique = true, nullable = false, length = 500)
    private String accessToken;

    @Column(name = "refresh_token", unique = true, length = 500)
    private String refreshToken;

    @NotBlank
    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "user_id")
    private UUID userId; // Nullable for client credentials flow

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "scope", length = 1000)
    private String scope;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "refresh_expires_at")
    private LocalDateTime refreshExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", length = 20)
    private TokenType tokenType = TokenType.BEARER;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type", length = 50)
    private OAuth2Client.GrantType grantType;

    @Column(name = "revoked")
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public OAuth2AccessToken() {}

    public OAuth2AccessToken(String accessToken, String refreshToken, String clientId, 
                           UUID userId, UUID tenantId, String scope, 
                           LocalDateTime expiresAt, LocalDateTime refreshExpiresAt,
                           OAuth2Client.GrantType grantType) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.clientId = clientId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.scope = scope;
        this.expiresAt = expiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.grantType = grantType;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRefreshExpiresAt() { return refreshExpiresAt; }
    public void setRefreshExpiresAt(LocalDateTime refreshExpiresAt) { this.refreshExpiresAt = refreshExpiresAt; }

    public TokenType getTokenType() { return tokenType; }
    public void setTokenType(TokenType tokenType) { this.tokenType = tokenType; }

    public OAuth2Client.GrantType getGrantType() { return grantType; }
    public void setGrantType(OAuth2Client.GrantType grantType) { this.grantType = grantType; }

    public Boolean getRevoked() { return revoked; }
    public void setRevoked(Boolean revoked) { this.revoked = revoked; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRefreshExpired() {
        return refreshExpiresAt != null && LocalDateTime.now().isAfter(refreshExpiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public boolean canRefresh() {
        return refreshToken != null && !revoked && !isRefreshExpired();
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
    }

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public enum TokenType {
        BEARER, MAC
    }
}