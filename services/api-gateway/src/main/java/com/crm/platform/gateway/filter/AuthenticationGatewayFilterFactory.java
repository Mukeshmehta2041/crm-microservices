package com.crm.platform.gateway.filter;

import com.crm.platform.security.jwt.JwtTokenProvider;
import com.crm.platform.security.jwt.InvalidJwtTokenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Authentication Gateway Filter Factory
 * 
 * This filter validates JWT tokens and adds user context to requests
 */
@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    public AuthenticationGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleUnauthorized(exchange, "Missing or invalid Authorization header");
            }
            
            String token = authHeader.substring(7);
            
            try {
                // Validate token
                if (!jwtTokenProvider.validateToken(token)) {
                    return handleUnauthorized(exchange, "Invalid JWT token");
                }
                
                // Check if token is expired
                if (jwtTokenProvider.isTokenExpired(token)) {
                    return handleUnauthorized(exchange, "JWT token has expired");
                }
                
                // Extract user information from token
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                UUID tenantId = jwtTokenProvider.getTenantIdFromToken(token);
                String tokenType = jwtTokenProvider.getTokenType(token);
                
                // Only allow access tokens for API calls
                if (!"access".equals(tokenType)) {
                    return handleUnauthorized(exchange, "Invalid token type for API access");
                }
                
                // Add user context to request headers
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-ID", userId.toString())
                        .header("X-Tenant-ID", tenantId.toString())
                        .header("X-Token-Type", tokenType)
                        .build();
                
                // Continue with modified request
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
                
            } catch (InvalidJwtTokenException e) {
                return handleUnauthorized(exchange, "Invalid JWT token: " + e.getMessage());
            } catch (Exception e) {
                return handleUnauthorized(exchange, "Authentication error: " + e.getMessage());
            }
        };
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        String errorResponse = String.format(
            "{\"success\":false,\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            message,
            java.time.Instant.now().toString()
        );
        
        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory()
                .wrap(errorResponse.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
}