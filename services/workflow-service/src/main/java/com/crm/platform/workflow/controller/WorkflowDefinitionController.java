package com.crm.platform.workflow.controller;

import com.crm.platform.workflow.dto.WorkflowDefinitionDto;
import com.crm.platform.workflow.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for workflow definition management
 */
@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow Definitions", description = "Workflow definition management operations")
@Validated
public class WorkflowDefinitionController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionController.class);

    private final WorkflowDefinitionService workflowDefinitionService;

    @Autowired
    public WorkflowDefinitionController(WorkflowDefinitionService workflowDefinitionService) {
        this.workflowDefinitionService = workflowDefinitionService;
    }

    @Operation(summary = "Create workflow definition")
    @PostMapping
    @PreAuthorize("hasPermission(#dto.tenantId, 'WORKFLOW', 'CREATE')")
    public ResponseEntity<WorkflowDefinitionDto> createWorkflow(
            @Valid @RequestBody WorkflowDefinitionDto dto,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.info("Creating workflow definition: {} for tenant: {}", dto.getName(), tenantId);
        
        dto.setTenantId(tenantId);
        dto.setCreatedBy(userId);
        dto.setUpdatedBy(userId);
        
        WorkflowDefinitionDto createdWorkflow = workflowDefinitionService.createWorkflow(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWorkflow);
    }

    @Operation(summary = "Update workflow definition")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'UPDATE')")
    public ResponseEntity<WorkflowDefinitionDto> updateWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID id,
            @Valid @RequestBody WorkflowDefinitionDto dto,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.info("Updating workflow definition: {} for tenant: {}", id, tenantId);
        
        dto.setTenantId(tenantId);
        dto.setUpdatedBy(userId);
        
        WorkflowDefinitionDto updatedWorkflow = workflowDefinitionService.updateWorkflow(id, dto);
        return ResponseEntity.ok(updatedWorkflow);
    }

    @Operation(summary = "Get workflow definition by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'READ')")
    public ResponseEntity<WorkflowDefinitionDto> getWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting workflow definition: {} for tenant: {}", id, tenantId);
        
        WorkflowDefinitionDto workflow = workflowDefinitionService.getWorkflow(tenantId, id);
        return ResponseEntity.ok(workflow);
    }

    @Operation(summary = "Get all workflow definitions")
    @GetMapping
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'READ')")
    public ResponseEntity<Page<WorkflowDefinitionDto>> getWorkflows(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Include only active workflows") @RequestParam(defaultValue = "false") boolean activeOnly,
            @Parameter(description = "Include only published workflows") @RequestParam(defaultValue = "false") boolean publishedOnly,
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Search by name") @RequestParam(required = false) String search,
            Pageable pageable) {
        
        logger.debug("Getting workflow definitions for tenant: {}", tenantId);
        
        Page<WorkflowDefinitionDto> workflows;
        
        if (search != null && !search.trim().isEmpty()) {
            workflows = workflowDefinitionService.searchWorkflows(tenantId, search, pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            workflows = workflowDefinitionService.getWorkflowsByCategory(tenantId, category, pageable);
        } else if (publishedOnly) {
            workflows = workflowDefinitionService.getPublishedWorkflows(tenantId, pageable);
        } else if (activeOnly) {
            workflows = workflowDefinitionService.getActiveWorkflows(tenantId, pageable);
        } else {
            workflows = workflowDefinitionService.getWorkflows(tenantId, pageable);
        }
        
        return ResponseEntity.ok(workflows);
    }

    @Operation(summary = "Get workflow versions by name")
    @GetMapping("/versions/{name}")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'READ')")
    public ResponseEntity<List<WorkflowDefinitionDto>> getWorkflowVersions(
            @Parameter(description = "Workflow name") @PathVariable String name,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting workflow versions for: {} tenant: {}", name, tenantId);
        
        List<WorkflowDefinitionDto> versions = workflowDefinitionService.getWorkflowVersions(tenantId, name);
        return ResponseEntity.ok(versions);
    }

    @Operation(summary = "Get latest workflow version by name")
    @GetMapping("/latest/{name}")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'READ')")
    public ResponseEntity<WorkflowDefinitionDto> getLatestWorkflowVersion(
            @Parameter(description = "Workflow name") @PathVariable String name,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting latest workflow version for: {} tenant: {}", name, tenantId);
        
        Optional<WorkflowDefinitionDto> workflow = workflowDefinitionService.getLatestWorkflowVersion(tenantId, name);
        return workflow.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Publish workflow definition")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'UPDATE')")
    public ResponseEntity<WorkflowDefinitionDto> publishWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.info("Publishing workflow definition: {} for tenant: {}", id, tenantId);
        
        WorkflowDefinitionDto publishedWorkflow = workflowDefinitionService.publishWorkflow(tenantId, id);
        return ResponseEntity.ok(publishedWorkflow);
    }

    @Operation(summary = "Unpublish workflow definition")
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'UPDATE')")
    public ResponseEntity<WorkflowDefinitionDto> unpublishWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.info("Unpublishing workflow definition: {} for tenant: {}", id, tenantId);
        
        WorkflowDefinitionDto unpublishedWorkflow = workflowDefinitionService.unpublishWorkflow(tenantId, id);
        return ResponseEntity.ok(unpublishedWorkflow);
    }

    @Operation(summary = "Activate workflow definition")
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'UPDATE')")
    public ResponseEntity<WorkflowDefinitionDto> activateWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.info("Activating workflow definition: {} for tenant: {}", id, tenantId);
        
        WorkflowDefinitionDto activatedWorkflow = workflowDefinitionService.toggleWorkflowStatus(tenantId, id, true);
        return ResponseEntity.ok(activatedWorkflow);
    }

    @Operation(summary = "Deactivate workflow definition")
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'UPDATE')")
    public ResponseEntity<WorkflowDefinitionDto> deactivateWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.info("Deactivating workflow definition: {} for tenant: {}", id, tenantId);
        
        WorkflowDefinitionDto deactivatedWorkflow = workflowDefinitionService.toggleWorkflowStatus(tenantId, id, false);
        return ResponseEntity.ok(deactivatedWorkflow);
    }

    @Operation(summary = "Clone workflow definition")
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'CREATE')")
    public ResponseEntity<WorkflowDefinitionDto> cloneWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID id,
            @Parameter(description = "New workflow name") @RequestParam String newName,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.info("Cloning workflow definition: {} to {} for tenant: {}", id, newName, tenantId);
        
        WorkflowDefinitionDto clonedWorkflow = workflowDefinitionService.cloneWorkflow(tenantId, id, newName);
        return ResponseEntity.status(HttpStatus.CREATED).body(clonedWorkflow);
    }

    @Operation(summary = "Delete workflow definition")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'DELETE')")
    public ResponseEntity<Void> deleteWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.info("Deleting workflow definition: {} for tenant: {}", id, tenantId);
        
        workflowDefinitionService.deleteWorkflow(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get workflow statistics")
    @GetMapping("/statistics")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW', 'READ')")
    public ResponseEntity<WorkflowDefinitionService.WorkflowStatistics> getWorkflowStatistics(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting workflow statistics for tenant: {}", tenantId);
        
        WorkflowDefinitionService.WorkflowStatistics statistics = 
            workflowDefinitionService.getWorkflowStatistics(tenantId);
        return ResponseEntity.ok(statistics);
    }
}