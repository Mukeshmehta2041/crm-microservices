package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.SecurityAlert;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.repository.SecurityAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing security alerts
 */
@Service
public class SecurityAlertService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityAuditLogRepository auditLogRepository;
    private final SecurityAuditService auditService;

    @Value("${security.alerts.retention-days:30}")
    private int alertRetentionDays;

    @Value("${security.alerts.auto-resolve-hours:24}")
    private int autoResolveHours;

    @Value("${security.alerts.duplicate-window-minutes:60}")
    private int duplicateWindowMinutes;

    // Alert thresholds
    @Value("${security.alerts.brute-force-threshold:10}")
    private int bruteForceThreshold;

    @Value("${security.alerts.failed-login-threshold:5}")
    private int failedLoginThreshold;

    @Value("${security.alerts.rate-limit-threshold:50}")
    private int rateLimitThreshold;

    @Autowired
    public SecurityAlertService(RedisTemplate<String, Object> redisTemplate,
                               SecurityAuditLogRepository auditLogRepository,
                               SecurityAuditService auditService) {
        this.redisTemplate = redisTemplate;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new security alert
     */
    public SecurityAlert createAlert(SecurityAlert.AlertType alertType, 
                                   SecurityAlert.Severity severity,
                                   String title, 
                                   String description,
                                   UUID userId,
                                   UUID tenantId,
                                   String ipAddress,
                                   String userAgent,
                                   Map<String, Object> metadata) {

        // Check for duplicate alerts within the window
        if (isDuplicateAlert(alertType, userId, ipAddress, duplicateWindowMinutes)) {
            logger.debug("Duplicate alert detected, updating existing alert instead");
            return updateExistingAlert(alertType, userId, ipAddress, metadata);
        }

        SecurityAlert alert = new SecurityAlert(alertType, severity, title, description);
        alert.setUserId(userId);
        alert.setTenantId(tenantId);
        alert.setIpAddress(ipAddress);
        alert.setUserAgent(userAgent);
        alert.setMetadata(metadata);
        alert.setSource("SecurityAlertService");

        // Calculate risk score
        alert.setRiskScore(calculateRiskScore(alert));

        // Count related events
        alert.setRelatedEventsCount(countRelatedEvents(alert));

        // Store alert in Redis
        storeAlert(alert);

        logger.info("Created security alert: {} - {} (ID: {})", alertType, title, alert.getAlertId());
        
        // Log the alert creation
        auditService.logSecurityEvent(userId, tenantId, "SECURITY_ALERT_CREATED",
            "Security alert created: " + alertType + " - " + title,
            SecurityAuditLog.AuditEventStatus.WARNING,
            ipAddress, userAgent, alert.getAlertId());

        return alert;
    }

    /**
     * Get all active alerts
     */
    public List<SecurityAlert> getActiveAlerts(UUID tenantId) {
        return getAllAlerts(tenantId).stream()
                .filter(SecurityAlert::isOpen)
                .sorted((a, b) -> {
                    // Sort by severity first, then by creation time
                    int severityCompare = Integer.compare(b.getSeverity().getLevel(), a.getSeverity().getLevel());
                    if (severityCompare != 0) return severityCompare;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get alerts by status
     */
    public List<SecurityAlert> getAlertsByStatus(SecurityAlert.AlertStatus status, UUID tenantId) {
        return getAllAlerts(tenantId).stream()
                .filter(alert -> alert.getStatus() == status)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Get alerts by type
     */
    public List<SecurityAlert> getAlertsByType(SecurityAlert.AlertType alertType, UUID tenantId) {
        return getAllAlerts(tenantId).stream()
                .filter(alert -> alert.getAlertType() == alertType)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Get high priority alerts
     */
    public List<SecurityAlert> getHighPriorityAlerts(UUID tenantId) {
        return getAllAlerts(tenantId).stream()
                .filter(SecurityAlert::isHighPriority)
                .filter(SecurityAlert::isOpen)
                .sorted((a, b) -> {
                    int severityCompare = Integer.compare(b.getSeverity().getLevel(), a.getSeverity().getLevel());
                    if (severityCompare != 0) return severityCompare;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get alert by ID
     */
    public Optional<SecurityAlert> getAlertById(String alertId) {
        try {
            String key = "security_alert:" + alertId;
            SecurityAlert alert = (SecurityAlert) redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(alert);
        } catch (Exception e) {
            logger.error("Error retrieving alert by ID: {}", alertId, e);
            return Optional.empty();
        }
    }

    /**
     * Resolve an alert
     */
    public boolean resolveAlert(String alertId, UUID resolvedBy, String resolutionNotes) {
        Optional<SecurityAlert> alertOpt = getAlertById(alertId);
        if (alertOpt.isEmpty()) {
            return false;
        }

        SecurityAlert alert = alertOpt.get();
        alert.resolve(resolvedBy, resolutionNotes);
        
        storeAlert(alert);

        logger.info("Resolved security alert: {} (ID: {})", alert.getAlertType(), alertId);
        
        auditService.logSecurityEvent(alert.getUserId(), alert.getTenantId(), "SECURITY_ALERT_RESOLVED",
            "Security alert resolved: " + alert.getAlertType() + " - " + resolutionNotes,
            SecurityAuditLog.AuditEventStatus.SUCCESS,
            alert.getIpAddress(), alert.getUserAgent(), alertId);

        return true;
    }

    /**
     * Mark alert as false positive
     */
    public boolean markAsFalsePositive(String alertId, UUID resolvedBy, String notes) {
        Optional<SecurityAlert> alertOpt = getAlertById(alertId);
        if (alertOpt.isEmpty()) {
            return false;
        }

        SecurityAlert alert = alertOpt.get();
        alert.markAsFalsePositive(resolvedBy, notes);
        
        storeAlert(alert);

        logger.info("Marked security alert as false positive: {} (ID: {})", alert.getAlertType(), alertId);
        
        auditService.logSecurityEvent(alert.getUserId(), alert.getTenantId(), "SECURITY_ALERT_FALSE_POSITIVE",
            "Security alert marked as false positive: " + alert.getAlertType() + " - " + notes,
            SecurityAuditLog.AuditEventStatus.SUCCESS,
            alert.getIpAddress(), alert.getUserAgent(), alertId);

        return true;
    }

    /**
     * Update alert status
     */
    public boolean updateAlertStatus(String alertId, SecurityAlert.AlertStatus status) {
        Optional<SecurityAlert> alertOpt = getAlertById(alertId);
        if (alertOpt.isEmpty()) {
            return false;
        }

        SecurityAlert alert = alertOpt.get();
        SecurityAlert.AlertStatus oldStatus = alert.getStatus();
        alert.setStatus(status);
        
        storeAlert(alert);

        logger.info("Updated security alert status: {} -> {} (ID: {})", oldStatus, status, alertId);
        
        auditService.logSecurityEvent(alert.getUserId(), alert.getTenantId(), "SECURITY_ALERT_STATUS_UPDATED",
            "Security alert status updated: " + oldStatus + " -> " + status,
            SecurityAuditLog.AuditEventStatus.SUCCESS,
            alert.getIpAddress(), alert.getUserAgent(), alertId);

        return true;
    }

    /**
     * Get alert statistics
     */
    public Map<String, Object> getAlertStatistics(UUID tenantId) {
        List<SecurityAlert> allAlerts = getAllAlerts(tenantId);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Count by status
        Map<SecurityAlert.AlertStatus, Long> statusCounts = allAlerts.stream()
                .collect(Collectors.groupingBy(SecurityAlert::getStatus, Collectors.counting()));
        stats.put("status_counts", statusCounts);
        
        // Count by severity
        Map<SecurityAlert.Severity, Long> severityCounts = allAlerts.stream()
                .collect(Collectors.groupingBy(SecurityAlert::getSeverity, Collectors.counting()));
        stats.put("severity_counts", severityCounts);
        
        // Count by type
        Map<SecurityAlert.AlertType, Long> typeCounts = allAlerts.stream()
                .collect(Collectors.groupingBy(SecurityAlert::getAlertType, Collectors.counting()));
        stats.put("type_counts", typeCounts);
        
        // General statistics
        stats.put("total_alerts", allAlerts.size());
        stats.put("open_alerts", allAlerts.stream().filter(SecurityAlert::isOpen).count());
        stats.put("high_priority_alerts", allAlerts.stream().filter(SecurityAlert::isHighPriority).count());
        stats.put("auto_resolved_alerts", allAlerts.stream().filter(SecurityAlert::isAutoResolved).count());
        
        // Average resolution time for resolved alerts
        OptionalDouble avgResolutionTime = allAlerts.stream()
                .filter(SecurityAlert::isResolved)
                .filter(alert -> alert.getResolvedAt() != null)
                .mapToLong(alert -> java.time.Duration.between(alert.getCreatedAt(), alert.getResolvedAt()).toMinutes())
                .average();
        stats.put("avg_resolution_time_minutes", avgResolutionTime.orElse(0.0));
        
        return stats;
    }

    /**
     * Auto-resolve old alerts
     */
    public void autoResolveOldAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(autoResolveHours);
        
        List<SecurityAlert> oldAlerts = getAllAlerts(null).stream()
                .filter(SecurityAlert::isOpen)
                .filter(alert -> alert.getCreatedAt().isBefore(cutoff))
                .collect(Collectors.toList());
        
        for (SecurityAlert alert : oldAlerts) {
            alert.setStatus(SecurityAlert.AlertStatus.RESOLVED);
            alert.setAutoResolved(true);
            alert.setResolutionNotes("Auto-resolved due to age");
            alert.setResolvedAt(LocalDateTime.now());
            
            storeAlert(alert);
            
            logger.info("Auto-resolved old security alert: {} (ID: {})", alert.getAlertType(), alert.getAlertId());
        }
        
        if (!oldAlerts.isEmpty()) {
            auditService.logSecurityEvent(null, null, "SECURITY_ALERTS_AUTO_RESOLVED",
                "Auto-resolved " + oldAlerts.size() + " old security alerts",
                SecurityAuditLog.AuditEventStatus.SUCCESS,
                null, null, null);
        }
    }

    /**
     * Clean up expired alerts
     */
    public void cleanupExpiredAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(alertRetentionDays);
        
        Set<String> alertKeys = redisTemplate.keys("security_alert:*");
        if (alertKeys == null) return;
        
        int deletedCount = 0;
        for (String key : alertKeys) {
            try {
                SecurityAlert alert = (SecurityAlert) redisTemplate.opsForValue().get(key);
                if (alert != null && alert.getCreatedAt().isBefore(cutoff)) {
                    redisTemplate.delete(key);
                    deletedCount++;
                }
            } catch (Exception e) {
                logger.error("Error processing alert key: {}", key, e);
            }
        }
        
        if (deletedCount > 0) {
            logger.info("Cleaned up {} expired security alerts", deletedCount);
        }
    }

    // Private helper methods

    private boolean isDuplicateAlert(SecurityAlert.AlertType alertType, UUID userId, String ipAddress, int windowMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        
        return getAllAlerts(null).stream()
                .anyMatch(alert -> 
                    alert.getAlertType() == alertType &&
                    Objects.equals(alert.getUserId(), userId) &&
                    Objects.equals(alert.getIpAddress(), ipAddress) &&
                    alert.getCreatedAt().isAfter(since) &&
                    alert.isOpen()
                );
    }

    private SecurityAlert updateExistingAlert(SecurityAlert.AlertType alertType, UUID userId, String ipAddress, Map<String, Object> metadata) {
        Optional<SecurityAlert> existingAlert = getAllAlerts(null).stream()
                .filter(alert -> 
                    alert.getAlertType() == alertType &&
                    Objects.equals(alert.getUserId(), userId) &&
                    Objects.equals(alert.getIpAddress(), ipAddress) &&
                    alert.isOpen()
                )
                .findFirst();
        
        if (existingAlert.isPresent()) {
            SecurityAlert alert = existingAlert.get();
            alert.setRelatedEventsCount(alert.getRelatedEventsCount() + 1);
            alert.setUpdatedAt(LocalDateTime.now());
            if (metadata != null) {
                alert.setMetadata(metadata);
            }
            
            storeAlert(alert);
            return alert;
        }
        
        // Fallback - create new alert
        return createAlert(alertType, SecurityAlert.Severity.MEDIUM, 
                          alertType.toString(), "Alert description", 
                          userId, null, ipAddress, null, metadata);
    }

    private double calculateRiskScore(SecurityAlert alert) {
        double score = 0.0;
        
        // Base score by severity
        switch (alert.getSeverity()) {
            case LOW -> score += 25.0;
            case MEDIUM -> score += 50.0;
            case HIGH -> score += 75.0;
            case CRITICAL -> score += 100.0;
        }
        
        // Adjust by alert type
        switch (alert.getAlertType()) {
            case BRUTE_FORCE_ATTACK, DDOS_ATTACK, DATA_BREACH_ATTEMPT -> score += 25.0;
            case PRIVILEGE_ESCALATION, SESSION_HIJACKING -> score += 20.0;
            case SUSPICIOUS_LOGIN, UNUSUAL_LOCATION -> score += 15.0;
            case MULTIPLE_FAILED_LOGINS, RATE_LIMIT_EXCEEDED -> score += 10.0;
            default -> score += 5.0;
        }
        
        // Adjust by IP reputation (if available in metadata)
        if (alert.getMetadata() != null) {
            Object ipReputation = alert.getMetadata().get("ip_reputation");
            if (ipReputation instanceof String reputation) {
                switch (reputation.toLowerCase()) {
                    case "malicious" -> score += 30.0;
                    case "suspicious" -> score += 15.0;
                    case "unknown" -> score += 5.0;
                }
            }
        }
        
        return Math.min(100.0, score);
    }

    private int countRelatedEvents(SecurityAlert alert) {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        
        // Count related audit log events
        List<String> relatedEventTypes = getRelatedEventTypes(alert.getAlertType());
        
        return (int) relatedEventTypes.stream()
                .mapToLong(eventType -> 
                    auditLogRepository.findByEventTypeAndTimestampBetween(eventType, since, LocalDateTime.now())
                            .stream()
                            .filter(log -> Objects.equals(log.getUserId(), alert.getUserId()) ||
                                          Objects.equals(log.getIpAddress(), alert.getIpAddress()))
                            .count()
                )
                .sum();
    }

    private List<String> getRelatedEventTypes(SecurityAlert.AlertType alertType) {
        return switch (alertType) {
            case BRUTE_FORCE_ATTACK -> List.of("LOGIN_FAILURE", "BRUTE_FORCE_ATTEMPT");
            case SUSPICIOUS_LOGIN -> List.of("LOGIN_SUCCESS", "LOGIN_FAILURE", "UNUSUAL_LOCATION");
            case ACCOUNT_LOCKOUT -> List.of("ACCOUNT_LOCKED", "LOGIN_FAILURE");
            case MULTIPLE_FAILED_LOGINS -> List.of("LOGIN_FAILURE");
            case RATE_LIMIT_EXCEEDED -> List.of("RATE_LIMIT_EXCEEDED");
            case IP_BLOCKED -> List.of("IP_BLOCKED_TEMPORARY", "IP_BLOCKED_PERMANENT");
            default -> List.of();
        };
    }

    private void storeAlert(SecurityAlert alert) {
        try {
            String key = "security_alert:" + alert.getAlertId();
            redisTemplate.opsForValue().set(key, alert, alertRetentionDays, TimeUnit.DAYS);
            
            // Also store in a set for easier querying
            String tenantKey = "security_alerts:tenant:" + (alert.getTenantId() != null ? alert.getTenantId() : "global");
            redisTemplate.opsForSet().add(tenantKey, alert.getAlertId());
            redisTemplate.expire(tenantKey, alertRetentionDays, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.error("Error storing security alert: {}", alert.getAlertId(), e);
        }
    }

    private List<SecurityAlert> getAllAlerts(UUID tenantId) {
        try {
            String tenantKey = "security_alerts:tenant:" + (tenantId != null ? tenantId : "global");
            Set<Object> alertIds = redisTemplate.opsForSet().members(tenantKey);
            
            if (alertIds == null || alertIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<SecurityAlert> alerts = new ArrayList<>();
            for (Object alertId : alertIds) {
                Optional<SecurityAlert> alert = getAlertById(alertId.toString());
                alert.ifPresent(alerts::add);
            }
            
            return alerts;
            
        } catch (Exception e) {
            logger.error("Error retrieving alerts for tenant: {}", tenantId, e);
            return new ArrayList<>();
        }
    }
}