package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuth2ClientResponse {

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    private String name;
    private String description;

    @JsonProperty("redirect_uris")
    private Set<String> redirectUris;

    private Set<String> scopes;

    @JsonProperty("grant_types")
    private Set<String> grantTypes;

    @JsonProperty("tenant_id")
    private UUID tenantId;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("access_token_validity_seconds")
    private Integer accessTokenValiditySeconds;

    @JsonProperty("refresh_token_validity_seconds")
    private Integer refreshTokenValiditySeconds;

    @JsonProperty("auto_approve")
    private Boolean autoApprove;

    @JsonProperty("additional_information")
    private String additionalInformation;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Statistics (optional)
    @JsonProperty("token_count")
    private Long tokenCount;

    @JsonProperty("last_used_at")
    private LocalDateTime lastUsedAt;

    // Constructors
    public OAuth2ClientResponse() {}

    public OAuth2ClientResponse(String clientId, String name, Set<String> redirectUris, 
                               Set<String> scopes, Set<String> grantTypes, UUID tenantId) {
        this.clientId = clientId;
        this.name = name;
        this.redirectUris = redirectUris;
        this.scopes = scopes;
        this.grantTypes = grantTypes;
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

    public Set<String> getGrantTypes() { return grantTypes; }
    public void setGrantTypes(Set<String> grantTypes) { this.grantTypes = grantTypes; }

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

    public String getAdditionalInformation() { return additionalInformation; }
    public void setAdditionalInformation(String additionalInformation) { 
        this.additionalInformation = additionalInformation; 
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getTokenCount() { return tokenCount; }
    public void setTokenCount(Long tokenCount) { this.tokenCount = tokenCount; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}