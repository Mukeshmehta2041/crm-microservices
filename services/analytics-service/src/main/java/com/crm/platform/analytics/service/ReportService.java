package com.crm.platform.analytics.service;

import com.crm.platform.analytics.dto.ReportRequest;
import com.crm.platform.analytics.dto.ReportResponse;
import com.crm.platform.analytics.entity.Report;
import com.crm.platform.analytics.entity.ReportExecution;
import com.crm.platform.analytics.repository.ReportRepository;
import com.crm.platform.analytics.repository.ReportExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportExecutionRepository reportExecutionRepository;
    private final AnalyticsQueryService analyticsQueryService;
    private final ReportMapper reportMapper;

    @Transactional
    public ReportResponse createReport(ReportRequest request, String userId, String organizationId) {
        // Validate report name uniqueness
        if (reportRepository.existsByNameAndOrganizationId(request.getName(), organizationId)) {
            throw new IllegalArgumentException("Report with name '" + request.getName() + "' already exists");
        }

        Report report = Report.builder()
                .name(request.getName())
                .description(request.getDescription())
                .reportType(request.getReportType())
                .queryDefinition(request.getQueryDefinition())
                .parameters(request.getParameters())
                .createdBy(userId)
                .organizationId(organizationId)
                .isActive(true)
                .isScheduled(request.getIsScheduled())
                .scheduleExpression(request.getScheduleExpression())
                .build();

        Report savedReport = reportRepository.save(report);
        log.info("Created report: {} for organization: {}", savedReport.getName(), organizationId);

        return reportMapper.toResponse(savedReport);
    }

    @Transactional
    public ReportResponse updateReport(Long reportId, ReportRequest request, String userId, String organizationId) {
        Report report = reportRepository.findByIdAndOrganizationId(reportId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        // Check name uniqueness if name is being changed
        if (!report.getName().equals(request.getName()) && 
            reportRepository.existsByNameAndOrganizationId(request.getName(), organizationId)) {
            throw new IllegalArgumentException("Report with name '" + request.getName() + "' already exists");
        }

        report.setName(request.getName());
        report.setDescription(request.getDescription());
        report.setReportType(request.getReportType());
        report.setQueryDefinition(request.getQueryDefinition());
        report.setParameters(request.getParameters());
        report.setIsScheduled(request.getIsScheduled());
        report.setScheduleExpression(request.getScheduleExpression());

        Report updatedReport = reportRepository.save(report);
        log.info("Updated report: {} for organization: {}", updatedReport.getName(), organizationId);

        return reportMapper.toResponse(updatedReport);
    }

    public ReportResponse getReport(Long reportId, String organizationId) {
        Report report = reportRepository.findByIdAndOrganizationId(reportId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        return reportMapper.toResponse(report);
    }

    public Page<ReportResponse> getReports(String organizationId, String reportType, String createdBy, 
                                         String searchTerm, Pageable pageable) {
        Page<Report> reports = reportRepository.findReportsWithFilters(
                organizationId, reportType, createdBy, searchTerm, pageable);

        return reports.map(reportMapper::toResponse);
    }

    @Transactional
    public void deleteReport(Long reportId, String organizationId) {
        Report report = reportRepository.findByIdAndOrganizationId(reportId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.setIsActive(false);
        reportRepository.save(report);
        log.info("Deleted report: {} for organization: {}", report.getName(), organizationId);
    }

    public CompletableFuture<ReportExecution> executeReportAsync(Long reportId, String userId, String organizationId) {
        return CompletableFuture.supplyAsync(() -> executeReport(reportId, userId, organizationId));
    }

    @Transactional
    public ReportExecution executeReport(Long reportId, String userId, String organizationId) {
        Report report = reportRepository.findByIdAndOrganizationId(reportId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        // Create execution record
        ReportExecution execution = ReportExecution.builder()
                .report(report)
                .status(ReportExecution.ExecutionStatus.PENDING)
                .executedBy(userId)
                .build();

        execution = reportExecutionRepository.save(execution);
        log.info("Started report execution: {} for report: {}", execution.getId(), report.getName());

        try {
            // Update status to running
            execution.setStatus(ReportExecution.ExecutionStatus.RUNNING);
            execution = reportExecutionRepository.save(execution);

            long startTime = System.currentTimeMillis();

            // Execute the report query
            String resultData = executeReportQuery(report, organizationId);
            
            long executionTime = System.currentTimeMillis() - startTime;

            // Update execution with results
            execution.setStatus(ReportExecution.ExecutionStatus.COMPLETED);
            execution.setResultData(resultData);
            execution.setExecutionTimeMs(executionTime);
            execution.setRecordCount(countRecordsInResult(resultData));

            execution = reportExecutionRepository.save(execution);
            log.info("Completed report execution: {} in {}ms", execution.getId(), executionTime);

        } catch (Exception e) {
            log.error("Error executing report: {}", e.getMessage(), e);
            execution.setStatus(ReportExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution = reportExecutionRepository.save(execution);
        }

        return execution;
    }

    private String executeReportQuery(Report report, String organizationId) {
        // This would integrate with the analytics query service
        // For now, return a placeholder
        return "{}"; // JSON result data
    }

    private Integer countRecordsInResult(String resultData) {
        // Parse JSON and count records
        // For now, return a placeholder
        return 0;
    }

    public Page<ReportExecution> getReportExecutions(Long reportId, String organizationId, Pageable pageable) {
        Report report = reportRepository.findByIdAndOrganizationId(reportId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        return reportExecutionRepository.findByReportIdOrderByExecutedAtDesc(reportId, pageable);
    }

    public List<Report> getScheduledReports(String organizationId) {
        return reportRepository.findByOrganizationIdAndIsActiveTrueAndIsScheduledTrue(organizationId);
    }

    public ReportExecution getReportExecution(Long executionId, String organizationId) {
        ReportExecution execution = reportExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Report execution not found"));

        if (!execution.getReport().getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Report execution not found");
        }

        return execution;
    }
}