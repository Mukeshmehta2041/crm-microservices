package com.crm.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RevokeTokenRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    @JsonProperty("token_type_hint")
    private String tokenTypeHint;

    public RevokeTokenRequest() {}

    public RevokeTokenRequest(String token, String tokenTypeHint) {
        this.token = token;
        this.tokenTypeHint = tokenTypeHint;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenTypeHint() { return tokenTypeHint; }
    public void setTokenTypeHint(String tokenTypeHint) { this.tokenTypeHint = tokenTypeHint; }
}