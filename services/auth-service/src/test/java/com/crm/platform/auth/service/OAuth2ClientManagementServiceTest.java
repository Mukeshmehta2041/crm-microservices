package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.OAuth2ClientRequest;
import com.crm.platform.auth.dto.OAuth2ClientResponse;
import com.crm.platform.auth.dto.OAuth2ClientUpdateRequest;
import com.crm.platform.auth.entity.OAuth2Client;
import com.crm.platform.auth.exception.OAuth2Exception;
import com.crm.platform.auth.repository.OAuth2AccessTokenRepository;
import com.crm.platform.auth.repository.OAuth2ClientRepository;
import com.crm.platform.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientManagementServiceTest {

    @Mock
    private OAuth2ClientRepository clientRepository;

    @Mock
    private OAuth2AccessTokenRepository accessTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityAuditService auditService;

    @InjectMocks
    private OAuth2ClientManagementService clientManagementService;

    private UUID testTenantId;
    private UUID testUserId;
    private OAuth2ClientRequest validClientRequest;
    private OAuth2Client testClient;

    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        // Create valid client request
        validClientRequest = new OAuth2ClientRequest();
        validClientRequest.setName("Test Client");
        validClientRequest.setDescription("Test OAuth2 Client");
        validClientRequest.setRedirectUris(Set.of("https://example.com/callback", "http://localhost:3000/callback"));
        validClientRequest.setScopes(Set.of("read", "write"));
        validClientRequest.setGrantTypes(Set.of("authorization_code", "client_credentials"));
        validClientRequest.setAccessTokenValiditySeconds(3600);
        validClientRequest.setRefreshTokenValiditySeconds(86400);
        validClientRequest.setAutoApprove(false);

        // Create test client entity
        testClient = new OAuth2Client("test-client-id", "hashed-secret", "Test Client", testTenantId);
        testClient.setDescription("Test OAuth2 Client");
        testClient.setRedirectUris(Set.of("https://example.com/callback"));
        testClient.setScopes(Set.of("read", "write"));
        testClient.setGrantTypes(Set.of(OAuth2Client.GrantType.AUTHORIZATION_CODE, OAuth2Client.GrantType.CLIENT_CREDENTIALS));
        testClient.setIsActive(true);
    }

    @Test
    void testRegisterClient_Success() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-secret");
        when(clientRepository.save(any(OAuth2Client.class))).thenAnswer(invocation -> {
            OAuth2Client client = invocation.getArgument(0);
            client.setCreatedAt(java.time.LocalDateTime.now());
            client.setUpdatedAt(java.time.LocalDateTime.now());
            return client;
        });

        // Act
        OAuth2ClientResponse response = clientManagementService.registerClient(validClientRequest, testTenantId, testUserId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret()); // Should be returned on creation
        assertEquals("Test Client", response.getName());
        assertEquals("Test OAuth2 Client", response.getDescription());
        assertEquals(validClientRequest.getRedirectUris(), response.getRedirectUris());
        assertEquals(validClientRequest.getScopes(), response.getScopes());
        assertEquals(testTenantId, response.getTenantId());
        assertTrue(response.getIsActive());

        verify(clientRepository).save(any(OAuth2Client.class));
        verify(auditService).logSecurityEvent(eq(testUserId), eq(testTenantId), eq("OAUTH2_CLIENT_REGISTERED"), 
            anyString(), any(), isNull(), isNull(), isNull());
    }

    @Test
    void testRegisterClient_InvalidRedirectUri() {
        // Arrange
        validClientRequest.setRedirectUris(Set.of("invalid-uri"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> clientManagementService.registerClient(validClientRequest, testTenantId, testUserId));
        
        assertTrue(exception.getMessage().contains("Invalid redirect URI format"));
    }

    @Test
    void testRegisterClient_HttpRedirectUriNotLocalhost() {
        // Arrange
        validClientRequest.setRedirectUris(Set.of("http://example.com/callback"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> clientManagementService.registerClient(validClientRequest, testTenantId, testUserId));
        
        assertTrue(exception.getMessage().contains("HTTP redirect URIs are only allowed for localhost"));
    }

    @Test
    void testRegisterClient_UnsupportedScope() {
        // Arrange
        validClientRequest.setScopes(Set.of("invalid-scope"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> clientManagementService.registerClient(validClientRequest, testTenantId, testUserId));
        
        assertTrue(exception.getMessage().contains("Unsupported scope"));
    }

    @Test
    void testRegisterClient_UnsupportedGrantType() {
        // Arrange
        validClientRequest.setGrantTypes(Set.of("invalid-grant-type"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> clientManagementService.registerClient(validClientRequest, testTenantId, testUserId));
        
        assertTrue(exception.getMessage().contains("Unsupported grant type"));
    }

    @Test
    void testGetClient_Success() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));
        when(accessTokenRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Collections.emptyList());

        // Act
        OAuth2ClientResponse response = clientManagementService.getClient("test-client-id", testTenantId);

        // Assert
        assertNotNull(response);
        assertEquals("test-client-id", response.getClientId());
        assertNull(response.getClientSecret()); // Should not be returned on retrieval
        assertEquals("Test Client", response.getName());
        assertEquals(testTenantId, response.getTenantId());
    }

    @Test
    void testGetClient_NotFound() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("non-existent", testTenantId))
            .thenReturn(Optional.empty());

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> clientManagementService.getClient("non-existent", testTenantId));
        
        assertEquals("invalid_client", exception.getError());
        assertEquals("Client not found", exception.getErrorDescription());
    }

    @Test
    void testUpdateClient_Success() {
        // Arrange
        OAuth2ClientUpdateRequest updateRequest = new OAuth2ClientUpdateRequest();
        updateRequest.setName("Updated Client Name");
        updateRequest.setDescription("Updated description");
        updateRequest.setIsActive(false);

        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));
        when(clientRepository.save(any(OAuth2Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OAuth2ClientResponse response = clientManagementService.updateClient(
            "test-client-id", updateRequest, testTenantId, testUserId);

        // Assert
        assertNotNull(response);
        assertEquals("Updated Client Name", response.getName());
        assertEquals("Updated description", response.getDescription());
        assertFalse(response.getIsActive());

        verify(clientRepository).save(any(OAuth2Client.class));
        verify(auditService).logSecurityEvent(eq(testUserId), eq(testTenantId), eq("OAUTH2_CLIENT_UPDATED"), 
            anyString(), any(), isNull(), isNull(), isNull());
    }

    @Test
    void testUpdateClient_NotFound() {
        // Arrange
        OAuth2ClientUpdateRequest updateRequest = new OAuth2ClientUpdateRequest();
        updateRequest.setName("Updated Name");

        when(clientRepository.findByClientIdAndTenantId("non-existent", testTenantId))
            .thenReturn(Optional.empty());

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> clientManagementService.updateClient("non-existent", updateRequest, testTenantId, testUserId));
        
        assertEquals("invalid_client", exception.getError());
    }

    @Test
    void testDeleteClient_Success() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));
        when(accessTokenRepository.countActiveTokensForUser(isNull(), eq(testTenantId), any()))
            .thenReturn(0L);

        // Act
        clientManagementService.deleteClient("test-client-id", testTenantId, testUserId);

        // Assert
        verify(clientRepository).delete(testClient);
        verify(auditService).logSecurityEvent(eq(testUserId), eq(testTenantId), eq("OAUTH2_CLIENT_DELETED"), 
            anyString(), any(), isNull(), isNull(), isNull());
    }

    @Test
    void testDeleteClient_HasActiveTokens() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));
        when(accessTokenRepository.countActiveTokensForUser(isNull(), eq(testTenantId), any()))
            .thenReturn(5L);

        // Act & Assert
        OAuth2Exception exception = assertThrows(OAuth2Exception.class, 
            () -> clientManagementService.deleteClient("test-client-id", testTenantId, testUserId));
        
        assertEquals("client_in_use", exception.getError());
        assertTrue(exception.getErrorDescription().contains("active tokens"));
    }

    @Test
    void testListClients_Success() {
        // Arrange
        List<OAuth2Client> clients = Arrays.asList(testClient);
        when(clientRepository.findByTenantId(testTenantId)).thenReturn(clients);
        when(accessTokenRepository.findByClientIdAndTenantId(anyString(), eq(testTenantId)))
            .thenReturn(Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<OAuth2ClientResponse> result = clientManagementService.listClients(testTenantId, pageable, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("test-client-id", result.getContent().get(0).getClientId());
    }

    @Test
    void testListClients_WithNameFilter() {
        // Arrange
        List<OAuth2Client> clients = Arrays.asList(testClient);
        when(clientRepository.findByTenantIdAndNameContainingIgnoreCase(testTenantId, "Test"))
            .thenReturn(clients);
        when(accessTokenRepository.findByClientIdAndTenantId(anyString(), eq(testTenantId)))
            .thenReturn(Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<OAuth2ClientResponse> result = clientManagementService.listClients(testTenantId, pageable, "Test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(clientRepository).findByTenantIdAndNameContainingIgnoreCase(testTenantId, "Test");
    }

    @Test
    void testRegenerateClientSecret_Success() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hashed-secret");
        when(clientRepository.save(any(OAuth2Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OAuth2ClientResponse response = clientManagementService.regenerateClientSecret(
            "test-client-id", testTenantId, testUserId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getClientSecret()); // New secret should be returned
        assertEquals("test-client-id", response.getClientId());

        verify(clientRepository).save(any(OAuth2Client.class));
        verify(accessTokenRepository).revokeAllTokensForUser(isNull(), eq(testTenantId), any());
        verify(auditService).logSecurityEvent(eq(testUserId), eq(testTenantId), eq("OAUTH2_CLIENT_SECRET_REGENERATED"), 
            anyString(), any(), isNull(), isNull(), isNull());
    }

    @Test
    void testValidateClientCredentials_Success() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("client-secret", "hashed-secret")).thenReturn(true);

        // Act
        boolean isValid = clientManagementService.validateClientCredentials(
            "test-client-id", "client-secret", testTenantId);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateClientCredentials_InvalidSecret() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches("wrong-secret", "hashed-secret")).thenReturn(false);

        // Act
        boolean isValid = clientManagementService.validateClientCredentials(
            "test-client-id", "wrong-secret", testTenantId);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateClientCredentials_ClientNotFound() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("non-existent", testTenantId))
            .thenReturn(Optional.empty());

        // Act
        boolean isValid = clientManagementService.validateClientCredentials(
            "non-existent", "any-secret", testTenantId);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateClientCredentials_InactiveClient() {
        // Arrange
        testClient.setIsActive(false);
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));

        // Act
        boolean isValid = clientManagementService.validateClientCredentials(
            "test-client-id", "client-secret", testTenantId);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateRedirectUri_Success() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));

        // Act
        boolean isValid = clientManagementService.validateRedirectUri(
            "test-client-id", "https://example.com/callback", testTenantId);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateRedirectUri_Invalid() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));

        // Act
        boolean isValid = clientManagementService.validateRedirectUri(
            "test-client-id", "https://malicious.com/callback", testTenantId);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateScope_Success() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));

        // Act
        boolean isValid = clientManagementService.validateScope("test-client-id", "read", testTenantId);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateScope_Invalid() {
        // Arrange
        when(clientRepository.findByClientIdAndTenantId("test-client-id", testTenantId))
            .thenReturn(Optional.of(testClient));

        // Act
        boolean isValid = clientManagementService.validateScope("test-client-id", "admin", testTenantId);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testGetAvailableScopes() {
        // Act
        Set<String> scopes = clientManagementService.getAvailableScopes();

        // Assert
        assertNotNull(scopes);
        assertTrue(scopes.contains("read"));
        assertTrue(scopes.contains("write"));
        assertTrue(scopes.contains("admin"));
        assertTrue(scopes.contains("profile"));
        assertTrue(scopes.contains("email"));
        assertTrue(scopes.contains("openid"));
    }

    @Test
    void testGetSupportedGrantTypes() {
        // Act
        Set<String> grantTypes = clientManagementService.getSupportedGrantTypes();

        // Assert
        assertNotNull(grantTypes);
        assertTrue(grantTypes.contains("authorization_code"));
        assertTrue(grantTypes.contains("client_credentials"));
        assertTrue(grantTypes.contains("refresh_token"));
        assertTrue(grantTypes.contains("implicit"));
        assertTrue(grantTypes.contains("password"));
    }
}