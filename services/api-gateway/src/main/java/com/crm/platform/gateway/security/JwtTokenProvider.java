package com.crm.platform.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Simple JWT Token Provider for API Gateway
 * Provides basic JWT validation functionality
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:myVerySecretKeyForJWTTokenGenerationAndValidation123456789}")
    private String jwtSecret;

    @Value("${jwt.access-token-validity-minutes:15}")
    private int accessTokenValidityMinutes;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String userIdStr = claims.get("user_id", String.class);
            return userIdStr != null ? UUID.fromString(userIdStr) : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public UUID getTenantIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String tenantIdStr = claims.get("tenant_id", String.class);
            return tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String getTokenType(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return claims.get("token_type", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
} 