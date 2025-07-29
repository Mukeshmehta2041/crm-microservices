package com.crm.platform.security.oauth2;

/**
 * OAuth2 provider configuration
 */
public class OAuth2Configuration {
    
    private final OAuth2Provider provider;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;
    private final boolean enabled;
    
    public OAuth2Configuration(OAuth2Provider provider, String clientId, String clientSecret,
                              String redirectUri, String scope, boolean enabled) {
        this.provider = provider;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.enabled = enabled;
    }
    
    public OAuth2Provider getProvider() {
        return provider;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public String getScope() {
        return scope;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getAuthorizationUrl(String state, String codeChallenge) {
        StringBuilder url = new StringBuilder(provider.getAuthorizationUrl());
        url.append("?client_id=").append(clientId);
        url.append("&redirect_uri=").append(redirectUri);
        url.append("&scope=").append(scope);
        url.append("&response_type=code");
        url.append("&state=").append(state);
        
        if (codeChallenge != null) {
            url.append("&code_challenge=").append(codeChallenge);
            url.append("&code_challenge_method=S256");
        }
        
        return url.toString();
    }
}