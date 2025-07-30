package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled cleanup and management of security alerts
 */
@Service
public class SecurityAlertCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertCleanupService.class);

    private final SecurityAlertService securityAlertService;
    private final SecurityAuditService auditService;

    @Autowired
    public SecurityAlertCleanupService(SecurityAlertService securityAlertService,
                                     SecurityAuditService auditService) {
        this.securityAlertService = securityAlertService;
        this.auditService = auditService;
    }

    /**
     * Auto-resolve old alerts every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void autoResolveOldAlerts() {
        try {
            logger.debug("Starting auto-resolve of old security alerts");
            
            securityAlertService.autoResolveOldAlerts();
            
            logger.debug("Completed auto-resolve of old security alerts");
            
        } catch (Exception e) {
            logger.error("Error during auto-resolve of old alerts", e);
        }
    }

    /**
     * Clean up expired alerts daily
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupExpiredAlerts() {
        try {
            logger.info("Starting cleanup of expired security alerts");
            
            securityAlertService.cleanupExpiredAlerts();
            
            logger.info("Completed cleanup of expired security alerts");
            
            // Log maintenance completion
            auditService.logSecurityEvent(null, null, "SECURITY_ALERTS_CLEANUP",
                "Daily security alerts cleanup completed",
                com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.SUCCESS,
                null, null, null);
            
        } catch (Exception e) {
            logger.error("Error during cleanup of expired alerts", e);
        }
    }

    /**
     * Generate alert statistics report every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void generateAlertStatistics() {
        try {
            logger.debug("Generating security alert statistics");
            
            var statistics = securityAlertService.getAlertStatistics(null);
            
            // Log statistics for monitoring
            logger.info("Security Alert Statistics: {}", statistics);
            
            // Check for concerning patterns
            Object totalAlerts = statistics.get("total_alerts");
            Object openAlerts = statistics.get("open_alerts");
            Object highPriorityAlerts = statistics.get("high_priority_alerts");
            
            if (totalAlerts instanceof Number total && total.intValue() > 1000) {
                logger.warn("High number of total alerts detected: {}", total);
                auditService.logSecurityEvent(null, null, "HIGH_ALERT_COUNT",
                    "High number of total alerts detected: " + total,
                    com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
                    null, null, null);
            }
            
            if (openAlerts instanceof Number open && open.intValue() > 100) {
                logger.warn("High number of open alerts detected: {}", open);
                auditService.logSecurityEvent(null, null, "HIGH_OPEN_ALERTS_COUNT",
                    "High number of open alerts detected: " + open,
                    com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
                    null, null, null);
            }
            
            if (highPriorityAlerts instanceof Number highPriority && highPriority.intValue() > 10) {
                logger.warn("High number of high priority alerts detected: {}", highPriority);
                auditService.logSecurityEvent(null, null, "HIGH_PRIORITY_ALERTS_COUNT",
                    "High number of high priority alerts detected: " + highPriority,
                    com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
                    null, null, null);
            }
            
        } catch (Exception e) {
            logger.error("Error generating alert statistics", e);
        }
    }

    /**
     * Manual maintenance trigger
     */
    public void performManualMaintenance() {
        try {
            logger.info("Performing manual security alert maintenance");
            
            securityAlertService.autoResolveOldAlerts();
            securityAlertService.cleanupExpiredAlerts();
            
            logger.info("Manual security alert maintenance completed");
            
        } catch (Exception e) {
            logger.error("Error during manual alert maintenance", e);
            throw new RuntimeException("Manual alert maintenance failed", e);
        }
    }
}