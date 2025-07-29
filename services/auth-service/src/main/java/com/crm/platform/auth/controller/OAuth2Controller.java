package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.OAuth2AuthorizationRequest;
import com.crm.platform.auth.dto.OAuth2CallbackRequest;
import com.crm.platform.auth.dto.LoginResponse;
import com.crm.platform.auth.dto.OAuth2TokenResponse;
import com.crm.platform.auth.dto.UserInfo;
import com.crm.platform.auth.service.OAuth2Service;
import java.util.UUID;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.logging.SecurityLog;
import com.crm.platform.common.monitoring.Monitored;
import com.crm.platform.common.monitoring.Timed;
import com.crm.platform.common.tracing.Traced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * OAuth2 authentication controller for third-party integrations
 */
@RestController
@RequestMapping("/api/v1/auth/oauth2")
public class OAuth2Controller {

    @Autowired
    private OAuth2Service oauth2Service;

    @PostMapping("/authorize/{provider}")
    @Timed(value = "oauth2.authorize", description = "OAuth2 authorization request")
    @Monitored("oauth2-authorize")
    @Traced(operationName = "oauth2-authorize", domain = "authentication")
    @SecurityLog(operation = "oauth2-authorize", type = SecurityLog.SecurityType.AUTHENTICATION, 
                riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<Map<String, String>>> authorize(
            @PathVariable String provider,
            @Valid @RequestBody OAuth2AuthorizationRequest request) {
        
        String authorizationUrl = oauth2Service.getAuthorizationUrl(provider, request);
        
        Map<String, String> data = Map.of(
            "authorization_url", authorizationUrl,
            "state", request.getState()
        );
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/callback/{provider}")
    @Timed(value = "oauth2.callback", description = "OAuth2 callback processing")
    @Monitored("oauth2-callback")
    @Traced(operationName = "oauth2-callback", domain = "authentication")
    @SecurityLog(operation = "oauth2-callback", type = SecurityLog.SecurityType.AUTHENTICATION, 
                riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<LoginResponse>> callback(
            @PathVariable String provider,
            @Valid @RequestBody OAuth2CallbackRequest request) {
        
        OAuth2TokenResponse oauthResponse = oauth2Service.processCallback(provider, request);
        
        // Convert OAuth2TokenResponse to LoginResponse
        LoginResponse response = new LoginResponse(
            oauthResponse.getAccessToken(),
            oauthResponse.getRefreshToken(),
            oauthResponse.getExpiresIn(),
            oauthResponse.getExpiresIn() * 7, // refresh token validity
            new UserInfo(UUID.randomUUID(), "oauth@user.com", "OAuth", "User", 
                        null, null, null, null, null, UUID.randomUUID())
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/providers")
    @Monitored("oauth2-providers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailableProviders() {
        Map<String, Object> providers = oauth2Service.getAvailableProviders();
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @PostMapping("/link/{provider}")
    @Timed(value = "oauth2.link", description = "Link OAuth2 account to existing user")
    @Monitored("oauth2-link")
    @SecurityLog(operation = "oauth2-link", type = SecurityLog.SecurityType.AUTHENTICATION, 
                riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<Map<String, String>>> linkAccount(
            @PathVariable String provider,
            @Valid @RequestBody OAuth2CallbackRequest request) {
        
        oauth2Service.linkAccount(provider, request);
        
        Map<String, String> data = Map.of(
            "status", "success",
            "message", "Account linked successfully"
        );
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/unlink/{provider}")
    @Monitored("oauth2-unlink")
    @SecurityLog(operation = "oauth2-unlink", type = SecurityLog.SecurityType.AUTHENTICATION, 
                riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<Map<String, String>>> unlinkAccount(@PathVariable String provider) {
        oauth2Service.unlinkAccount(provider);
        
        Map<String, String> data = Map.of(
            "status", "success",
            "message", "Account unlinked successfully"
        );
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}