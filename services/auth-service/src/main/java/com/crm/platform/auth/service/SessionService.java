package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.SessionInfo;
import com.crm.platform.auth.dto.SessionValidationRequest;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.entity.UserSession;
import com.crm.platform.auth.exception.OAuth2Exception;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.auth.repository.UserSessionRepository;
import com.crm.platform.security.jwt.JwtTokenProvider;
import com.crm.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Session management service providing session lifecycle management,
 * device fingerprinting, session validation, and security monitoring.
 */
@Service
@Transactional
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private static final int SESSION_TOKEN_LENGTH = 32;
    private static final int MAX_SESSIONS_PER_USER = 10;
    private static final int SUSPICIOUS_LOGIN_THRESHOLD = 5;

    // User agent patterns for device detection
    private static final Pattern MOBILE_PATTERN = Pattern.compile(
        "(?i).*(mobile|android|iphone|ipad|phone|blackberry|opera mini).*"
    );
    private static final Pattern TABLET_PATTERN = Pattern.compile(
        "(?i).*(tablet|ipad).*"
    );
    private static final Pattern DESKTOP_PATTERN = Pattern.compile(
        "(?i).*(windows|macintosh|linux|x11).*"
    );

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private UserCredentialsRepository userCredentialsRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SecurityAuditService auditService;

    @Autowired
    private DeviceTrustService deviceTrustService;

    @Value("${auth.session.default-expiry-hours:24}")
    private int defaultSessionExpiryHours;

    @Value("${auth.session.refresh-expiry-days:30}")
    private int refreshTokenExpiryDays;

    @Value("${auth.session.enable-device-tracking:true}")
    private boolean enableDeviceTracking;

    @Value("${auth.session.enable-location-tracking:true}")
    private boolean enableLocationTracking;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Create a new user session with device fingerprinting
     */
    public UserSession createSession(UUID userId, UUID tenantId, HttpServletRequest request) {
        try {
            // Check session limits
            enforceSessionLimits(userId, tenantId);

            // Generate session tokens
            String tokenId = generateSessionToken();
            String refreshToken = generateRefreshToken();

            // Calculate expiry times
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(defaultSessionExpiryHours);
            LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(refreshTokenExpiryDays);

            // Extract device information
            String ipAddress = extractClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String deviceType = detectDeviceType(userAgent);
            String browser = extractBrowser(userAgent);
            String operatingSystem = extractOperatingSystem(userAgent);
            String deviceFingerprint = generateDeviceFingerprint(request);
            String location = null;

            // Get location if enabled
            if (enableLocationTracking) {
                location = getLocationFromIp(ipAddress);
            }

            // Create session entity
            UserSession session = new UserSession(userId, tenantId, tokenId, refreshToken, 
                                                expiresAt, refreshExpiresAt, ipAddress, userAgent);
            session.setDeviceType(deviceType);
            session.setBrowser(browser);
            session.setOperatingSystem(operatingSystem);
            session.setLocation(location);
            session.setDeviceFingerprint(deviceFingerprint);

            // Save session
            UserSession savedSession = sessionRepository.save(session);

            // Check for suspicious activity
            checkSuspiciousActivity(userId, tenantId, ipAddress, deviceFingerprint);

            // Audit log
            auditService.logSecurityEvent(userId, tenantId, "SESSION_CREATED", 
                "New session created from " + ipAddress + " (" + deviceType + ")",
                SecurityAuditLog.AuditEventStatus.SUCCESS, ipAddress, userAgent, tokenId);

            logger.info("Session created for user: {} from IP: {} ({})", userId, ipAddress, deviceType);
            return savedSession;

        } catch (Exception e) {
            logger.error("Error creating session for user: {}", userId, e);
            throw new BusinessException("Failed to create session");
        }
    }

    /**
     * Validate session and return session information
     */
    public Map<String, Object> validateSession(SessionValidationRequest request) {
        try {
            Optional<UserSession> sessionOpt = sessionRepository.findByTokenId(request.getSessionId());
            
            if (sessionOpt.isEmpty()) {
                return Map.of(
                    "valid", false,
                    "reason", "Session not found"
                );
            }

            UserSession session = sessionOpt.get();

            // Check if session is active and not expired
            if (!session.isActive()) {
                return Map.of(
                    "valid", false,
                    "reason", "Session is not active",
                    "status", session.getStatus().name()
                );
            }

            // Update last accessed time
            session.updateLastAccessed();
            sessionRepository.save(session);

            return Map.of(
                "valid", true,
                "user_id", session.getUserId(),
                "tenant_id", session.getTenantId(),
                "expires_at", session.getExpiresAt(),
                "last_accessed_at", session.getLastAccessedAt(),
                "device_type", session.getDeviceType(),
                "location", session.getLocation()
            );

        } catch (Exception e) {
            logger.error("Error validating session: {}", request.getSessionId(), e);
            return Map.of(
                "valid", false,
                "reason", "Internal error during validation"
            );
        }
    }

    /**
     * Renew session expiry time
     */
    public UserSession renewSession(String sessionId) {
        try {
            UserSession session = sessionRepository.findByTokenId(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));

            if (!session.isActive()) {
                throw new BusinessException("Cannot renew inactive session");
            }

            // Extend session expiry
            session.setExpiresAt(LocalDateTime.now().plusHours(defaultSessionExpiryHours));
            session.updateLastAccessed();

            UserSession renewedSession = sessionRepository.save(session);

            // Audit log
            auditService.logSecurityEvent(session.getUserId(), session.getTenantId(), "SESSION_RENEWED", 
                "Session renewed: " + sessionId,
                SecurityAuditLog.AuditEventStatus.SUCCESS, session.getIpAddress(), 
                session.getUserAgent(), sessionId);

            logger.debug("Session renewed: {} for user: {}", sessionId, session.getUserId());
            return renewedSession;

        } catch (Exception e) {
            logger.error("Error renewing session: {}", sessionId, e);
            throw new BusinessException("Failed to renew session");
        }
    }

    /**
     * Terminate a specific session
     */
    public Map<String, Object> terminateSession(String authorization, String sessionId, HttpServletRequest request) {
        try {
            // Extract current user from authorization (simplified - would use JWT in real implementation)
            UUID currentUserId = extractUserIdFromAuthorization(authorization);
            
            UserSession session = sessionRepository.findByTokenId(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));

            // Verify user owns the session
            if (!session.getUserId().equals(currentUserId)) {
                throw new BusinessException("Access denied: Cannot terminate another user's session");
            }

            // Terminate session
            session.setStatus(UserSession.SessionStatus.LOGGED_OUT);
            sessionRepository.save(session);

            // Audit log
            auditService.logSecurityEvent(currentUserId, session.getTenantId(), "SESSION_TERMINATED", 
                "Session terminated: " + sessionId,
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                extractClientIpAddress(request), request.getHeader("User-Agent"), sessionId);

            logger.info("Session terminated: {} by user: {}", sessionId, currentUserId);

            return Map.of(
                "terminated", true,
                "session_id", sessionId,
                "terminated_at", LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("Error terminating session: {}", sessionId, e);
            throw new BusinessException("Failed to terminate session");
        }
    }

    /**
     * Terminate all sessions for a user except current
     */
    public Map<String, Object> terminateAllSessions(String authorization, HttpServletRequest request) {
        try {
            UUID currentUserId = extractUserIdFromAuthorization(authorization);
            String currentSessionId = extractSessionIdFromAuthorization(authorization);

            // Get all active sessions for user
            List<UserSession> userSessions = sessionRepository.findByUserIdAndStatus(
                currentUserId, UserSession.SessionStatus.ACTIVE);

            int terminatedCount = 0;
            for (UserSession session : userSessions) {
                // Skip current session
                if (!session.getTokenId().equals(currentSessionId)) {
                    session.setStatus(UserSession.SessionStatus.LOGGED_OUT);
                    sessionRepository.save(session);
                    terminatedCount++;
                }
            }

            // Audit log
            auditService.logSecurityEvent(currentUserId, null, "ALL_SESSIONS_TERMINATED", 
                "All sessions terminated except current (" + terminatedCount + " sessions)",
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                extractClientIpAddress(request), request.getHeader("User-Agent"), currentSessionId);

            logger.info("All sessions terminated for user: {} (count: {})", currentUserId, terminatedCount);

            return Map.of(
                "terminated_all", true,
                "terminated_count", terminatedCount,
                "current_session_preserved", true,
                "terminated_at", LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("Error terminating all sessions", e);
            throw new BusinessException("Failed to terminate all sessions");
        }
    }

    /**
     * List all active sessions for a user
     */
    public List<SessionInfo> listUserSessions(String authorization) {
        try {
            UUID userId = extractUserIdFromAuthorization(authorization);
            
            List<UserSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

            return sessions.stream()
                .map(this::convertToSessionInfo)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error listing user sessions", e);
            throw new BusinessException("Failed to list sessions");
        }
    }

    /**
     * Get current session information
     */
    public SessionInfo getCurrentSession(String authorization) {
        try {
            String sessionId = extractSessionIdFromAuthorization(authorization);
            
            UserSession session = sessionRepository.findByTokenId(sessionId)
                .orElseThrow(() -> new BusinessException("Current session not found"));

            return convertToSessionInfo(session);

        } catch (Exception e) {
            logger.error("Error getting current session", e);
            throw new BusinessException("Failed to get current session");
        }
    }

    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Mark expired sessions
            sessionRepository.expireOldSessions(now);
            
            // Delete old expired sessions (older than 30 days)
            LocalDateTime cutoff = now.minusDays(30);
            sessionRepository.deleteByStatusAndCreatedAtBefore(UserSession.SessionStatus.EXPIRED, cutoff);

            logger.debug("Expired sessions cleanup completed");

        } catch (Exception e) {
            logger.error("Error during session cleanup", e);
        }
    }

    /**
     * Monitor sessions for suspicious activity
     */
    public void monitorSuspiciousActivity() {
        try {
            LocalDateTime recentTime = LocalDateTime.now().minusHours(1);
            
            // This would implement more sophisticated suspicious activity detection
            // For now, we'll just log that monitoring is running
            logger.debug("Session monitoring for suspicious activity completed");

        } catch (Exception e) {
            logger.error("Error during suspicious activity monitoring", e);
        }
    }

    // Private helper methods

    private void enforceSessionLimits(UUID userId, UUID tenantId) {
        long activeSessionCount = sessionRepository.countActiveSessionsByUser(userId);
        
        if (activeSessionCount >= MAX_SESSIONS_PER_USER) {
            // Terminate oldest session
            List<UserSession> sessions = sessionRepository.findByUserIdAndStatus(
                userId, UserSession.SessionStatus.ACTIVE);
            
            sessions.stream()
                .min(Comparator.comparing(UserSession::getCreatedAt))
                .ifPresent(oldestSession -> {
                    oldestSession.setStatus(UserSession.SessionStatus.EXPIRED);
                    sessionRepository.save(oldestSession);
                    
                    logger.info("Terminated oldest session for user {} due to session limit", userId);
                });
        }
    }

    private String generateSessionToken() {
        byte[] bytes = new byte[SESSION_TOKEN_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String extractClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String detectDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }

        if (MOBILE_PATTERN.matcher(userAgent).matches()) {
            return "Mobile";
        } else if (TABLET_PATTERN.matcher(userAgent).matches()) {
            return "Tablet";
        } else if (DESKTOP_PATTERN.matcher(userAgent).matches()) {
            return "Desktop";
        } else {
            return "Unknown";
        }
    }

    private String extractBrowser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }

        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        if (userAgent.contains("Opera")) return "Opera";
        
        return "Unknown";
    }

    private String extractOperatingSystem(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }

        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac OS")) return "macOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iOS")) return "iOS";
        
        return "Unknown";
    }

    private String generateDeviceFingerprint(HttpServletRequest request) {
        try {
            StringBuilder fingerprint = new StringBuilder();
            fingerprint.append(request.getHeader("User-Agent"));
            fingerprint.append(request.getHeader("Accept-Language"));
            fingerprint.append(request.getHeader("Accept-Encoding"));
            fingerprint.append(extractClientIpAddress(request));

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fingerprint.toString().getBytes());
            
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            logger.error("Error generating device fingerprint", e);
            return "unknown";
        }
    }

    private String getLocationFromIp(String ipAddress) {
        // This would integrate with a geolocation service
        // For now, return a placeholder
        if ("127.0.0.1".equals(ipAddress) || "localhost".equals(ipAddress)) {
            return "Local";
        }
        return "Unknown Location";
    }

    private void checkSuspiciousActivity(UUID userId, UUID tenantId, String ipAddress, String deviceFingerprint) {
        try {
            // Check for multiple sessions from different locations
            LocalDateTime recentTime = LocalDateTime.now().minusHours(1);
            List<UserSession> recentSessions = sessionRepository.findByUserIdAndStatus(
                userId, UserSession.SessionStatus.ACTIVE);

            Set<String> recentIps = recentSessions.stream()
                .filter(s -> s.getCreatedAt().isAfter(recentTime))
                .map(UserSession::getIpAddress)
                .collect(Collectors.toSet());

            if (recentIps.size() > SUSPICIOUS_LOGIN_THRESHOLD) {
                auditService.logSecurityEvent(userId, tenantId, "SUSPICIOUS_ACTIVITY_DETECTED", 
                    "Multiple sessions from different IPs: " + recentIps.size(),
                    SecurityAuditLog.AuditEventStatus.WARNING, ipAddress, null, null);
                
                logger.warn("Suspicious activity detected for user: {} - multiple IPs: {}", userId, recentIps.size());
            }

        } catch (Exception e) {
            logger.error("Error checking suspicious activity", e);
        }
    }

    private UUID extractUserIdFromAuthorization(String authorization) {
        // This would extract user ID from JWT token
        // For now, return a placeholder
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                return jwtTokenProvider.getUserIdFromToken(token);
            } catch (Exception e) {
                logger.error("Error extracting user ID from token", e);
                throw new OAuth2Exception("invalid_token", "Invalid authorization token");
            }
        }
        throw new OAuth2Exception("invalid_token", "Missing or invalid authorization header");
    }

    private String extractSessionIdFromAuthorization(String authorization) {
        // This would extract session ID from JWT token
        // For now, return a placeholder
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                return jwtTokenProvider.getSessionIdFromToken(token);
            } catch (Exception e) {
                logger.debug("No session ID in token or error extracting", e);
                return "unknown-session";
            }
        }
        return "unknown-session";
    }

    private SessionInfo convertToSessionInfo(UserSession session) {
        SessionInfo info = new SessionInfo();
        info.setSessionId(session.getTokenId());
        info.setUserId(session.getUserId());
        info.setTenantId(session.getTenantId());
        info.setIpAddress(session.getIpAddress());
        info.setDeviceType(session.getDeviceType());
        info.setBrowser(session.getBrowser());
        info.setOperatingSystem(session.getOperatingSystem());
        info.setLocation(session.getLocation());
        info.setStatus(session.getStatus().name());
        info.setCreatedAt(session.getCreatedAt());
        info.setLastAccessedAt(session.getLastAccessedAt());
        info.setExpiresAt(session.getExpiresAt());
        info.setCurrent(false); // Would be set based on current session comparison
        
        return info;
    }
}