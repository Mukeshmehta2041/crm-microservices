package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.*;
import com.crm.platform.auth.service.AuthenticationService;
import com.crm.platform.auth.service.OAuth2Service;
import com.crm.platform.auth.service.PasswordService;
import com.crm.platform.auth.service.SessionService;
import com.crm.platform.auth.service.MfaService;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.logging.BusinessLog;
import com.crm.platform.common.logging.SecurityLog;
import com.crm.platform.common.monitoring.Monitored;
import com.crm.platform.common.monitoring.Timed;
import com.crm.platform.common.tracing.Traced;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive Authentication controller implementing OAuth2, MFA, session management, and security features.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private OAuth2Service oauth2Service;
    
    @Autowired
    private PasswordService passwordService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private MfaService mfaService;

    // ==================== OAuth2 Authorization Flows ====================

    @GetMapping("/authorize")
    @Operation(summary = "OAuth2 Authorization Endpoint", description = "Initiates OAuth2 authorization code flow")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "Redirect to authorization page"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public void authorize(
            @Parameter(description = "Response type (must be 'code')") @RequestParam("response_type") String responseType,
            @Parameter(description = "Client ID") @RequestParam("client_id") String clientId,
            @Parameter(description = "Redirect URI") @RequestParam("redirect_uri") String redirectUri,
            @Parameter(description = "Requested scopes") @RequestParam("scope") String scope,
            @Parameter(description = "CSRF protection state") @RequestParam("state") String state,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        OAuth2AuthorizationRequest authRequest = new OAuth2AuthorizationRequest(
            responseType, clientId, redirectUri, scope, state);
        oauth2Service.authorize(authRequest, request, response);
    }

    @PostMapping("/token")
    @Operation(summary = "OAuth2 Token Endpoint", description = "Exchange authorization code or refresh token for access token")
    @Timed(value = "auth.oauth2.token", description = "OAuth2 token exchange")
    @Monitored("oauth2-token")
    @SecurityLog(operation = "oauth2-token", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<OAuth2TokenResponse>> token(
            @Valid @RequestBody OAuth2TokenRequest request,
            HttpServletRequest httpRequest) {
        
        OAuth2TokenResponse tokenResponse = oauth2Service.exchangeToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "Refresh OAuth2 Token", description = "Refresh an expired access token using refresh token")
    @Timed(value = "auth.token.refresh", description = "Token refresh operation")
    @Monitored("token-refresh")
    @SecurityLog(operation = "token-refresh", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<ApiResponse<OAuth2TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        OAuth2TokenResponse tokenResponse = oauth2Service.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @PostMapping("/token/revoke")
    @Operation(summary = "Revoke Token", description = "Revoke an access or refresh token")
    @SecurityLog(operation = "token-revoke", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<Map<String, Object>>> revokeToken(
            @Valid @RequestBody RevokeTokenRequest request,
            HttpServletRequest httpRequest) {
        
        oauth2Service.revokeToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "revoked", true,
            "revokedAt", java.time.Instant.now()
        )));
    }

    @GetMapping("/userinfo")
    @Operation(summary = "Get User Info", description = "Get user information from access token")
    @SecurityLog(operation = "userinfo", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<ApiResponse<UserInfo>> getUserInfo(
            @RequestHeader("Authorization") String authorization) {
        
        UserInfo userInfo = oauth2Service.getUserInfo(authorization);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    // ==================== User Authentication ====================

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate user with email/password")
    @Timed(value = "auth.login", description = "User login operation")
    @Monitored("user-login")
    @Traced(operationName = "user-login", domain = "authentication", includeParameters = false)
    @BusinessLog(operation = "user-login", domain = "auth", action = BusinessLog.BusinessAction.PROCESS)
    @SecurityLog(operation = "user-login", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        LoginResponse response = authenticationService.authenticate(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Logout user and invalidate session")
    @Monitored("user-logout")
    @Traced(operationName = "user-logout", domain = "authentication")
    @SecurityLog(operation = "user-logout", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(
            @RequestHeader("Authorization") String authorization,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = authenticationService.logout(authorization, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/register")
    @Operation(summary = "User Registration", description = "Register new user account")
    @Monitored("user-register")
    @SecurityLog(operation = "user-register", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        RegistrationResponse response = authenticationService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ==================== Password Management ====================

    @PostMapping("/password/reset")
    @Operation(summary = "Request Password Reset", description = "Send password reset email")
    @Monitored("password-reset-request")
    @SecurityLog(operation = "password-reset-request", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = passwordService.requestPasswordReset(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/password/confirm")
    @Operation(summary = "Confirm Password Reset", description = "Reset password with token")
    @Monitored("password-reset-confirm")
    @SecurityLog(operation = "password-reset-confirm", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = passwordService.confirmPasswordReset(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/password/change")
    @Operation(summary = "Change Password", description = "Change user password")
    @Monitored("password-change")
    @SecurityLog(operation = "password-change", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = passwordService.changePassword(authorization, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Email Verification ====================

    @PostMapping("/email/verify")
    @Operation(summary = "Verify Email", description = "Verify user email with token")
    @Monitored("email-verify")
    @SecurityLog(operation = "email-verify", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = authenticationService.verifyEmail(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/email/resend-verification")
    @Operation(summary = "Resend Verification Email", description = "Resend email verification")
    @Monitored("email-resend-verification")
    @SecurityLog(operation = "email-resend-verification", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = authenticationService.resendVerificationEmail(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Session Management ====================

    @GetMapping("/sessions")
    @Operation(summary = "List Active Sessions", description = "Get user's active sessions")
    @SecurityLog(operation = "list-sessions", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<ApiResponse<List<SessionInfo>>> listSessions(
            @RequestHeader("Authorization") String authorization) {
        
        List<SessionInfo> sessions = sessionService.listUserSessions(authorization);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Terminate Session", description = "Terminate specific session")
    @SecurityLog(operation = "terminate-session", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<Map<String, Object>>> terminateSession(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = sessionService.terminateSession(authorization, sessionId, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/sessions")
    @Operation(summary = "Terminate All Sessions", description = "Terminate all user sessions except current")
    @SecurityLog(operation = "terminate-all-sessions", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<Map<String, Object>>> terminateAllSessions(
            @RequestHeader("Authorization") String authorization,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = sessionService.terminateAllSessions(authorization, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/sessions/validate")
    @Operation(summary = "Validate Session", description = "Validate session token")
    @Timed(value = "auth.session.validate", description = "Session validation operation")
    @SecurityLog(operation = "session-validation", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSession(
            @Valid @RequestBody SessionValidationRequest request) {
        
        Map<String, Object> result = sessionService.validateSession(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/sessions/current")
    @Operation(summary = "Get Current Session", description = "Get current session information")
    @SecurityLog(operation = "get-current-session", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<ApiResponse<SessionInfo>> getCurrentSession(
            @RequestHeader("Authorization") String authorization) {
        
        SessionInfo session = sessionService.getCurrentSession(authorization);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    // ==================== Multi-Factor Authentication ====================

    @PostMapping("/mfa/setup")
    @Operation(summary = "Setup MFA", description = "Setup multi-factor authentication")
    @SecurityLog(operation = "mfa-setup", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody MfaSetupRequest request,
            HttpServletRequest httpRequest) {
        
        MfaSetupResponse response = mfaService.setupMfa(authorization, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/setup/verify")
    @Operation(summary = "Verify MFA Setup", description = "Verify MFA setup with code")
    @SecurityLog(operation = "mfa-setup-verify", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMfaSetup(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody MfaVerifySetupRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = mfaService.verifyMfaSetup(authorization, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Verify MFA Code", description = "Verify MFA code during login")
    @Monitored("mfa-verify")
    @SecurityLog(operation = "mfa-verify", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<LoginResponse>> verifyMfa(
            @Valid @RequestBody MfaVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        LoginResponse response = mfaService.verifyMfa(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/disable")
    @Operation(summary = "Disable MFA", description = "Disable multi-factor authentication")
    @SecurityLog(operation = "mfa-disable", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<Map<String, Object>>> disableMfa(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody MfaDisableRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> result = mfaService.disableMfa(authorization, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/mfa/backup-codes")
    @Operation(summary = "Get MFA Backup Codes", description = "Get MFA backup codes")
    @SecurityLog(operation = "mfa-backup-codes", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<ApiResponse<MfaBackupCodesResponse>> getMfaBackupCodes(
            @RequestHeader("Authorization") String authorization) {
        
        MfaBackupCodesResponse response = mfaService.getBackupCodes(authorization);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/backup-codes/regenerate")
    @Operation(summary = "Regenerate MFA Backup Codes", description = "Generate new MFA backup codes")
    @SecurityLog(operation = "mfa-backup-codes-regenerate", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    public ResponseEntity<ApiResponse<MfaBackupCodesResponse>> regenerateMfaBackupCodes(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody MfaBackupCodesRegenerateRequest request,
            HttpServletRequest httpRequest) {
        
        MfaBackupCodesResponse response = mfaService.regenerateBackupCodes(authorization, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check authentication service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "auth-service",
            "timestamp", java.time.Instant.now()
        )));
    }
}