package com.crm.platform.common.tracing;

import brave.Tracer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Custom filter to enhance tracing with additional context and correlation IDs.
 */
@Component
public class CustomTraceFilter extends OncePerRequestFilter {

  @Autowired
  private Tracer tracer;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // Generate or extract correlation ID
    String correlationId = request.getHeader("X-Correlation-ID");
    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    }

    // Extract tenant ID for multi-tenant tracing
    String tenantId = request.getHeader("X-Tenant-ID");

    // Extract user ID from JWT or session
    String userId = extractUserId(request);

    try {
      // Add to MDC for logging
      MDC.put("correlationId", correlationId);
      MDC.put("tenantId", tenantId != null ? tenantId : "unknown");
      MDC.put("userId", userId != null ? userId : "anonymous");

      // Add to trace span
      if (tracer.currentSpan() != null) {
        tracer.currentSpan()
            .tag("correlation.id", correlationId)
            .tag("tenant.id", tenantId != null ? tenantId : "unknown")
            .tag("user.id", userId != null ? userId : "anonymous")
            .tag("request.method", request.getMethod())
            .tag("request.uri", request.getRequestURI());
      }

      // Add correlation ID to response headers
      response.setHeader("X-Correlation-ID", correlationId);

      filterChain.doFilter(request, response);

    } finally {
      // Clean up MDC
      MDC.remove("correlationId");
      MDC.remove("tenantId");
      MDC.remove("userId");
    }
  }

  private String extractUserId(HttpServletRequest request) {
    // Extract user ID from JWT token or session
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      // In a real implementation, you would decode the JWT token here
      // For now, return a placeholder
      return "user-from-jwt";
    }
    return null;
  }
}