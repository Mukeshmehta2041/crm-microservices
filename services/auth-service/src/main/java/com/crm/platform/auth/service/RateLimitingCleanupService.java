package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled cleanup and monitoring of rate limiting components
 */
@Service
public class RateLimitingCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingCleanupService.class);

    private final RateLimitingService rateLimitingService;
    private final CaptchaService captchaService;
    private final SecurityAuditService auditService;

    @Autowired
    public RateLimitingCleanupService(RateLimitingService rateLimitingService,
                                    CaptchaService captchaService,
                                    SecurityAuditService auditService) {
        this.rateLimitingService = rateLimitingService;
        this.captchaService = captchaService;
        this.auditService = auditService;
    }

    /**
     * Clean up expired rate limiting entries every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void cleanupExpiredRateLimitingEntries() {
        try {
            logger.debug("Starting rate limiting cleanup");
            
            rateLimitingService.cleanupExpiredEntries();
            captchaService.cleanupExpiredCaptchas();
            
            logger.debug("Completed rate limiting cleanup");
            
        } catch (Exception e) {
            logger.error("Error during rate limiting cleanup", e);
        }
    }

    /**
     * Monitor and report rate limiting statistics every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void monitorRateLimitingStatistics() {
        try {
            logger.debug("Monitoring rate limiting statistics");
            
            var statistics = rateLimitingService.getRateLimitingStatistics();
            
            // Log statistics for monitoring
            logger.info("Rate Limiting Statistics: {}", statistics);
            
            // Check for concerning patterns
            long blockedIpsCount = (Long) statistics.get("blocked_ips_count");
            long temporaryBlocksCount = (Long) statistics.get("temporary_blocks_count");
            
            if (blockedIpsCount > 100) {
                logger.warn("High number of blocked IPs detected: {}", blockedIpsCount);
                auditService.logSecurityEvent(null, null, "HIGH_BLOCKED_IPS_COUNT",
                    "High number of blocked IPs detected: " + blockedIpsCount,
                    com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
                    null, null, null);
            }
            
            if (temporaryBlocksCount > 50) {
                logger.warn("High number of temporary blocks detected: {}", temporaryBlocksCount);
                auditService.logSecurityEvent(null, null, "HIGH_TEMPORARY_BLOCKS_COUNT",
                    "High number of temporary blocks detected: " + temporaryBlocksCount,
                    com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
                    null, null, null);
            }
            
        } catch (Exception e) {
            logger.error("Error monitoring rate limiting statistics", e);
        }
    }

    /**
     * Daily maintenance tasks
     */
    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    public void dailyMaintenance() {
        try {
            logger.info("Starting daily rate limiting maintenance");
            
            // Perform comprehensive cleanup
            rateLimitingService.cleanupExpiredEntries();
            captchaService.cleanupExpiredCaptchas();
            
            // Generate daily statistics report
            var statistics = rateLimitingService.getRateLimitingStatistics();
            logger.info("Daily Rate Limiting Report: {}", statistics);
            
            // Log maintenance completion
            auditService.logSecurityEvent(null, null, "RATE_LIMITING_MAINTENANCE",
                "Daily rate limiting maintenance completed",
                com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.SUCCESS,
                null, null, null);
            
            logger.info("Completed daily rate limiting maintenance");
            
        } catch (Exception e) {
            logger.error("Error during daily rate limiting maintenance", e);
        }
    }

    /**
     * Manual cleanup trigger
     */
    public void performManualCleanup() {
        try {
            logger.info("Performing manual rate limiting cleanup");
            
            rateLimitingService.cleanupExpiredEntries();
            captchaService.cleanupExpiredCaptchas();
            
            logger.info("Manual rate limiting cleanup completed");
            
        } catch (Exception e) {
            logger.error("Error during manual cleanup", e);
            throw new RuntimeException("Manual cleanup failed", e);
        }
    }
}