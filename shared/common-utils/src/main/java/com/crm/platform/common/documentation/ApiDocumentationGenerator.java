package com.crm.platform.common.documentation;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility for generating comprehensive API documentation
 */
@Component
public class ApiDocumentationGenerator {
    
    @Autowired
    private OpenAPI openAPI;
    
    /**
     * Add comprehensive examples to API responses
     */
    public void addResponseExamples() {
        // Add success response examples
        addSuccessResponseExamples();
        
        // Add error response examples
        addErrorResponseExamples();
        
        // Add pagination examples
        addPaginationExamples();
        
        // Add bulk operation examples
        addBulkOperationExamples();
    }
    
    private void addSuccessResponseExamples() {
        // Contact success response example
        Example contactExample = new Example();
        contactExample.setValue(Map.of(
                "success", true,
                "data", Map.of(
                        "id", "123e4567-e89b-12d3-a456-426614174000",
                        "firstName", "John",
                        "lastName", "Doe",
                        "email", "john.doe@example.com",
                        "phone", "+1-555-123-4567",
                        "company", "Acme Corporation",
                        "title", "Software Engineer",
                        "status", "ACTIVE",
                        "leadScore", 85,
                        "tags", java.util.List.of("prospect", "enterprise"),
                        "customFields", Map.of(
                                "industry", "Technology",
                                "budget", 50000
                        ),
                        "createdAt", "2024-01-24T10:30:00Z",
                        "updatedAt", "2024-01-24T15:45:00Z"
                ),
                "meta", Map.of(
                        "timestamp", "2024-01-24T15:45:00Z",
                        "version", "v1",
                        "requestId", "req_123456789",
                        "processingTime", "45ms"
                )
        ));
        
        addExampleToResponse("ContactResponse", "200", "application/json", contactExample);
    }
    
    private void addErrorResponseExamples() {
        // Validation error example
        Example validationErrorExample = new Example();
        validationErrorExample.setValue(Map.of(
                "success", false,
                "errors", java.util.List.of(
                        Map.of(
                                "code", "VALIDATION_ERROR",
                                "message", "Email is required",
                                "field", "email",
                                "value", null,
                                "constraint", "NotNull"
                        ),
                        Map.of(
                                "code", "VALIDATION_ERROR",
                                "message", "First name must be between 1 and 100 characters",
                                "field", "firstName",
                                "value", "",
                                "constraint", "Size"
                        )
                ),
                "meta", Map.of(
                        "timestamp", "2024-01-24T15:45:00Z",
                        "version", "v1",
                        "requestId", "req_123456789"
                )
        ));
        
        addExampleToResponse("ValidationError", "400", "application/json", validationErrorExample);
        
        // Not found error example
        Example notFoundExample = new Example();
        notFoundExample.setValue(Map.of(
                "success", false,
                "errors", java.util.List.of(
                        Map.of(
                                "code", "ENTITY_NOT_FOUND",
                                "message", "Contact with ID 123e4567-e89b-12d3-a456-426614174000 not found"
                        )
                ),
                "meta", Map.of(
                        "timestamp", "2024-01-24T15:45:00Z",
                        "version", "v1",
                        "requestId", "req_123456789"
                )
        ));
        
        addExampleToResponse("NotFoundError", "404", "application/json", notFoundExample);
    }
    
    private void addPaginationExamples() {
        Example paginationExample = new Example();
        paginationExample.setValue(Map.of(
                "success", true,
                "data", java.util.List.of(
                        Map.of(
                                "id", "123e4567-e89b-12d3-a456-426614174000",
                                "firstName", "John",
                                "lastName", "Doe",
                                "email", "john.doe@example.com"
                        ),
                        Map.of(
                                "id", "123e4567-e89b-12d3-a456-426614174001",
                                "firstName", "Jane",
                                "lastName", "Smith",
                                "email", "jane.smith@example.com"
                        )
                ),
                "meta", Map.of(
                        "timestamp", "2024-01-24T15:45:00Z",
                        "version", "v1",
                        "requestId", "req_123456789",
                        "processingTime", "125ms",
                        "pagination", Map.of(
                                "page", 1,
                                "limit", 20,
                                "total", 150,
                                "totalPages", 8,
                                "hasNext", true,
                                "hasPrev", false
                        ),
                        "filters", Map.of(
                                "status", "ACTIVE",
                                "company", "Acme"
                        )
                )
        ));
        
        addExampleToResponse("PaginatedResponse", "200", "application/json", paginationExample);
    }
    
    private void addBulkOperationExamples() {
        Example bulkOperationExample = new Example();
        bulkOperationExample.setValue(Map.of(
                "success", true,
                "data", Map.of(
                        "jobId", "bulk_123e4567-e89b-12d3-a456-426614174000",
                        "status", "PROCESSING",
                        "totalRecords", 1000,
                        "processedRecords", 250,
                        "successfulRecords", 240,
                        "failedRecords", 10,
                        "skippedRecords", 0,
                        "progress", 25.0,
                        "startedAt", "2024-01-24T15:30:00Z",
                        "estimatedTimeRemaining", 180,
                        "errors", java.util.List.of(
                                Map.of(
                                        "recordIndex", 15,
                                        "errorCode", "DUPLICATE_EMAIL",
                                        "errorMessage", "Email already exists",
                                        "field", "email",
                                        "value", "duplicate@example.com"
                                )
                        ),
                        "warnings", java.util.List.of(
                                Map.of(
                                        "recordIndex", 23,
                                        "warningCode", "MISSING_PHONE",
                                        "warningMessage", "Phone number is recommended but not required"
                                )
                        )
                ),
                "meta", Map.of(
                        "timestamp", "2024-01-24T15:45:00Z",
                        "version", "v1",
                        "requestId", "req_123456789"
                )
        ));
        
        addExampleToResponse("BulkOperationResponse", "202", "application/json", bulkOperationExample);
    }
    
    private void addExampleToResponse(String operationId, String statusCode, String mediaType, Example example) {
        // This would add the example to the OpenAPI specification
        // Implementation depends on how you're managing the OpenAPI spec
        System.out.println("Adding example for " + operationId + " " + statusCode + ": " + example.getValue());
    }
    
    /**
     * Generate comprehensive API documentation with examples
     */
    public String generateApiDocumentation() {
        StringBuilder doc = new StringBuilder();
        
        doc.append("# CRM Platform API Documentation\n\n");
        
        doc.append("## Overview\n");
        doc.append("The CRM Platform provides a comprehensive REST API for managing customer relationships, ");
        doc.append("sales processes, and business workflows. This API follows RESTful conventions and ");
        doc.append("provides consistent response formats across all endpoints.\n\n");
        
        doc.append("## Authentication\n");
        doc.append("All API requests require authentication using JWT tokens. Include the token in the ");
        doc.append("Authorization header:\n\n");
        doc.append("```\nAuthorization: Bearer <your-jwt-token>\n```\n\n");
        
        doc.append("## Base URL\n");
        doc.append("```\nhttps://api.crm.example.com\n```\n\n");
        
        doc.append("## Request Headers\n");
        doc.append("| Header | Required | Description |\n");
        doc.append("|--------|----------|-------------|\n");
        doc.append("| Authorization | Yes | JWT token for authentication |\n");
        doc.append("| X-Tenant-ID | Yes | Tenant identifier for multi-tenancy |\n");
        doc.append("| X-API-Version | No | API version (default: v1) |\n");
        doc.append("| Content-Type | Yes* | application/json for POST/PUT requests |\n");
        doc.append("| X-Request-ID | No | Unique request identifier for tracing |\n\n");
        
        doc.append("## Response Format\n");
        doc.append("All API responses follow a consistent format:\n\n");
        doc.append("```json\n");
        doc.append("{\n");
        doc.append("  \"success\": boolean,\n");
        doc.append("  \"data\": object | array,\n");
        doc.append("  \"meta\": {\n");
        doc.append("    \"timestamp\": \"ISO8601\",\n");
        doc.append("    \"version\": \"string\",\n");
        doc.append("    \"requestId\": \"string\",\n");
        doc.append("    \"processingTime\": \"string\",\n");
        doc.append("    \"pagination\": { /* pagination info */ },\n");
        doc.append("    \"rateLimit\": { /* rate limit info */ }\n");
        doc.append("  },\n");
        doc.append("  \"errors\": [{ /* error details */ }]\n");
        doc.append("}\n");
        doc.append("```\n\n");
        
        doc.append("## Pagination\n");
        doc.append("List endpoints support pagination with the following parameters:\n\n");
        doc.append("| Parameter | Type | Default | Description |\n");
        doc.append("|-----------|------|---------|-------------|\n");
        doc.append("| page | integer | 1 | Page number (1-based) |\n");
        doc.append("| limit | integer | 20 | Items per page (max: 1000) |\n");
        doc.append("| sort | string[] | - | Sort criteria (field:direction) |\n\n");
        
        doc.append("Example: `GET /api/v1/contacts?page=2&limit=50&sort=lastName:asc,createdAt:desc`\n\n");
        
        doc.append("## Filtering\n");
        doc.append("Advanced filtering is supported through query parameters:\n\n");
        doc.append("### Simple Filtering\n");
        doc.append("```\nGET /api/v1/contacts?status=active&company=Acme\n```\n\n");
        
        doc.append("### Operator-based Filtering\n");
        doc.append("```\nGET /api/v1/contacts?leadScore>80&createdAt>=2024-01-01\n```\n\n");
        
        doc.append("### Custom Field Filtering\n");
        doc.append("```\nGET /api/v1/contacts?customFields.industry=Technology\n```\n\n");
        
        doc.append("### Complex Filtering (POST /search)\n");
        doc.append("For complex queries, use the search endpoint with a JSON body:\n\n");
        doc.append("```json\n");
        doc.append("{\n");
        doc.append("  \"filters\": [\n");
        doc.append("    {\n");
        doc.append("      \"field\": \"status\",\n");
        doc.append("      \"operator\": \"EQUALS\",\n");
        doc.append("      \"value\": \"active\"\n");
        doc.append("    },\n");
        doc.append("    {\n");
        doc.append("      \"field\": \"leadScore\",\n");
        doc.append("      \"operator\": \"GREATER_THAN\",\n");
        doc.append("      \"value\": \"80\"\n");
        doc.append("    }\n");
        doc.append("  ],\n");
        doc.append("  \"sort\": [\n");
        doc.append("    {\n");
        doc.append("      \"field\": \"lastName\",\n");
        doc.append("      \"direction\": \"ASC\"\n");
        doc.append("    }\n");
        doc.append("  ],\n");
        doc.append("  \"page\": {\n");
        doc.append("    \"page\": 1,\n");
        doc.append("    \"limit\": 20\n");
        doc.append("  }\n");
        doc.append("}\n");
        doc.append("```\n\n");
        
        doc.append("## Bulk Operations\n");
        doc.append("Bulk operations support processing multiple records efficiently:\n\n");
        doc.append("### Synchronous Bulk Operations\n");
        doc.append("Small batches (< 100 records) are processed immediately:\n\n");
        doc.append("```json\n");
        doc.append("{\n");
        doc.append("  \"operation\": \"CREATE\",\n");
        doc.append("  \"data\": [\n");
        doc.append("    { /* contact data */ },\n");
        doc.append("    { /* contact data */ }\n");
        doc.append("  ],\n");
        doc.append("  \"validationOnly\": false,\n");
        doc.append("  \"continueOnError\": true\n");
        doc.append("}\n");
        doc.append("```\n\n");
        
        doc.append("### Asynchronous Bulk Operations\n");
        doc.append("Large batches are processed in the background with progress tracking:\n\n");
        doc.append("```json\n");
        doc.append("{\n");
        doc.append("  \"operation\": \"CREATE\",\n");
        doc.append("  \"data\": [ /* large array */ ],\n");
        doc.append("  \"async\": true,\n");
        doc.append("  \"batchSize\": 100\n");
        doc.append("}\n");
        doc.append("```\n\n");
        
        doc.append("## Error Handling\n");
        doc.append("The API uses standard HTTP status codes and provides detailed error information:\n\n");
        doc.append("| Status Code | Description |\n");
        doc.append("|-------------|-------------|\n");
        doc.append("| 200 | Success |\n");
        doc.append("| 201 | Created |\n");
        doc.append("| 204 | No Content |\n");
        doc.append("| 400 | Bad Request (validation errors) |\n");
        doc.append("| 401 | Unauthorized |\n");
        doc.append("| 403 | Forbidden |\n");
        doc.append("| 404 | Not Found |\n");
        doc.append("| 409 | Conflict (duplicate data) |\n");
        doc.append("| 422 | Unprocessable Entity |\n");
        doc.append("| 429 | Too Many Requests (rate limited) |\n");
        doc.append("| 500 | Internal Server Error |\n\n");
        
        doc.append("## Rate Limiting\n");
        doc.append("API requests are rate limited per tenant and user:\n\n");
        doc.append("| Plan | Requests per Hour |\n");
        doc.append("|------|------------------|\n");
        doc.append("| Standard | 1,000 |\n");
        doc.append("| Premium | 5,000 |\n");
        doc.append("| Enterprise | 10,000 |\n\n");
        
        doc.append("Rate limit information is included in response headers and metadata.\n\n");
        
        doc.append("## Webhooks\n");
        doc.append("Configure webhooks to receive real-time notifications:\n\n");
        doc.append("```json\n");
        doc.append("{\n");
        doc.append("  \"url\": \"https://your-app.com/webhooks/crm\",\n");
        doc.append("  \"events\": [\"contact.created\", \"deal.stage_changed\"],\n");
        doc.append("  \"secret\": \"your-webhook-secret\"\n");
        doc.append("}\n");
        doc.append("```\n\n");
        
        return doc.toString();
    }
}