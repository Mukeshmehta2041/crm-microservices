package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.LoginRequest;
import com.crm.platform.auth.dto.LoginResponse;
import com.crm.platform.auth.service.AuthService;
import com.crm.platform.common.logging.BusinessLog;
import com.crm.platform.common.logging.SecurityLog;
import com.crm.platform.common.monitoring.Monitored;
import com.crm.platform.common.monitoring.Timed;
import com.crm.platform.common.tracing.Traced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Authentication controller with comprehensive monitoring and logging.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Timed(value = "auth.login", description = "User login operation")
    @Monitored("user-login")
    @Traced(operationName = "user-login", domain = "authentication", includeParameters = false)
    @BusinessLog(operation = "user-login", domain = "auth", action = BusinessLog.BusinessAction.PROCESS)
    @SecurityLog(operation = "user-login", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Monitored("user-logout")
    @Traced(operationName = "user-logout", domain = "authentication")
    @SecurityLog(operation = "user-logout", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Timed(value = "auth.refresh", description = "Token refresh operation")
    @Monitored("token-refresh")
    @SecurityLog(operation = "token-refresh", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<LoginResponse> refresh(@RequestHeader("Authorization") String refreshToken) {
        LoginResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    @Timed(value = "auth.validate", description = "Token validation operation")
    @SecurityLog(operation = "token-validation", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String token) {
        boolean isValid = authService.validateToken(token);
        return isValid ? ResponseEntity.ok().build() : ResponseEntity.unauthorized().build();
    }
}