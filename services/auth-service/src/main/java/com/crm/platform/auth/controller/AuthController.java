package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.LoginRequest;
import com.crm.platform.auth.dto.LoginResponse;
import com.crm.platform.auth.dto.PasswordResetRequest;
import com.crm.platform.auth.dto.RefreshTokenRequest;
import com.crm.platform.auth.service.AuthenticationService;
import com.crm.platform.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthenticationService authenticationService;

    @Autowired
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        LoginResponse response = authenticationService.authenticate(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        LoginResponse response = authenticationService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        
        // Extract token ID from JWT token (this would be implemented in a filter)
        String tokenId = extractTokenIdFromHeader(authHeader);
        authenticationService.logout(tokenId, httpRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        String tokenId = extractTokenIdFromHeader(authHeader);
        boolean isValid = authenticationService.validateToken(tokenId);
        
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        
        // TODO: Implement password reset functionality
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth service is healthy"));
    }

    private String extractTokenIdFromHeader(String authHeader) {
        // This is a simplified implementation
        // In a real implementation, you would decode the JWT token to extract the token ID
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // For now, return a placeholder - this would be properly implemented with JWT parsing
            return "token-id-placeholder";
        }
        return null;
    }
}