package com.crm.platform.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Enhanced JWT token provider for authentication and authorization with comprehensive security features
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    private final SecretKey secretKey;
    private final long accessTokenValidityInMinutes;
    private final long refreshTokenValidityInDays;
    private final String issuer;
    private final SecureRandom secureRandom;
    
    public JwtTokenProvider(
            @Value("${jwt.secret:mySecretKey}") String secret,
            @Value("${jwt.access-token-validity-minutes:15}") long accessTokenValidityInMinutes,
            @Value("${jwt.refresh-token-validity-days:7}") long refreshTokenValidityInDays,
            @Value("${jwt.issuer:crm-platform}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMinutes = accessTokenValidityInMinutes;
        this.refreshTokenValidityInDays = refreshTokenValidityInDays;
        this.issuer = issuer;
        this.secureRandom = new SecureRandom();
    }
    
    public String createAccessToken(UUID userId, UUID tenantId, List<String> roles, List<String> permissions) {
        return createAccessToken(userId, tenantId, roles, permissions, null, null);
    }
    
    public String createAccessToken(UUID userId, UUID tenantId, List<String> roles, List<String> permissions, 
                                  String sessionId, String deviceId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenValidityInMinutes, ChronoUnit.MINUTES);
        String jti = generateJti();
        
        JwtBuilder builder = Jwts.builder()
                .setId(jti)
                .setSubject(userId.toString())
                .setIssuer(issuer)
                .setAudience("crm-services")
                .claim("tenant_id", tenantId.toString())
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .claim("permissions", permissions != null ? permissions : Collections.emptyList())
                .claim("type", "access")
                .claim("scope", "api:read api:write")
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(expiration));
        
        if (sessionId != null) {
            builder.claim("session_id", sessionId);
        }
        
        if (deviceId != null) {
            builder.claim("device_id", deviceId);
        }
        
        return builder.signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }
    
    public String createRefreshToken(UUID userId, UUID tenantId) {
        return createRefreshToken(userId, tenantId, null, null);
    }
    
    public String createRefreshToken(UUID userId, UUID tenantId, String sessionId, String deviceId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenValidityInDays, ChronoUnit.DAYS);
        String jti = generateJti();
        
        JwtBuilder builder = Jwts.builder()
                .setId(jti)
                .setSubject(userId.toString())
                .setIssuer(issuer)
                .setAudience("crm-auth")
                .claim("tenant_id", tenantId.toString())
                .claim("type", "refresh")
                .claim("scope", "refresh_token")
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(expiration));
        
        if (sessionId != null) {
            builder.claim("session_id", sessionId);
        }
        
        if (deviceId != null) {
            builder.claim("device_id", deviceId);
        }
        
        return builder.signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    // New methods for auth service with User entity and token ID
    public String createAccessToken(Object user, String tokenId) {
        // This is a simplified implementation - in a real scenario, you'd extract user details
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenValidityInMinutes, ChronoUnit.MINUTES);
        
        return Jwts.builder()
                .setSubject("user-id") // Would extract from user object
                .claim("token_id", tokenId)
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String createRefreshToken(Object user, String tokenId) {
        // This is a simplified implementation - in a real scenario, you'd extract user details
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenValidityInDays, ChronoUnit.DAYS);
        
        return Jwts.builder()
                .setSubject("user-id") // Would extract from user object
                .claim("token_id", tokenId)
                .claim("type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtTokenException("Invalid JWT token", e);
        }
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }
    
    public UUID getTenantIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.get("tenant_id", String.class));
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", List.class);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("permissions", List.class);
    }
    
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }
    
    public String getTokenIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("token_id", String.class);
    }
    
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    public String getSessionIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("session_id", String.class);
    }
    
    public String getDeviceIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("device_id", String.class);
    }
    
    public String getJtiFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }
    
    public String getIssuerFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getIssuer();
    }
    
    public String getAudienceFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getAudience();
    }
    
    public Date getIssuedAtFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getIssuedAt();
    }
    
    public Date getNotBeforeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getNotBefore();
    }
    
    public boolean validateTokenStructure(String token) {
        try {
            Claims claims = parseToken(token);
            
            // Validate required claims
            if (claims.getSubject() == null || claims.getSubject().isEmpty()) {
                logger.warn("Token validation failed: missing subject");
                return false;
            }
            
            if (claims.get("tenant_id") == null) {
                logger.warn("Token validation failed: missing tenant_id");
                return false;
            }
            
            if (claims.get("type") == null) {
                logger.warn("Token validation failed: missing type");
                return false;
            }
            
            // Validate issuer
            if (!issuer.equals(claims.getIssuer())) {
                logger.warn("Token validation failed: invalid issuer. Expected: {}, Got: {}", 
                           issuer, claims.getIssuer());
                return false;
            }
            
            // Validate not before
            if (claims.getNotBefore() != null && claims.getNotBefore().after(new Date())) {
                logger.warn("Token validation failed: token not yet valid");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean validateTokenForRefresh(String refreshToken) {
        try {
            Claims claims = parseToken(refreshToken);
            
            // Validate it's a refresh token
            if (!"refresh".equals(claims.get("type", String.class))) {
                logger.warn("Token validation failed: not a refresh token");
                return false;
            }
            
            return validateTokenStructure(refreshToken);
        } catch (Exception e) {
            logger.warn("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean validateTokenForAccess(String accessToken) {
        try {
            Claims claims = parseToken(accessToken);
            
            // Validate it's an access token
            if (!"access".equals(claims.get("type", String.class))) {
                logger.warn("Token validation failed: not an access token");
                return false;
            }
            
            return validateTokenStructure(accessToken);
        } catch (Exception e) {
            logger.warn("Access token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    public Map<String, Object> getTokenClaims(String token) {
        Claims claims = parseToken(token);
        return new HashMap<>(claims);
    }
    
    public long getTokenExpirationTime(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().getTime();
    }
    
    public long getTokenRemainingTime(String token) {
        long expirationTime = getTokenExpirationTime(token);
        return Math.max(0, expirationTime - System.currentTimeMillis());
    }
    
    private String generateJti() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}