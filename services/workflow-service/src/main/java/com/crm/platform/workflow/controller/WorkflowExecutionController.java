package com.crm.platform.workflow.controller;

import com.crm.platform.workflow.dto.WorkflowExecutionDto;
import com.crm.platform.workflow.entity.WorkflowExecution;
import com.crm.platform.workflow.service.WorkflowExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for workflow execution management
 */
@RestController
@RequestMapping("/api/v1/workflow-executions")
@Tag(name = "Workflow Executions", description = "Workflow execution management operations")
@Validated
public class WorkflowExecutionController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionController.class);

    private final WorkflowExecutionService workflowExecutionService;

    @Autowired
    public WorkflowExecutionController(WorkflowExecutionService workflowExecutionService) {
        this.workflowExecutionService = workflowExecutionService;
    }

    @Operation(summary = "Start workflow execution")
    @PostMapping("/start")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'CREATE')")
    public ResponseEntity<WorkflowExecutionDto> startWorkflow(
            @Parameter(description = "Workflow definition ID") @RequestParam UUID workflowDefinitionId,
            @Parameter(description = "Trigger type") @RequestParam String triggerType,
            @Parameter(description = "Trigger data") @RequestBody(required = false) JsonNode triggerData,
            @Parameter(description = "Initial variables") @RequestParam(required = false) JsonNode variables,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.info("Starting workflow execution for definition: {} tenant: {}", workflowDefinitionId, tenantId);
        
        WorkflowExecutionDto execution = workflowExecutionService.startWorkflow(
            tenantId, workflowDefinitionId, triggerType, triggerData, variables, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(execution);
    }

    @Operation(summary = "Get workflow execution by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public ResponseEntity<WorkflowExecutionDto> getExecution(
            @Parameter(description = "Execution ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting workflow execution: {} for tenant: {}", id, tenantId);
        
        WorkflowExecutionDto execution = workflowExecutionService.getExecution(tenantId, id);
        return ResponseEntity.ok(execution);
    }

    @Operation(summary = "Get workflow execution by execution key")
    @GetMapping("/key/{executionKey}")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public ResponseEntity<WorkflowExecutionDto> getExecutionByKey(
            @Parameter(description = "Execution key") @PathVariable String executionKey,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting workflow execution by key: {} for tenant: {}", executionKey, tenantId);
        
        Optional<WorkflowExecutionDto> execution = workflowExecutionService.getExecutionByKey(tenantId, executionKey);
        return execution.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all workflow executions")
    @GetMapping
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public ResponseEntity<Page<WorkflowExecutionDto>> getExecutions(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) WorkflowExecution.ExecutionStatus status,
            @Parameter(description = "Filter by workflow definition ID") @RequestParam(required = false) UUID workflowDefinitionId,
            @Parameter(description = "Filter by trigger type") @RequestParam(required = false) String triggerType,
            @Parameter(description = "Filter by created by user") @RequestParam(required = false) UUID createdBy,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        
        logger.debug("Getting workflow executions for tenant: {}", tenantId);
        
        Page<WorkflowExecutionDto> executions;
        
        if (workflowDefinitionId != null) {
            executions = workflowExecutionService.getExecutionsByWorkflow(tenantId, workflowDefinitionId, pageable);
        } else if (status != null) {
            executions = workflowExecutionService.getExecutionsByStatus(tenantId, status, pageable);
        } else {
            executions = workflowExecutionService.getExecutions(tenantId, pageable);
        }
        
        return ResponseEntity.ok(executions);
    }

    @Operation(summary = "Get running workflow executions")
    @GetMapping("/running")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public ResponseEntity<List<WorkflowExecutionDto>> getRunningExecutions(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting running workflow executions for tenant: {}", tenantId);
        
        List<WorkflowExecutionDto> executions = workflowExecutionService.getRunningExecutions(tenantId);
        return ResponseEntity.ok(executions);
    }

    @Operation(summary = "Cancel workflow execution")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'UPDATE')")
    public ResponseEntity<WorkflowExecutionDto> cancelExecution(
            @Parameter(description = "Execution ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.info("Cancelling workflow execution: {} for tenant: {}", id, tenantId);
        
        WorkflowExecutionDto execution = workflowExecutionService.cancelExecution(tenantId, id, userId);
        return ResponseEntity.ok(execution);
    }

    @Operation(summary = "Suspend workflow execution")
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'UPDATE')")
    public ResponseEntity<WorkflowExecutionDto> suspendExecution(
            @Parameter(description = "Execution ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.info("Suspending workflow execution: {} for tenant: {}", id, tenantId);
        
        WorkflowExecutionDto execution = workflowExecutionService.suspendExecution(tenantId, id);
        return ResponseEntity.ok(execution);
    }

    @Operation(summary = "Resume workflow execution")
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'UPDATE')")
    public ResponseEntity<WorkflowExecutionDto> resumeExecution(
            @Parameter(description = "Execution ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.info("Resuming workflow execution: {} for tenant: {}", id, tenantId);
        
        WorkflowExecutionDto execution = workflowExecutionService.resumeExecution(tenantId, id);
        return ResponseEntity.ok(execution);
    }

    @Operation(summary = "Retry failed workflow execution")
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'UPDATE')")
    public ResponseEntity<WorkflowExecutionDto> retryExecution(
            @Parameter(description = "Execution ID") @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.info("Retrying workflow execution: {} for tenant: {}", id, tenantId);
        
        WorkflowExecutionDto execution = workflowExecutionService.retryExecution(tenantId, id, userId);
        return ResponseEntity.ok(execution);
    }

    @Operation(summary = "Get workflow execution statistics")
    @GetMapping("/statistics")
    @PreAuthorize("hasPermission(#tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public ResponseEntity<WorkflowExecutionService.ExecutionStatistics> getExecutionStatistics(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("Getting execution statistics for tenant: {}", tenantId);
        
        WorkflowExecutionService.ExecutionStatistics statistics = 
            workflowExecutionService.getExecutionStatistics(tenantId);
        return ResponseEntity.ok(statistics);
    }
}