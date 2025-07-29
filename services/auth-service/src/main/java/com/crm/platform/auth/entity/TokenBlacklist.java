package com.crm.platform.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_token_blacklist_jti", columnList = "jti", unique = true),
    @Index(name = "idx_token_blacklist_expires_at", columnList = "expires_at"),
    @Index(name = "idx_token_blacklist_user_id", columnList = "user_id"),
    @Index(name = "idx_token_blacklist_tenant_id", columnList = "tenant_id")
})
@EntityListeners(AuditingEntityListener.class)
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "jti", unique = true, nullable = false, length = 255)
    private String jti; // JWT ID

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", length = 20)
    private TokenType tokenType;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "reason", length = 100)
    private String reason;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public TokenBlacklist() {}

    public TokenBlacklist(String jti, UUID userId, UUID tenantId, TokenType tokenType, 
                         LocalDateTime expiresAt, String reason, UUID revokedBy) {
        this.jti = jti;
        this.userId = userId;
        this.tenantId = tenantId;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.revokedBy = revokedBy;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public TokenType getTokenType() { return tokenType; }
    public void setTokenType(TokenType tokenType) { this.tokenType = tokenType; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UUID getRevokedBy() { return revokedBy; }
    public void setRevokedBy(UUID revokedBy) { this.revokedBy = revokedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public enum TokenType {
        ACCESS, REFRESH, OAUTH2_ACCESS, OAUTH2_REFRESH
    }
}