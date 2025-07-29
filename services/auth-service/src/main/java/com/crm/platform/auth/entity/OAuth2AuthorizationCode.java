package com.crm.platform.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth2_authorization_codes", indexes = {
    @Index(name = "idx_oauth2_auth_codes_code", columnList = "code", unique = true),
    @Index(name = "idx_oauth2_auth_codes_client_id", columnList = "client_id"),
    @Index(name = "idx_oauth2_auth_codes_user_id", columnList = "user_id"),
    @Index(name = "idx_oauth2_auth_codes_expires_at", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
public class OAuth2AuthorizationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 255)
    private String code;

    @NotBlank
    @Column(name = "client_id", nullable = false)
    private String clientId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Column(name = "redirect_uri", nullable = false, length = 500)
    private String redirectUri;

    @Column(name = "scope", length = 1000)
    private String scope;

    @Column(name = "state", length = 255)
    private String state;

    @Column(name = "code_challenge", length = 255)
    private String codeChallenge;

    @Column(name = "code_challenge_method", length = 10)
    private String codeChallengeMethod;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used")
    private Boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public OAuth2AuthorizationCode() {}

    public OAuth2AuthorizationCode(String code, String clientId, UUID userId, UUID tenantId, 
                                 String redirectUri, String scope, String state, 
                                 LocalDateTime expiresAt) {
        this.code = code;
        this.clientId = clientId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.state = state;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCodeChallenge() { return codeChallenge; }
    public void setCodeChallenge(String codeChallenge) { this.codeChallenge = codeChallenge; }

    public String getCodeChallengeMethod() { return codeChallengeMethod; }
    public void setCodeChallengeMethod(String codeChallengeMethod) { this.codeChallengeMethod = codeChallengeMethod; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}