package com.crm.platform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing security configuration and policies
 */
@Service
public class SecurityConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigurationService.class);

    @Value("${security.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${security.monitoring.suspicious-threshold:10}")
    private int suspiciousThreshold;

    @Value("${security.monitoring.alert-window-hours:1}")
    private int alertWindowHours;

    @Value("${security.audit.retention-days:90}")
    private int auditRetentionDays;

    @Value("${security.brute-force.max-attempts:5}")
    private int maxBruteForceAttempts;

    @Value("${security.brute-force.window-minutes:15}")
    private int bruteForceWindowMinutes;

    @Value("${security.session.max-concurrent:5}")
    private int maxConcurrentSessions;

    @Value("${security.password.min-length:8}")
    private int passwordMinLength;

    @Value("${security.password.require-special-chars:true}")
    private boolean passwordRequireSpecialChars;

    @Value("${security.password.require-numbers:true}")
    private boolean passwordRequireNumbers;

    @Value("${security.password.require-uppercase:true}")
    private boolean passwordRequireUppercase;

    @Value("${security.password.history-count:5}")
    private int passwordHistoryCount;

    @Value("${security.mfa.backup-codes-count:10}")
    private int mfaBackupCodesCount;

    @Value("${security.compliance.score-threshold:80.0}")
    private double complianceScoreThreshold;

    /**
     * Get all security configuration settings
     */
    public Map<String, Object> getSecurityConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Monitoring settings
        config.put("monitoring.enabled", monitoringEnabled);
        config.put("monitoring.suspicious_threshold", suspiciousThreshold);
        config.put("monitoring.alert_window_hours", alertWindowHours);
        
        // Audit settings
        config.put("audit.retention_days", auditRetentionDays);
        
        // Brute force protection
        config.put("brute_force.max_attempts", maxBruteForceAttempts);
        config.put("brute_force.window_minutes", bruteForceWindowMinutes);
        
        // Session settings
        config.put("session.max_concurrent", maxConcurrentSessions);
        
        // Password policy
        config.put("password.min_length", passwordMinLength);
        config.put("password.require_special_chars", passwordRequireSpecialChars);
        config.put("password.require_numbers", passwordRequireNumbers);
        config.put("password.require_uppercase", passwordRequireUppercase);
        config.put("password.history_count", passwordHistoryCount);
        
        // MFA settings
        config.put("mfa.backup_codes_count", mfaBackupCodesCount);
        
        // Compliance settings
        config.put("compliance.score_threshold", complianceScoreThreshold);
        
        return config;
    }

    /**
     * Update security configuration setting
     */
    public void updateSecurityConfiguration(String key, Object value) {
        logger.info("Updating security configuration: {} = {}", key, value);
        
        switch (key) {
            case "monitoring.enabled" -> monitoringEnabled = (Boolean) value;
            case "monitoring.suspicious_threshold" -> suspiciousThreshold = (Integer) value;
            case "monitoring.alert_window_hours" -> alertWindowHours = (Integer) value;
            case "audit.retention_days" -> auditRetentionDays = (Integer) value;
            case "brute_force.max_attempts" -> maxBruteForceAttempts = (Integer) value;
            case "brute_force.window_minutes" -> bruteForceWindowMinutes = (Integer) value;
            case "session.max_concurrent" -> maxConcurrentSessions = (Integer) value;
            case "password.min_length" -> passwordMinLength = (Integer) value;
            case "password.require_special_chars" -> passwordRequireSpecialChars = (Boolean) value;
            case "password.require_numbers" -> passwordRequireNumbers = (Boolean) value;
            case "password.require_uppercase" -> passwordRequireUppercase = (Boolean) value;
            case "password.history_count" -> passwordHistoryCount = (Integer) value;
            case "mfa.backup_codes_count" -> mfaBackupCodesCount = (Integer) value;
            case "compliance.score_threshold" -> complianceScoreThreshold = (Double) value;
            default -> logger.warn("Unknown security configuration key: {}", key);
        }
    }

    /**
     * Get password policy configuration
     */
    public Map<String, Object> getPasswordPolicy() {
        Map<String, Object> policy = new HashMap<>();
        policy.put("min_length", passwordMinLength);
        policy.put("require_special_chars", passwordRequireSpecialChars);
        policy.put("require_numbers", passwordRequireNumbers);
        policy.put("require_uppercase", passwordRequireUppercase);
        policy.put("history_count", passwordHistoryCount);
        return policy;
    }

    /**
     * Get brute force protection settings
     */
    public Map<String, Object> getBruteForceProtection() {
        Map<String, Object> protection = new HashMap<>();
        protection.put("max_attempts", maxBruteForceAttempts);
        protection.put("window_minutes", bruteForceWindowMinutes);
        return protection;
    }

    /**
     * Check if monitoring is enabled
     */
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    /**
     * Get suspicious activity threshold
     */
    public int getSuspiciousThreshold() {
        return suspiciousThreshold;
    }

    /**
     * Get alert window in hours
     */
    public int getAlertWindowHours() {
        return alertWindowHours;
    }

    /**
     * Get audit retention period in days
     */
    public int getAuditRetentionDays() {
        return auditRetentionDays;
    }

    /**
     * Get maximum brute force attempts
     */
    public int getMaxBruteForceAttempts() {
        return maxBruteForceAttempts;
    }

    /**
     * Get brute force window in minutes
     */
    public int getBruteForceWindowMinutes() {
        return bruteForceWindowMinutes;
    }

    /**
     * Get maximum concurrent sessions
     */
    public int getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }

    /**
     * Get compliance score threshold
     */
    public double getComplianceScoreThreshold() {
        return complianceScoreThreshold;
    }

    /**
     * Validate security configuration values
     */
    public boolean validateConfiguration(String key, Object value) {
        try {
            switch (key) {
                case "monitoring.suspicious_threshold":
                    return (Integer) value > 0 && (Integer) value <= 100;
                case "monitoring.alert_window_hours":
                    return (Integer) value > 0 && (Integer) value <= 168; // Max 1 week
                case "audit.retention_days":
                    return (Integer) value >= 30 && (Integer) value <= 2555; // Min 30 days, max 7 years
                case "brute_force.max_attempts":
                    return (Integer) value > 0 && (Integer) value <= 20;
                case "brute_force.window_minutes":
                    return (Integer) value > 0 && (Integer) value <= 1440; // Max 24 hours
                case "session.max_concurrent":
                    return (Integer) value > 0 && (Integer) value <= 50;
                case "password.min_length":
                    return (Integer) value >= 6 && (Integer) value <= 128;
                case "password.history_count":
                    return (Integer) value >= 0 && (Integer) value <= 24;
                case "mfa.backup_codes_count":
                    return (Integer) value >= 5 && (Integer) value <= 20;
                case "compliance.score_threshold":
                    return (Double) value >= 0.0 && (Double) value <= 100.0;
                default:
                    return true; // Allow boolean values and unknown keys
            }
        } catch (ClassCastException e) {
            logger.error("Invalid value type for configuration key: {}", key, e);
            return false;
        }
    }

    /**
     * Reset configuration to defaults
     */
    public void resetToDefaults() {
        logger.info("Resetting security configuration to defaults");
        
        monitoringEnabled = true;
        suspiciousThreshold = 10;
        alertWindowHours = 1;
        auditRetentionDays = 90;
        maxBruteForceAttempts = 5;
        bruteForceWindowMinutes = 15;
        maxConcurrentSessions = 5;
        passwordMinLength = 8;
        passwordRequireSpecialChars = true;
        passwordRequireNumbers = true;
        passwordRequireUppercase = true;
        passwordHistoryCount = 5;
        mfaBackupCodesCount = 10;
        complianceScoreThreshold = 80.0;
    }
}