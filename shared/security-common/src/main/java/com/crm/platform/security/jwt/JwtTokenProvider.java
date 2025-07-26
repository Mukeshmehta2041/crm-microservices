package com.crm.platform.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token provider for authentication and authorization
 */
@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long accessTokenValidityInMinutes;
    private final long refreshTokenValidityInDays;
    
    public JwtTokenProvider(
            @Value("${jwt.secret:mySecretKey}") String secret,
            @Value("${jwt.access-token-validity-minutes:15}") long accessTokenValidityInMinutes,
            @Value("${jwt.refresh-token-validity-days:7}") long refreshTokenValidityInDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMinutes = accessTokenValidityInMinutes;
        this.refreshTokenValidityInDays = refreshTokenValidityInDays;
    }
    
    public String createAccessToken(UUID userId, UUID tenantId, List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenValidityInMinutes, ChronoUnit.MINUTES);
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String createRefreshToken(UUID userId, UUID tenantId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenValidityInDays, ChronoUnit.DAYS);
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
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
}