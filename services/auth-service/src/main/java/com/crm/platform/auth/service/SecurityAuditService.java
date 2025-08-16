package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.ComplianceReport;
import com.crm.platform.auth.dto.SecurityMetrics;
import com.crm.platform.auth.dto.SuspiciousActivityAlert;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.entity.UserSession;
import com.crm.platform.auth.repository.SecurityAuditLogRepository;
import com.crm.platform.auth.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SecurityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);

    private final SecurityAuditLogRepository auditLogRepository;
    private final UserSessionRepository sessionRepository;

    @Value("${security.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${security.monitoring.suspicious-threshold:10}")
    private int suspiciousThreshold;

    @Value("${security.monitoring.alert-window-hours:1}")
    private int alertWindowHours;

    @Autowired
    public SecurityAuditService(SecurityAuditLogRepository auditLogRepository,
                               UserSessionRepository sessionRepository) {
        this.auditLogRepository = auditLogRepository;
        this.sessionRepository = sessionRepository;
    }

    @Async
    public void logSecurityEvent(UUID userId, UUID tenantId, String eventType, 
                               String description, SecurityAuditLog.AuditEventStatus status,
                               String ipAddress, String userAgent, String sessionId) {
        SecurityAuditLog auditLog = new SecurityAuditLog(userId, tenantId, eventType, description, status);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setSessionId(sessionId);
        
        auditLogRepository.save(auditLog);
    }

    @Async
    public void logSecurityEvent(UUID userId, UUID tenantId, String eventType, 
                               String description, SecurityAuditLog.AuditEventStatus status,
                               String ipAddress, String userAgent, String sessionId, 
                               String additionalData) {
        SecurityAuditLog auditLog = new SecurityAuditLog(userId, tenantId, eventType, description, status);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setSessionId(sessionId);
        auditLog.setAdditionalData(additionalData);
        
        auditLogRepository.save(auditLog);
    }

    // OAuth2-specific audit methods

    @Async
    public void logOAuth2Authorization(UUID userId, String clientId, String scope) {
        logSecurityEvent(userId, null, "OAUTH2_AUTHORIZATION", 
                        "OAuth2 authorization granted for client: " + clientId + ", scope: " + scope,
                        SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
    }

    @Async
    public void logOAuth2Login(UUID userId, String provider, String email) {
        logSecurityEvent(userId, null, "OAUTH2_LOGIN", 
            "OAuth2 login via " + provider + " for email: " + email,
            SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
    }

    @Async
    public void logOAuth2AccountLink(UUID userId, String provider, String email) {
        logSecurityEvent(userId, null, "OAUTH2_ACCOUNT_LINKED", 
            "OAuth2 account linked: " + provider + " (" + email + ")",
            SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
    }

    @Async
    public void logOAuth2AccountUnlink(UUID userId, String provider, String email) {
        logSecurityEvent(userId, null, "OAUTH2_ACCOUNT_UNLINKED", 
            "OAuth2 account unlinked: " + provider + " (" + email + ")",
            SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
    }

    @Async
    public void logTokenGeneration(UUID userId, String clientId, String grantType) {
        logSecurityEvent(userId, null, "OAUTH2_TOKEN_GENERATED", 
                        "OAuth2 token generated for client: " + clientId + ", grant type: " + grantType,
                        SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
    }

    @Async
    public void logTokenRefresh(UUID userId, String clientId) {
        logSecurityEvent(userId, null, "OAUTH2_TOKEN_REFRESHED", 
                        "OAuth2 token refreshed for client: " + clientId,
                        SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
    }

    @Async
    public void logTokenRevocation(UUID userId, String clientId, String token) {
        logSecurityEvent(userId, null, "OAUTH2_TOKEN_REVOKED", 
                        "OAuth2 token revoked for client: " + clientId,
                        SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
    }

    // Password management audit methods

    @Async
    public void logPasswordResetRequest(UUID userId, String ipAddress, String userAgent) {
        logSecurityEvent(userId, null, "PASSWORD_RESET_REQUESTED", 
                        "Password reset requested",
                        SecurityAuditLog.AuditEventStatus.SUCCESS, ipAddress, userAgent, null);
    }

    @Async
    public void logPasswordReset(UUID userId, String ipAddress, String userAgent) {
        logSecurityEvent(userId, null, "PASSWORD_RESET_COMPLETED", 
                        "Password reset completed successfully",
                        SecurityAuditLog.AuditEventStatus.SUCCESS, ipAddress, userAgent, null);
    }

    @Async
    public void logPasswordChange(UUID userId, String ipAddress, String userAgent) {
        logSecurityEvent(userId, null, "PASSWORD_CHANGED", 
                        "Password changed successfully",
                        SecurityAuditLog.AuditEventStatus.SUCCESS, ipAddress, userAgent, null);
    }

    @Async
    public void logFailedPasswordChange(UUID userId, String ipAddress, String userAgent) {
        logSecurityEvent(userId, null, "PASSWORD_CHANGE_FAILED", 
                        "Password change failed - incorrect current password",
                        SecurityAuditLog.AuditEventStatus.FAILURE, ipAddress, userAgent, null);
    }

    // Email verification audit methods

    @Async
    public void logEmailVerificationTokenGenerated(UUID userId, String email, String verificationType, 
                                                 String ipAddress, String userAgent) {
        logSecurityEvent(userId, null, "EMAIL_VERIFICATION_TOKEN_GENERATED", 
                        "Email verification token generated for " + email + " (type: " + verificationType + ")",
                        SecurityAuditLog.AuditEventStatus.SUCCESS, ipAddress, userAgent, null);
    }

    @Async
    public void logEmailVerificationSuccess(UUID userId, String email, String verificationType, 
                                          String ipAddress, String userAgent) {
        logSecurityEvent(userId, null, "EMAIL_VERIFICATION_SUCCESS", 
                        "Email verification successful for " + email + " (type: " + verificationType + ")",
                        SecurityAuditLog.AuditEventStatus.SUCCESS, ipAddress, userAgent, null);
    }

    @Async
    public void logEmailVerificationFailed(UUID userId, String email, String reason, 
                                         String ipAddress, String userAgent) {
        logSecurityEvent(userId, null, "EMAIL_VERIFICATION_FAILED", 
                        "Email verification failed for " + email + " - " + reason,
                        SecurityAuditLog.AuditEventStatus.FAILURE, ipAddress, userAgent, null);
    }

    // Enhanced security monitoring methods

    /**
     * Generate comprehensive security metrics for a given time period
     */
    public SecurityMetrics generateSecurityMetrics(LocalDateTime startTime, LocalDateTime endTime, UUID tenantId) {
        logger.debug("Generating security metrics for period {} to {}", startTime, endTime);
        
        SecurityMetrics metrics = new SecurityMetrics(startTime, endTime);
        
        try {
            // Basic authentication metrics
            metrics.setSuccessfulLogins(countEventsByTypeAndPeriod("LOGIN_SUCCESS", startTime, endTime, tenantId));
            metrics.setFailedLogins(countEventsByTypeAndPeriod("LOGIN_FAILURE", startTime, endTime, tenantId));
            metrics.setTotalLoginAttempts(metrics.getSuccessfulLogins() + metrics.getFailedLogins());
            
            // Security event metrics
            metrics.setAccountLockouts(countEventsByTypeAndPeriod("ACCOUNT_LOCKED", startTime, endTime, tenantId));
            metrics.setBruteForceAttempts(countEventsByTypeAndPeriod("BRUTE_FORCE_ATTEMPT", startTime, endTime, tenantId));
            metrics.setSuspiciousActivities(countSuspiciousActivities(startTime, endTime, tenantId));
            
            // MFA metrics
            metrics.setMfaChallenges(countEventsByTypeAndPeriod("MFA_CHALLENGE_ISSUED", startTime, endTime, tenantId));
            metrics.setMfaSuccesses(countEventsByTypeAndPeriod("MFA_VERIFICATION_SUCCESS", startTime, endTime, tenantId));
            metrics.setMfaFailures(countEventsByTypeAndPeriod("MFA_VERIFICATION_FAILED", startTime, endTime, tenantId));
            
            // Password and token metrics
            metrics.setPasswordResets(countEventsByTypeAndPeriod("PASSWORD_RESET_COMPLETED", startTime, endTime, tenantId));
            metrics.setTokenRefreshes(countEventsByTypeAndPeriod("TOKEN_REFRESH", startTime, endTime, tenantId));
            metrics.setOauth2Authorizations(countEventsByTypeAndPeriod("OAUTH2_AUTHORIZATION", startTime, endTime, tenantId));
            
            // User and session metrics
            metrics.setUniqueUsers(countUniqueUsers(startTime, endTime, tenantId));
            metrics.setUniqueIpAddresses(countUniqueIpAddresses(startTime, endTime, tenantId));
            metrics.setActiveSessions(countActiveSessions(tenantId));
            
            // Distribution metrics
            metrics.setTopFailureReasons(getTopFailureReasons(startTime, endTime, tenantId));
            metrics.setGeographicDistribution(getGeographicDistribution(startTime, endTime, tenantId));
            metrics.setDeviceTypeDistribution(getDeviceTypeDistribution(startTime, endTime, tenantId));
            metrics.setHourlyActivity(getHourlyActivity(startTime, endTime, tenantId));
            
            logger.debug("Generated security metrics with {} total login attempts", metrics.getTotalLoginAttempts());
            
        } catch (Exception e) {
            logger.error("Error generating security metrics", e);
        }
        
        return metrics;
    }

    /**
     * Generate compliance report for audit purposes
     */
    public ComplianceReport generateComplianceReport(ComplianceReport.ReportType reportType, 
                                                   String tenantId, 
                                                   LocalDateTime startTime, 
                                                   LocalDateTime endTime) {
        logger.debug("Generating compliance report of type {} for tenant {}", reportType, tenantId);
        
        ComplianceReport report = new ComplianceReport(reportType, tenantId, startTime, endTime);
        report.setReportId(UUID.randomUUID().toString());
        
        try {
            UUID tenantUuid = tenantId != null ? UUID.fromString(tenantId) : null;
            
            // Calculate total events
            report.setTotalEvents(countAllEvents(startTime, endTime, tenantUuid));
            
            // Security events summary
            ComplianceReport.SecurityEventsSummary securityEvents = new ComplianceReport.SecurityEventsSummary();
            securityEvents.setAuthenticationEvents(countAuthenticationEvents(startTime, endTime, tenantUuid));
            securityEvents.setAuthorizationEvents(countAuthorizationEvents(startTime, endTime, tenantUuid));
            securityEvents.setFailedAttempts(countEventsByTypeAndPeriod("LOGIN_FAILURE", startTime, endTime, tenantUuid));
            securityEvents.setAccountLockouts(countEventsByTypeAndPeriod("ACCOUNT_LOCKED", startTime, endTime, tenantUuid));
            securityEvents.setSuspiciousActivities(countSuspiciousActivities(startTime, endTime, tenantUuid));
            securityEvents.setPrivilegeEscalations(countPrivilegeEscalations(startTime, endTime, tenantUuid));
            report.setSecurityEvents(securityEvents);
            
            // Access patterns summary
            ComplianceReport.AccessPatternsSummary accessPatterns = new ComplianceReport.AccessPatternsSummary();
            accessPatterns.setUniqueUsers(countUniqueUsers(startTime, endTime, tenantUuid));
            accessPatterns.setPeakConcurrentSessions(calculatePeakConcurrentSessions(startTime, endTime, tenantUuid));
            accessPatterns.setAverageSessionDuration(calculateAverageSessionDuration(startTime, endTime, tenantUuid));
            accessPatterns.setGeographicLocations(getGeographicDistribution(startTime, endTime, tenantUuid));
            accessPatterns.setDeviceTypes(getDeviceTypeDistribution(startTime, endTime, tenantUuid));
            accessPatterns.setAccessTimes(getAccessTimeDistribution(startTime, endTime, tenantUuid));
            report.setAccessPatterns(accessPatterns);
            
            // Policy violations
            report.setPolicyViolations(identifyPolicyViolations(startTime, endTime, tenantUuid));
            
            // Risk indicators
            report.setRiskIndicators(calculateRiskIndicators(startTime, endTime, tenantUuid));
            
            // Generate recommendations
            report.setRecommendations(generateSecurityRecommendations(report));
            
            // Calculate compliance score
            report.setComplianceScore(calculateComplianceScore(report));
            
            logger.debug("Generated compliance report with {} total events", report.getTotalEvents());
            
        } catch (Exception e) {
            logger.error("Error generating compliance report", e);
        }
        
        return report;
    }

    /**
     * Detect and alert on suspicious activities
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void monitorSuspiciousActivity() {
        if (!monitoringEnabled) {
            return;
        }
        
        logger.debug("Starting suspicious activity monitoring");
        
        try {
            LocalDateTime alertWindow = LocalDateTime.now().minusHours(alertWindowHours);
            
            // Check for brute force attacks
            detectBruteForceAttacks(alertWindow);
            
            // Check for unusual access patterns
            detectUnusualAccessPatterns(alertWindow);
            
            // Check for privilege escalation attempts
            detectPrivilegeEscalationAttempts(alertWindow);
            
            // Check for geographic anomalies
            detectGeographicAnomalies(alertWindow);
            
            logger.debug("Completed suspicious activity monitoring");
            
        } catch (Exception e) {
            logger.error("Error during suspicious activity monitoring", e);
        }
    }

    /**
     * Get security audit logs with filtering and pagination
     */
    public Page<SecurityAuditLog> getAuditLogs(UUID userId, UUID tenantId, String eventType, 
                                             LocalDateTime startTime, LocalDateTime endTime, 
                                             Pageable pageable) {
        if (userId != null) {
            return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        } else if (tenantId != null) {
            return auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
        } else {
            // Return all logs with pagination (admin only)
            return auditLogRepository.findAll(pageable);
        }
    }

    /**
     * Clean up old audit logs based on retention policy
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void cleanupOldAuditLogs() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90); // 90-day retention
            auditLogRepository.deleteByTimestampBefore(cutoffDate);
            logger.info("Cleaned up audit logs older than {}", cutoffDate);
        } catch (Exception e) {
            logger.error("Error cleaning up old audit logs", e);
        }
    }

    // Private helper methods

    private long countEventsByTypeAndPeriod(String eventType, LocalDateTime start, LocalDateTime end, UUID tenantId) {
        return auditLogRepository.findByEventTypeAndTimestampBetween(eventType, start, end)
                .stream()
                .filter(log -> tenantId == null || Objects.equals(log.getTenantId(), tenantId))
                .count();
    }

    private long countSuspiciousActivities(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        List<String> suspiciousEventTypes = Arrays.asList(
            "BRUTE_FORCE_ATTEMPT", "ACCOUNT_LOCKED", "UNUSUAL_LOCATION", 
            "MULTIPLE_DEVICES", "MULTIPLE_LOCATIONS"
        );
        
        return suspiciousEventTypes.stream()
                .mapToLong(eventType -> countEventsByTypeAndPeriod(eventType, start, end, tenantId))
                .sum();
    }

    private long countUniqueUsers(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        return auditLogRepository.findByEventTypeAndTimestampBetween("LOGIN_SUCCESS", start, end)
                .stream()
                .filter(log -> tenantId == null || Objects.equals(log.getTenantId(), tenantId))
                .map(SecurityAuditLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long countUniqueIpAddresses(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        return auditLogRepository.findByEventTypeAndTimestampBetween("LOGIN_SUCCESS", start, end)
                .stream()
                .filter(log -> tenantId == null || Objects.equals(log.getTenantId(), tenantId))
                .map(SecurityAuditLog::getIpAddress)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long countActiveSessions(UUID tenantId) {
        return sessionRepository.findAll()
                .stream()
                .filter(session -> tenantId == null || Objects.equals(session.getTenantId(), tenantId))
                .filter(session -> !session.isExpired())
                .count();
    }

    private Map<String, Long> getTopFailureReasons(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        return auditLogRepository.findByEventTypeAndTimestampBetween("LOGIN_FAILURE", start, end)
                .stream()
                .filter(log -> tenantId == null || Objects.equals(log.getTenantId(), tenantId))
                .collect(Collectors.groupingBy(
                    SecurityAuditLog::getEventDescription,
                    Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
    }

    private Map<String, Long> getGeographicDistribution(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        // This would typically integrate with a GeoIP service
        // For now, return a placeholder implementation
        return new HashMap<>();
    }

    private Map<String, Long> getDeviceTypeDistribution(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        // Extract device type from user agent strings
        return new HashMap<>();
    }

    private Map<Integer, Long> getHourlyActivity(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        return auditLogRepository.findByEventTypeAndTimestampBetween("LOGIN_SUCCESS", start, end)
                .stream()
                .filter(log -> tenantId == null || Objects.equals(log.getTenantId(), tenantId))
                .collect(Collectors.groupingBy(
                    log -> log.getTimestamp().getHour(),
                    Collectors.counting()
                ));
    }

    private long countAllEvents(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        // This would need a custom query for better performance
        return auditLogRepository.findAll()
                .stream()
                .filter(log -> log.getTimestamp().isAfter(start) && log.getTimestamp().isBefore(end))
                .filter(log -> tenantId == null || Objects.equals(log.getTenantId(), tenantId))
                .count();
    }

    private long countAuthenticationEvents(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        List<String> authEventTypes = Arrays.asList("LOGIN_SUCCESS", "LOGIN_FAILURE", "LOGOUT");
        return authEventTypes.stream()
                .mapToLong(eventType -> countEventsByTypeAndPeriod(eventType, start, end, tenantId))
                .sum();
    }

    private long countAuthorizationEvents(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        List<String> authzEventTypes = Arrays.asList("OAUTH2_AUTHORIZATION", "TOKEN_REFRESH");
        return authzEventTypes.stream()
                .mapToLong(eventType -> countEventsByTypeAndPeriod(eventType, start, end, tenantId))
                .sum();
    }

    private long countPrivilegeEscalations(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        // This would track role changes and permission escalations
        return 0; // Placeholder
    }

    private long calculatePeakConcurrentSessions(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        // This would require time-series analysis of session data
        return 0; // Placeholder
    }

    private double calculateAverageSessionDuration(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        // Calculate average session duration from session data
        return 0.0; // Placeholder
    }

    private Map<String, Long> getAccessTimeDistribution(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        // Group access by time periods (morning, afternoon, evening, night)
        return new HashMap<>();
    }

    private List<ComplianceReport.PolicyViolation> identifyPolicyViolations(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        List<ComplianceReport.PolicyViolation> violations = new ArrayList<>();
        
        // Check for excessive failed login attempts
        long failedLogins = countEventsByTypeAndPeriod("LOGIN_FAILURE", start, end, tenantId);
        if (failedLogins > 100) {
            violations.add(new ComplianceReport.PolicyViolation(
                "EXCESSIVE_FAILED_LOGINS",
                "Excessive failed login attempts detected",
                ComplianceReport.Severity.HIGH,
                failedLogins
            ));
        }
        
        return violations;
    }

    private List<ComplianceReport.RiskIndicator> calculateRiskIndicators(LocalDateTime start, LocalDateTime end, UUID tenantId) {
        List<ComplianceReport.RiskIndicator> indicators = new ArrayList<>();
        
        // Calculate failure rate
        long totalLogins = countEventsByTypeAndPeriod("LOGIN_SUCCESS", start, end, tenantId) +
                          countEventsByTypeAndPeriod("LOGIN_FAILURE", start, end, tenantId);
        long failedLogins = countEventsByTypeAndPeriod("LOGIN_FAILURE", start, end, tenantId);
        
        if (totalLogins > 0) {
            double failureRate = (double) failedLogins / totalLogins * 100;
            ComplianceReport.RiskLevel riskLevel = failureRate > 20 ? ComplianceReport.RiskLevel.HIGH :
                                                  failureRate > 10 ? ComplianceReport.RiskLevel.MEDIUM :
                                                  ComplianceReport.RiskLevel.LOW;
            
            indicators.add(new ComplianceReport.RiskIndicator(
                "LOGIN_FAILURE_RATE",
                riskLevel,
                "Percentage of failed login attempts",
                failureRate,
                10.0
            ));
        }
        
        return indicators;
    }

    private List<String> generateSecurityRecommendations(ComplianceReport report) {
        List<String> recommendations = new ArrayList<>();
        
        // Analyze the report and generate recommendations
        if (report.getSecurityEvents().getFailedAttempts() > 100) {
            recommendations.add("Consider implementing stronger rate limiting for login attempts");
        }
        
        if (report.getSecurityEvents().getAccountLockouts() > 10) {
            recommendations.add("Review account lockout policies and user education");
        }
        
        return recommendations;
    }

    private double calculateComplianceScore(ComplianceReport report) {
        // Calculate a compliance score based on various factors
        double score = 100.0;
        
        // Deduct points for policy violations
        for (ComplianceReport.PolicyViolation violation : report.getPolicyViolations()) {
            switch (violation.getSeverity()) {
                case CRITICAL -> score -= 20;
                case HIGH -> score -= 10;
                case MEDIUM -> score -= 5;
                case LOW -> score -= 2;
            }
        }
        
        // Deduct points for high-risk indicators
        for (ComplianceReport.RiskIndicator indicator : report.getRiskIndicators()) {
            if (indicator.getRiskLevel() == ComplianceReport.RiskLevel.HIGH) {
                score -= 5;
            } else if (indicator.getRiskLevel() == ComplianceReport.RiskLevel.MEDIUM) {
                score -= 2;
            }
        }
        
        return Math.max(0, score);
    }

    private void detectBruteForceAttacks(LocalDateTime since) {
        // Detect IP addresses with excessive failed login attempts
        Map<String, Long> failuresByIp = auditLogRepository
                .findByEventTypeAndTimestampAfter("LOGIN_FAILURE", since)
                .stream()
                .filter(log -> log.getIpAddress() != null)
                .collect(Collectors.groupingBy(
                    SecurityAuditLog::getIpAddress,
                    Collectors.counting()
                ));
        
        failuresByIp.entrySet().stream()
                .filter(entry -> entry.getValue() > suspiciousThreshold)
                .forEach(entry -> {
                    logger.warn("Potential brute force attack detected from IP: {} with {} failed attempts", 
                               entry.getKey(), entry.getValue());
                    
                    // Log the suspicious activity
                    logSecurityEvent(null, null, "BRUTE_FORCE_DETECTED",
                            "Brute force attack detected from IP: " + entry.getKey() + 
                            " with " + entry.getValue() + " failed attempts",
                            SecurityAuditLog.AuditEventStatus.WARNING,
                            entry.getKey(), null, null);
                });
    }

    private void detectUnusualAccessPatterns(LocalDateTime since) {
        // Detect users with unusual access patterns
        // This is a placeholder for more sophisticated pattern analysis
        logger.debug("Checking for unusual access patterns since {}", since);
    }

    private void detectPrivilegeEscalationAttempts(LocalDateTime since) {
        // Detect potential privilege escalation attempts
        // This would analyze role changes and permission modifications
        logger.debug("Checking for privilege escalation attempts since {}", since);
    }

    private void detectGeographicAnomalies(LocalDateTime since) {
        // Detect logins from unusual geographic locations
        // This would require GeoIP integration
        logger.debug("Checking for geographic anomalies since {}", since);
    }


}