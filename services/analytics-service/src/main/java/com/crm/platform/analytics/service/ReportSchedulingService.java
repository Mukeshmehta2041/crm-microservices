package com.crm.platform.analytics.service;

import com.crm.platform.analytics.entity.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportSchedulingService {

    private final Scheduler scheduler;
    private final ReportService reportService;

    @PostConstruct
    public void initializeScheduledReports() {
        try {
            // This would typically load all organizations and their scheduled reports
            // For now, we'll use a placeholder organization
            String organizationId = "default-org";
            List<Report> scheduledReports = reportService.getScheduledReports(organizationId);
            
            for (Report report : scheduledReports) {
                scheduleReport(report);
            }
            
            log.info("Initialized {} scheduled reports", scheduledReports.size());
        } catch (Exception e) {
            log.error("Error initializing scheduled reports: {}", e.getMessage(), e);
        }
    }

    public void scheduleReport(Report report) {
        try {
            if (!report.getIsScheduled() || report.getScheduleExpression() == null) {
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(ReportExecutionJob.class)
                    .withIdentity("report-" + report.getId(), "reports")
                    .usingJobData("reportId", report.getId())
                    .usingJobData("organizationId", report.getOrganizationId())
                    .usingJobData("createdBy", report.getCreatedBy())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("report-trigger-" + report.getId(), "reports")
                    .withSchedule(CronScheduleBuilder.cronSchedule(report.getScheduleExpression()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled report: {} with expression: {}", report.getName(), report.getScheduleExpression());
            
        } catch (SchedulerException e) {
            log.error("Error scheduling report {}: {}", report.getId(), e.getMessage(), e);
        }
    }

    public void unscheduleReport(Long reportId) {
        try {
            JobKey jobKey = new JobKey("report-" + reportId, "reports");
            scheduler.deleteJob(jobKey);
            log.info("Unscheduled report: {}", reportId);
        } catch (SchedulerException e) {
            log.error("Error unscheduling report {}: {}", reportId, e.getMessage(), e);
        }
    }

    public void rescheduleReport(Report report) {
        unscheduleReport(report.getId());
        if (report.getIsScheduled()) {
            scheduleReport(report);
        }
    }

    @DisallowConcurrentExecution
    public static class ReportExecutionJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            Long reportId = dataMap.getLong("reportId");
            String organizationId = dataMap.getString("organizationId");
            String createdBy = dataMap.getString("createdBy");

            try {
                // Get the report service from the application context
                ReportService reportService = (ReportService) context.getScheduler()
                        .getContext().get("reportService");

                if (reportService != null) {
                    log.info("Executing scheduled report: {} for organization: {}", reportId, organizationId);
                    reportService.executeReport(reportId, createdBy, organizationId);
                } else {
                    log.error("ReportService not found in scheduler context");
                }
            } catch (Exception e) {
                log.error("Error executing scheduled report {}: {}", reportId, e.getMessage(), e);
                throw new JobExecutionException(e);
            }
        }
    }
}