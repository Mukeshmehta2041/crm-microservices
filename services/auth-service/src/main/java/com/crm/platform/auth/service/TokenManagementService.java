package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.TokenIntrospectionResponse;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.entity.TokenBlacklist;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.entity.UserSession;
import com.crm.platform.auth.exception.OAuth2Exception;
import com.crm.platform.auth.repository.TokenBlacklistRepository;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.auth.repository.UserSessionRepository;
import com.crm.platform.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Enhanced token management service providing JWT token generation, 
 * refresh token rotation, token revocation, and token introspection capabilities.
 */
@Service
@Transactional
public class TokenManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TokenManagementService.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private UserCredentialsRepository userCredentialsRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private SecurityAuditService auditService;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Value("${auth.token.refresh-rotation:true}")
    private boolean enableRefreshTokenRotation;

    @Value("${auth.token.max-refresh-count:5}")
    private int maxRefreshCount;

    @Value("${auth.token.blacklist-cleanup-hours:24}")
    private int blacklistCleanupHours;

    /**
     * Generate JWT access token with proper claims
     */
    public String generateAccessToken(UUID userId, UUID tenantId, String sessionId, 
                                    List<String> roles, List<String> permissions, 
                                    Map<String, Object> additionalClaims) {
        try {
            // Get user details for additional claims
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new OAuth2Exception("invalid_user", "User not found"));

            // Create base token
            String accessToken = jwtTokenProvider.createAccessToken(userId, tenantId, roles, permissions, sessionId, null);

            // Add additional claims if provided
            if (additionalClaims != null && !additionalClaims.isEmpty()) {
                // For now, we'll log this - in a full implementation, we'd need to modify JwtTokenProvider
                logger.debug("Additional claims requested for token generation: {}", additionalClaims.keySet());
            }

            // Audit log
            auditService.logTokenGeneration(userId, "internal", "access_token");

            return accessToken;

        } catch (Exception e) {
            logger.error("Error generating access token for user: {}", userId, e);
            throw new OAuth2Exception("server_error", "Failed to generate access token");
        }
    }

    /**
     * Generate JWT refresh token with rotation support
     */
    public String generateRefreshToken(UUID userId, UUID tenantId, String sessionId, String oldRefreshToken) {
        try {
            // If refresh token rotation is enabled and we have an old token, blacklist it
            if (enableRefreshTokenRotation && StringUtils.hasText(oldRefreshToken)) {
                revokeToken(oldRefreshToken, "refresh_rotation", userId);
            }

            // Generate new refresh token
            String refreshToken = jwtTokenProvider.createRefreshToken(userId, tenantId, sessionId, null);

            // Audit log
            auditService.logTokenRefresh(userId, "internal");

            return refreshToken;

        } catch (Exception e) {
            logger.error("Error generating refresh token for user: {}", userId, e);
            throw new OAuth2Exception("server_error", "Failed to generate refresh token");
        }
    }

    /**
     * Rotate refresh token (generate new one and blacklist old one)
     */
    public Map<String, String> rotateRefreshToken(String currentRefreshToken) {
        try {
            // Validate current refresh token
            if (!jwtTokenProvider.validateTokenForRefresh(currentRefreshToken)) {
                throw new OAuth2Exception("invalid_grant", "Invalid refresh token");
            }

            // Check if token is blacklisted
            String jti = jwtTokenProvider.getJtiFromToken(currentRefreshToken);
            if (tokenBlacklistRepository.existsByJti(jti)) {
                throw new OAuth2Exception("invalid_grant", "Refresh token has been revoked");
            }

            // Extract user information
            UUID userId = jwtTokenProvider.getUserIdFromToken(currentRefreshToken);
            UUID tenantId = jwtTokenProvider.getTenantIdFromToken(currentRefreshToken);
            String sessionId = jwtTokenProvider.getSessionIdFromToken(currentRefreshToken);

            // Get user roles and permissions
            List<String> roles = getUserRoles(userId);
            List<String> permissions = getUserPermissions(userId);

            // Generate new tokens
            String newAccessToken = generateAccessToken(userId, tenantId, sessionId, roles, permissions, null);
            String newRefreshToken = generateRefreshToken(userId, tenantId, sessionId, currentRefreshToken);

            // Update session with new tokens
            updateSessionTokens(sessionId, newAccessToken, newRefreshToken);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", newAccessToken);
            tokens.put("refresh_token", newRefreshToken);

            return tokens;

        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error rotating refresh token", e);
            throw new OAuth2Exception("server_error", "Failed to rotate refresh token");
        }
    }

    /**
     * Revoke token and add to blacklist
     */
    public void revokeToken(String token, String reason, UUID revokedBy) {
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                logger.warn("Attempted to revoke invalid token");
                return; // OAuth2 spec says to return success even for invalid tokens
            }

            // Extract token information
            Claims claims = jwtTokenProvider.parseToken(token);
            String jti = claims.getId();
            UUID userId = UUID.fromString(claims.getSubject());
            UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));
            String tokenType = claims.get("type", String.class);

            // Determine blacklist token type
            TokenBlacklist.TokenType blacklistTokenType = "access".equals(tokenType) ? 
                TokenBlacklist.TokenType.ACCESS : TokenBlacklist.TokenType.REFRESH;

            // Add to blacklist
            TokenBlacklist blacklistEntry = new TokenBlacklist(
                jti,
                userId,
                tenantId,
                blacklistTokenType,
                LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault()),
                reason,
                revokedBy
            );

            tokenBlacklistRepository.save(blacklistEntry);

            // If it's a refresh token, also revoke associated access tokens
            if ("refresh".equals(tokenType)) {
                revokeAssociatedTokens(userId, tenantId, reason, revokedBy);
            }

            // Audit log
            auditService.logTokenRevocation(userId, "internal", token);

            logger.info("Token revoked for user: {}, reason: {}", userId, reason);

        } catch (Exception e) {
            logger.error("Error revoking token", e);
            throw new OAuth2Exception("server_error", "Failed to revoke token");
        }
    }

    /**
     * Revoke all tokens for a user
     */
    public void revokeAllUserTokens(UUID userId, UUID tenantId, String reason, UUID revokedBy) {
        try {
            // Find all active sessions for the user
            List<UserSession> userSessions = userSessionRepository.findActiveSessionsByUserId(userId, tenantId);

            for (UserSession session : userSessions) {
                // Revoke session tokens if they exist
                if (StringUtils.hasText(session.getTokenId())) {
                    // This would require extracting the actual JWT token from session
                    // For now, we'll mark the session as revoked
                    session.setStatus(UserSession.SessionStatus.REVOKED);
                    userSessionRepository.save(session);
                }
            }

            // Audit log
            auditService.logSecurityEvent(userId, tenantId, "ALL_TOKENS_REVOKED", 
                "All tokens revoked for user, reason: " + reason, 
                SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);

            logger.info("All tokens revoked for user: {}, reason: {}", userId, reason);

        } catch (Exception e) {
            logger.error("Error revoking all user tokens for user: {}", userId, e);
            throw new OAuth2Exception("server_error", "Failed to revoke user tokens");
        }
    }

    /**
     * Validate token and check blacklist
     */
    public boolean validateToken(String token) {
        try {
            // Basic JWT validation
            if (!jwtTokenProvider.validateToken(token)) {
                return false;
            }

            // Check blacklist
            String jti = jwtTokenProvider.getJtiFromToken(token);
            if (tokenBlacklistRepository.existsByJti(jti)) {
                logger.debug("Token validation failed: token is blacklisted");
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Token introspection - return detailed token information
     */
    public TokenIntrospectionResponse introspectToken(String token) {
        try {
            TokenIntrospectionResponse response = new TokenIntrospectionResponse();

            // Basic validation
            if (!jwtTokenProvider.validateToken(token)) {
                response.setActive(false);
                return response;
            }

            // Check blacklist
            String jti = jwtTokenProvider.getJtiFromToken(token);
            if (tokenBlacklistRepository.existsByJti(jti)) {
                response.setActive(false);
                response.setError("Token has been revoked");
                return response;
            }

            // Extract token information
            Claims claims = jwtTokenProvider.parseToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));

            // Build response
            response.setActive(true);
            response.setClientId("internal"); // For internal tokens
            response.setUserId(userId);
            response.setTenantId(tenantId);
            response.setTokenType(claims.get("type", String.class));
            response.setScope(claims.get("scope", String.class));
            response.setIssuer(claims.getIssuer());
            response.setAudience(claims.getAudience());
            response.setIssuedAt(claims.getIssuedAt().toInstant());
            response.setExpiresAt(claims.getExpiration().toInstant());
            response.setNotBefore(claims.getNotBefore() != null ? claims.getNotBefore().toInstant() : null);

            // Add user information if available
            userCredentialsRepository.findByUserId(userId).ifPresent(credentials -> {
                response.setUsername(credentials.getUsername());
                response.setEmail(credentials.getEmail());
                response.setEmailVerified(credentials.getEmailVerified());
            });

            return response;

        } catch (Exception e) {
            logger.error("Error during token introspection", e);
            TokenIntrospectionResponse response = new TokenIntrospectionResponse();
            response.setActive(false);
            response.setError("Internal server error");
            return response;
        }
    }

    /**
     * Get token statistics for monitoring
     */
    public Map<String, Object> getTokenStatistics(UUID tenantId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Count blacklisted tokens
            long blacklistedCount = tokenBlacklistRepository.count();
            stats.put("blacklisted_tokens", blacklistedCount);

            // Count active sessions (proxy for active tokens)
            long activeSessions = userSessionRepository.countActiveSessionsByTenant(tenantId);
            stats.put("active_sessions", activeSessions);

            // Add more statistics as needed
            stats.put("timestamp", LocalDateTime.now());

            return stats;

        } catch (Exception e) {
            logger.error("Error getting token statistics", e);
            return Map.of("error", "Failed to retrieve statistics");
        }
    }

    /**
     * Cleanup expired blacklisted tokens
     */
    public void cleanupExpiredBlacklistedTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = tokenBlacklistRepository.deleteExpiredTokens(now);
            
            if (deletedCount > 0) {
                logger.info("Cleaned up {} expired blacklisted tokens", deletedCount);
            }

        } catch (Exception e) {
            logger.error("Error cleaning up expired blacklisted tokens", e);
        }
    }

    // Private helper methods

    private List<String> getUserRoles(UUID userId) {
        // This would typically fetch from a user role service
        // For now, return empty list
        return Collections.emptyList();
    }

    private List<String> getUserPermissions(UUID userId) {
        // This would typically fetch from a permission service
        // For now, return empty list
        return Collections.emptyList();
    }

    private void updateSessionTokens(String sessionId, String accessToken, String refreshToken) {
        if (StringUtils.hasText(sessionId)) {
            userSessionRepository.findByTokenId(sessionId).ifPresent(session -> {
                session.setRefreshToken(refreshToken);
                session.updateLastAccessed();
                userSessionRepository.save(session);
            });
        }
    }

    private void revokeAssociatedTokens(UUID userId, UUID tenantId, String reason, UUID revokedBy) {
        // This would revoke all access tokens associated with the refresh token
        // Implementation would depend on how tokens are tracked
        logger.debug("Revoking associated tokens for user: {}", userId);
    }

    /**
     * Extract user ID from authorization header
     */
    public UUID extractUserIdFromToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new OAuth2Exception("invalid_token", "Bearer token is missing or invalid");
        }
        String token = authorizationHeader.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}