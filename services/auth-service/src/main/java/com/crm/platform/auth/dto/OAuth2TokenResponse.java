package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OAuth2TokenResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType = "Bearer";
    
    @JsonProperty("expires_in")
    private long expiresIn;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    private String scope;
    
    @JsonProperty("tenant_id")
    private String tenantId;

    public OAuth2TokenResponse() {}

    public OAuth2TokenResponse(String accessToken, String refreshToken, long expiresIn, String scope, String tenantId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.scope = scope;
        this.tenantId = tenantId;
    }

    public OAuth2TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken, String scope) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    // Getters and Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}