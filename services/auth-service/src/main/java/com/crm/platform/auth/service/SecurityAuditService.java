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

    // OAuth2-specific audit methods

    @Async
    public void logOAuth2Authorization(UUID userId, String clientId, String scope) {
        logSecurityEvent(userId, null, "OAUTH2_AUTHORIZATION", 
                        "OAuth2 authorization granted for client: " + clientId + ", scope: " + scope,
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
}