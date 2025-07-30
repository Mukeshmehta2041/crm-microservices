package com.crm.platform.auth.service;

import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.repository.SecurityAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for tenant-specific audit trails and compliance
 */
@Service
public class TenantAuditService {

    private static final Logger logger = LoggerFactory.getLogger(TenantAuditService.class);

    private final SecurityAuditLogRepository auditLogRepository;
    private final TenantContextService tenantContextService;
    private final TenantValidationService tenantValidationService;

    @Autowired
    public TenantAuditService(SecurityAuditLogRepository auditLogRepository,
                            TenantContextService tenantContextService,
                            TenantValidationService tenantValidationService) {
        this.auditLogRepository = auditLogRepository;
        this.tenantContextService = tenantContextService;
        this.tenantValidationService = tenantValidationService;
    }

    /**
     * Get audit logs for current tenant
     */
    public Page<SecurityAuditLog> getTenantAuditLogs(Pageable pageable) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();
        
        return auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
    }

    /**
     * Get audit logs for current tenant with filtering
     */
    public Page<SecurityAuditLog> getTenantAuditLogs(String eventType, 
                                                   LocalDateTime startTime, 
                                                   LocalDateTime endTime,
                                                   Pageable pageable) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();
        
        // Apply tenant isolation
        tenantValidationService.enforceTenantIsolation(tenantId);
        
        if (eventType != null && startTime != null && endTime != null) {
            return auditLogRepository.findByTenantIdAndEventTypeAndTimestampBetween(
                tenantId, eventType, startTime, endTime, pageable);
        } else if (startTime != null && endTime != null) {
            return auditLogRepository.findByTenantIdAndTimestampBetween(
                tenantId, startTime, endTime, pageable);
        } else if (eventType != null) {
            return auditLogRepository.findByTenantIdAndEventType(tenantId, eventType, pageable);
        } else {
            return auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
        }
    }

    /**
     * Get audit logs for a specific user within current tenant
     */
    public Page<SecurityAuditLog> getUserAuditLogs(UUID userId, Pageable pageable) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();
        
        // Ensure user belongs to current tenant (this would require user service integration)
        validateUserBelongsToTenant(userId, tenantId);
        
        return auditLogRepository.findByTenantIdAndUserIdOrderByTimestampDesc(tenantId, userId, pageable);
    }

    /**
     * Get tenant-specific audit statistics
     */
    public Map<String, Object> getTenantAuditStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();
        
        List<SecurityAuditLog> logs = auditLogRepository.findByTenantIdAndTimestampBetween(
            tenantId, startTime, endTime);
        
        Map<String, Long> eventTypeCounts = logs.stream()
            .collect(Collectors.groupingBy(
                SecurityAuditLog::getEventType,
                Collectors.counting()
            ));
        
        Map<String, Long> statusCounts = logs.stream()
            .collect(Collectors.groupingBy(
                log -> log.getStatus().toString(),
                Collectors.counting()
            ));
        
        long uniqueUsers = logs.stream()
            .map(SecurityAuditLog::getUserId)
            .filter(userId -> userId != null)
            .distinct()
            .count();
        
        long uniqueIPs = logs.stream()
            .map(SecurityAuditLog::getIpAddress)
            .filter(ip -> ip != null)
            .distinct()
            .count();
        
        return Map.of(
            "tenant_id", tenantId,
            "period_start", startTime,
            "period_end", endTime,
            "total_events", logs.size(),
            "event_type_counts", eventTypeCounts,
            "status_counts", statusCounts,
            "unique_users", uniqueUsers,
            "unique_ip_addresses", uniqueIPs
        );
    }

    /**
     * Export tenant audit logs for compliance
     */
    public List<SecurityAuditLog> exportTenantAuditLogs(LocalDateTime startTime, 
                                                       LocalDateTime endTime,
                                                       List<String> eventTypes) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();
        
        List<SecurityAuditLog> logs;
        
        if (eventTypes != null && !eventTypes.isEmpty()) {
            logs = auditLogRepository.findByTenantIdAndEventTypeInAndTimestampBetween(
                tenantId, eventTypes, startTime, endTime);
        } else {
            logs = auditLogRepository.findByTenantIdAndTimestampBetween(
                tenantId, startTime, endTime);
        }
        
        // Log the export activity
        logTenantAuditExport(tenantId, startTime, endTime, logs.size());
        
        return logs;
    }

    /**
     * Get tenant compliance summary
     */
    public Map<String, Object> getTenantComplianceSummary(LocalDateTime startTime, LocalDateTime endTime) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();
        
        Map<String, Object> statistics = getTenantAuditStatistics(startTime, endTime);
        
        // Calculate compliance metrics
        List<SecurityAuditLog> logs = auditLogRepository.findByTenantIdAndTimestampBetween(
            tenantId, startTime, endTime);
        
        long failureEvents = logs.stream()
            .filter(log -> log.getStatus() == SecurityAuditLog.AuditEventStatus.FAILURE)
            .count();
        
        long warningEvents = logs.stream()
            .filter(log -> log.getStatus() == SecurityAuditLog.AuditEventStatus.WARNING)
            .count();
        
        long securityIncidents = logs.stream()
            .filter(log -> isSecurityIncident(log.getEventType()))
            .count();
        
        double complianceScore = calculateComplianceScore(logs);
        
        statistics.put("failure_events", failureEvents);
        statistics.put("warning_events", warningEvents);
        statistics.put("security_incidents", securityIncidents);
        statistics.put("compliance_score", complianceScore);
        
        return statistics;
    }

    /**
     * Delete old audit logs for tenant (data retention)
     */
    public int cleanupOldTenantAuditLogs(UUID tenantId, LocalDateTime cutoffDate) {
        // Validate tenant access
        if (!tenantContextService.validateTenantAccess(tenantId)) {
            throw new TenantContextService.TenantContextException("Invalid tenant access for cleanup");
        }
        
        List<SecurityAuditLog> oldLogs = auditLogRepository.findByTenantIdAndTimestampBefore(tenantId, cutoffDate);
        int deletedCount = oldLogs.size();
        
        if (deletedCount > 0) {
            auditLogRepository.deleteAll(oldLogs);
            logger.info("Deleted {} old audit logs for tenant: {}", deletedCount, tenantId);
            
            // Log the cleanup activity
            logTenantAuditCleanup(tenantId, cutoffDate, deletedCount);
        }
        
        return deletedCount;
    }

    /**
     * Validate cross-tenant access prevention
     */
    public void validateTenantAuditAccess(UUID requestedTenantId) {
        UUID currentTenantId = tenantContextService.getCurrentTenantId();
        
        if (currentTenantId == null) {
            throw new TenantContextService.TenantContextException("No tenant context for audit access");
        }
        
        if (!currentTenantId.equals(requestedTenantId)) {
            logger.error("Cross-tenant audit access attempt: current={}, requested={}", 
                        currentTenantId, requestedTenantId);
            throw new TenantValidationService.TenantIsolationException(
                "Cross-tenant audit access denied");
        }
    }

    // Private helper methods

    private void validateUserBelongsToTenant(UUID userId, UUID tenantId) {
        // This would typically call the user service to validate user-tenant relationship
        // For now, we'll assume it's valid if we have tenant context
        logger.debug("Validating user {} belongs to tenant {}", userId, tenantId);
    }

    private boolean isSecurityIncident(String eventType) {
        return eventType.contains("FAILURE") ||
               eventType.contains("LOCKED") ||
               eventType.contains("BRUTE_FORCE") ||
               eventType.contains("SUSPICIOUS") ||
               eventType.contains("VIOLATION");
    }

    private double calculateComplianceScore(List<SecurityAuditLog> logs) {
        if (logs.isEmpty()) {
            return 100.0;
        }
        
        long totalEvents = logs.size();
        long failureEvents = logs.stream()
            .filter(log -> log.getStatus() == SecurityAuditLog.AuditEventStatus.FAILURE)
            .count();
        
        long warningEvents = logs.stream()
            .filter(log -> log.getStatus() == SecurityAuditLog.AuditEventStatus.WARNING)
            .count();
        
        // Calculate score based on failure and warning rates
        double failureRate = (double) failureEvents / totalEvents;
        double warningRate = (double) warningEvents / totalEvents;
        
        double score = 100.0 - (failureRate * 50) - (warningRate * 25);
        return Math.max(0.0, Math.min(100.0, score));
    }

    private void logTenantAuditExport(UUID tenantId, LocalDateTime startTime, LocalDateTime endTime, int recordCount) {
        try {
            SecurityAuditLog exportLog = new SecurityAuditLog(
                null, tenantId, "TENANT_AUDIT_EXPORT",
                String.format("Tenant audit logs exported: %d records from %s to %s", 
                             recordCount, startTime, endTime),
                SecurityAuditLog.AuditEventStatus.SUCCESS
            );
            auditLogRepository.save(exportLog);
        } catch (Exception e) {
            logger.error("Error logging tenant audit export", e);
        }
    }

    private void logTenantAuditCleanup(UUID tenantId, LocalDateTime cutoffDate, int deletedCount) {
        try {
            SecurityAuditLog cleanupLog = new SecurityAuditLog(
                null, tenantId, "TENANT_AUDIT_CLEANUP",
                String.format("Tenant audit logs cleanup: %d records deleted before %s", 
                             deletedCount, cutoffDate),
                SecurityAuditLog.AuditEventStatus.SUCCESS
            );
            auditLogRepository.save(cleanupLog);
        } catch (Exception e) {
            logger.error("Error logging tenant audit cleanup", e);
        }
    }
}