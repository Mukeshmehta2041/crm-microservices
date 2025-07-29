package com.crm.platform.common.config;

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
    
    @Value("${app.name:CRM Platform}")
    private String appName;
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Value("${app.description:Comprehensive CRM microservices platform}")
    private String appDescription;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Development server"),
                        new Server().url("https://api-staging.crm.example.com").description("Staging server"),
                        new Server().url("https://api.crm.example.com").description("Production server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from /api/v1/auth/login"))
                        .addSecuritySchemes("apiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API key for service-to-service authentication"))
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
    
    private Info apiInfo() {
        return new Info()
                .title(appName + " API")
                .version(appVersion)
                .description(appDescription + "\n\n" +
                        "## Features\n" +
                        "- **Authentication**: JWT-based authentication with refresh tokens\n" +
                        "- **Multi-tenancy**: Complete tenant isolation and management\n" +
                        "- **Advanced Filtering**: Complex query support with custom fields\n" +
                        "- **Bulk Operations**: Efficient batch processing with progress tracking\n" +
                        "- **Real-time Updates**: WebSocket support for live data\n" +
                        "- **API Versioning**: Backward-compatible versioning strategy\n" +
                        "- **Rate Limiting**: Configurable rate limits per tenant/user\n" +
                        "- **Comprehensive Validation**: Field-level validation with detailed error messages\n\n" +
                        
                        "## API Versioning\n" +
                        "This API supports versioning through multiple methods:\n" +
                        "- **URL Path**: `/api/v1/contacts`\n" +
                        "- **Header**: `X-API-Version: 1`\n" +
                        "- **Query Parameter**: `?version=1`\n" +
                        "- **Accept Header**: `Accept: application/json; version=1`\n\n" +
                        
                        "## Pagination\n" +
                        "All list endpoints support pagination with the following parameters:\n" +
                        "- `page`: Page number (1-based, default: 1)\n" +
                        "- `limit`: Items per page (default: 20, max: 1000)\n" +
                        "- `sort`: Sort criteria (format: `field:direction`, e.g., `name:asc,createdAt:desc`)\n\n" +
                        
                        "## Filtering\n" +
                        "Advanced filtering is supported through query parameters:\n" +
                        "- **Simple**: `?status=active&company=Acme`\n" +
                        "- **Operators**: `?amount>1000&createdAt>=2024-01-01`\n" +
                        "- **Custom Fields**: `?customFields.industry=Technology`\n" +
                        "- **Complex**: Use POST `/search` endpoint for complex queries\n\n" +
                        
                        "## Bulk Operations\n" +
                        "Bulk operations support:\n" +
                        "- **Synchronous**: Small batches processed immediately\n" +
                        "- **Asynchronous**: Large batches processed in background\n" +
                        "- **Progress Tracking**: Real-time status updates\n" +
                        "- **Error Handling**: Detailed error reporting per record\n\n" +
                        
                        "## Error Handling\n" +
                        "All errors follow a consistent format:\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"success\": false,\n" +
                        "  \"errors\": [\n" +
                        "    {\n" +
                        "      \"code\": \"VALIDATION_ERROR\",\n" +
                        "      \"message\": \"Email is required\",\n" +
                        "      \"field\": \"email\",\n" +
                        "      \"value\": null,\n" +
                        "      \"constraint\": \"NotNull\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"meta\": {\n" +
                        "    \"timestamp\": \"2024-01-24T10:30:00Z\",\n" +
                        "    \"requestId\": \"abc123\"\n" +
                        "  }\n" +
                        "}\n" +
                        "```\n\n" +
                        
                        "## Rate Limiting\n" +
                        "Rate limits are enforced per tenant and user:\n" +
                        "- **Standard**: 1000 requests/hour\n" +
                        "- **Premium**: 5000 requests/hour\n" +
                        "- **Enterprise**: 10000 requests/hour\n" +
                        "Rate limit information is included in response headers and metadata.\n\n" +
                        
                        "## Custom Fields\n" +
                        "All entities support custom fields stored as JSONB:\n" +
                        "- **Dynamic Creation**: Add fields without schema changes\n" +
                        "- **Type Validation**: Support for various data types\n" +
                        "- **Searchable**: Full-text search and filtering support\n" +
                        "- **Indexed**: Automatic indexing for performance\n\n"
                )
                .contact(new Contact()
                        .name("CRM Platform Team")
                        .email("api-support@crm.example.com")
                        .url("https://docs.crm.example.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }
}