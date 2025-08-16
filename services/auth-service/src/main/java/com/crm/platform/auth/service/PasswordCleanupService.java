package com.crm.platform.auth.service;

import com.crm.platform.auth.repository.PasswordHistoryRepository;
import com.crm.platform.auth.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for cleaning up password-related data
 */
@Service
@Transactional
public class PasswordCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordCleanupService.class);

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.security.password.history-retention-days:365}")
    private int passwordHistoryRetentionDays;

    @Value("${app.security.password.reset-token-cleanup-days:7}")
    private int resetTokenCleanupDays;

    @Autowired
    public PasswordCleanupService(PasswordHistoryRepository passwordHistoryRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /**
     * Clean up expired password reset tokens
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredResetTokens() {
        try {
            logger.debug("Starting cleanup of expired password reset tokens");
            
            LocalDateTime now = LocalDateTime.now();
            passwordResetTokenRepository.deleteExpiredTokens(now);
            
            logger.debug("Completed cleanup of expired password reset tokens");
        } catch (Exception e) {
            logger.error("Error during password reset token cleanup", e);
        }
    }

    /**
     * Clean up old password reset tokens
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldResetTokens() {
        try {
            logger.info("Starting cleanup of old password reset tokens");
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(resetTokenCleanupDays);
            passwordResetTokenRepository.deleteTokensOlderThan(cutoffDate);
            
            logger.info("Completed cleanup of old password reset tokens");
        } catch (Exception e) {
            logger.error("Error during old password reset token cleanup", e);
        }
    }

    /**
     * Clean up old password history entries
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldPasswordHistory() {
        try {
            logger.info("Starting cleanup of old password history");
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(passwordHistoryRetentionDays);
            passwordHistoryRepository.deletePasswordHistoryOlderThan(cutoffDate);
            
            logger.info("Completed cleanup of old password history");
        } catch (Exception e) {
            logger.error("Error during password history cleanup", e);
        }
    }
}