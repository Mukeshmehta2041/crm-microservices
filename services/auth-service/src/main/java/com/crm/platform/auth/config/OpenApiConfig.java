package com.crm.platform.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for comprehensive API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:CRM Platform Authentication Service}")
    private String appDescription;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CRM Platform Authentication Service API")
                        .version(appVersion)
                        .description(buildDescription())
                        .contact(new Contact()
                                .name("CRM Platform Team")
                                .email("support@crmplatform.com")
                                .url("https://docs.crmplatform.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(serverUrl).description("Development Server"),
                        new Server().url("https://api.crmplatform.com").description("Production Server"),
                        new Server().url("https://staging-api.crmplatform.com").description("Staging Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token authentication"))
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth2 authentication flow"))
                        .addSecuritySchemes("apiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API Key authentication")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private String buildDescription() {
        return """
                # CRM Platform Authentication Service API
                
                ## Overview
                The CRM Platform Authentication Service provides comprehensive authentication, authorization, 
                and user management capabilities for the CRM Platform ecosystem. This service handles:
                
                - **OAuth2 Authentication Flows** - Authorization code, client credentials, and refresh token flows
                - **Multi-Factor Authentication (MFA)** - TOTP-based MFA with backup codes
                - **Session Management** - Secure session handling with device tracking
                - **Password Management** - Password reset, change, and policy enforcement
                - **Email Verification** - Email verification and management
                - **Security Monitoring** - Comprehensive security audit and monitoring
                - **Rate Limiting** - Advanced rate limiting with IP blocking and CAPTCHA
                - **Tenant Management** - Multi-tenant isolation and management
                
                ## Authentication Methods
                
                ### 1. JWT Bearer Token
                Most endpoints require a valid JWT bearer token in the Authorization header:
                ```
                Authorization: Bearer <your-jwt-token>
                ```
                
                ### 2. OAuth2 Flow
                For third-party integrations, use the OAuth2 authorization flow:
                1. Redirect to `/api/v1/auth/oauth2/authorize`
                2. Exchange authorization code at `/api/v1/auth/oauth2/token`
                3. Use access token for API calls
                
                ### 3. API Key (Limited endpoints)
                Some endpoints support API key authentication:
                ```
                X-API-Key: <your-api-key>
                ```
                
                ## Error Handling
                All API responses follow a standardized format:
                
                ### Success Response
                ```json
                {
                  "success": true,
                  "data": { ... },
                  "meta": {
                    "timestamp": "2024-01-01T12:00:00Z",
                    "version": "1.0.0",
                    "requestId": "req-123",
                    "processingTime": "45ms"
                  }
                }
                ```
                
                ### Error Response
                ```json
                {
                  "success": false,
                  "errors": [
                    {
                      "code": "INVALID_CREDENTIALS",
                      "message": "Invalid username or password",
                      "field": "password",
                      "constraint": "AUTHENTICATION_FAILED"
                    }
                  ],
                  "meta": {
                    "timestamp": "2024-01-01T12:00:00Z",
                    "version": "1.0.0",
                    "requestId": "req-123"
                  }
                }
                ```
                
                ## Rate Limiting
                API endpoints are rate limited to ensure fair usage:
                - **Authentication endpoints**: 5 requests per minute, 20 per hour
                - **Token endpoints**: 10 requests per minute, 100 per hour
                - **General endpoints**: 100 requests per minute
                
                Rate limit headers are included in responses:
                ```
                X-RateLimit-Limit: 100
                X-RateLimit-Remaining: 95
                X-RateLimit-Reset: 1640995200
                ```
                
                ## Tenant Context
                Most operations require a tenant context. Provide tenant information via:
                - **Header**: `X-Tenant-ID: <tenant-uuid>`
                - **Subdomain**: `https://<tenant>.api.crmplatform.com`
                - **JWT Token**: Tenant ID embedded in token claims
                
                ## Security Features
                - **Account Lockout**: Automatic lockout after failed login attempts
                - **IP Blocking**: Automatic IP blocking for suspicious activity
                - **CAPTCHA**: CAPTCHA challenges for suspicious requests
                - **Audit Logging**: Comprehensive security audit trails
                - **MFA Support**: Time-based one-time passwords (TOTP)
                
                ## Support
                - **Documentation**: https://docs.crmplatform.com
                - **Support Email**: support@crmplatform.com
                - **Status Page**: https://status.crmplatform.com
                """;
    }
}