package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.OAuth2ClientRequest;
import com.crm.platform.auth.dto.OAuth2ClientResponse;
import com.crm.platform.auth.dto.OAuth2ClientUpdateRequest;
import com.crm.platform.auth.entity.OAuth2Client;
import com.crm.platform.auth.exception.OAuth2Exception;
import com.crm.platform.auth.repository.OAuth2AccessTokenRepository;
import com.crm.platform.auth.repository.OAuth2ClientRepository;
import com.crm.platform.common.exception.BusinessException;
import com.crm.platform.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth2 client management service providing client registration, authentication,
 * redirect URI validation, scope management, and client credentials management.
 */
@Service
@Transactional
public class OAuth2ClientManagementService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientManagementService.class);

    private static final int CLIENT_ID_LENGTH = 32;
    private static final int CLIENT_SECRET_LENGTH = 64;
    private static final int DEFAULT_ACCESS_TOKEN_VALIDITY = 3600; // 1 hour
    private static final int DEFAULT_REFRESH_TOKEN_VALIDITY = 2592000; // 30 days

    // Supported grant types
    private static final Set<String> SUPPORTED_GRANT_TYPES = Set.of(
        "authorization_code", "client_credentials", "refresh_token", "implicit", "password"
    );

    // Default available scopes
    private static final Set<String> DEFAULT_AVAILABLE_SCOPES = Set.of(
        "read", "write", "admin", "profile", "email", "openid"
    );

    @Autowired
    private OAuth2ClientRepository clientRepository;

    @Autowired
    private OAuth2AccessTokenRepository accessTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityAuditService auditService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Register a new OAuth2 client
     */
    public OAuth2ClientResponse registerClient(OAuth2ClientRequest request, UUID tenantId, UUID createdBy) {
        try {
            // Validate request
            validateClientRequest(request);

            // Generate client credentials
            String clientId = generateClientId();
            String clientSecret = generateClientSecret();
            String hashedSecret = passwordEncoder.encode(clientSecret);

            // Convert grant types from strings to enums
            Set<OAuth2Client.GrantType> grantTypes = convertGrantTypes(request.getGrantTypes());

            // Create client entity
            OAuth2Client client = new OAuth2Client(clientId, hashedSecret, request.getName(), tenantId);
            client.setDescription(request.getDescription());
            client.setRedirectUris(request.getRedirectUris());
            client.setScopes(request.getScopes());
            client.setGrantTypes(grantTypes);
            client.setAccessTokenValiditySeconds(
                request.getAccessTokenValiditySeconds() != null ? 
                request.getAccessTokenValiditySeconds() : DEFAULT_ACCESS_TOKEN_VALIDITY
            );
            client.setRefreshTokenValiditySeconds(
                request.getRefreshTokenValiditySeconds() != null ? 
                request.getRefreshTokenValiditySeconds() : DEFAULT_REFRESH_TOKEN_VALIDITY
            );
            client.setAutoApprove(request.getAutoApprove() != null ? request.getAutoApprove() : false);

            // Save client
            OAuth2Client savedClient = clientRepository.save(client);

            // Audit log
            auditService.logSecurityEvent(createdBy, tenantId, "OAUTH2_CLIENT_REGISTERED", 
                "OAuth2 client registered: " + clientId, 
                SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);

            // Convert to response (include client secret only on creation)
            OAuth2ClientResponse response = convertToResponse(savedClient);
            response.setClientSecret(clientSecret); // Only returned on creation

            logger.info("OAuth2 client registered: {} for tenant: {}", clientId, tenantId);
            return response;

        } catch (Exception e) {
            logger.error("Error registering OAuth2 client", e);
            throw new OAuth2Exception("server_error", "Failed to register client");
        }
    }

    /**
     * Get client by ID
     */
    public OAuth2ClientResponse getClient(String clientId, UUID tenantId) {
        OAuth2Client client = clientRepository.findByClientIdAndTenantId(clientId, tenantId)
            .orElseThrow(() -> new OAuth2Exception("invalid_client", "Client not found"));

        OAuth2ClientResponse response = convertToResponse(client);
        
        // Add statistics
        addClientStatistics(response, client);
        
        return response;
    }

    /**
     * Update existing client
     */
    public OAuth2ClientResponse updateClient(String clientId, OAuth2ClientUpdateRequest request, 
                                           UUID tenantId, UUID updatedBy) {
        try {
            OAuth2Client client = clientRepository.findByClientIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new OAuth2Exception("invalid_client", "Client not found"));

            // Update fields if provided
            if (StringUtils.hasText(request.getName())) {
                client.setName(request.getName());
            }
            
            if (request.getDescription() != null) {
                client.setDescription(request.getDescription());
            }
            
            if (request.getRedirectUris() != null) {
                validateRedirectUris(request.getRedirectUris());
                client.setRedirectUris(request.getRedirectUris());
            }
            
            if (request.getScopes() != null) {
                validateScopes(request.getScopes());
                client.setScopes(request.getScopes());
            }
            
            if (request.getGrantTypes() != null) {
                validateGrantTypes(request.getGrantTypes());
                client.setGrantTypes(convertGrantTypes(request.getGrantTypes()));
            }
            
            if (request.getAccessTokenValiditySeconds() != null) {
                client.setAccessTokenValiditySeconds(request.getAccessTokenValiditySeconds());
            }
            
            if (request.getRefreshTokenValiditySeconds() != null) {
                client.setRefreshTokenValiditySeconds(request.getRefreshTokenValiditySeconds());
            }
            
            if (request.getAutoApprove() != null) {
                client.setAutoApprove(request.getAutoApprove());
            }
            
            if (request.getIsActive() != null) {
                client.setIsActive(request.getIsActive());
            }

            // Save updated client
            OAuth2Client updatedClient = clientRepository.save(client);

            // Audit log
            auditService.logSecurityEvent(updatedBy, tenantId, "OAUTH2_CLIENT_UPDATED", 
                "OAuth2 client updated: " + clientId, 
                SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);

            logger.info("OAuth2 client updated: {} for tenant: {}", clientId, tenantId);
            return convertToResponse(updatedClient);

        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating OAuth2 client: {}", clientId, e);
            throw new OAuth2Exception("server_error", "Failed to update client");
        }
    }

    /**
     * Delete client
     */
    public void deleteClient(String clientId, UUID tenantId, UUID deletedBy) {
        try {
            OAuth2Client client = clientRepository.findByClientIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new OAuth2Exception("invalid_client", "Client not found"));

            // Check if client has active tokens
            long activeTokenCount = accessTokenRepository.countActiveTokensForUser(null, tenantId, LocalDateTime.now());
            if (activeTokenCount > 0) {
                throw new OAuth2Exception("client_in_use", 
                    "Cannot delete client with active tokens. Revoke all tokens first.");
            }

            // Delete client
            clientRepository.delete(client);

            // Audit log
            auditService.logSecurityEvent(deletedBy, tenantId, "OAUTH2_CLIENT_DELETED", 
                "OAuth2 client deleted: " + clientId, 
                SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);

            logger.info("OAuth2 client deleted: {} for tenant: {}", clientId, tenantId);

        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting OAuth2 client: {}", clientId, e);
            throw new OAuth2Exception("server_error", "Failed to delete client");
        }
    }

    /**
     * List clients for tenant with pagination
     */
    public Page<OAuth2ClientResponse> listClients(UUID tenantId, Pageable pageable, String nameFilter) {
        try {
            List<OAuth2Client> clients;
            
            if (StringUtils.hasText(nameFilter)) {
                clients = clientRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, nameFilter);
            } else {
                clients = clientRepository.findByTenantId(tenantId);
            }

            // Convert to responses
            List<OAuth2ClientResponse> responses = clients.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

            // Add statistics for each client
            responses.forEach(response -> {
                OAuth2Client client = clients.stream()
                    .filter(c -> c.getClientId().equals(response.getClientId()))
                    .findFirst()
                    .orElse(null);
                if (client != null) {
                    addClientStatistics(response, client);
                }
            });

            // Apply pagination manually (in a real implementation, this would be done at the database level)
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), responses.size());
            List<OAuth2ClientResponse> pageContent = responses.subList(start, end);

            return new PageImpl<>(pageContent, pageable, responses.size());

        } catch (Exception e) {
            logger.error("Error listing OAuth2 clients for tenant: {}", tenantId, e);
            throw new OAuth2Exception("server_error", "Failed to list clients");
        }
    }

    /**
     * Regenerate client secret
     */
    public OAuth2ClientResponse regenerateClientSecret(String clientId, UUID tenantId, UUID regeneratedBy) {
        try {
            OAuth2Client client = clientRepository.findByClientIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new OAuth2Exception("invalid_client", "Client not found"));

            // Generate new secret
            String newClientSecret = generateClientSecret();
            String hashedSecret = passwordEncoder.encode(newClientSecret);
            
            client.setClientSecret(hashedSecret);
            OAuth2Client updatedClient = clientRepository.save(client);

            // Revoke all existing tokens for this client
            accessTokenRepository.revokeAllTokensForUser(null, tenantId, LocalDateTime.now());

            // Audit log
            auditService.logSecurityEvent(regeneratedBy, tenantId, "OAUTH2_CLIENT_SECRET_REGENERATED", 
                "OAuth2 client secret regenerated: " + clientId, 
                SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);

            OAuth2ClientResponse response = convertToResponse(updatedClient);
            response.setClientSecret(newClientSecret); // Return new secret

            logger.info("OAuth2 client secret regenerated: {} for tenant: {}", clientId, tenantId);
            return response;

        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error regenerating client secret for: {}", clientId, e);
            throw new OAuth2Exception("server_error", "Failed to regenerate client secret");
        }
    }

    /**
     * Validate client credentials
     */
    public boolean validateClientCredentials(String clientId, String clientSecret, UUID tenantId) {
        try {
            Optional<OAuth2Client> clientOpt = clientRepository.findByClientIdAndTenantId(clientId, tenantId);
            
            if (clientOpt.isEmpty()) {
                return false;
            }

            OAuth2Client client = clientOpt.get();
            
            // Check if client is active
            if (!client.getIsActive()) {
                return false;
            }

            // Validate secret
            return passwordEncoder.matches(clientSecret, client.getClientSecret());

        } catch (Exception e) {
            logger.error("Error validating client credentials for: {}", clientId, e);
            return false;
        }
    }

    /**
     * Validate redirect URI for client
     */
    public boolean validateRedirectUri(String clientId, String redirectUri, UUID tenantId) {
        try {
            Optional<OAuth2Client> clientOpt = clientRepository.findByClientIdAndTenantId(clientId, tenantId);
            
            if (clientOpt.isEmpty()) {
                return false;
            }

            return clientOpt.get().isValidRedirectUri(redirectUri);

        } catch (Exception e) {
            logger.error("Error validating redirect URI for client: {}", clientId, e);
            return false;
        }
    }

    /**
     * Validate scope for client
     */
    public boolean validateScope(String clientId, String scope, UUID tenantId) {
        try {
            Optional<OAuth2Client> clientOpt = clientRepository.findByClientIdAndTenantId(clientId, tenantId);
            
            if (clientOpt.isEmpty()) {
                return false;
            }

            return clientOpt.get().hasScope(scope);

        } catch (Exception e) {
            logger.error("Error validating scope for client: {}", clientId, e);
            return false;
        }
    }

    /**
     * Get available scopes
     */
    public Set<String> getAvailableScopes() {
        return new HashSet<>(DEFAULT_AVAILABLE_SCOPES);
    }

    /**
     * Get supported grant types
     */
    public Set<String> getSupportedGrantTypes() {
        return new HashSet<>(SUPPORTED_GRANT_TYPES);
    }

    // Private helper methods

    private void validateClientRequest(OAuth2ClientRequest request) {
        // Validate redirect URIs
        validateRedirectUris(request.getRedirectUris());
        
        // Validate scopes
        validateScopes(request.getScopes());
        
        // Validate grant types
        validateGrantTypes(request.getGrantTypes());
    }

    private void validateRedirectUris(Set<String> redirectUris) {
        if (redirectUris == null || redirectUris.isEmpty()) {
            throw new ValidationException("At least one redirect URI is required");
        }

        for (String uri : redirectUris) {
            try {
                URI parsedUri = new URI(uri);
                
                // Must be absolute URI
                if (!parsedUri.isAbsolute()) {
                    throw new ValidationException("Redirect URI must be absolute: " + uri);
                }
                
                // Must use HTTPS in production (allow HTTP for localhost in development)
                String scheme = parsedUri.getScheme().toLowerCase();
                if (!"https".equals(scheme) && !"http".equals(scheme)) {
                    throw new ValidationException("Redirect URI must use HTTP or HTTPS: " + uri);
                }
                
                // Validate localhost exception for HTTP
                if ("http".equals(scheme)) {
                    String host = parsedUri.getHost();
                    if (host != null && !host.equals("localhost") && !host.equals("127.0.0.1")) {
                        throw new ValidationException("HTTP redirect URIs are only allowed for localhost: " + uri);
                    }
                }
                
            } catch (URISyntaxException e) {
                throw new ValidationException("Invalid redirect URI format: " + uri);
            }
        }
    }

    private void validateScopes(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new ValidationException("At least one scope is required");
        }

        for (String scope : scopes) {
            if (!DEFAULT_AVAILABLE_SCOPES.contains(scope)) {
                throw new ValidationException("Unsupported scope: " + scope);
            }
        }
    }

    private void validateGrantTypes(Set<String> grantTypes) {
        if (grantTypes == null || grantTypes.isEmpty()) {
            throw new ValidationException("At least one grant type is required");
        }

        for (String grantType : grantTypes) {
            if (!SUPPORTED_GRANT_TYPES.contains(grantType)) {
                throw new ValidationException("Unsupported grant type: " + grantType);
            }
        }
    }

    private Set<OAuth2Client.GrantType> convertGrantTypes(Set<String> grantTypeStrings) {
        return grantTypeStrings.stream()
            .map(this::convertGrantType)
            .collect(Collectors.toSet());
    }

    private OAuth2Client.GrantType convertGrantType(String grantType) {
        switch (grantType.toLowerCase()) {
            case "authorization_code": return OAuth2Client.GrantType.AUTHORIZATION_CODE;
            case "client_credentials": return OAuth2Client.GrantType.CLIENT_CREDENTIALS;
            case "refresh_token": return OAuth2Client.GrantType.REFRESH_TOKEN;
            case "implicit": return OAuth2Client.GrantType.IMPLICIT;
            case "password": return OAuth2Client.GrantType.PASSWORD;
            default: throw new ValidationException("Unsupported grant type: " + grantType);
        }
    }

    private Set<String> convertGrantTypesToStrings(Set<OAuth2Client.GrantType> grantTypes) {
        return grantTypes.stream()
            .map(this::convertGrantTypeToString)
            .collect(Collectors.toSet());
    }

    private String convertGrantTypeToString(OAuth2Client.GrantType grantType) {
        switch (grantType) {
            case AUTHORIZATION_CODE: return "authorization_code";
            case CLIENT_CREDENTIALS: return "client_credentials";
            case REFRESH_TOKEN: return "refresh_token";
            case IMPLICIT: return "implicit";
            case PASSWORD: return "password";
            default: return grantType.name().toLowerCase();
        }
    }

    private OAuth2ClientResponse convertToResponse(OAuth2Client client) {
        OAuth2ClientResponse response = new OAuth2ClientResponse();
        response.setClientId(client.getClientId());
        // Note: client secret is not included in response for security
        response.setName(client.getName());
        response.setDescription(client.getDescription());
        response.setRedirectUris(client.getRedirectUris());
        response.setScopes(client.getScopes());
        response.setGrantTypes(convertGrantTypesToStrings(client.getGrantTypes()));
        response.setTenantId(client.getTenantId());
        response.setIsActive(client.getIsActive());
        response.setAccessTokenValiditySeconds(client.getAccessTokenValiditySeconds());
        response.setRefreshTokenValiditySeconds(client.getRefreshTokenValiditySeconds());
        response.setAutoApprove(client.getAutoApprove());
        response.setCreatedAt(client.getCreatedAt());
        response.setUpdatedAt(client.getUpdatedAt());
        
        return response;
    }

    private void addClientStatistics(OAuth2ClientResponse response, OAuth2Client client) {
        try {
            // Get token count for this client
            long tokenCount = accessTokenRepository.findByClientIdAndTenantId(
                client.getClientId(), client.getTenantId()).size();
            response.setTokenCount(tokenCount);

            // Get last used timestamp (this would require additional tracking in a real implementation)
            // For now, we'll leave it null
            response.setLastUsedAt(null);

        } catch (Exception e) {
            logger.debug("Error getting client statistics for: {}", client.getClientId(), e);
            // Don't fail the main operation for statistics
        }
    }

    private String generateClientId() {
        byte[] bytes = new byte[CLIENT_ID_LENGTH / 2];
        secureRandom.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private String generateClientSecret() {
        byte[] bytes = new byte[CLIENT_SECRET_LENGTH / 2];
        secureRandom.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}