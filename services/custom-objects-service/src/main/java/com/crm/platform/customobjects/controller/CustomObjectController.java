package com.crm.platform.customobjects.controller;

import com.crm.platform.customobjects.dto.*;
import com.crm.platform.customobjects.service.CustomObjectService;
import com.crm.platform.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for custom objects management
 */
@RestController
@RequestMapping("/api/v1/custom-objects")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CustomObjectController {

    private static final Logger logger = LoggerFactory.getLogger(CustomObjectController.class);

    private final CustomObjectService customObjectService;

    @Autowired
    public CustomObjectController(CustomObjectService customObjectService) {
        this.customObjectService = customObjectService;
    }

    /**
     * Create a new custom object
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomObjectResponse>> createCustomObject(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId,
            @Valid @RequestBody CustomObjectRequest request) {
        
        logger.info("Creating custom object for tenant: {}, user: {}", tenantId, userId);
        
        CustomObjectResponse response = customObjectService.createCustomObject(tenantId, request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Update an existing custom object
     */
    @PutMapping("/{customObjectId}")
    public ResponseEntity<Map<String, Object>> updateCustomObject(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId,
            @PathVariable UUID customObjectId,
            @Valid @RequestBody CustomObjectRequest request) {
        
        logger.info("Updating custom object: {} for tenant: {}, user: {}", customObjectId, tenantId, userId);
        
        CustomObjectResponse response = customObjectService.updateCustomObject(tenantId, customObjectId, request, userId);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", response,
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast"
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get custom object by ID
     */
    @GetMapping("/{customObjectId}")
    public ResponseEntity<Map<String, Object>> getCustomObject(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID customObjectId,
            @RequestParam(defaultValue = "false") boolean includeFields) {
        
        logger.debug("Getting custom object: {} for tenant: {}", customObjectId, tenantId);
        
        CustomObjectResponse response = customObjectService.getCustomObject(tenantId, customObjectId, includeFields);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", response,
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast"
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get custom object by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<Map<String, Object>> getCustomObjectByName(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean includeFields) {
        
        logger.debug("Getting custom object by name: {} for tenant: {}", name, tenantId);
        
        CustomObjectResponse response = customObjectService.getCustomObjectByName(tenantId, name, includeFields);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", response,
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast"
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get custom object by API name
     */
    @GetMapping("/api-name/{apiName}")
    public ResponseEntity<Map<String, Object>> getCustomObjectByApiName(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable String apiName,
            @RequestParam(defaultValue = "false") boolean includeFields) {
        
        logger.debug("Getting custom object by API name: {} for tenant: {}", apiName, tenantId);
        
        CustomObjectResponse response = customObjectService.getCustomObjectByApiName(tenantId, apiName, includeFields);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", response,
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast"
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get all custom objects for tenant
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCustomObjects(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        
        logger.debug("Getting all custom objects for tenant: {}, activeOnly: {}", tenantId, activeOnly);
        
        List<CustomObjectResponse> response = customObjectService.getAllCustomObjects(tenantId, activeOnly);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", response,
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast",
                "total", response.size()
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Search custom objects
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCustomObjects(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Valid @RequestBody CustomObjectSearchRequest searchRequest) {
        
        logger.debug("Searching custom objects for tenant: {} with request: {}", tenantId, searchRequest);
        
        Page<CustomObjectResponse> response = customObjectService.searchCustomObjects(tenantId, searchRequest);
        
        Map<String, Object> pagination = Map.of(
            "page", response.getNumber(),
            "size", response.getSize(),
            "total", response.getTotalElements(),
            "totalPages", response.getTotalPages(),
            "hasNext", response.hasNext(),
            "hasPrev", response.hasPrevious()
        );
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", response.getContent(),
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast",
                "pagination", pagination
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Delete custom object (soft delete)
     */
    @DeleteMapping("/{customObjectId}")
    public ResponseEntity<Map<String, Object>> deleteCustomObject(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId,
            @PathVariable UUID customObjectId) {
        
        logger.info("Deleting custom object: {} for tenant: {}, user: {}", customObjectId, tenantId, userId);
        
        customObjectService.deleteCustomObject(tenantId, customObjectId, userId);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "message", "Custom object deleted successfully",
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast"
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get custom objects for reports
     */
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getCustomObjectsForReports(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting custom objects for reports for tenant: {}", tenantId);
        
        List<CustomObjectResponse> response = customObjectService.getCustomObjectsForReports(tenantId);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", response,
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast",
                "total", response.size()
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get custom objects for activities
     */
    @GetMapping("/activities")
    public ResponseEntity<Map<String, Object>> getCustomObjectsForActivities(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting custom objects for activities for tenant: {}", tenantId);
        
        List<CustomObjectResponse> response = customObjectService.getCustomObjectsForActivities(tenantId);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", response,
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast",
                "total", response.size()
            )
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get custom object statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCustomObjectStatistics(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting custom object statistics for tenant: {}", tenantId);
        
        Map<String, Object> statistics = customObjectService.getCustomObjectStatistics(tenantId);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "data", statistics,
            "meta", Map.of(
                "timestamp", LocalDateTime.now(),
                "version", "v1",
                "processingTime", "fast"
            )
        );
        
        return ResponseEntity.ok(result);
    }
}