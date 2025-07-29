package com.crm.platform.security.oauth2;

/**
 * Enumeration of supported OAuth2 providers
 */
public enum OAuth2Provider {
    GOOGLE("google", "Google", "https://accounts.google.com/o/oauth2/auth", 
           "https://oauth2.googleapis.com/token", "https://www.googleapis.com/oauth2/v2/userinfo"),
    
    MICROSOFT("microsoft", "Microsoft", "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
              "https://login.microsoftonline.com/common/oauth2/v2.0/token", "https://graph.microsoft.com/v1.0/me"),
    
    GITHUB("github", "GitHub", "https://github.com/login/oauth/authorize",
           "https://github.com/login/oauth/access_token", "https://api.github.com/user"),
    
    LINKEDIN("linkedin", "LinkedIn", "https://www.linkedin.com/oauth/v2/authorization",
             "https://www.linkedin.com/oauth/v2/accessToken", "https://api.linkedin.com/v2/people/~"),
    
    SALESFORCE("salesforce", "Salesforce", "https://login.salesforce.com/services/oauth2/authorize",
               "https://login.salesforce.com/services/oauth2/token", "https://login.salesforce.com/services/oauth2/userinfo");
    
    private final String code;
    private final String displayName;
    private final String authorizationUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    
    OAuth2Provider(String code, String displayName, String authorizationUrl, 
                   String tokenUrl, String userInfoUrl) {
        this.code = code;
        this.displayName = displayName;
        this.authorizationUrl = authorizationUrl;
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }
    
    public String getTokenUrl() {
        return tokenUrl;
    }
    
    public String getUserInfoUrl() {
        return userInfoUrl;
    }
    
    public static OAuth2Provider fromCode(String code) {
        for (OAuth2Provider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown OAuth2 provider: " + code);
    }
}