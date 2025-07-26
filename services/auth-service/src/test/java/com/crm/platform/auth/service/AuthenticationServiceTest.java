package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.LoginRequest;
import com.crm.platform.auth.dto.LoginResponse;
import com.crm.platform.auth.entity.User;
import com.crm.platform.auth.exception.InvalidCredentialsException;
import com.crm.platform.auth.repository.UserRepository;
import com.crm.platform.auth.repository.UserSessionRepository;
import com.crm.platform.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SecurityAuditService auditService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private HttpServletRequest httpRequest;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
            userRepository, sessionRepository, passwordEncoder, 
            jwtTokenProvider, auditService, rateLimitingService
        );
    }

    @Test
    void authenticate_ValidCredentials_ReturnsLoginResponse() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        User user = createTestUser();
        
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test Agent");
        when(rateLimitingService.isAllowed(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any(UUID.class), any(UUID.class), anyList(), anyList())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(any(UUID.class), any(UUID.class))).thenReturn("refresh-token");

        // Act
        LoginResponse response = authenticationService.authenticate(request, httpRequest);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        assertEquals(user.getId(), response.getUser().getId());
        
        verify(sessionRepository).save(any());
        verify(userRepository).updateLastLoginTime(eq(user.getId()), any());
        verify(auditService).logSecurityEvent(any(), any(), anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void authenticate_InvalidCredentials_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        User user = createTestUser();
        
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test Agent");
        when(rateLimitingService.isAllowed(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authenticationService.authenticate(request, httpRequest);
        });
        
        verify(userRepository).updateFailedLoginAttempts(eq(user.getId()), eq(1));
    }

    @Test
    void authenticate_UserNotFound_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent", "password123");
        
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test Agent");
        when(rateLimitingService.isAllowed(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authenticationService.authenticate(request, httpRequest);
        });
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setTenantId(UUID.randomUUID());
        user.setStatus(User.UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        return user;
    }
}