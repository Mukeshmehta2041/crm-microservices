package com.crm.platform.users.service;

import com.crm.platform.users.entity.User;
import com.crm.platform.users.entity.UserAuditLog;
import com.crm.platform.users.repository.UserAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing user audit trails and change tracking
 */
@Service
public class UserAuditService {

    private static final Logger logger = LoggerFactory.getLogger(UserAuditService.class);

    private final UserAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserAuditService(UserAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Log user creation
     */
    @Async
    public void logUserCreation(User user, UUID performedBy, HttpServletRequest request) {
        UserAuditLog auditLog = new UserAuditLog(
            user.getId(), 
            user.getTenantId(), 
            UserAuditLog.ACTION_CREATE, 
            "User account created", 
            performedBy
        );
        
        setRequestInfo(auditLog, request);
        auditLog.setSeverity(UserAuditLog.AuditSeverity.INFO);
        
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("jobTitle", user.getJobTitle());
            userData.put("department", user.getDepartment());
            auditLog.setAdditionalData(objectMapper.writeValueAsString(userData));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize user data for audit log", e);
        }
        
        auditLogRepository.save(auditLog);
        logger.debug("Logged user creation for user: {}", user.getId());
    }

    /**
     * Log user update with field-level changes
     */
    @Async
    public void logUserUpdate(User oldUser, User newUser, UUID performedBy, HttpServletRequest request) {
        // Compare fields and log changes
        Map<String, Object> changes = compareUsers(oldUser, newUser);
        
        if (changes.isEmpty()) {
            return; // No changes to log
        }

        for (Map.Entry<String, Object> change : changes.entrySet()) {
            String fieldName = change.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldChange = (Map<String, Object>) change.getValue();
            
            UserAuditLog auditLog = new UserAuditLog(
                newUser.getId(),
                newUser.getTenantId(),
                UserAuditLog.ACTION_UPDATE,
                fieldName,
                String.valueOf(fieldChange.get("oldValue")),
                String.valueOf(fieldChange.get("newValue")),
                performedBy
            );
            
            auditLog.setDescription("User field updated: " + fieldName);
            setRequestInfo(auditLog, request);
            auditLog.setSeverity(UserAuditLog.AuditSeverity.INFO);
            
            auditLogRepository.save(auditLog);
        }
        
        logger.debug("Logged user update for user: {} with {} field changes", newUser.getId(), changes.size());
    }

    /**
     * Log user status change
     */
    @Async
    public void logUserStatusChange(User user, User.UserStatus oldStatus, User.UserStatus newStatus, 
                                   UUID performedBy, HttpServletRequest request) {
        UserAuditLog auditLog = new UserAuditLog(
            user.getId(),
            user.getTenantId(),
            UserAuditLog.ACTION_STATUS_CHANGE,
            "status",
            oldStatus != null ? oldStatus.name() : null,
            newStatus.name(),
            performedBy
        );
        
        auditLog.setDescription("User status changed from " + oldStatus + " to " + newStatus);
        setRequestInfo(auditLog, request);
        auditLog.setSeverity(UserAuditLog.AuditSeverity.MEDIUM);
        
        auditLogRepository.save(auditLog);
        logger.debug("Logged status change for user: {} from {} to {}", user.getId(), oldStatus, newStatus);
    }

    /**
     * Log user deletion
     */
    @Async
    public void logUserDeletion(User user, UUID performedBy, HttpServletRequest request) {
        UserAuditLog auditLog = new UserAuditLog(
            user.getId(),
            user.getTenantId(),
            UserAuditLog.ACTION_DELETE,
            "User account deleted",
            performedBy
        );
        
        setRequestInfo(auditLog, request);
        auditLog.setSeverity(UserAuditLog.AuditSeverity.HIGH);
        
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("fullName", user.getFullName());
            userData.put("status", user.getStatus());
            auditLog.setAdditionalData(objectMapper.writeValueAsString(userData));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize user data for deletion audit log", e);
        }
        
        auditLogRepository.save(auditLog);
        logger.debug("Logged user deletion for user: {}", user.getId());
    }

    /**
     * Log user login
     */
    @Async
    public void logUserLogin(UUID userId, UUID tenantId, HttpServletRequest request) {
        UserAuditLog auditLog = new UserAuditLog(
            userId,
            tenantId,
            UserAuditLog.ACTION_LOGIN,
            "User logged in",
            userId
        );
        
        setRequestInfo(auditLog, request);
        auditLog.setSeverity(UserAuditLog.AuditSeverity.INFO);
        
        auditLogRepository.save(auditLog);
        logger.debug("Logged user login for user: {}", userId);
    }

    /**
     * Log user logout
     */
    @Async
    public void logUserLogout(UUID userId, UUID tenantId, HttpServletRequest request) {
        UserAuditLog auditLog = new UserAuditLog(
            userId,
            tenantId,
            UserAuditLog.ACTION_LOGOUT,
            "User logged out",
            userId
        );
        
        setRequestInfo(auditLog, request);
        auditLog.setSeverity(UserAuditLog.AuditSeverity.INFO);
        
        auditLogRepository.save(auditLog);
        logger.debug("Logged user logout for user: {}", userId);
    }

    /**
     * Log GDPR consent change
     */
    @Async
    public void logGdprConsentChange(UUID userId, UUID tenantId, boolean consentGiven, UUID performedBy, HttpServletRequest request) {
        UserAuditLog auditLog = new UserAuditLog(
            userId,
            tenantId,
            UserAuditLog.ACTION_CONSENT_CHANGE,
            "GDPR consent " + (consentGiven ? "granted" : "revoked"),
            performedBy
        );
        
        auditLog.setFieldName("gdprConsentGiven");
        auditLog.setNewValue(String.valueOf(consentGiven));
        setRequestInfo(auditLog, request);
        auditLog.setSeverity(UserAuditLog.AuditSeverity.MEDIUM);
        
        auditLogRepository.save(auditLog);
        logger.debug("Logged GDPR consent change for user: {} to {}", userId, consentGiven);
    }

    /**
     * Log data export request
     */
    @Async
    public void logDataExportRequest(UUID userId, UUID tenantId, UUID performedBy, HttpServletRequest request) {
        UserAuditLog auditLog = new UserAuditLog(
            userId,
            tenantId,
            UserAuditLog.ACTION_DATA_EXPORT,
            "User data export requested",
            performedBy
        );
        
        setRequestInfo(auditLog, request);
        auditLog.setSeverity(UserAuditLog.AuditSeverity.MEDIUM);
        
        auditLogRepository.save(auditLog);
        logger.debug("Logged data export request for user: {}", userId);
    }

    /**
     * Get audit logs for a user
     */
    public Page<UserAuditLog> getUserAuditLogs(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get audit logs for a tenant
     */
    public Page<UserAuditLog> getTenantAuditLogs(UUID tenantId, Pageable pageable) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    /**
     * Get audit logs by date range
     */
    public Page<UserAuditLog> getAuditLogsByDateRange(UUID tenantId, LocalDateTime startDate, 
                                                     LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate, pageable);
    }

    /**
     * Get audit statistics by action
     */
    public List<Object[]> getAuditStatsByAction(UUID tenantId, LocalDateTime since) {
        return auditLogRepository.getAuditStatsByAction(tenantId, since);
    }

    /**
     * Get recent suspicious activities
     */
    public List<UserAuditLog> getSuspiciousActivities(UUID tenantId, LocalDateTime since) {
        return auditLogRepository.findSuspiciousActivities(tenantId, since);
    }

    /**
     * Clean up old audit logs
     */
    public void cleanupOldAuditLogs(LocalDateTime cutoffDate) {
        auditLogRepository.deleteOldAuditLogs(cutoffDate);
        logger.info("Cleaned up audit logs older than {}", cutoffDate);
    }

    // Private helper methods

    private void setRequestInfo(UserAuditLog auditLog, HttpServletRequest request) {
        if (request != null) {
            auditLog.setIpAddress(getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private Map<String, Object> compareUsers(User oldUser, User newUser) {
        Map<String, Object> changes = new HashMap<>();
        
        // Compare basic fields
        compareField(changes, "email", oldUser.getEmail(), newUser.getEmail());
        compareField(changes, "firstName", oldUser.getFirstName(), newUser.getFirstName());
        compareField(changes, "lastName", oldUser.getLastName(), newUser.getLastName());
        compareField(changes, "middleName", oldUser.getMiddleName(), newUser.getMiddleName());
        compareField(changes, "displayName", oldUser.getDisplayName(), newUser.getDisplayName());
        compareField(changes, "phoneNumber", oldUser.getPhoneNumber(), newUser.getPhoneNumber());
        compareField(changes, "jobTitle", oldUser.getJobTitle(), newUser.getJobTitle());
        compareField(changes, "department", oldUser.getDepartment(), newUser.getDepartment());
        compareField(changes, "managerId", oldUser.getManagerId(), newUser.getManagerId());
        compareField(changes, "teamId", oldUser.getTeamId(), newUser.getTeamId());
        compareField(changes, "status", oldUser.getStatus(), newUser.getStatus());
        
        // Compare other important fields
        compareField(changes, "timezone", oldUser.getTimezone(), newUser.getTimezone());
        compareField(changes, "language", oldUser.getLanguage(), newUser.getLanguage());
        compareField(changes, "profileVisibility", oldUser.getProfileVisibility(), newUser.getProfileVisibility());
        compareField(changes, "emailNotificationsEnabled", oldUser.getEmailNotificationsEnabled(), newUser.getEmailNotificationsEnabled());
        
        return changes;
    }

    private void compareField(Map<String, Object> changes, String fieldName, Object oldValue, Object newValue) {
        if (!java.util.Objects.equals(oldValue, newValue)) {
            Map<String, Object> change = new HashMap<>();
            change.put("oldValue", oldValue);
            change.put("newValue", newValue);
            changes.put(fieldName, change);
        }
    }
}