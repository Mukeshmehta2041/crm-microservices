package com.crm.platform.pipelines.controller;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.pipelines.dto.*;
import com.crm.platform.pipelines.service.PipelineService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipelines")
@CrossOrigin(origins = "*")
public class PipelineController {

    private static final Logger logger = LoggerFactory.getLogger(PipelineController.class);

    private final PipelineService pipelineService;

    @Autowired
    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    /**
     * Create a new pipeline
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PipelineResponse>> createPipeline(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId,
            @Valid @RequestBody PipelineRequest request) {
        
        logger.info("Creating pipeline for tenant: {} by user: {}", tenantId, userId);
        
        PipelineResponse response = pipelineService.createPipeline(tenantId, request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Pipeline created successfully"));
    }

    /**
     * Update an existing pipeline
     */
    @PutMapping("/{pipelineId}")
    public ResponseEntity<ApiResponse<PipelineResponse>> updatePipeline(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId,
            @PathVariable UUID pipelineId,
            @Valid @RequestBody PipelineRequest request) {
        
        logger.info("Updating pipeline: {} for tenant: {} by user: {}", pipelineId, tenantId, userId);
        
        PipelineResponse response = pipelineService.updatePipeline(tenantId, pipelineId, request, userId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline updated successfully"));
    }

    /**
     * Get pipeline by ID
     */
    @GetMapping("/{pipelineId}")
    public ResponseEntity<ApiResponse<PipelineResponse>> getPipeline(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID pipelineId) {
        
        logger.debug("Getting pipeline: {} for tenant: {}", pipelineId, tenantId);
        
        PipelineResponse response = pipelineService.getPipeline(tenantId, pipelineId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline retrieved successfully"));
    }

    /**
     * Get all pipelines for tenant
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PipelineResponse>>> getAllPipelines(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        
        logger.debug("Getting pipelines for tenant: {} activeOnly: {}", tenantId, activeOnly);
        
        List<PipelineResponse> response = activeOnly 
                ? pipelineService.getActivePipelines(tenantId)
                : pipelineService.getAllPipelines(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Pipelines retrieved successfully"));
    }

    /**
     * Get default pipeline for tenant
     */
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<PipelineResponse>> getDefaultPipeline(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting default pipeline for tenant: {}", tenantId);
        
        Optional<PipelineResponse> response = pipelineService.getDefaultPipeline(tenantId);
        
        if (response.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(response.get(), "Default pipeline retrieved successfully"));
        } else {
            return ResponseEntity.ok(ApiResponse.success(null, "No default pipeline found"));
        }
    }

    /**
     * Search pipelines
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<PipelineResponse>>> searchPipelines(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Valid @RequestBody PipelineSearchRequest searchRequest) {
        
        logger.debug("Searching pipelines for tenant: {} with criteria: {}", tenantId, searchRequest);
        
        Page<PipelineResponse> response = pipelineService.searchPipelines(tenantId, searchRequest);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline search completed successfully"));
    }

    /**
     * Delete pipeline
     */
    @DeleteMapping("/{pipelineId}")
    public ResponseEntity<ApiResponse<Void>> deletePipeline(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId,
            @PathVariable UUID pipelineId) {
        
        logger.info("Deleting pipeline: {} for tenant: {} by user: {}", pipelineId, tenantId, userId);
        
        pipelineService.deletePipeline(tenantId, pipelineId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Pipeline deleted successfully"));
    }

    /**
     * Clone pipeline from template
     */
    @PostMapping("/{templateId}/clone")
    public ResponseEntity<ApiResponse<PipelineResponse>> clonePipeline(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId,
            @PathVariable UUID templateId,
            @RequestParam String newName) {
        
        logger.info("Cloning pipeline from template: {} for tenant: {} by user: {}", templateId, tenantId, userId);
        
        PipelineResponse response = pipelineService.clonePipeline(tenantId, templateId, newName, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Pipeline cloned successfully"));
    }

    /**
     * Get pipeline templates
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<PipelineResponse>>> getPipelineTemplates(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting pipeline templates for tenant: {}", tenantId);
        
        List<PipelineResponse> response = pipelineService.getPipelineTemplates(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Pipeline templates retrieved successfully"));
    }

    /**
     * Reorder pipelines
     */
    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderPipelines(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId,
            @RequestBody List<UUID> pipelineIds) {
        
        logger.info("Reordering pipelines for tenant: {} by user: {}", tenantId, userId);
        
        pipelineService.reorderPipelines(tenantId, pipelineIds, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Pipelines reordered successfully"));
    }
}