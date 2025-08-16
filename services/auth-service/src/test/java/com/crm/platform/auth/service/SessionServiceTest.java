package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.SessionInfo;
import com.crm.platform.auth.dto.SessionValidationRequest;
import com.crm.platform.auth.entity.UserSession;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.auth.repository.UserSessionRepository;
import com.crm.platform.security.jwt.JwtTokenProvider;
import com.crm.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
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
class SessionServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private UserCredentialsRepository userCredentialsRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SecurityAuditService auditService;

    @Mock
    private DeviceTrustService deviceTrustService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private SessionService sessionService;

    private UUID testUserId;
    private UUID testTenantId;
    private UserSession testSession;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();

        // Set configuration values
        ReflectionTestUtils.setField(sessionService, "defaultSessionExpiryHours", 24);
        ReflectionTestUtils.setField(sessionService, "refreshTokenExpiryDays", 30);
        ReflectionTestUtils.setField(sessionService, "enableDeviceTracking", true);
        ReflectionTestUtils.setField(sessionService, "enableLocationTracking", true);

        // Create test session
        testSession = new UserSession();
        testSession.setId(UUID.randomUUID());
        testSession.setUserId(testUserId);
        testSession.setTenantId(testTenantId);
        testSession.setTokenId("test-session-token");
        testSession.setRefreshToken("test-refresh-token");
        testSession.setStatus(UserSession.SessionStatus.ACTIVE);
        testSession.setIpAddress("192.168.1.1");
        testSession.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        testSession.setDeviceType("Desktop");
        testSession.setBrowser("Chrome");
        testSession.setOperatingSystem("Windows");
        testSession.setLocation("Test Location");
        testSession.setExpiresAt(LocalDateTime.now().plusHours(24));
        testSession.setRefreshExpiresAt(LocalDateTime.now().plusDays(30));
        testSession.setCreatedAt(LocalDateTime.now());
        testSession.setLastAccessedAt(LocalDateTime.now());
    }

    @Test
    void testCreateSession_Success() {
        // Arrange
        when(sessionRepository.countActiveSessionsByUser(testUserId)).thenReturn(5L);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> {
            UserSession session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            session.setCreatedAt(LocalDateTime.now());
            return session;
        });

        // Act
        UserSession result = sessionService.createSession(testUserId, testTenantId, httpServletRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testTenantId, result.getTenantId());
        assertNotNull(result.getTokenId());
        assertNotNull(result.getRefreshToken());
        assertEquals("Desktop", result.getDeviceType());
        assertEquals("Chrome", result.getBrowser());
        assertEquals("Windows", result.getOperatingSystem());
        assertEquals("192.168.1.1", result.getIpAddress());

        verify(sessionRepository).save(any(UserSession.class));
        verify(auditService).logSecurityEvent(eq(testUserId), eq(testTenantId), eq("SESSION_CREATED"), 
            anyString(), any(), eq("192.168.1.1"), anyString(), anyString());
    }

    @Test
    void testCreateSession_EnforceSessionLimits() {
        // Arrange
        when(sessionRepository.countActiveSessionsByUser(testUserId)).thenReturn(10L); // At limit
        
        UserSession oldestSession = new UserSession();
        oldestSession.setCreatedAt(LocalDateTime.now().minusDays(1));
        oldestSession.setStatus(UserSession.SessionStatus.ACTIVE);
        
        when(sessionRepository.findByUserIdAndStatus(testUserId, UserSession.SessionStatus.ACTIVE))
            .thenReturn(Arrays.asList(oldestSession));
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserSession result = sessionService.createSession(testUserId, testTenantId, httpServletRequest);

        // Assert
        assertNotNull(result);
        verify(sessionRepository, times(2)).save(any(UserSession.class)); // Once for expiring old, once for new
    }

    @Test
    void testValidateSession_ValidSession() {
        // Arrange
        SessionValidationRequest request = new SessionValidationRequest();
        request.setSessionId("test-session-token");

        when(sessionRepository.findByTokenId("test-session-token")).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> result = sessionService.validateSession(request);

        // Assert
        assertTrue((Boolean) result.get("valid"));
        assertEquals(testUserId, result.get("user_id"));
        assertEquals(testTenantId, result.get("tenant_id"));
        assertEquals("Desktop", result.get("device_type"));
        assertNotNull(result.get("expires_at"));

        verify(sessionRepository).save(testSession); // Should update last accessed time
    }

    @Test
    void testValidateSession_SessionNotFound() {
        // Arrange
        SessionValidationRequest request = new SessionValidationRequest();
        request.setSessionId("non-existent-session");

        when(sessionRepository.findByTokenId("non-existent-session")).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = sessionService.validateSession(request);

        // Assert
        assertFalse((Boolean) result.get("valid"));
        assertEquals("Session not found", result.get("reason"));
    }

    @Test
    void testValidateSession_InactiveSession() {
        // Arrange
        testSession.setStatus(UserSession.SessionStatus.EXPIRED);
        SessionValidationRequest request = new SessionValidationRequest();
        request.setSessionId("test-session-token");

        when(sessionRepository.findByTokenId("test-session-token")).thenReturn(Optional.of(testSession));

        // Act
        Map<String, Object> result = sessionService.validateSession(request);

        // Assert
        assertFalse((Boolean) result.get("valid"));
        assertEquals("Session is not active", result.get("reason"));
        assertEquals("EXPIRED", result.get("status"));
    }

    @Test
    void testRenewSession_Success() {
        // Arrange
        when(sessionRepository.findByTokenId("test-session-token")).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime originalExpiry = testSession.getExpiresAt();

        // Act
        UserSession result = sessionService.renewSession("test-session-token");

        // Assert
        assertNotNull(result);
        assertTrue(result.getExpiresAt().isAfter(originalExpiry));
        assertNotNull(result.getLastAccessedAt());

        verify(sessionRepository).save(testSession);
        verify(auditService).logSecurityEvent(eq(testUserId), eq(testTenantId), eq("SESSION_RENEWED"), 
            anyString(), any(), anyString(), anyString(), eq("test-session-token"));
    }

    @Test
    void testRenewSession_SessionNotFound() {
        // Arrange
        when(sessionRepository.findByTokenId("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> sessionService.renewSession("non-existent"));
        
        assertEquals("Session not found", exception.getMessage());
    }

    @Test
    void testRenewSession_InactiveSession() {
        // Arrange
        testSession.setStatus(UserSession.SessionStatus.EXPIRED);
        when(sessionRepository.findByTokenId("test-session-token")).thenReturn(Optional.of(testSession));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> sessionService.renewSession("test-session-token"));
        
        assertEquals("Cannot renew inactive session", exception.getMessage());
    }

    @Test
    void testTerminateSession_Success() {
        // Arrange
        String authorization = "Bearer test-jwt-token";
        when(jwtTokenProvider.getUserIdFromToken("test-jwt-token")).thenReturn(testUserId);
        when(sessionRepository.findByTokenId("test-session-token")).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        // Act
        Map<String, Object> result = sessionService.terminateSession(authorization, "test-session-token", httpServletRequest);

        // Assert
        assertTrue((Boolean) result.get("terminated"));
        assertEquals("test-session-token", result.get("session_id"));
        assertNotNull(result.get("terminated_at"));
        assertEquals(UserSession.SessionStatus.LOGGED_OUT, testSession.getStatus());

        verify(sessionRepository).save(testSession);
        verify(auditService).logSecurityEvent(eq(testUserId), eq(testTenantId), eq("SESSION_TERMINATED"), 
            anyString(), any(), eq("192.168.1.1"), eq("Mozilla/5.0"), eq("test-session-token"));
    }

    @Test
    void testTerminateSession_UnauthorizedUser() {
        // Arrange
        String authorization = "Bearer test-jwt-token";
        UUID differentUserId = UUID.randomUUID();
        
        when(jwtTokenProvider.getUserIdFromToken("test-jwt-token")).thenReturn(differentUserId);
        when(sessionRepository.findByTokenId("test-session-token")).thenReturn(Optional.of(testSession));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> sessionService.terminateSession(authorization, "test-session-token", httpServletRequest));
        
        assertTrue(exception.getMessage().contains("Access denied"));
    }

    @Test
    void testTerminateAllSessions_Success() {
        // Arrange
        String authorization = "Bearer test-jwt-token";
        String currentSessionId = "current-session";
        
        UserSession session1 = new UserSession();
        session1.setTokenId("session-1");
        session1.setUserId(testUserId);
        session1.setStatus(UserSession.SessionStatus.ACTIVE);
        
        UserSession session2 = new UserSession();
        session2.setTokenId("session-2");
        session2.setUserId(testUserId);
        session2.setStatus(UserSession.SessionStatus.ACTIVE);
        
        UserSession currentSession = new UserSession();
        currentSession.setTokenId(currentSessionId);
        currentSession.setUserId(testUserId);
        currentSession.setStatus(UserSession.SessionStatus.ACTIVE);

        when(jwtTokenProvider.getUserIdFromToken("test-jwt-token")).thenReturn(testUserId);
        when(jwtTokenProvider.getSessionIdFromToken("test-jwt-token")).thenReturn(currentSessionId);
        when(sessionRepository.findByUserIdAndStatus(testUserId, UserSession.SessionStatus.ACTIVE))
            .thenReturn(Arrays.asList(session1, session2, currentSession));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        // Act
        Map<String, Object> result = sessionService.terminateAllSessions(authorization, httpServletRequest);

        // Assert
        assertTrue((Boolean) result.get("terminated_all"));
        assertEquals(2, result.get("terminated_count")); // Should terminate 2 out of 3 sessions
        assertTrue((Boolean) result.get("current_session_preserved"));
        
        // Verify that non-current sessions were terminated
        assertEquals(UserSession.SessionStatus.LOGGED_OUT, session1.getStatus());
        assertEquals(UserSession.SessionStatus.LOGGED_OUT, session2.getStatus());
        assertEquals(UserSession.SessionStatus.ACTIVE, currentSession.getStatus()); // Current session preserved

        verify(sessionRepository, times(2)).save(any(UserSession.class)); // Only non-current sessions saved
    }

    @Test
    void testListUserSessions_Success() {
        // Arrange
        String authorization = "Bearer test-jwt-token";
        
        UserSession session1 = new UserSession();
        session1.setTokenId("session-1");
        session1.setUserId(testUserId);
        session1.setTenantId(testTenantId);
        session1.setDeviceType("Mobile");
        session1.setBrowser("Safari");
        session1.setOperatingSystem("iOS");
        session1.setIpAddress("192.168.1.2");
        session1.setLocation("Location 1");
        session1.setStatus(UserSession.SessionStatus.ACTIVE);
        session1.setCreatedAt(LocalDateTime.now().minusHours(1));
        session1.setLastAccessedAt(LocalDateTime.now().minusMinutes(30));
        session1.setExpiresAt(LocalDateTime.now().plusHours(23));

        when(jwtTokenProvider.getUserIdFromToken("test-jwt-token")).thenReturn(testUserId);
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
            .thenReturn(Arrays.asList(testSession, session1));

        // Act
        List<SessionInfo> result = sessionService.listUserSessions(authorization);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        SessionInfo firstSession = result.get(0);
        assertEquals("test-session-token", firstSession.getSessionId());
        assertEquals(testUserId, firstSession.getUserId());
        assertEquals(testTenantId, firstSession.getTenantId());
        assertEquals("Desktop", firstSession.getDeviceType());
        assertEquals("Chrome", firstSession.getBrowser());
        assertEquals("Windows", firstSession.getOperatingSystem());
        
        SessionInfo secondSession = result.get(1);
        assertEquals("session-1", secondSession.getSessionId());
        assertEquals("Mobile", secondSession.getDeviceType());
        assertEquals("Safari", secondSession.getBrowser());
        assertEquals("iOS", secondSession.getOperatingSystem());
    }

    @Test
    void testGetCurrentSession_Success() {
        // Arrange
        String authorization = "Bearer test-jwt-token";
        
        when(jwtTokenProvider.getSessionIdFromToken("test-jwt-token")).thenReturn("test-session-token");
        when(sessionRepository.findByTokenId("test-session-token")).thenReturn(Optional.of(testSession));

        // Act
        SessionInfo result = sessionService.getCurrentSession(authorization);

        // Assert
        assertNotNull(result);
        assertEquals("test-session-token", result.getSessionId());
        assertEquals(testUserId, result.getUserId());
        assertEquals(testTenantId, result.getTenantId());
        assertEquals("Desktop", result.getDeviceType());
        assertEquals("Chrome", result.getBrowser());
        assertEquals("Windows", result.getOperatingSystem());
        assertEquals("192.168.1.1", result.getIpAddress());
        assertEquals("Test Location", result.getLocation());
    }

    @Test
    void testGetCurrentSession_SessionNotFound() {
        // Arrange
        String authorization = "Bearer test-jwt-token";
        
        when(jwtTokenProvider.getSessionIdFromToken("test-jwt-token")).thenReturn("non-existent-session");
        when(sessionRepository.findByTokenId("non-existent-session")).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> sessionService.getCurrentSession(authorization));
        
        assertEquals("Current session not found", exception.getMessage());
    }

    @Test
    void testCleanupExpiredSessions() {
        // Arrange
        // No specific mocking needed as this method primarily calls repository methods

        // Act
        sessionService.cleanupExpiredSessions();

        // Assert
        verify(sessionRepository).expireOldSessions(any(LocalDateTime.class));
        verify(sessionRepository).deleteByStatusAndCreatedAtBefore(
            eq(UserSession.SessionStatus.EXPIRED), any(LocalDateTime.class));
    }

    @Test
    void testDeviceTypeDetection() {
        // Test mobile detection
        when(httpServletRequest.getHeader("User-Agent"))
            .thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)");
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(sessionRepository.countActiveSessionsByUser(testUserId)).thenReturn(0L);
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSession result = sessionService.createSession(testUserId, testTenantId, httpServletRequest);
        assertEquals("Mobile", result.getDeviceType());
        assertEquals("Safari", result.getBrowser());
        assertEquals("iOS", result.getOperatingSystem());
    }

    @Test
    void testBrowserDetection() {
        // Test Chrome detection
        when(httpServletRequest.getHeader("User-Agent"))
            .thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(sessionRepository.countActiveSessionsByUser(testUserId)).thenReturn(0L);
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSession result = sessionService.createSession(testUserId, testTenantId, httpServletRequest);
        assertEquals("Chrome", result.getBrowser());
        assertEquals("Windows", result.getOperatingSystem());
    }
}