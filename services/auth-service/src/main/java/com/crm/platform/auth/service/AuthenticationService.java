package com.crm.platform.auth.service;

import com.crm.platform.auth.client.UserServiceClient;
import com.crm.platform.auth.dto.LoginRequest;
import com.crm.platform.auth.dto.LoginResponse;
import com.crm.platform.auth.dto.RefreshTokenRequest;
import com.crm.platform.auth.dto.UserInfo;
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
                               UserServiceClient userServiceClient) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
        this.rateLimitingService = rateLimitingService;
        this.userServiceClient = userServiceClient;
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

        // Reset failed attempts on successful login
        if (credentials.getFailedLoginAttempts() > 0) {
            userCredentialsRepository.updateFailedLoginAttempts(credentials.getId(), 0);
        }

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
        Optional<User> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            throw new InvalidTokenException("User account is not active");
        }

        User user = userOpt.get();

        // Generate new tokens
        String newTokenId = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getTenantId(), 
            List.of(), List.of()); // Empty roles and permissions for now
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getTenantId());

        // Update session
        LocalDateTime now = LocalDateTime.now();
        session.setTokenId(newTokenId);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(now.plusSeconds(accessTokenValiditySeconds));
        session.setRefreshExpiresAt(now.plusSeconds(refreshTokenValiditySeconds));
        session.updateLastAccessed();
        sessionRepository.save(session);

        // Log token refresh
        auditService.logSecurityEvent(user.getId(), user.getTenantId(),
            SecurityAuditLog.EVENT_TOKEN_REFRESH, "Token refreshed successfully",
            SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, newTokenId);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
            user.getId(), user.getUsername(), user.getEmail(),
            user.getFirstName(), user.getLastName(), user.getTenantId(), user.getLastLoginAt());

        return new LoginResponse(newAccessToken, newRefreshToken, accessTokenValiditySeconds,
                               refreshTokenValiditySeconds, userInfo);
    }

    public void logout(String tokenId, HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Optional<UserSession> sessionOpt = sessionRepository.findByTokenId(tokenId);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setStatus(UserSession.SessionStatus.LOGGED_OUT);
            sessionRepository.save(session);

            Optional<User> userOpt = userRepository.findById(session.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                auditService.logSecurityEvent(user.getId(), user.getTenantId(),
                    SecurityAuditLog.EVENT_LOGOUT, "User logged out",
                    SecurityAuditLog.AuditEventStatus.SUCCESS, clientIp, userAgent, tokenId);
            }
        }
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