package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.TokenIntrospectionResponse;
import com.crm.platform.auth.entity.TokenBlacklist;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.exception.OAuth2Exception;
import com.crm.platform.auth.repository.TokenBlacklistRepository;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.auth.repository.UserSessionRepository;
import com.crm.platform.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenManagementServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private UserCredentialsRepository userCredentialsRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private SecurityAuditService auditService;

    @Mock
    private RateLimitingService rateLimitingService;

    @InjectMocks
    private TokenManagementService tokenManagementService;

    private UUID testUserId;
    private UUID testTenantId;
    private UserCredentials testCredentials;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        
        testCredentials = new UserCredentials();
        testCredentials.setUserId(testUserId);
        testCredentials.setUsername("testuser");
        testCredentials.setEmail("test@example.com");
        testCredentials.setEmailVerified(true);
        testCredentials.setTenantId(testTenantId);

        // Set default configuration values
        ReflectionTestUtils.setField(tokenManagementService, "enableRefreshTokenRotation", true);
        ReflectionTestUtils.setField(tokenManagementService, "maxRefreshCount", 5);
        ReflectionTestUtils.setField(tokenManagementService, "blacklistCleanupHours", 24);
    }

    @Test
    void testGenerateAccessToken_Success() {
        // Arrange
        String sessionId = "session-123";
        List<String> roles = Arrays.asList("USER", "ADMIN");
        List<String> permissions = Arrays.asList("READ", "WRITE");
        String expectedToken = "access-token-123";

        when(userCredentialsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCredentials));
        when(jwtTokenProvider.createAccessToken(testUserId, testTenantId, roles, permissions, sessionId, null))
            .thenReturn(expectedToken);

        // Act
        String actualToken = tokenManagementService.generateAccessToken(
            testUserId, testTenantId, sessionId, roles, permissions, null);

        // Assert
        assertEquals(expectedToken, actualToken);
        verify(auditService).logTokenGeneration(testUserId, "internal", "access_token");
    }

    @Test
    void testGenerateAccessToken_UserNotFound() {
        // Arrange
        when(userCredentialsRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> tokenManagementService.generateAccessToken(testUserId, testTenantId, "session", null, null, null));
        
        assertEquals("invalid_user", exception.getError());
        assertEquals("User not found", exception.getErrorDescription());
    }

    @Test
    void testGenerateRefreshToken_WithRotation() {
        // Arrange
        String sessionId = "session-123";
        String oldRefreshToken = "old-refresh-token";
        String newRefreshToken = "new-refresh-token";

        when(jwtTokenProvider.createRefreshToken(testUserId, testTenantId, sessionId, null))
            .thenReturn(newRefreshToken);

        // Act
        String actualToken = tokenManagementService.generateRefreshToken(
            testUserId, testTenantId, sessionId, oldRefreshToken);

        // Assert
        assertEquals(newRefreshToken, actualToken);
        verify(auditService).logTokenRefresh(testUserId, "internal");
    }

    @Test
    void testValidateToken_ValidToken() {
        // Arrange
        String token = "valid-token";
        String jti = "jti-123";

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(token)).thenReturn(jti);
        when(tokenBlacklistRepository.existsByJti(jti)).thenReturn(false);

        // Act
        boolean isValid = tokenManagementService.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_BlacklistedToken() {
        // Arrange
        String token = "blacklisted-token";
        String jti = "jti-123";

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(token)).thenReturn(jti);
        when(tokenBlacklistRepository.existsByJti(jti)).thenReturn(true);

        // Act
        boolean isValid = tokenManagementService.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Arrange
        String token = "invalid-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // Act
        boolean isValid = tokenManagementService.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testRevokeToken_Success() {
        // Arrange
        String token = "token-to-revoke";
        String reason = "user_requested";
        UUID revokedBy = UUID.randomUUID();

        Claims claims = new DefaultClaims();
        claims.setId("jti-123");
        claims.setSubject(testUserId.toString());
        claims.put("tenant_id", testTenantId.toString());
        claims.put("type", "access");
        claims.setExpiration(new Date(System.currentTimeMillis() + 3600000)); // 1 hour from now

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.parseToken(token)).thenReturn(claims);
        when(tokenBlacklistRepository.save(any(TokenBlacklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        tokenManagementService.revokeToken(token, reason, revokedBy);

        // Assert
        verify(tokenBlacklistRepository).save(any(TokenBlacklist.class));
        verify(auditService).logTokenRevocation(testUserId, "internal", token);
    }

    @Test
    void testIntrospectToken_ActiveToken() {
        // Arrange
        String token = "active-token";
        String jti = "jti-123";

        Claims claims = new DefaultClaims();
        claims.setId(jti);
        claims.setSubject(testUserId.toString());
        claims.put("tenant_id", testTenantId.toString());
        claims.put("type", "access");
        claims.put("scope", "read write");
        claims.setIssuer("crm-platform");
        claims.setAudience("crm-services");
        claims.setIssuedAt(new Date());
        claims.setExpiration(new Date(System.currentTimeMillis() + 3600000));
        claims.setNotBefore(new Date());

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(token)).thenReturn(jti);
        when(tokenBlacklistRepository.existsByJti(jti)).thenReturn(false);
        when(jwtTokenProvider.parseToken(token)).thenReturn(claims);
        when(userCredentialsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCredentials));

        // Act
        TokenIntrospectionResponse response = tokenManagementService.introspectToken(token);

        // Assert
        assertTrue(response.isActive());
        assertEquals("internal", response.getClientId());
        assertEquals(testUserId, response.getUserId());
        assertEquals(testTenantId, response.getTenantId());
        assertEquals("access", response.getTokenType());
        assertEquals("read write", response.getScope());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getEmailVerified());
    }

    @Test
    void testIntrospectToken_InactiveToken() {
        // Arrange
        String token = "inactive-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // Act
        TokenIntrospectionResponse response = tokenManagementService.introspectToken(token);

        // Assert
        assertFalse(response.isActive());
        assertNull(response.getClientId());
        assertNull(response.getUserId());
    }

    @Test
    void testIntrospectToken_BlacklistedToken() {
        // Arrange
        String token = "blacklisted-token";
        String jti = "jti-123";

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(token)).thenReturn(jti);
        when(tokenBlacklistRepository.existsByJti(jti)).thenReturn(true);

        // Act
        TokenIntrospectionResponse response = tokenManagementService.introspectToken(token);

        // Assert
        assertFalse(response.isActive());
        assertEquals("Token has been revoked", response.getError());
    }

    @Test
    void testRotateRefreshToken_Success() {
        // Arrange
        String currentRefreshToken = "current-refresh-token";
        String sessionId = "session-123";
        String jti = "jti-123";

        Claims claims = new DefaultClaims();
        claims.setId(jti);
        claims.setSubject(testUserId.toString());
        claims.put("tenant_id", testTenantId.toString());
        claims.put("session_id", sessionId);

        when(jwtTokenProvider.validateTokenForRefresh(currentRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(currentRefreshToken)).thenReturn(jti);
        when(tokenBlacklistRepository.existsByJti(jti)).thenReturn(false);
        when(jwtTokenProvider.getUserIdFromToken(currentRefreshToken)).thenReturn(testUserId);
        when(jwtTokenProvider.getTenantIdFromToken(currentRefreshToken)).thenReturn(testTenantId);
        when(jwtTokenProvider.getSessionIdFromToken(currentRefreshToken)).thenReturn(sessionId);
        when(userCredentialsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCredentials));
        when(jwtTokenProvider.createAccessToken(eq(testUserId), eq(testTenantId), any(), any(), eq(sessionId), isNull()))
            .thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(testUserId, testTenantId, sessionId, null))
            .thenReturn("new-refresh-token");

        // Act
        Map<String, String> tokens = tokenManagementService.rotateRefreshToken(currentRefreshToken);

        // Assert
        assertEquals("new-access-token", tokens.get("access_token"));
        assertEquals("new-refresh-token", tokens.get("refresh_token"));
    }

    @Test
    void testRotateRefreshToken_InvalidToken() {
        // Arrange
        String invalidRefreshToken = "invalid-refresh-token";

        when(jwtTokenProvider.validateTokenForRefresh(invalidRefreshToken)).thenReturn(false);

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> tokenManagementService.rotateRefreshToken(invalidRefreshToken));
        
        assertEquals("invalid_grant", exception.getError());
        assertEquals("Invalid refresh token", exception.getErrorDescription());
    }

    @Test
    void testRotateRefreshToken_BlacklistedToken() {
        // Arrange
        String blacklistedRefreshToken = "blacklisted-refresh-token";
        String jti = "jti-123";

        when(jwtTokenProvider.validateTokenForRefresh(blacklistedRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(blacklistedRefreshToken)).thenReturn(jti);
        when(tokenBlacklistRepository.existsByJti(jti)).thenReturn(true);

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> tokenManagementService.rotateRefreshToken(blacklistedRefreshToken));
        
        assertEquals("invalid_grant", exception.getError());
        assertEquals("Refresh token has been revoked", exception.getErrorDescription());
    }

    @Test
    void testGetTokenStatistics() {
        // Arrange
        when(tokenBlacklistRepository.count()).thenReturn(10L);
        when(userSessionRepository.countActiveSessionsByTenant(testTenantId)).thenReturn(25L);

        // Act
        Map<String, Object> stats = tokenManagementService.getTokenStatistics(testTenantId);

        // Assert
        assertEquals(10L, stats.get("blacklisted_tokens"));
        assertEquals(25L, stats.get("active_sessions"));
        assertNotNull(stats.get("timestamp"));
    }

    @Test
    void testCleanupExpiredBlacklistedTokens() {
        // Arrange
        when(tokenBlacklistRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(5);

        // Act
        tokenManagementService.cleanupExpiredBlacklistedTokens();

        // Assert
        verify(tokenBlacklistRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }
}