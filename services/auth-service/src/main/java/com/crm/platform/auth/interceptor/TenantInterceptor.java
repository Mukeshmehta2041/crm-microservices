package com.crm.platform.auth.interceptor;

import com.crm.platform.auth.service.TenantContextService;
import com.crm.platform.auth.service.TenantValidationService;
import com.crm.platform.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor for tenant context validation and isolation
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);

    private final TenantContextService tenantContextService;
    private final TenantValidationService tenantValidationService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${tenant.interceptor.enabled:true}")
    private boolean interceptorEnabled;

    @Value("${tenant.header.name:X-Tenant-ID}")
    private String tenantHeaderName;

    @Value("${tenant.subdomain.header.name:X-Tenant-Subdomain}")
    private String subdomainHeaderName;

    @Autowired
    public TenantInterceptor(TenantContextService tenantContextService,
                           TenantValidationService tenantValidationService,
                           JwtTokenProvider jwtTokenProvider) {
        this.tenantContextService = tenantContextService;
        this.tenantValidationService = tenantValidationService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!interceptorEnabled) {
            return true;
        }

        // Skip tenant validation for certain endpoints
        if (shouldSkipTenantValidation(request)) {
            return true;
        }

        try {
            // Clear any existing tenant context
            tenantContextService.clearTenantContext();

            // Try to extract tenant context from various sources
            UUID tenantId = extractTenantId(request);
            String subdomain = extractSubdomain(request);

            if (tenantId != null) {
                // Validate and set tenant context by ID
                if (!tenantValidationService.validateAndSetTenantContext(tenantId)) {
                    logger.warn("Tenant validation failed for ID: {}", tenantId);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Invalid or inactive tenant\"}");
                    return false;
                }
            } else if (subdomain != null) {
                // Validate and set tenant context by subdomain
                if (!tenantValidationService.validateAndSetTenantContextBySubdomain(subdomain)) {
                    logger.warn("Tenant validation failed for subdomain: {}", subdomain);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Invalid or inactive tenant subdomain\"}");
                    return false;
                }
            } else {
                // No tenant context found
                logger.warn("No tenant context found in request: {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"Tenant context required\"}");
                return false;
            }

            logger.debug("Tenant context validated for request: {} (tenant: {})", 
                        request.getRequestURI(), tenantContextService.getCurrentTenantId());
            
            return true;

        } catch (Exception e) {
            logger.error("Error in tenant interceptor", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Tenant validation error\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Clear tenant context after request completion
        tenantContextService.clearTenantContext();
    }

    /**
     * Extract tenant ID from various sources in order of priority
     */
    private UUID extractTenantId(HttpServletRequest request) {
        // 1. Check explicit tenant header
        String tenantHeader = request.getHeader(tenantHeaderName);
        if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
            try {
                return UUID.fromString(tenantHeader);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid tenant ID in header: {}", tenantHeader);
            }
        }

        // 2. Extract from JWT token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtTokenProvider.getTenantIdFromToken(token);
            } catch (Exception e) {
                logger.debug("Could not extract tenant ID from JWT token: {}", e.getMessage());
            }
        }

        // 3. Check request parameter
        String tenantParam = request.getParameter("tenantId");
        if (tenantParam != null && !tenantParam.trim().isEmpty()) {
            try {
                return UUID.fromString(tenantParam);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid tenant ID in parameter: {}", tenantParam);
            }
        }

        return null;
    }

    /**
     * Extract subdomain from various sources
     */
    private String extractSubdomain(HttpServletRequest request) {
        // 1. Check explicit subdomain header
        String subdomainHeader = request.getHeader(subdomainHeaderName);
        if (subdomainHeader != null && !subdomainHeader.trim().isEmpty()) {
            return subdomainHeader.trim();
        }

        // 2. Extract from Host header
        String host = request.getHeader("Host");
        if (host != null) {
            String subdomain = extractSubdomainFromHost(host);
            if (subdomain != null) {
                return subdomain;
            }
        }

        // 3. Check request parameter
        String subdomainParam = request.getParameter("subdomain");
        if (subdomainParam != null && !subdomainParam.trim().isEmpty()) {
            return subdomainParam.trim();
        }

        return null;
    }

    /**
     * Extract subdomain from host header
     */
    private String extractSubdomainFromHost(String host) {
        try {
            // Remove port if present
            if (host.contains(":")) {
                host = host.substring(0, host.indexOf(":"));
            }

            // Split by dots
            String[] parts = host.split("\\.");
            
            // If we have at least 3 parts (subdomain.domain.tld), extract subdomain
            if (parts.length >= 3) {
                String subdomain = parts[0];
                
                // Skip common prefixes
                if (!"www".equals(subdomain) && !"api".equals(subdomain)) {
                    return subdomain;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error extracting subdomain from host: {}", host, e);
            return null;
        }
    }

    /**
     * Check if tenant validation should be skipped for this request
     */
    private boolean shouldSkipTenantValidation(String requestURI) {
        // Skip for health checks, metrics, and public endpoints
        return requestURI.startsWith("/actuator/") ||
               requestURI.startsWith("/health") ||
               requestURI.startsWith("/metrics") ||
               requestURI.startsWith("/api/v1/auth/public/") ||
               requestURI.equals("/") ||
               requestURI.equals("/favicon.ico");
    }

    /**
     * Check if tenant validation should be skipped for this request
     */
    private boolean shouldSkipTenantValidation(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // Skip for specific URIs
        if (shouldSkipTenantValidation(requestURI)) {
            return true;
        }

        // Skip for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // Skip for certain auth endpoints that don't require tenant context
        if (requestURI.startsWith("/api/v1/auth/")) {
            // Allow these endpoints without tenant context
            return requestURI.contains("/login") ||
                   requestURI.contains("/register") ||
                   requestURI.contains("/forgot-password") ||
                   requestURI.contains("/reset-password") ||
                   requestURI.contains("/verify-email") ||
                   requestURI.contains("/captcha/");
        }

        return false;
    }
}