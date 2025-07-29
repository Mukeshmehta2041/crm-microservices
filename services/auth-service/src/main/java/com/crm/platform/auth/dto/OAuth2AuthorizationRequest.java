package com.crm.platform.auth.dto;

import javax.validation.constraints.NotBlank;

/**
 * OAuth2 authorization request DTO
 */
public class OAuth2AuthorizationRequest {
    
    @NotBlank(message = "State is required")
    private String state;
    
    private String redirectUri;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String scope;
    
    public OAuth2AuthorizationRequest() {}
    
    public OAuth2AuthorizationRequest(String state, String redirectUri) {
        this.state = state;
        this.redirectUri = redirectUri;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    
    public String getCodeChallenge() {
        return codeChallenge;
    }
    
    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }
    
    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }
    
    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
}