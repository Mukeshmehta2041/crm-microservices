package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("refresh_expires_in")
    private long refreshExpiresIn;

    private UserInfo user;

    // MFA-related fields
    @JsonProperty("mfa_required")
    private boolean mfaRequired = false;

    @JsonProperty("mfa_token")
    private String mfaToken;

    @JsonProperty("mfa_method")
    private String mfaMethod;

    @JsonProperty("mfa_verified")
    private boolean mfaVerified = false;

    private String message;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(String accessToken, String refreshToken, long expiresIn, 
                        long refreshExpiresIn, UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.user = user;
    }

    // Getters and Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public long getRefreshExpiresIn() { return refreshExpiresIn; }
    public void setRefreshExpiresIn(long refreshExpiresIn) { this.refreshExpiresIn = refreshExpiresIn; }

    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    public boolean isMfaRequired() { return mfaRequired; }
    public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }

    public String getMfaToken() { return mfaToken; }
    public void setMfaToken(String mfaToken) { this.mfaToken = mfaToken; }

    public String getMfaMethod() { return mfaMethod; }
    public void setMfaMethod(String mfaMethod) { this.mfaMethod = mfaMethod; }

    public boolean isMfaVerified() { return mfaVerified; }
    public void setMfaVerified(boolean mfaVerified) { this.mfaVerified = mfaVerified; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}