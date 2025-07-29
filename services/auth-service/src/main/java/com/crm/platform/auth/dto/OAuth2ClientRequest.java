package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class OAuth2ClientRequest {

    @NotBlank(message = "Client name is required")
    @Size(max = 255, message = "Client name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotEmpty(message = "At least one redirect URI is required")
    @JsonProperty("redirect_uris")
    private Set<String> redirectUris;

    @NotEmpty(message = "At least one scope is required")
    private Set<String> scopes;

    @NotEmpty(message = "At least one grant type is required")
    @JsonProperty("grant_types")
    private Set<String> grantTypes;

    @JsonProperty("access_token_validity_seconds")
    private Integer accessTokenValiditySeconds;

    @JsonProperty("refresh_token_validity_seconds")
    private Integer refreshTokenValiditySeconds;

    @JsonProperty("auto_approve")
    private Boolean autoApprove;

    @JsonProperty("additional_information")
    private String additionalInformation;

    // Constructors
    public OAuth2ClientRequest() {}

    public OAuth2ClientRequest(String name, Set<String> redirectUris, Set<String> scopes, Set<String> grantTypes) {
        this.name = name;
        this.redirectUris = redirectUris;
        this.scopes = scopes;
        this.grantTypes = grantTypes;
    }

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

    public String getAdditionalInformation() { return additionalInformation; }
    public void setAdditionalInformation(String additionalInformation) { 
        this.additionalInformation = additionalInformation; 
    }
}