package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class OAuth2AuthorizationRequest {
    
    @NotBlank(message = "Response type is required")
    private String responseType;
    
    @NotBlank(message = "Client ID is required")
    private String clientId;
    
    @NotBlank(message = "Redirect URI is required")
    private String redirectUri;
    
    private String scope;
    private String state;
    
    // PKCE fields
    private String codeChallenge;
    private String codeChallengeMethod;

    public OAuth2AuthorizationRequest() {}

    public OAuth2AuthorizationRequest(String responseType, String clientId, String redirectUri, String scope, String state) {
        this.responseType = responseType;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.state = state;
    }

    // Getters and Setters
    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

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
}