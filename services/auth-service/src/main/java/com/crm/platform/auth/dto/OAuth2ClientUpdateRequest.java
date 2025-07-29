package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class OAuth2ClientUpdateRequest {

    @Size(max = 255, message = "Client name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @JsonProperty("redirect_uris")
    private Set<String> redirectUris;

    private Set<String> scopes;

    @JsonProperty("grant_types")
    private Set<String> grantTypes;

    @JsonProperty("access_token_validity_seconds")
    private Integer accessTokenValiditySeconds;

    @JsonProperty("refresh_token_validity_seconds")
    private Integer refreshTokenValiditySeconds;

    @JsonProperty("auto_approve")
    private Boolean autoApprove;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("additional_information")
    private String additionalInformation;

    // Constructors
    public OAuth2ClientUpdateRequest() {}

    // Getters and Setters
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

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getAdditionalInformation() { return additionalInformation; }
    public void setAdditionalInformation(String additionalInformation) { 
        this.additionalInformation = additionalInformation; 
    }
}