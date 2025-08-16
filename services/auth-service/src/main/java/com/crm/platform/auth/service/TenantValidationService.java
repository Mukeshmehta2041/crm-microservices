package com.crm.platform.auth.service;

import com.crm.platform.auth.entity.SecurityAuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for validating tenant access and enforcing tenant isolation
 */
@Service
public class TenantValidationService {

    private static final Logger logger = LoggerFactory.getLogger(TenantValidationService.class);

    private final TenantContextService tenantContextService;
    private final SecurityAuditService auditService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${tenant.service.url:http://tenant-service:8080}")
    private String tenantServiceUrl;

    @Value("${tenant.validation.cache-ttl-minutes:60}")
    private int validationCacheTtlMinutes;

    @Value("${tenant.validation.enabled:true}")
    private boolean tenantValidationEnabled;

    @Autowired
    public TenantValidationService(TenantContextService tenantContextService,
                                 SecurityAuditService auditService,
                                 RedisTemplate<String, Object> redisTemplate,
                                 RestTemplate restTemplate) {
        this.tenantContextService = tenantContextService;
        this.auditService = auditService;
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    /**
     * Validate tenant by ID
     */
    public TenantValidationResult validateTenant(UUID tenantId) {
        if (!tenantValidationEnabled) {
            return new TenantValidationResult(true, "Tenant validation disabled", null);
        }

        if (tenantId == null) {
            return new TenantValidationResult(false, "Tenant ID is null", null);
        }

        try {
            // Check cache first
            String cacheKey = "tenant_validation:" + tenantId;
            TenantInfo cachedTenant = (TenantInfo) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedTenant != null) {
                logger.debug("Tenant validation cache hit: {}", tenantId);
                return new TenantValidationResult(cachedTenant.isActive(), "Cached validation", cachedTenant);
            }

            // Validate with tenant service
            TenantInfo tenantInfo = fetchTenantInfo(tenantId);
            
            if (tenantInfo == null) {
                logger.warn("Tenant not found: {}", tenantId);
                auditService.logSecurityEvent(null, tenantId, "TENANT_NOT_FOUND",
                    "Tenant validation failed - tenant not found: " + tenantId,
                    SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
                return new TenantValidationResult(false, "Tenant not found", null);
            }

            // Check tenant status
            if (!tenantInfo.isActive()) {
                logger.warn("Tenant is not active: {} (status: {})", tenantId, tenantInfo.getStatus());
                auditService.logSecurityEvent(null, tenantId, "TENANT_INACTIVE",
                    "Tenant validation failed - tenant is not active: " + tenantId + " (status: " + tenantInfo.getStatus() + ")",
                    SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
                return new TenantValidationResult(false, "Tenant is not active: " + tenantInfo.getStatus(), tenantInfo);
            }

            // Check subscription status
            if (tenantInfo.isSubscriptionExpired()) {
                logger.warn("Tenant subscription expired: {}", tenantId);
                auditService.logSecurityEvent(null, tenantId, "TENANT_SUBSCRIPTION_EXPIRED",
                    "Tenant validation failed - subscription expired: " + tenantId,
                    SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
                return new TenantValidationResult(false, "Tenant subscription expired", tenantInfo);
            }

            // Check trial status
            if (tenantInfo.isTrial() && tenantInfo.isTrialExpired()) {
                logger.warn("Tenant trial expired: {}", tenantId);
                auditService.logSecurityEvent(null, tenantId, "TENANT_TRIAL_EXPIRED",
                    "Tenant validation failed - trial expired: " + tenantId,
                    SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
                return new TenantValidationResult(false, "Tenant trial expired", tenantInfo);
            }

            // Cache successful validation
            redisTemplate.opsForValue().set(cacheKey, tenantInfo, validationCacheTtlMinutes, TimeUnit.MINUTES);
            
            logger.debug("Tenant validation successful: {}", tenantId);
            return new TenantValidationResult(true, "Tenant validation successful", tenantInfo);

        } catch (Exception e) {
            logger.error("Error validating tenant: {}", tenantId, e);
            auditService.logSecurityEvent(null, tenantId, "TENANT_VALIDATION_ERROR",
                "Tenant validation error: " + e.getMessage(),
                SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
            return new TenantValidationResult(false, "Tenant validation error: " + e.getMessage(), null);
        }
    }

    /**
     * Validate tenant by subdomain
     */
    public TenantValidationResult validateTenantBySubdomain(String subdomain) {
        if (!tenantValidationEnabled) {
            return new TenantValidationResult(true, "Tenant validation disabled", null);
        }

        if (subdomain == null || subdomain.trim().isEmpty()) {
            return new TenantValidationResult(false, "Subdomain is null or empty", null);
        }

        try {
            // Check cache first
            String cacheKey = "tenant_subdomain:" + subdomain;
            UUID cachedTenantId = (UUID) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedTenantId != null) {
                logger.debug("Tenant subdomain cache hit: {}", subdomain);
                return validateTenant(cachedTenantId);
            }

            // Fetch tenant by subdomain
            TenantInfo tenantInfo = fetchTenantBySubdomain(subdomain);
            
            if (tenantInfo == null) {
                logger.warn("Tenant not found for subdomain: {}", subdomain);
                auditService.logSecurityEvent(null, null, "TENANT_SUBDOMAIN_NOT_FOUND",
                    "Tenant validation failed - subdomain not found: " + subdomain,
                    SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
                return new TenantValidationResult(false, "Tenant not found for subdomain", null);
            }

            // Cache subdomain to tenant ID mapping
            redisTemplate.opsForValue().set(cacheKey, tenantInfo.getId(), validationCacheTtlMinutes, TimeUnit.MINUTES);
            
            // Validate the tenant
            return validateTenant(tenantInfo.getId());

        } catch (Exception e) {
            logger.error("Error validating tenant by subdomain: {}", subdomain, e);
            auditService.logSecurityEvent(null, null, "TENANT_SUBDOMAIN_VALIDATION_ERROR",
                "Tenant subdomain validation error: " + e.getMessage(),
                SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
            return new TenantValidationResult(false, "Tenant subdomain validation error: " + e.getMessage(), null);
        }
    }

    /**
     * Validate and set tenant context
     */
    public boolean validateAndSetTenantContext(UUID tenantId) {
        TenantValidationResult result = validateTenant(tenantId);
        
        if (result.isValid() && result.getTenantInfo() != null) {
            tenantContextService.setCurrentTenant(tenantId, result.getTenantInfo().getSubdomain());
            tenantContextService.markTenantAsValidated();
            return true;
        }
        
        return false;
    }

    /**
     * Validate and set tenant context by subdomain
     */
    public boolean validateAndSetTenantContextBySubdomain(String subdomain) {
        TenantValidationResult result = validateTenantBySubdomain(subdomain);
        
        if (result.isValid() && result.getTenantInfo() != null) {
            tenantContextService.setCurrentTenant(result.getTenantInfo().getId(), subdomain);
            tenantContextService.markTenantAsValidated();
            return true;
        }
        
        return false;
    }

    /**
     * Enforce tenant isolation - validate that operation is within tenant context
     */
    public void enforceTenantIsolation(UUID requestedTenantId) {
        if (!tenantValidationEnabled) {
            return;
        }

        UUID currentTenantId = tenantContextService.getCurrentTenantId();
        
        if (currentTenantId == null) {
            logger.error("Tenant isolation violation: No tenant context set");
            auditService.logSecurityEvent(null, requestedTenantId, "TENANT_ISOLATION_VIOLATION",
                "Tenant isolation violation - no tenant context set",
                SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
            throw new TenantIsolationException("No tenant context set");
        }

        if (requestedTenantId != null && !currentTenantId.equals(requestedTenantId)) {
            logger.error("Tenant isolation violation: current={}, requested={}", currentTenantId, requestedTenantId);
            auditService.logSecurityEvent(null, currentTenantId, "TENANT_ISOLATION_VIOLATION",
                "Tenant isolation violation - cross-tenant access attempt: current=" + currentTenantId + ", requested=" + requestedTenantId,
                SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
            throw new TenantIsolationException("Cross-tenant access denied");
        }

        if (!tenantContextService.isTenantValidated()) {
            logger.error("Tenant isolation violation: Tenant context not validated");
            auditService.logSecurityEvent(null, currentTenantId, "TENANT_ISOLATION_VIOLATION",
                "Tenant isolation violation - tenant context not validated",
                SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
            throw new TenantIsolationException("Tenant context not validated");
        }
    }

    /**
     * Clear tenant validation cache
     */
    public void clearTenantCache(UUID tenantId) {
        try {
            String cacheKey = "tenant_validation:" + tenantId;
            redisTemplate.delete(cacheKey);
            logger.debug("Cleared tenant validation cache: {}", tenantId);
        } catch (Exception e) {
            logger.error("Error clearing tenant cache: {}", tenantId, e);
        }
    }

    /**
     * Clear tenant subdomain cache
     */
    public void clearSubdomainCache(String subdomain) {
        try {
            String cacheKey = "tenant_subdomain:" + subdomain;
            redisTemplate.delete(cacheKey);
            logger.debug("Cleared tenant subdomain cache: {}", subdomain);
        } catch (Exception e) {
            logger.error("Error clearing subdomain cache: {}", subdomain, e);
        }
    }

    // Private helper methods

    private TenantInfo fetchTenantInfo(UUID tenantId) {
        try {
            String url = tenantServiceUrl + "/api/v1/tenants/" + tenantId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> tenantData = (Map<String, Object>) response.getBody().get("data");
                if (tenantData != null) {
                    return mapToTenantInfo(tenantData);
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error fetching tenant info: {}", tenantId, e);
            throw new RuntimeException("Failed to fetch tenant info", e);
        }
    }

    private TenantInfo fetchTenantBySubdomain(String subdomain) {
        try {
            String url = tenantServiceUrl + "/api/v1/tenants/by-subdomain/" + subdomain;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> tenantData = (Map<String, Object>) response.getBody().get("data");
                if (tenantData != null) {
                    return mapToTenantInfo(tenantData);
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error fetching tenant by subdomain: {}", subdomain, e);
            throw new RuntimeException("Failed to fetch tenant by subdomain", e);
        }
    }

    private TenantInfo mapToTenantInfo(Map<String, Object> tenantData) {
        TenantInfo info = new TenantInfo();
        info.setId(UUID.fromString((String) tenantData.get("id")));
        info.setName((String) tenantData.get("name"));
        info.setSubdomain((String) tenantData.get("subdomain"));
        info.setStatus((String) tenantData.get("status"));
        info.setPlanType((String) tenantData.get("planType"));
        info.setMaxUsers((Integer) tenantData.get("maxUsers"));
        info.setTrial((Boolean) tenantData.get("isTrial"));
        
        // Parse dates
        String subscriptionExpiresAt = (String) tenantData.get("subscriptionExpiresAt");
        if (subscriptionExpiresAt != null) {
            info.setSubscriptionExpiresAt(LocalDateTime.parse(subscriptionExpiresAt));
        }
        
        String trialEndsAt = (String) tenantData.get("trialEndsAt");
        if (trialEndsAt != null) {
            info.setTrialEndsAt(LocalDateTime.parse(trialEndsAt));
        }
        
        return info;
    }

    // Inner classes

    public static class TenantValidationResult {
        private final boolean valid;
        private final String message;
        private final TenantInfo tenantInfo;

        public TenantValidationResult(boolean valid, String message, TenantInfo tenantInfo) {
            this.valid = valid;
            this.message = message;
            this.tenantInfo = tenantInfo;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public TenantInfo getTenantInfo() { return tenantInfo; }
    }

    public static class TenantInfo {
        private UUID id;
        private String name;
        private String subdomain;
        private String status;
        private String planType;
        private Integer maxUsers;
        private boolean trial;
        private LocalDateTime subscriptionExpiresAt;
        private LocalDateTime trialEndsAt;

        // Getters and Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSubdomain() { return subdomain; }
        public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPlanType() { return planType; }
        public void setPlanType(String planType) { this.planType = planType; }

        public Integer getMaxUsers() { return maxUsers; }
        public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

        public boolean isTrial() { return trial; }
        public void setTrial(boolean trial) { this.trial = trial; }

        public LocalDateTime getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
        public void setSubscriptionExpiresAt(LocalDateTime subscriptionExpiresAt) { this.subscriptionExpiresAt = subscriptionExpiresAt; }

        public LocalDateTime getTrialEndsAt() { return trialEndsAt; }
        public void setTrialEndsAt(LocalDateTime trialEndsAt) { this.trialEndsAt = trialEndsAt; }

        // Helper methods
        public boolean isActive() {
            return "ACTIVE".equals(status);
        }

        public boolean isSubscriptionExpired() {
            return subscriptionExpiresAt != null && subscriptionExpiresAt.isBefore(LocalDateTime.now());
        }

        public boolean isTrialExpired() {
            return trialEndsAt != null && trialEndsAt.isBefore(LocalDateTime.now());
        }
    }

    public static class TenantIsolationException extends RuntimeException {
        public TenantIsolationException(String message) {
            super(message);
        }

        public TenantIsolationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}