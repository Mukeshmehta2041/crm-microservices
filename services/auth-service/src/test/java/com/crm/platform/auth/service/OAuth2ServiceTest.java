package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.OAuth2TokenRequest;
import com.crm.platform.auth.dto.OAuth2TokenResponse;
import com.crm.platform.auth.entity.OAuth2Client;
import com.crm.platform.auth.entity.OAuth2AccessToken;
import com.crm.platform.auth.exception.OAuth2Exception;
import com.crm.platform.auth.repository.OAuth2ClientRepository;
import com.crm.platform.auth.repository.OAuth2AccessTokenRepository;
import com.crm.platform.auth.repository.OAuth2AuthorizationCodeRepository;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ServiceTest {

    @Mock
    private OAuth2ClientRepository clientRepository;

    @Mock
    private OAuth2AuthorizationCodeRepository authCodeRepository;

    @Mock
    private OAuth2AccessTokenRepository accessTokenRepository;

    @Mock
    private UserCredentialsRepository userCredentialsRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityAuditService auditService;

    @InjectMocks
    private OAuth2Service oauth2Service;

    private OAuth2Client testClient;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testClient = new OAuth2Client("test-client", "hashed-secret", "Test Client", testTenantId);
        testClient.setScopes(Set.of("read", "write"));
        testClient.setGrantTypes(Set.of(OAuth2Client.GrantType.CLIENT_CREDENTIALS, OAuth2Client.GrantType.AUTHORIZATION_CODE));
        testClient.setIsActive(true);
    }

    @Test
    void testClientCredentialsGrant_Success() {
        // Arrange
        OAuth2TokenRequest request = new OAuth2TokenRequest();
        request.setGrantType("client_credentials");
        request.setClientId("test-client");
        request.setClientSecret("client-secret");
        request.setScope("read write");

        when(clientRepository.findActiveByClientId("test-client")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "hashed-secret")).thenReturn(true);
        when(accessTokenRepository.save(any(OAuth2AccessToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OAuth2TokenResponse response = oauth2Service.exchangeToken(request, null);

        // Assert
        assertNotNull(response);
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertNotNull(response.getAccessToken());
        assertNull(response.getRefreshToken()); // No refresh token for client credentials
        assertEquals("read write", response.getScope());

        verify(accessTokenRepository).save(any(OAuth2AccessToken.class));
        verify(auditService).logTokenGeneration(isNull(), eq("test-client"), eq("client_credentials"));
    }

    @Test
    void testClientCredentialsGrant_InvalidClient() {
        // Arrange
        OAuth2TokenRequest request = new OAuth2TokenRequest();
        request.setGrantType("client_credentials");
        request.setClientId("invalid-client");
        request.setClientSecret("client-secret");

        when(clientRepository.findActiveByClientId("invalid-client")).thenReturn(Optional.empty());

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> oauth2Service.exchangeToken(request, null));
        
        assertEquals("invalid_client", exception.getError());
        assertEquals("Client not found or inactive", exception.getErrorDescription());
    }

    @Test
    void testClientCredentialsGrant_InvalidCredentials() {
        // Arrange
        OAuth2TokenRequest request = new OAuth2TokenRequest();
        request.setGrantType("client_credentials");
        request.setClientId("test-client");
        request.setClientSecret("wrong-secret");

        when(clientRepository.findActiveByClientId("test-client")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("wrong-secret", "hashed-secret")).thenReturn(false);

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> oauth2Service.exchangeToken(request, null));
        
        assertEquals("invalid_client", exception.getError());
        assertEquals("Invalid client credentials", exception.getErrorDescription());
    }

    @Test
    void testClientCredentialsGrant_UnsupportedGrantType() {
        // Arrange
        OAuth2Client clientWithoutCC = new OAuth2Client("test-client", "hashed-secret", "Test Client", testTenantId);
        clientWithoutCC.setGrantTypes(Set.of(OAuth2Client.GrantType.AUTHORIZATION_CODE)); // No client credentials
        clientWithoutCC.setIsActive(true);

        OAuth2TokenRequest request = new OAuth2TokenRequest();
        request.setGrantType("client_credentials");
        request.setClientId("test-client");
        request.setClientSecret("client-secret");

        when(clientRepository.findActiveByClientId("test-client")).thenReturn(Optional.of(clientWithoutCC));
        when(passwordEncoder.matches("client-secret", "hashed-secret")).thenReturn(true);

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> oauth2Service.exchangeToken(request, null));
        
        assertEquals("unauthorized_client", exception.getError());
        assertEquals("Client not authorized for client credentials grant", exception.getErrorDescription());
    }

    @Test
    void testClientCredentialsGrant_InvalidScope() {
        // Arrange
        OAuth2TokenRequest request = new OAuth2TokenRequest();
        request.setGrantType("client_credentials");
        request.setClientId("test-client");
        request.setClientSecret("client-secret");
        request.setScope("invalid-scope");

        when(clientRepository.findActiveByClientId("test-client")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "hashed-secret")).thenReturn(true);

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> oauth2Service.exchangeToken(request, null));
        
        assertEquals("invalid_scope", exception.getError());
        assertTrue(exception.getErrorDescription().contains("Requested scope not allowed"));
    }

    @Test
    void testUnsupportedGrantType() {
        // Arrange
        OAuth2TokenRequest request = new OAuth2TokenRequest();
        request.setGrantType("password");
        request.setClientId("test-client");
        request.setClientSecret("client-secret");

        when(clientRepository.findActiveByClientId("test-client")).thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "hashed-secret")).thenReturn(true);

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> oauth2Service.exchangeToken(request, null));
        
        assertEquals("unsupported_grant_type", exception.getError());
        assertEquals("Grant type not supported", exception.getErrorDescription());
    }
}