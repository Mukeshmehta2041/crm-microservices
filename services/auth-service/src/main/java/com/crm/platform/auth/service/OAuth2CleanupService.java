package com.crm.platform.auth.service;

import com.crm.platform.auth.repository.OAuth2AccessTokenRepository;
import com.crm.platform.auth.repository.OAuth2AuthorizationCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for cleaning up expired OAuth2 tokens and authorization codes
 */
@Service
@Transactional
public class OAuth2CleanupService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2CleanupService.class);

    @Autowired
    private OAuth2AccessTokenRepository accessTokenRepository;

    @Autowired
    private OAuth2AuthorizationCodeRepository authCodeRepository;

    /**
     * Clean up expired authorization codes every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredAuthorizationCodes() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCodes = authCodeRepository.deleteExpiredCodes(now);
            
            if (deletedCodes > 0) {
                logger.info("Cleaned up {} expired authorization codes", deletedCodes);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired authorization codes", e);
        }
    }

    /**
     * Clean up used authorization codes older than 24 hours
     */
    @Scheduled(fixedRate = 86400000) // 24 hours
    public void cleanupUsedAuthorizationCodes() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
            int deletedCodes = authCodeRepository.deleteUsedCodesOlderThan(cutoff);
            
            if (deletedCodes > 0) {
                logger.info("Cleaned up {} used authorization codes older than 24 hours", deletedCodes);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up used authorization codes", e);
        }
    }

    /**
     * Clean up expired access tokens every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void cleanupExpiredAccessTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedTokens = accessTokenRepository.deleteExpiredTokens(now);
            
            if (deletedTokens > 0) {
                logger.info("Cleaned up {} expired access tokens", deletedTokens);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired access tokens", e);
        }
    }

    /**
     * Clean up revoked tokens older than 30 days
     */
    @Scheduled(fixedRate = 86400000) // 24 hours
    public void cleanupRevokedTokens() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            int deletedTokens = accessTokenRepository.deleteRevokedTokensOlderThan(cutoff);
            
            if (deletedTokens > 0) {
                logger.info("Cleaned up {} revoked tokens older than 30 days", deletedTokens);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up revoked tokens", e);
        }
    }

    /**
     * Manual cleanup method for immediate execution
     */
    public void performManualCleanup() {
        logger.info("Starting manual OAuth2 cleanup");
        
        cleanupExpiredAuthorizationCodes();
        cleanupUsedAuthorizationCodes();
        cleanupExpiredAccessTokens();
        cleanupRevokedTokens();
        
        logger.info("Manual OAuth2 cleanup completed");
    }
}