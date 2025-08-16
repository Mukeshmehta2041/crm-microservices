package com.crm.platform.analytics.controller;

import com.crm.platform.analytics.dto.ReportRequest;
import com.crm.platform.analytics.dto.ReportResponse;
import com.crm.platform.analytics.entity.ReportExecution;
import com.crm.platform.analytics.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody ReportRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Creating report: {} for user: {} in organization: {}", request.getName(), userId, organizationId);
        ReportResponse response = reportService.createReport(request, userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{reportId}")
    public ResponseEntity<ReportResponse> updateReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Updating report: {} for user: {} in organization: {}", reportId, userId, organizationId);
        ReportResponse response = reportService.updateReport(reportId, request, userId, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable Long reportId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting report: {} for organization: {}", reportId, organizationId);
        ReportResponse response = reportService.getReport(reportId, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ReportResponse>> getReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String search,
            @RequestHeader("X-Organization-Id") String organizationId,
            Pageable pageable) {
        
        log.info("Getting reports for organization: {} with filters - type: {}, createdBy: {}, search: {}", 
                organizationId, reportType, createdBy, search);
        Page<ReportResponse> response = reportService.getReports(organizationId, reportType, createdBy, search, pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long reportId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Deleting report: {} for organization: {}", reportId, organizationId);
        reportService.deleteReport(reportId, organizationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reportId}/execute")
    public ResponseEntity<ReportExecution> executeReport(
            @PathVariable Long reportId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Executing report: {} for user: {} in organization: {}", reportId, userId, organizationId);
        ReportExecution execution = reportService.executeReport(reportId, userId, organizationId);
        return ResponseEntity.ok(execution);
    }

    @PostMapping("/{reportId}/execute/async")
    public ResponseEntity<CompletableFuture<ReportExecution>> executeReportAsync(
            @PathVariable Long reportId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Executing report async: {} for user: {} in organization: {}", reportId, userId, organizationId);
        CompletableFuture<ReportExecution> future = reportService.executeReportAsync(reportId, userId, organizationId);
        return ResponseEntity.ok(future);
    }

    @GetMapping("/{reportId}/executions")
    public ResponseEntity<Page<ReportExecution>> getReportExecutions(
            @PathVariable Long reportId,
            @RequestHeader("X-Organization-Id") String organizationId,
            Pageable pageable) {
        
        log.info("Getting executions for report: {} in organization: {}", reportId, organizationId);
        Page<ReportExecution> executions = reportService.getReportExecutions(reportId, organizationId, pageable);
        return ResponseEntity.ok(executions);
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ReportExecution> getReportExecution(
            @PathVariable Long executionId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        
        log.info("Getting report execution: {} for organization: {}", executionId, organizationId);
        ReportExecution execution = reportService.getReportExecution(executionId, organizationId);
        return ResponseEntity.ok(execution);
    }
}