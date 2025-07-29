package com.crm.platform.security.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based session management for JWT tokens
 */
@Component
public class SessionManager {
    
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Store session information
     */
    public void createSession(String sessionId, SessionInfo sessionInfo, Duration expiration) {
        String sessionKey = SESSION_PREFIX + sessionId;
        String userSessionsKey = USER_SESSIONS_PREFIX + sessionInfo.getUserId();
        
        // Store session info
        redisTemplate.opsForValue().set(sessionKey, sessionInfo, expiration);
        
        // Add to user's active sessions
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, expiration);
    }
    
    /**
     * Get session information
     */
    public SessionInfo getSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        return (SessionInfo) redisTemplate.opsForValue().get(sessionKey);
    }
    
    /**
     * Update session last accessed time
     */
    public void updateSessionAccess(String sessionId) {
        SessionInfo sessionInfo = getSession(sessionId);
        if (sessionInfo != null) {
            sessionInfo.updateLastAccessed();
            String sessionKey = SESSION_PREFIX + sessionId;
            
            // Get current TTL and maintain it
            Long ttl = redisTemplate.getExpire(sessionKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                redisTemplate.opsForValue().set(sessionKey, sessionInfo, Duration.ofSeconds(ttl));
            }
        }
    }
    
    /**
     * Invalidate a specific session
     */
    public void invalidateSession(String sessionId) {
        SessionInfo sessionInfo = getSession(sessionId);
        if (sessionInfo != null) {
            String sessionKey = SESSION_PREFIX + sessionId;
            String userSessionsKey = USER_SESSIONS_PREFIX + sessionInfo.getUserId();
            
            // Remove session
            redisTemplate.delete(sessionKey);
            
            // Remove from user's active sessions
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
        }
    }
    
    /**
     * Invalidate all sessions for a user
     */
    public void invalidateUserSessions(UUID userId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        
        if (sessionIds != null) {
            for (Object sessionId : sessionIds) {
                String sessionKey = SESSION_PREFIX + sessionId;
                redisTemplate.delete(sessionKey);
            }
            
            // Clear user's session set
            redisTemplate.delete(userSessionsKey);
        }
    }
    
    /**
     * Get all active sessions for a user
     */
    public Set<Object> getUserActiveSessions(UUID userId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        return redisTemplate.opsForSet().members(userSessionsKey);
    }
    
    /**
     * Check if session is valid and active
     */
    public boolean isSessionValid(String sessionId) {
        SessionInfo sessionInfo = getSession(sessionId);
        return sessionInfo != null && sessionInfo.isActive();
    }
    
    /**
     * Blacklist a token (for logout/revocation)
     */
    public void blacklistToken(String tokenId, Duration expiration) {
        String blacklistKey = BLACKLIST_PREFIX + tokenId;
        redisTemplate.opsForValue().set(blacklistKey, "blacklisted", expiration);
    }
    
    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String tokenId) {
        String blacklistKey = BLACKLIST_PREFIX + tokenId;
        return redisTemplate.hasKey(blacklistKey);
    }
    
    /**
     * Get session count for a user
     */
    public long getUserSessionCount(UUID userId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        Long count = redisTemplate.opsForSet().size(userSessionsKey);
        return count != null ? count : 0;
    }
    
    /**
     * Cleanup expired sessions for a user
     */
    public void cleanupExpiredSessions(UUID userId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        
        if (sessionIds != null) {
            for (Object sessionId : sessionIds) {
                String sessionKey = SESSION_PREFIX + sessionId;
                if (!redisTemplate.hasKey(sessionKey)) {
                    // Session expired, remove from user's active sessions
                    redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
                }
            }
        }
    }
}