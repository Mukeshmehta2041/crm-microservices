package com.crm.platform.analytics.service;

import com.crm.platform.analytics.dto.ReportResponse;
import com.crm.platform.analytics.entity.Report;
import com.crm.platform.analytics.entity.ReportExecution;
import com.crm.platform.analytics.repository.ReportExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportMapper {

    private final ReportExecutionRepository reportExecutionRepository;

    public ReportResponse toResponse(Report report) {
        ReportResponse.ReportResponseBuilder builder = ReportResponse.builder()
                .id(report.getId())
                .name(report.getName())
                .description(report.getDescription())
                .reportType(report.getReportType())
                .queryDefinition(report.getQueryDefinition())
                .parameters(report.getParameters())
                .createdBy(report.getCreatedBy())
                .organizationId(report.getOrganizationId())
                .isActive(report.getIsActive())
                .isScheduled(report.getIsScheduled())
                .scheduleExpression(report.getScheduleExpression())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt());

        // Add last execution summary
        List<ReportExecution> lastExecutions = reportExecutionRepository
                .findByReportIdOrderByExecutedAtDesc(report.getId(), PageRequest.of(0, 1))
                .getContent();

        if (!lastExecutions.isEmpty()) {
            ReportExecution lastExecution = lastExecutions.get(0);
            builder.lastExecution(ReportResponse.ReportExecutionSummary.builder()
                    .executionId(lastExecution.getId())
                    .status(lastExecution.getStatus().name())
                    .executedAt(lastExecution.getExecutedAt())
                    .executionTimeMs(lastExecution.getExecutionTimeMs())
                    .recordCount(lastExecution.getRecordCount())
                    .errorMessage(lastExecution.getErrorMessage())
                    .build());
        }

        return builder.build();
    }
}