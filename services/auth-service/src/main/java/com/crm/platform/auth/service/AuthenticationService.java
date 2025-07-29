package com.crm.platform.auth.service;

import com.crm.platform.auth.client.UserServiceClient;
import com.crm.platform.auth.dto.*;
import java.util.Map;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.entity.UserSession;
import com.crm.platform.auth.exception.AuthenticationException;
import com.crm.platform.auth.exception.AccountLockedException;
import com.crm.platform.auth.exception.InvalidCredentialsException;
import com.crm.platform.auth.exception.InvalidTokenException;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.auth.repository.UserSessionRepository;
import com.crm.platform.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserCredentialsRepository userCredentialsRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityAuditService auditService;
    private final RateLimitingService rateLimitingService;
    private final UserServiceClient userServiceClient;
    private final MfaService mfaService;
    private final DeviceTrustService deviceTrustService;

    @Value("${auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${auth.token.access-token-validity-seconds:3600}")
    private long accessTokenValiditySeconds;

    @Value("${auth.token.refresh-token-validity-seconds:604800}")
    private long refreshTokenValiditySeconds;

    @Autowired
    public AuthenticationService(UserCredentialsRepository userCredentialsRepository,
                               UserSessionRepository sessionRepository,
                               PasswordEncoder passwordEncoder,
                               JwtTokenProvider jwtTokenProvider,
                               SecurityAuditService auditService,
                               RateLimitingService rateLimitingService,
                               UserServiceClient userServiceClient,
                               MfaService mfaService,
                               DeviceTrustService deviceTrustService) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
        this.rateLimitingService = rateLimitingService;
        this.userServiceClient = userServiceClient;
        this.mfaService = mfaService;
        this.deviceTrustService = deviceTrustService;
    }

    public LoginResponse authenticate(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Check rate limiting
        if (!rateLimitingService.isAllowed(clientIp, "login")) {
            auditService.logSecurityEvent(null, null, SecurityAuditLog.EVENT_BRUTE_FORCE_ATTEMPT,
                "Rate limit exceeded for IP: " + clientIp, SecurityAuditLog.AuditEventStatus.FAILURE,
                clientIp, userAgent, null);
            throw new AuthenticationException("Too many login attempts. Please try again later.");
        }

        // Find user credentials
        Optional<UserCredentials> credentialsOpt = userCredentialsRepository.findByUsernameOrEmail(
            request.getUsernameOrEmail(), request.getUsernameOrEmail());

        if (credentialsOpt.isEmpty()) {
            auditService.logSecurityEvent(null, null, SecurityAuditLog.EVENT_LOGIN_FAILURE,
                "User credentials not found: " + request.getUsernameOrEmail(), 
                SecurityAuditLog.AuditEventStatus.FAILURE, clientIp, userAgent, null);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        UserCredentials credentials = credentialsOpt.get();

        // Check if account is locked
        if (credentials.isAccountLocked()) {
            auditService.logSecurityEvent(credentials.getUserId(), credentials.getTenantId(), 
                SecurityAuditLog.EVENT_LOGIN_FAILURE, "Account is locked",
                SecurityAuditLog.AuditEventStatus.FAILURE, clientIp, userAgent, null);
            throw new AccountLockedException("Account is temporarily locked due to multiple failed login attempts");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            handleFailedLogin(credentials, clientIp, userAgent);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Reset failed attempts on successful password verification
        if (credentials.getFailedLoginAttempts() > 0) {
            userCredentialsRepository.updateFailedLoginAttempts(credentials.getId(), 0);
        }

        // Check if MFA is required
        if (Boolean.TRUE.equals(credentials.getMfaEnabled())) {
            // Check if device is trusted (MFA bypass)
            boolean isDeviceTrusted = deviceTrustService.isDeviceTrusted(credentials.getUserId(), httpRequest);
            
            if (!isDeviceTrusted) {
                // MFA is required - generate MFA token and return challenge
                String mfaToken = generateMfaToken(credentials.getUserId(), credentials.getTenantId());
                
                // Log MFA challenge
                auditService.logSecurityEvent(credentials.getUserId(), credentials.getTenantId(), 
                    "MFA_CHALLENGE_ISSUED", "MFA challenge issued after successful password verification",
                    SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, null);

                // Return MFA challenge response
                LoginResponse mfaResponse = new LoginResponse();
                mfaResponse.setMfaRequired(true);
                mfaResponse.setMfaToken(mfaToken);
                mfaResponse.setMfaMethod(credentials.getMfaMethod().name());
                mfaResponse.setMessage("Multi-factor authentication required");
                
                return mfaResponse;
            } else {
                // Device is trusted - log bypass
                auditService.logSecurityEvent(credentials.getUserId(), credentials.getTenantId(), 
                    "MFA_BYPASSED_TRUSTED_DEVICE", "MFA bypassed due to trusted device",
                    SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, null);
            }
        }

        // Complete login (either no MFA required or trusted device)
        return completeLogin(credentials, httpRequest);
    }

    public LoginResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Find session by refresh token
        Optional<UserSession> sessionOpt = sessionRepository.findByRefreshToken(request.getRefreshToken());
        if (sessionOpt.isEmpty() || sessionOpt.get().isRefreshExpired()) {
            auditService.logSecurityEvent(null, null, SecurityAuditLog.EVENT_TOKEN_REFRESH,
                "Invalid or expired refresh token", SecurityAuditLog.AuditEventStatus.FAILURE,
                clientIp, userAgent, null);
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        UserSession session = sessionOpt.get();
        UserInfo userInfo = userServiceClient.getUserById(session.getUserId());
        if (userInfo == null) {
            throw new InvalidTokenException("User account not found");
        }

        // Generate new tokens
        String newTokenId = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.createAccessToken(userInfo.getId(), userInfo.getTenantId(), 
            List.of(), List.of()); // Empty roles and permissions for now
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userInfo.getId(), userInfo.getTenantId());

        // Update session
        LocalDateTime now = LocalDateTime.now();
        session.setTokenId(newTokenId);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(now.plusSeconds(accessTokenValiditySeconds));
        session.setRefreshExpiresAt(now.plusSeconds(refreshTokenValiditySeconds));
        session.updateLastAccessed();
        sessionRepository.save(session);

        // Log token refresh
        auditService.logSecurityEvent(userInfo.getId(), userInfo.getTenantId(),
            SecurityAuditLog.EVENT_TOKEN_REFRESH, "Token refreshed successfully",
            SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, newTokenId);

        // Create UserInfo for login response
        UserInfo loginUserInfo = new UserInfo(
            userInfo.getId(), userInfo.getEmail(), userInfo.getFirstName(), userInfo.getLastName(),
            userInfo.getPhoneNumber(), userInfo.getJobTitle(), userInfo.getDepartment(),
            userInfo.getProfileImageUrl(), userInfo.getRoles(), userInfo.getTenantId());

        return new LoginResponse(newAccessToken, newRefreshToken, accessTokenValiditySeconds,
                               refreshTokenValiditySeconds, loginUserInfo);
    }

    public void logout(String tokenId, HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Optional<UserSession> sessionOpt = sessionRepository.findByTokenId(tokenId);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setStatus(UserSession.SessionStatus.LOGGED_OUT);
            sessionRepository.save(session);

            UserInfo userInfo = userServiceClient.getUserById(session.getUserId());
            if (userInfo != null) {
                auditService.logSecurityEvent(userInfo.getId(), userInfo.getTenantId(),
                    SecurityAuditLog.EVENT_LOGOUT, "User logged out",
                    SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, tokenId);
            }
        }
    }

    public Map<String, Object> logout(String tokenId, LogoutRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Optional<UserSession> sessionOpt = sessionRepository.findByTokenId(tokenId);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setStatus(UserSession.SessionStatus.LOGGED_OUT);
            sessionRepository.save(session);

            UserInfo userInfo = userServiceClient.getUserById(session.getUserId());
            if (userInfo != null) {
                auditService.logSecurityEvent(userInfo.getId(), userInfo.getTenantId(),
                    SecurityAuditLog.EVENT_LOGOUT, "User logged out",
                    SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, tokenId);
            }
        }

        return Map.of(
            "success", true,
            "message", "Logout successful",
            "logout_time", LocalDateTime.now()
        );
    }

    public RegistrationResponse register(RegistrationRequest request, HttpServletRequest httpRequest) {
        // TODO: Implement user registration
        return new RegistrationResponse(
            UUID.randomUUID(), 
            request.getEmail(), 
            true, 
            true, 
            UUID.randomUUID(), 
            Instant.now()
        );
    }

    public Map<String, Object> verifyEmail(EmailVerificationRequest request, HttpServletRequest httpRequest) {
        // TODO: Implement email verification
        return Map.of("success", true, "message", "Email verified successfully");
    }

    public Map<String, Object> resendVerificationEmail(ResendVerificationRequest request, HttpServletRequest httpRequest) {
        // TODO: Implement resend verification email
        return Map.of("success", true, "message", "Verification email sent");
    }

    public boolean validateToken(String tokenId) {
        Optional<UserSession> sessionOpt = sessionRepository.findByTokenId(tokenId);
        if (sessionOpt.isEmpty()) {
            return false;
        }

        UserSession session = sessionOpt.get();
        if (!session.isActive()) {
            return false;
        }

        // Update last accessed time
        session.updateLastAccessed();
        sessionRepository.save(session);

        return true;
    }

    private void handleFailedLogin(UserCredentials credentials, String clientIp, String userAgent) {
        int newFailedAttempts = credentials.getFailedLoginAttempts() + 1;
        userCredentialsRepository.updateFailedLoginAttempts(credentials.getId(), newFailedAttempts);

        if (newFailedAttempts >= maxFailedAttempts) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
            userCredentialsRepository.lockAccount(credentials.getId(), lockUntil);

            auditService.logSecurityEvent(credentials.getUserId(), credentials.getTenantId(),
                SecurityAuditLog.EVENT_ACCOUNT_LOCKED, 
                "Account locked due to " + newFailedAttempts + " failed login attempts",
                SecurityAuditLog.AuditEventStatus.WARNING, clientIp, userAgent, null);
        } else {
            auditService.logSecurityEvent(credentials.getUserId(), credentials.getTenantId(),
                SecurityAuditLog.EVENT_LOGIN_FAILURE,
                "Failed login attempt " + newFailedAttempts + " of " + maxFailedAttempts,
                SecurityAuditLog.AuditEventStatus.FAILURE, clientIp, userAgent, null);
        }
    }

    /**
     * Complete MFA verification and login
     */
    public LoginResponse completeMfaLogin(String mfaToken, String mfaCode, String backupCode, 
                                        HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            // Validate MFA token and extract user info
            UUID userId = extractUserIdFromMfaToken(mfaToken);
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid MFA token"));

            // Check if MFA is enabled
            if (!Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                throw new AuthenticationException("MFA is not enabled for this user");
            }

            boolean mfaValid = false;
            String verificationMethod = null;

            // Try TOTP code first
            if (mfaCode != null && !mfaCode.trim().isEmpty()) {
                mfaValid = verifyTotpCode(credentials.getMfaSecret(), mfaCode);
                verificationMethod = "TOTP";
            }

            // Try backup code if TOTP failed
            if (!mfaValid && backupCode != null && !backupCode.trim().isEmpty()) {
                mfaValid = verifyAndConsumeBackupCode(credentials, backupCode);
                verificationMethod = "BACKUP_CODE";
            }

            if (!mfaValid) {
                auditService.logSecurityEvent(userId, credentials.getTenantId(), 
                    "MFA_VERIFICATION_FAILED", "MFA verification failed during login",
                    SecurityAuditLog.AuditEventStatus.FAILURE, clientIp, userAgent, null);
                throw new InvalidCredentialsException("Invalid MFA code");
            }

            // Log successful MFA verification
            auditService.logSecurityEvent(userId, credentials.getTenantId(), 
                "MFA_VERIFICATION_SUCCESS", "MFA verification successful using " + verificationMethod,
                SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, null);

            // Complete login
            LoginResponse response = completeLogin(credentials, httpRequest);
            response.setMfaVerified(true);
            
            return response;

        } catch (Exception e) {
            logger.error("Error completing MFA login", e);
            throw new AuthenticationException("MFA verification failed: " + e.getMessage());
        }
    }

    /**
     * Add current device to trusted devices after successful MFA
     */
    public void addTrustedDevice(String authorization, String deviceName, 
                               HttpServletRequest httpRequest) {
        try {
            // Extract user ID from authorization token
            UUID userId = extractUserIdFromToken(authorization);
            
            // Create trusted device request
            com.crm.platform.auth.dto.TrustedDeviceRequest deviceRequest = 
                new com.crm.platform.auth.dto.TrustedDeviceRequest();
            deviceRequest.setDeviceName(deviceName);
            deviceRequest.setDeviceType(detectDeviceType(httpRequest.getHeader("User-Agent")));
            deviceRequest.setUserAgent(httpRequest.getHeader("User-Agent"));
            
            // Add trusted device
            deviceTrustService.addTrustedDevice(userId, deviceRequest, httpRequest);
            
        } catch (Exception e) {
            logger.error("Error adding trusted device", e);
            throw new AuthenticationException("Failed to add trusted device: " + e.getMessage());
        }
    }

    /**
     * Handle MFA recovery using backup codes
     */
    public LoginResponse recoverWithBackupCode(String mfaToken, String backupCode, 
                                             HttpServletRequest httpRequest) {
        return completeMfaLogin(mfaToken, null, backupCode, httpRequest);
    }

    /**
     * Check if MFA recovery is available for user
     */
    public boolean isMfaRecoveryAvailable(String mfaToken) {
        try {
            UUID userId = extractUserIdFromMfaToken(mfaToken);
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElse(null);
            
            if (credentials == null || !Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                return false;
            }

            // Check if backup codes are available
            return credentials.getBackupCodes() != null && 
                   !credentials.getBackupCodes().trim().isEmpty() &&
                   !credentials.getBackupCodes().equals("[]");
                   
        } catch (Exception e) {
            logger.error("Error checking MFA recovery availability", e);
            return false;
        }
    }

    // Private helper methods

    private LoginResponse completeLogin(UserCredentials credentials, HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Fetch user profile information from User Service
        UserInfo userProfile = userServiceClient.getUserById(credentials.getUserId());
        if (userProfile == null) {
            logger.warn("User profile not found for user ID: {}", credentials.getUserId());
            // Create minimal user info from credentials
            userProfile = new UserInfo(
                credentials.getUserId(),
                credentials.getEmail(),
                null, null, null, null, null, null,
                java.util.Set.of(),
                credentials.getTenantId()
            );
        }

        // Generate tokens
        String tokenId = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.createAccessToken(credentials.getUserId(), credentials.getTenantId(), 
            List.of(), List.of()); // Empty roles and permissions for now
        String refreshToken = jwtTokenProvider.createRefreshToken(credentials.getUserId(), credentials.getTenantId());

        // Create session
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessTokenExpiry = now.plusSeconds(accessTokenValiditySeconds);
        LocalDateTime refreshTokenExpiry = now.plusSeconds(refreshTokenValiditySeconds);

        UserSession session = new UserSession(credentials.getUserId(), tokenId, refreshToken, 
                                            accessTokenExpiry, refreshTokenExpiry);
        session.setIpAddress(clientIp);
        session.setUserAgent(userAgent);
        session.setTenantId(credentials.getTenantId());
        sessionRepository.save(session);

        // Update last login time
        userCredentialsRepository.updateLastLoginTime(credentials.getId(), now);

        // Log successful login
        auditService.logSecurityEvent(credentials.getUserId(), credentials.getTenantId(), 
            SecurityAuditLog.EVENT_LOGIN_SUCCESS, "User logged in successfully",
            SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, tokenId);

        return new LoginResponse(accessToken, refreshToken, accessTokenValiditySeconds,
                               refreshTokenValiditySeconds, userProfile);
    }

    private String generateMfaToken(UUID userId, UUID tenantId) {
        // Generate a temporary MFA token (valid for a short time)
        // In a real implementation, this would be a JWT token with short expiry
        return "mfa_" + userId.toString() + "_" + System.currentTimeMillis();
    }

    private UUID extractUserIdFromMfaToken(String mfaToken) {
        // Extract user ID from MFA token
        // In a real implementation, this would parse a JWT token
        try {
            String[] parts = mfaToken.split("_");
            if (parts.length >= 2 && "mfa".equals(parts[0])) {
                return UUID.fromString(parts[1]);
            }
        } catch (Exception e) {
            logger.error("Error extracting user ID from MFA token", e);
        }
        throw new InvalidCredentialsException("Invalid MFA token");
    }

    private UUID extractUserIdFromToken(String authorization) {
        // Extract user ID from authorization token
        // In a real implementation, this would parse the JWT token
        // For now, return a placeholder
        return UUID.randomUUID();
    }

    private boolean verifyTotpCode(String secret, String code) {
        try {
            // Use the same TOTP verification logic as MfaService
            // This is a simplified version - in practice, you'd call MfaService
            return mfaService.verifyTotpCode(secret, code);
        } catch (Exception e) {
            logger.error("Error verifying TOTP code", e);
            return false;
        }
    }

    private boolean verifyAndConsumeBackupCode(UserCredentials credentials, String backupCode) {
        try {
            // Parse backup codes
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>> typeRef = 
                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {};
            
            java.util.List<String> backupCodes = objectMapper.readValue(credentials.getBackupCodes(), typeRef);
            
            if (backupCodes.remove(backupCode)) {
                // Update backup codes in database
                credentials.setBackupCodes(objectMapper.writeValueAsString(backupCodes));
                userCredentialsRepository.save(credentials);
                return true;
            }
            return false;

        } catch (Exception e) {
            logger.error("Error verifying backup code", e);
            return false;
        }
    }

    private String detectDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}