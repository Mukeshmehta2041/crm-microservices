package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for cleaning up email verification tokens
 */
@Service
public class EmailVerificationCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationCleanupService.class);

    private final EmailVerificationService emailVerificationService;

    @Autowired
    public EmailVerificationCleanupService(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Clean up expired email verification tokens
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTokens() {
        try {
            logger.debug("Starting cleanup of expired email verification tokens");
            emailVerificationService.cleanupExpiredTokens();
            logger.debug("Completed cleanup of expired email verification tokens");
        } catch (Exception e) {
            logger.error("Error during email verification token cleanup", e);
        }
    }
}