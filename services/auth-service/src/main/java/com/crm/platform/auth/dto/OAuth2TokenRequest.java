package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAuth2TokenRequest {
    
    @NotBlank(message = "Grant type is required")
    @JsonProperty("grant_type")
    private String grantType;
    
    private String code;
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("client_secret")
    private String clientSecret;
    
    @JsonProperty("redirect_uri")
    private String redirectUri;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    private String scope;
    
    @JsonProperty("code_verifier")
    private String codeVerifier;

    public OAuth2TokenRequest() {}

    // Getters and Setters
    public String getGrantType() { return grantType; }
    public void setGrantType(String grantType) { this.grantType = grantType; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getCodeVerifier() { return codeVerifier; }
    public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }
}