package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service for session cleanup and monitoring
 */
@Service
@Transactional
public class SessionCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(SessionCleanupService.class);

    @Autowired
    private SessionService sessionService;

    /**
     * Clean up expired sessions every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredSessions() {
        try {
            logger.info("Starting expired sessions cleanup");
            sessionService.cleanupExpiredSessions();
            logger.info("Completed expired sessions cleanup");
        } catch (Exception e) {
            logger.error("Error during expired sessions cleanup", e);
        }
    }

    /**
     * Monitor for suspicious activity every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void monitorSuspiciousActivity() {
        try {
            logger.debug("Starting suspicious activity monitoring");
            sessionService.monitorSuspiciousActivity();
            logger.debug("Completed suspicious activity monitoring");
        } catch (Exception e) {
            logger.error("Error during suspicious activity monitoring", e);
        }
    }

    /**
     * Comprehensive session maintenance - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void performDailyMaintenance() {
        try {
            logger.info("Starting daily session maintenance");
            
            // Clean up expired sessions
            sessionService.cleanupExpiredSessions();
            
            // Monitor suspicious activity
            sessionService.monitorSuspiciousActivity();
            
            logger.info("Completed daily session maintenance");
        } catch (Exception e) {
            logger.error("Error during daily session maintenance", e);
        }
    }

    /**
     * Manual cleanup method for immediate execution
     */
    public void performManualCleanup() {
        logger.info("Starting manual session cleanup");
        
        cleanupExpiredSessions();
        monitorSuspiciousActivity();
        
        logger.info("Manual session cleanup completed");
    }
}