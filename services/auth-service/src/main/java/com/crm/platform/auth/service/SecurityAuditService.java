package com.crm.platform.auth.service;

import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.repository.SecurityAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;

    @Autowired
    public SecurityAuditService(SecurityAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
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
}