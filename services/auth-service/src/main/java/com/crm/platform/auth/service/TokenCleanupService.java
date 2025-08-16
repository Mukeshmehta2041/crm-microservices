package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service for automatic cleanup of expired tokens and blacklisted entries
 */
@Service
@Transactional
public class TokenCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);

    @Autowired
    private TokenManagementService tokenManagementService;

    @Autowired
    private OAuth2CleanupService oauth2CleanupService;

    /**
     * Clean up expired blacklisted tokens every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void cleanupExpiredBlacklistedTokens() {
        try {
            logger.info("Starting cleanup of expired blacklisted tokens");
            tokenManagementService.cleanupExpiredBlacklistedTokens();
            logger.info("Completed cleanup of expired blacklisted tokens");
        } catch (Exception e) {
            logger.error("Error during blacklisted token cleanup", e);
        }
    }

    /**
     * Clean up OAuth2 tokens and codes every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupOAuth2Tokens() {
        try {
            logger.info("Starting OAuth2 token and code cleanup");
            oauth2CleanupService.performManualCleanup();
            logger.info("Completed OAuth2 token and code cleanup");
        } catch (Exception e) {
            logger.error("Error during OAuth2 cleanup", e);
        }
    }

    /**
     * Comprehensive cleanup - runs daily
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void performDailyCleanup() {
        try {
            logger.info("Starting daily comprehensive token cleanup");
            
            // Clean up blacklisted tokens
            tokenManagementService.cleanupExpiredBlacklistedTokens();
            
            // Clean up OAuth2 tokens and codes
            oauth2CleanupService.performManualCleanup();
            
            logger.info("Completed daily comprehensive token cleanup");
        } catch (Exception e) {
            logger.error("Error during daily cleanup", e);
        }
    }

    /**
     * Manual cleanup method for immediate execution
     */
    public void performManualCleanup() {
        logger.info("Starting manual token cleanup");
        
        cleanupExpiredBlacklistedTokens();
        cleanupOAuth2Tokens();
        
        logger.info("Manual token cleanup completed");
    }
}