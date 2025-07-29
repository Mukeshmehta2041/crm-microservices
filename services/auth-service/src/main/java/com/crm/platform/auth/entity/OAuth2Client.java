package com.crm.platform.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "oauth2_clients", indexes = {
    @Index(name = "idx_oauth2_clients_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_oauth2_clients_status", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
public class OAuth2Client {

    @Id
    @Column(name = "client_id", length = 255)
    private String clientId;

    @NotBlank
    @Size(min = 32, max = 255)
    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_redirect_uris", 
                    joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "redirect_uri", length = 500)
    private Set<String> redirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_scopes", 
                    joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope", length = 100)
    private Set<String> scopes;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "oauth2_client_grant_types", 
                    joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant_type", length = 50)
    private Set<GrantType> grantTypes;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "access_token_validity_seconds")
    private Integer accessTokenValiditySeconds = 3600; // 1 hour

    @Column(name = "refresh_token_validity_seconds")
    private Integer refreshTokenValiditySeconds = 2592000; // 30 days

    @Column(name = "auto_approve")
    private Boolean autoApprove = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public OAuth2Client() {}

    public OAuth2Client(String clientId, String clientSecret, String name, UUID tenantId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.name = name;
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getRedirectUris() { return redirectUris; }
    public void setRedirectUris(Set<String> redirectUris) { this.redirectUris = redirectUris; }

    public Set<String> getScopes() { return scopes; }
    public void setScopes(Set<String> scopes) { this.scopes = scopes; }

    public Set<GrantType> getGrantTypes() { return grantTypes; }
    public void setGrantTypes(Set<GrantType> grantTypes) { this.grantTypes = grantTypes; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getAccessTokenValiditySeconds() { return accessTokenValiditySeconds; }
    public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) { 
        this.accessTokenValiditySeconds = accessTokenValiditySeconds; 
    }

    public Integer getRefreshTokenValiditySeconds() { return refreshTokenValiditySeconds; }
    public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) { 
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds; 
    }

    public Boolean getAutoApprove() { return autoApprove; }
    public void setAutoApprove(Boolean autoApprove) { this.autoApprove = autoApprove; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean isValidRedirectUri(String redirectUri) {
        return redirectUris != null && redirectUris.contains(redirectUri);
    }

    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }

    public boolean supportsGrantType(GrantType grantType) {
        return grantTypes != null && grantTypes.contains(grantType);
    }

    public enum GrantType {
        AUTHORIZATION_CODE,
        CLIENT_CREDENTIALS,
        REFRESH_TOKEN,
        IMPLICIT,
        PASSWORD
    }
}