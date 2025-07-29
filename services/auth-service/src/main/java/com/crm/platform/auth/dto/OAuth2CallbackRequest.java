package com.crm.platform.auth.dto;

import javax.validation.constraints.NotBlank;

/**
 * OAuth2 callback request DTO
 */
public class OAuth2CallbackRequest {
    
    @NotBlank(message = "Authorization code is required")
    private String code;
    
    @NotBlank(message = "State is required")
    private String state;
    
    private String codeVerifier;
    private String redirectUri;
    
    public OAuth2CallbackRequest() {}
    
    public OAuth2CallbackRequest(String code, String state) {
        this.code = code;
        this.state = state;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getCodeVerifier() {
        return codeVerifier;
    }
    
    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}