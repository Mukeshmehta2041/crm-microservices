package com.crm.platform.security.filter;

import com.crm.platform.security.context.SecurityContext;
import com.crm.platform.security.context.SecurityContextHolder;
import com.crm.platform.security.jwt.JwtTokenProvider;
import com.crm.platform.security.rbac.Permission;
import com.crm.platform.security.rbac.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JWT authentication filter for processing and validating JWT tokens
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtTokenProvider.validateTokenForAccess(token)) {
                SecurityContext securityContext = createSecurityContext(token, request);
                SecurityContextHolder.setContext(securityContext);
                
                logger.debug("Authentication successful for user: {} (tenant: {})",
                           securityContext.getUserId(), securityContext.getTenantId());
            }
        } catch (Exception e) {
            logger.warn("JWT authentication failed: {}", e.getMessage());
            // Don't set security context on authentication failure
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    private SecurityContext createSecurityContext(String token, HttpServletRequest request) {
        Claims claims = jwtTokenProvider.parseToken(token);
        
        UUID userId = UUID.fromString(claims.getSubject());
        UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));
        String sessionId = claims.get("session_id", String.class);
        String deviceId = claims.get("device_id", String.class);
        
        // Extract roles and permissions from token
        @SuppressWarnings("unchecked")
        List<String> roleCodes = claims.get("roles", List.class);
        @SuppressWarnings("unchecked")
        List<String> permissionCodes = claims.get("permissions", List.class);
        
        Set<Role> roles = new HashSet<>();
        if (roleCodes != null) {
            for (String roleCode : roleCodes) {
                try {
                    roles.add(Role.fromCode(roleCode));
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown role code in token: {}", roleCode);
                }
            }
        }
        
        Set<Permission> permissions = new HashSet<>();
        if (permissionCodes != null) {
            for (String permissionCode : permissionCodes) {
                try {
                    permissions.add(Permission.fromCode(permissionCode));
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown permission code in token: {}", permissionCode);
                }
            }
        }
        
        // Add permissions from roles
        for (Role role : roles) {
            permissions.addAll(role.getPermissions());
        }
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        return new SecurityContext(
            userId, tenantId, null, null, // username and email not in token
            roles, permissions, sessionId, deviceId,
            ipAddress, userAgent,
            claims.getIssuedAt().getTime(),
            claims.getExpiration().getTime()
        );
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip authentication for public endpoints
        return path.startsWith("/actuator/") ||
               path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/refresh") ||
               path.startsWith("/api/v1/auth/password/reset") ||
               path.equals("/api/v1/auth/health");
    }
}