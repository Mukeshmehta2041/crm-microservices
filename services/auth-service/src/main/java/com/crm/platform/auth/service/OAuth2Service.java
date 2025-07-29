package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.*;
import com.crm.platform.auth.entity.*;
import com.crm.platform.auth.exception.OAuth2Exception;
import com.crm.platform.auth.repository.*;
import com.crm.platform.security.jwt.JwtTokenProvider;
import com.crm.platform.common.exception.BusinessException;
import com.crm.platform.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * OAuth2 Service implementing authorization code flow, client credentials flow,
 * token management, and PKCE support.
 */
@Service
@Transactional
public class OAuth2Service {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Service.class);

    private static final int AUTHORIZATION_CODE_EXPIRY_MINUTES = 10;
    private static final int ACCESS_TOKEN_EXPIRY_HOURS = 1;
    private static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    private static final String PKCE_CODE_CHALLENGE_METHOD_S256 = "S256";
    private static final String PKCE_CODE_CHALLENGE_METHOD_PLAIN = "plain";

    @Autowired
    private OAuth2ClientRepository clientRepository;

    @Autowired
    private OAuth2AuthorizationCodeRepository authCodeRepository;

    @Autowired
    private OAuth2AccessTokenRepository accessTokenRepository;

    @Autowired
    private UserCredentialsRepository userCredentialsRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityAuditService auditService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Get authorization URL for OAuth2 provider
     */
    public String getAuthorizationUrl(String provider, OAuth2AuthorizationRequest request) {
        // TODO: Implement provider-specific authorization URL generation
        return "https://oauth2-provider.com/authorize?" + 
               "client_id=" + request.getClientId() + 
               "&redirect_uri=" + request.getRedirectUri() + 
               "&response_type=code" +
               "&scope=" + request.getScope();
    }

    /**
     * Process OAuth2 callback
     */
    public OAuth2TokenResponse processCallback(String provider, OAuth2CallbackRequest request) {
        // TODO: Implement provider-specific callback processing
        return new OAuth2TokenResponse("access_token", "refresh_token", 3600L, "scope", "tenant_id");
    }

    /**
     * Get available OAuth2 providers
     */
    public Map<String, Object> getAvailableProviders() {
        Map<String, Object> providers = new HashMap<>();
        providers.put("google", Map.of("name", "Google", "enabled", true));
        providers.put("microsoft", Map.of("name", "Microsoft", "enabled", true));
        providers.put("github", Map.of("name", "GitHub", "enabled", true));
        providers.put("linkedin", Map.of("name", "LinkedIn", "enabled", true));
        return providers;
    }

    /**
     * Link OAuth2 account to user
     */
    public void linkAccount(String provider, OAuth2CallbackRequest request) {
        // TODO: Implement account linking
        logger.info("Linking account for provider: {}", provider);
    }

    /**
     * Unlink OAuth2 account from user
     */
    public void unlinkAccount(String provider) {
        // TODO: Implement account unlinking
        logger.info("Unlinking account for provider: {}", provider);
    }

    /**
     * Handle OAuth2 authorization request
     */
    public void authorize(OAuth2AuthorizationRequest request, HttpServletRequest httpRequest, 
                         HttpServletResponse httpResponse) {
        try {
            // Validate client
            OAuth2Client client = validateClient(request.getClientId());
            
            // Validate redirect URI
            if (!client.isValidRedirectUri(request.getRedirectUri())) {
                throw new OAuth2Exception("invalid_request", "Invalid redirect URI");
            }

            // Validate response type
            if (!"code".equals(request.getResponseType())) {
                redirectWithError(httpResponse, request.getRedirectUri(), request.getState(), 
                                "unsupported_response_type", "Only authorization code flow is supported");
                return;
            }

            // Validate scopes
            validateScopes(client, request.getScope());

            // For now, we'll assume the user is already authenticated
            // In a real implementation, this would redirect to login if not authenticated
            UUID userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                redirectToLogin(httpResponse, request);
                return;
            }

            // Generate authorization code
            String authCode = generateAuthorizationCode();
            
            // Store authorization code
            OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                authCode,
                client.getClientId(),
                userId,
                client.getTenantId(),
                request.getRedirectUri(),
                request.getScope(),
                request.getState(),
                LocalDateTime.now().plusMinutes(AUTHORIZATION_CODE_EXPIRY_MINUTES)
            );

            // Handle PKCE
            if (StringUtils.hasText(request.getCodeChallenge())) {
                authorizationCode.setCodeChallenge(request.getCodeChallenge());
                authorizationCode.setCodeChallengeMethod(
                    StringUtils.hasText(request.getCodeChallengeMethod()) ? 
                    request.getCodeChallengeMethod() : PKCE_CODE_CHALLENGE_METHOD_PLAIN
                );
            }

            authCodeRepository.save(authorizationCode);

            // Audit log
            auditService.logOAuth2Authorization(userId, client.getClientId(), request.getScope());

            // Redirect with authorization code
            redirectWithCode(httpResponse, request.getRedirectUri(), authCode, request.getState());

        } catch (OAuth2Exception e) {
            redirectWithError(httpResponse, request.getRedirectUri(), request.getState(), 
                            e.getError(), e.getErrorDescription());
        } catch (Exception e) {
            logger.error("OAuth2 authorization error", e);
            redirectWithError(httpResponse, request.getRedirectUri(), request.getState(), 
                            "server_error", "Internal server error");
        }
    }

    /**
     * Exchange authorization code or refresh token for access token
     */
    public OAuth2TokenResponse exchangeToken(OAuth2TokenRequest request, HttpServletRequest httpRequest) {
        try {
            OAuth2Client client = validateClientCredentials(request.getClientId(), request.getClientSecret());

            switch (request.getGrantType()) {
                case "authorization_code":
                    return handleAuthorizationCodeGrant(request, client);
                case "client_credentials":
                    return handleClientCredentialsGrant(request, client);
                case "refresh_token":
                    return handleRefreshTokenGrant(request, client);
                default:
                    throw new OAuth2Exception("unsupported_grant_type", "Grant type not supported");
            }
        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            logger.error("Token exchange error", e);
            throw new OAuth2Exception("server_error", "Internal server error");
        }
    }

    /**
     * Refresh access token
     */
    public OAuth2TokenResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        try {
            OAuth2AccessToken existingToken = accessTokenRepository.findValidRefreshToken(
                request.getRefreshToken(), LocalDateTime.now())
                .orElseThrow(() -> new OAuth2Exception("invalid_grant", "Invalid or expired refresh token"));

            OAuth2Client client = clientRepository.findById(existingToken.getClientId())
                .orElseThrow(() -> new OAuth2Exception("invalid_client", "Client not found"));

            // Generate new tokens
            String newAccessToken = generateAccessToken();
            String newRefreshToken = generateRefreshToken();

            // Update existing token
            existingToken.setAccessToken(newAccessToken);
            existingToken.setRefreshToken(newRefreshToken);
            existingToken.setExpiresAt(LocalDateTime.now().plusHours(ACCESS_TOKEN_EXPIRY_HOURS));
            existingToken.setRefreshExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS));

            accessTokenRepository.save(existingToken);

            // Audit log
            auditService.logTokenRefresh(existingToken.getUserId(), client.getClientId());

            return new OAuth2TokenResponse(
                newAccessToken,
                "Bearer",
                ACCESS_TOKEN_EXPIRY_HOURS * 3600,
                newRefreshToken,
                existingToken.getScope()
            );

        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            logger.error("Token refresh error", e);
            throw new OAuth2Exception("server_error", "Internal server error");
        }
    }

    /**
     * Revoke access or refresh token
     */
    public void revokeToken(RevokeTokenRequest request, HttpServletRequest httpRequest) {
        try {
            // Try to find by access token first
            Optional<OAuth2AccessToken> tokenOpt = accessTokenRepository.findByAccessToken(request.getToken());
            
            // If not found, try refresh token
            if (tokenOpt.isEmpty()) {
                tokenOpt = accessTokenRepository.findByRefreshToken(request.getToken());
            }

            if (tokenOpt.isPresent()) {
                OAuth2AccessToken token = tokenOpt.get();
                token.revoke();
                accessTokenRepository.save(token);

                // Audit log
                auditService.logTokenRevocation(token.getUserId(), token.getClientId(), request.getToken());
            }
            // OAuth2 spec says to return success even if token doesn't exist

        } catch (Exception e) {
            logger.error("Token revocation error", e);
            throw new OAuth2Exception("server_error", "Internal server error");
        }
    }

    /**
     * Get user info from access token
     */
    public UserInfo getUserInfo(String authorization) {
        try {
            String accessToken = extractBearerToken(authorization);
            
            OAuth2AccessToken token = accessTokenRepository.findValidAccessToken(accessToken, LocalDateTime.now())
                .orElseThrow(() -> new OAuth2Exception("invalid_token", "Invalid or expired access token"));

            if (token.getUserId() == null) {
                throw new OAuth2Exception("invalid_token", "Token not associated with a user");
            }

            UserCredentials credentials = userCredentialsRepository.findByUserId(token.getUserId())
                .orElseThrow(() -> new OAuth2Exception("invalid_token", "User not found"));

            // Update last used
            token.updateLastUsed();
            accessTokenRepository.save(token);

            // Return user info based on granted scopes
            return buildUserInfo(credentials, token.getScope());

        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            logger.error("Get user info error", e);
            throw new OAuth2Exception("server_error", "Internal server error");
        }
    }

    // Private helper methods

    private OAuth2TokenResponse handleAuthorizationCodeGrant(OAuth2TokenRequest request, OAuth2Client client) {
        // Validate authorization code
        OAuth2AuthorizationCode authCode = authCodeRepository.findValidCode(
            request.getCode(), client.getClientId(), LocalDateTime.now())
            .orElseThrow(() -> new OAuth2Exception("invalid_grant", "Invalid or expired authorization code"));

        // Validate redirect URI
        if (!authCode.getRedirectUri().equals(request.getRedirectUri())) {
            throw new OAuth2Exception("invalid_grant", "Redirect URI mismatch");
        }

        // Validate PKCE if present
        if (StringUtils.hasText(authCode.getCodeChallenge())) {
            validatePKCE(authCode, request.getCodeVerifier());
        }

        // Mark code as used
        authCode.markAsUsed();
        authCodeRepository.save(authCode);

        // Generate tokens
        String accessToken = generateAccessToken();
        String refreshToken = generateRefreshToken();

        // Store access token
        OAuth2AccessToken token = new OAuth2AccessToken(
            accessToken,
            refreshToken,
            client.getClientId(),
            authCode.getUserId(),
            authCode.getTenantId(),
            authCode.getScope(),
            LocalDateTime.now().plusHours(ACCESS_TOKEN_EXPIRY_HOURS),
            LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS),
            OAuth2Client.GrantType.AUTHORIZATION_CODE
        );

        accessTokenRepository.save(token);

        // Audit log
        auditService.logTokenGeneration(authCode.getUserId(), client.getClientId(), "authorization_code");

        return new OAuth2TokenResponse(
            accessToken,
            "Bearer",
            ACCESS_TOKEN_EXPIRY_HOURS * 3600,
            refreshToken,
            authCode.getScope()
        );
    }

    private OAuth2TokenResponse handleClientCredentialsGrant(OAuth2TokenRequest request, OAuth2Client client) {
        // Validate that client supports client credentials grant
        if (!client.supportsGrantType(OAuth2Client.GrantType.CLIENT_CREDENTIALS)) {
            throw new OAuth2Exception("unauthorized_client", "Client not authorized for client credentials grant");
        }

        // Validate scopes
        validateScopes(client, request.getScope());

        // Generate access token (no refresh token for client credentials)
        String accessToken = generateAccessToken();

        // Store access token
        OAuth2AccessToken token = new OAuth2AccessToken(
            accessToken,
            null, // No refresh token for client credentials
            client.getClientId(),
            null, // No user for client credentials
            client.getTenantId(),
            request.getScope(),
            LocalDateTime.now().plusHours(ACCESS_TOKEN_EXPIRY_HOURS),
            null, // No refresh expiry
            OAuth2Client.GrantType.CLIENT_CREDENTIALS
        );

        accessTokenRepository.save(token);

        // Audit log
        auditService.logTokenGeneration(null, client.getClientId(), "client_credentials");

        return new OAuth2TokenResponse(
            accessToken,
            "Bearer",
            ACCESS_TOKEN_EXPIRY_HOURS * 3600,
            null, // No refresh token
            request.getScope()
        );
    }

    private OAuth2TokenResponse handleRefreshTokenGrant(OAuth2TokenRequest request, OAuth2Client client) {
        OAuth2AccessToken existingToken = accessTokenRepository.findValidRefreshToken(
            request.getRefreshToken(), LocalDateTime.now())
            .orElseThrow(() -> new OAuth2Exception("invalid_grant", "Invalid or expired refresh token"));

        if (!existingToken.getClientId().equals(client.getClientId())) {
            throw new OAuth2Exception("invalid_grant", "Refresh token belongs to different client");
        }

        // Generate new tokens
        String newAccessToken = generateAccessToken();
        String newRefreshToken = generateRefreshToken();

        // Update existing token
        existingToken.setAccessToken(newAccessToken);
        existingToken.setRefreshToken(newRefreshToken);
        existingToken.setExpiresAt(LocalDateTime.now().plusHours(ACCESS_TOKEN_EXPIRY_HOURS));
        existingToken.setRefreshExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS));

        accessTokenRepository.save(existingToken);

        // Audit log
        auditService.logTokenRefresh(existingToken.getUserId(), client.getClientId());

        return new OAuth2TokenResponse(
            newAccessToken,
            "Bearer",
            ACCESS_TOKEN_EXPIRY_HOURS * 3600,
            newRefreshToken,
            existingToken.getScope()
        );
    }

    private OAuth2Client validateClient(String clientId) {
        return clientRepository.findActiveByClientId(clientId)
            .orElseThrow(() -> new OAuth2Exception("invalid_client", "Client not found or inactive"));
    }

    private OAuth2Client validateClientCredentials(String clientId, String clientSecret) {
        OAuth2Client client = validateClient(clientId);
        
        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            throw new OAuth2Exception("invalid_client", "Invalid client credentials");
        }
        
        return client;
    }

    private void validateScopes(OAuth2Client client, String requestedScopes) {
        if (!StringUtils.hasText(requestedScopes)) {
            return; // No scopes requested is valid
        }

        String[] scopes = requestedScopes.split(" ");
        for (String scope : scopes) {
            if (!client.hasScope(scope)) {
                throw new OAuth2Exception("invalid_scope", "Requested scope not allowed: " + scope);
            }
        }
    }

    private void validatePKCE(OAuth2AuthorizationCode authCode, String codeVerifier) {
        if (!StringUtils.hasText(codeVerifier)) {
            throw new OAuth2Exception("invalid_request", "Code verifier required for PKCE");
        }

        String expectedChallenge;
        if (PKCE_CODE_CHALLENGE_METHOD_S256.equals(authCode.getCodeChallengeMethod())) {
            expectedChallenge = base64UrlEncode(sha256(codeVerifier));
        } else {
            expectedChallenge = codeVerifier;
        }

        if (!expectedChallenge.equals(authCode.getCodeChallenge())) {
            throw new OAuth2Exception("invalid_grant", "Code verifier does not match code challenge");
        }
    }

    private UUID getCurrentUserId(HttpServletRequest request) {
        // This would typically extract user ID from session or JWT token
        // For now, return null to indicate user needs to authenticate
        return null;
    }

    private String generateAuthorizationCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return base64UrlEncode(bytes);
    }

    private String generateAccessToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return base64UrlEncode(bytes);
    }

    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new OAuth2Exception("invalid_token", "Invalid authorization header");
        }
        return authorization.substring(7);
    }

    private UserInfo buildUserInfo(UserCredentials credentials, String scopes) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(credentials.getUserId());
        userInfo.setEmail(credentials.getEmail());
        userInfo.setEmailVerified(credentials.getEmailVerified());
        
        // Add more fields based on scopes
        if (scopes != null && scopes.contains("profile")) {
            // Add profile information
        }
        
        return userInfo;
    }

    private void redirectWithCode(HttpServletResponse response, String redirectUri, String code, String state) {
        try {
            StringBuilder url = new StringBuilder(redirectUri);
            url.append(redirectUri.contains("?") ? "&" : "?");
            url.append("code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));
            
            if (StringUtils.hasText(state)) {
                url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            }
            
            response.sendRedirect(url.toString());
        } catch (IOException e) {
            logger.error("Error redirecting with authorization code", e);
        }
    }

    private void redirectWithError(HttpServletResponse response, String redirectUri, String state, 
                                 String error, String errorDescription) {
        try {
            StringBuilder url = new StringBuilder(redirectUri);
            url.append(redirectUri.contains("?") ? "&" : "?");
            url.append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
            
            if (StringUtils.hasText(errorDescription)) {
                url.append("&error_description=").append(URLEncoder.encode(errorDescription, StandardCharsets.UTF_8));
            }
            
            if (StringUtils.hasText(state)) {
                url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            }
            
            response.sendRedirect(url.toString());
        } catch (IOException e) {
            logger.error("Error redirecting with OAuth2 error", e);
        }
    }

    private void redirectToLogin(HttpServletResponse response, OAuth2AuthorizationRequest request) {
        // This would redirect to the login page with the OAuth2 request parameters
        // For now, just return an error
        redirectWithError(response, request.getRedirectUri(), request.getState(), 
                        "access_denied", "User authentication required");
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}