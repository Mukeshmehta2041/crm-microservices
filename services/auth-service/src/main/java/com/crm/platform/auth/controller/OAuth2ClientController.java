package com.crm.platform.auth.controller;

import com.crm.platform.auth.dto.OAuth2ClientRequest;
import com.crm.platform.auth.dto.OAuth2ClientResponse;
import com.crm.platform.auth.dto.OAuth2ClientUpdateRequest;
import com.crm.platform.auth.service.OAuth2ClientManagementService;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.logging.SecurityLog;
import com.crm.platform.common.monitoring.Monitored;
import com.crm.platform.common.monitoring.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * OAuth2 client management controller providing client registration, management,
 * and validation endpoints for OAuth2 client applications.
 */
@RestController
@RequestMapping("/api/v1/oauth2/clients")
@Tag(name = "OAuth2 Client Management", description = "OAuth2 client registration and management endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OAuth2ClientController {

    @Autowired
    private OAuth2ClientManagementService clientManagementService;

    // ==================== Client Registration ====================

    @PostMapping
    @Operation(summary = "Register OAuth2 Client", description = "Register a new OAuth2 client application")
    @Timed(value = "auth.oauth2.client.register", description = "OAuth2 client registration")
    @Monitored("oauth2-client-register")
    @SecurityLog(operation = "oauth2-client-register", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "201", description = "Client registered successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid client data"),
        @SwaggerApiResponse(responseCode = "409", description = "Client already exists")
    })
    public ResponseEntity<ApiResponse<OAuth2ClientResponse>> registerClient(
            @Parameter(description = "Client registration data", required = true)
            @Valid @RequestBody OAuth2ClientRequest request,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        // In a real implementation, createdBy would be extracted from authentication context
        UUID createdBy = null; // TODO: Extract from security context

        OAuth2ClientResponse response = clientManagementService.registerClient(request, tenantId, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ==================== Client Retrieval ====================

    @GetMapping("/{clientId}")
    @Operation(summary = "Get OAuth2 Client", description = "Retrieve OAuth2 client details by client ID")
    @SecurityLog(operation = "oauth2-client-get", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Client retrieved successfully"),
        @SwaggerApiResponse(responseCode = "404", description = "Client not found"),
        @SwaggerApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<OAuth2ClientResponse>> getClient(
            @Parameter(description = "Client ID", required = true)
            @PathVariable @NotBlank String clientId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        OAuth2ClientResponse response = clientManagementService.getClient(clientId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List OAuth2 Clients", description = "List OAuth2 clients for a tenant with pagination")
    @SecurityLog(operation = "oauth2-client-list", type = SecurityLog.SecurityType.AUTHORIZATION, riskLevel = SecurityLog.RiskLevel.LOW)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Clients retrieved successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<ApiResponse<Page<OAuth2ClientResponse>>> listClients(
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "name")
            @RequestParam(value = "sort", defaultValue = "name") String sort,
            @Parameter(description = "Sort direction", example = "asc")
            @RequestParam(value = "direction", defaultValue = "asc") String direction,
            @Parameter(description = "Filter by client name")
            @RequestParam(value = "name", required = false) String nameFilter) {

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<OAuth2ClientResponse> clients = clientManagementService.listClients(tenantId, pageable, nameFilter);
        return ResponseEntity.ok(ApiResponse.success(clients));
    }

    // ==================== Client Updates ====================

    @PutMapping("/{clientId}")
    @Operation(summary = "Update OAuth2 Client", description = "Update OAuth2 client configuration")
    @Monitored("oauth2-client-update")
    @SecurityLog(operation = "oauth2-client-update", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Client updated successfully"),
        @SwaggerApiResponse(responseCode = "404", description = "Client not found"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid update data")
    })
    public ResponseEntity<ApiResponse<OAuth2ClientResponse>> updateClient(
            @Parameter(description = "Client ID", required = true)
            @PathVariable @NotBlank String clientId,
            @Parameter(description = "Client update data", required = true)
            @Valid @RequestBody OAuth2ClientUpdateRequest request,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        // In a real implementation, updatedBy would be extracted from authentication context
        UUID updatedBy = null; // TODO: Extract from security context

        OAuth2ClientResponse response = clientManagementService.updateClient(clientId, request, tenantId, updatedBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{clientId}/regenerate-secret")
    @Operation(summary = "Regenerate Client Secret", description = "Generate a new client secret")
    @Monitored("oauth2-client-regenerate-secret")
    @SecurityLog(operation = "oauth2-client-regenerate-secret", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Client secret regenerated successfully"),
        @SwaggerApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<ApiResponse<OAuth2ClientResponse>> regenerateClientSecret(
            @Parameter(description = "Client ID", required = true)
            @PathVariable @NotBlank String clientId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        // In a real implementation, regeneratedBy would be extracted from authentication context
        UUID regeneratedBy = null; // TODO: Extract from security context

        OAuth2ClientResponse response = clientManagementService.regenerateClientSecret(clientId, tenantId, regeneratedBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Client Deletion ====================

    @DeleteMapping("/{clientId}")
    @Operation(summary = "Delete OAuth2 Client", description = "Delete an OAuth2 client")
    @SecurityLog(operation = "oauth2-client-delete", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.HIGH)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "204", description = "Client deleted successfully"),
        @SwaggerApiResponse(responseCode = "404", description = "Client not found"),
        @SwaggerApiResponse(responseCode = "409", description = "Client has active tokens")
    })
    public ResponseEntity<Void> deleteClient(
            @Parameter(description = "Client ID", required = true)
            @PathVariable @NotBlank String clientId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        // In a real implementation, deletedBy would be extracted from authentication context
        UUID deletedBy = null; // TODO: Extract from security context

        clientManagementService.deleteClient(clientId, tenantId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    // ==================== Client Validation ====================

    @PostMapping("/{clientId}/validate-credentials")
    @Operation(summary = "Validate Client Credentials", description = "Validate client ID and secret")
    @Timed(value = "auth.oauth2.client.validate", description = "OAuth2 client credential validation")
    @SecurityLog(operation = "oauth2-client-validate", type = SecurityLog.SecurityType.AUTHENTICATION, riskLevel = SecurityLog.RiskLevel.MEDIUM)
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Validation completed"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateClientCredentials(
            @Parameter(description = "Client ID", required = true)
            @PathVariable @NotBlank String clientId,
            @Parameter(description = "Client secret", required = true)
            @RequestParam("client_secret") @NotBlank String clientSecret,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        boolean isValid = clientManagementService.validateClientCredentials(clientId, clientSecret, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "valid", isValid,
            "client_id", clientId,
            "timestamp", System.currentTimeMillis()
        )));
    }

    @PostMapping("/{clientId}/validate-redirect-uri")
    @Operation(summary = "Validate Redirect URI", description = "Validate if redirect URI is allowed for client")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Validation completed"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateRedirectUri(
            @Parameter(description = "Client ID", required = true)
            @PathVariable @NotBlank String clientId,
            @Parameter(description = "Redirect URI", required = true)
            @RequestParam("redirect_uri") @NotBlank String redirectUri,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        boolean isValid = clientManagementService.validateRedirectUri(clientId, redirectUri, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "valid", isValid,
            "client_id", clientId,
            "redirect_uri", redirectUri,
            "timestamp", System.currentTimeMillis()
        )));
    }

    @PostMapping("/{clientId}/validate-scope")
    @Operation(summary = "Validate Scope", description = "Validate if scope is allowed for client")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Validation completed"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateScope(
            @Parameter(description = "Client ID", required = true)
            @PathVariable @NotBlank String clientId,
            @Parameter(description = "Scope", required = true)
            @RequestParam("scope") @NotBlank String scope,
            @Parameter(description = "Tenant ID", required = true)
            @RequestParam("tenant_id") UUID tenantId) {

        boolean isValid = clientManagementService.validateScope(clientId, scope, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "valid", isValid,
            "client_id", clientId,
            "scope", scope,
            "timestamp", System.currentTimeMillis()
        )));
    }

    // ==================== Configuration Information ====================

    @GetMapping("/scopes")
    @Operation(summary = "Get Available Scopes", description = "Get list of available OAuth2 scopes")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Scopes retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Set<String>>> getAvailableScopes() {
        Set<String> scopes = clientManagementService.getAvailableScopes();
        return ResponseEntity.ok(ApiResponse.success(scopes));
    }

    @GetMapping("/grant-types")
    @Operation(summary = "Get Supported Grant Types", description = "Get list of supported OAuth2 grant types")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Grant types retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Set<String>>> getSupportedGrantTypes() {
        Set<String> grantTypes = clientManagementService.getSupportedGrantTypes();
        return ResponseEntity.ok(ApiResponse.success(grantTypes));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Client Management Health", description = "Check OAuth2 client management service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "oauth2-client-management",
            "timestamp", System.currentTimeMillis()
        )));
    }
}