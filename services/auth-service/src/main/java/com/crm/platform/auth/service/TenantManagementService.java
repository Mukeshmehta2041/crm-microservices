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
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for comprehensive tenant management features
 */
@Service
public class TenantManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TenantManagementService.class);

    private final TenantContextService tenantContextService;
    private final TenantValidationService tenantValidationService;
    private final SecurityAuditService auditService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${tenant.service.url:http://tenant-service:8080}")
    private String tenantServiceUrl;

    @Value("${users.service.url:http://users-service:8080}")
    private String usersServiceUrl;

    @Value("${tenant.usage.cache-ttl-minutes:15}")
    private int usageCacheTtlMinutes;

    @Autowired
    public TenantManagementService(TenantContextService tenantContextService,
                                 TenantValidationService tenantValidationService,
                                 SecurityAuditService auditService,
                                 RedisTemplate<String, Object> redisTemplate,
                                 RestTemplate restTemplate) {
        this.tenantContextService = tenantContextService;
        this.tenantValidationService = tenantValidationService;
        this.auditService = auditService;
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    // Tenant Configuration Management

    /**
     * Get tenant configuration
     */
    public Map<String, Object> getTenantConfiguration() {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            String url = tenantServiceUrl + "/api/v1/tenants/" + tenantId + "/configuration";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> config = (Map<String, Object>) response.getBody().get("data");
                
                auditService.logSecurityEvent(null, tenantId, "TENANT_CONFIG_ACCESSED",
                    "Tenant configuration accessed",
                    SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
                
                return config != null ? config : new HashMap<>();
            }
            
            return new HashMap<>();
        } catch (Exception e) {
            logger.error("Error retrieving tenant configuration for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to retrieve tenant configuration", e);
        }
    }

    /**
     * Update tenant configuration
     */
    public void updateTenantConfiguration(Map<String, Object> configuration) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            String url = tenantServiceUrl + "/api/v1/tenants/" + tenantId + "/configuration";
            restTemplate.put(url, configuration);
            
            // Clear cache
            clearTenantConfigurationCache(tenantId);
            
            auditService.logSecurityEvent(null, tenantId, "TENANT_CONFIG_UPDATED",
                "Tenant configuration updated: " + configuration.keySet(),
                SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
                
            logger.info("Updated tenant configuration for tenant: {}", tenantId);
            
        } catch (Exception e) {
            logger.error("Error updating tenant configuration for tenant: {}", tenantId, e);
            auditService.logSecurityEvent(null, tenantId, "TENANT_CONFIG_UPDATE_FAILED",
                "Failed to update tenant configuration: " + e.getMessage(),
                SecurityAuditLog.AuditEventStatus.FAILURE, null, null, null);
            throw new RuntimeException("Failed to update tenant configuration", e);
        }
    }

    /**
     * Get tenant security policies
     */
    public TenantSecurityPolicies getTenantSecurityPolicies() {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            // Check cache first
            String cacheKey = "tenant_security_policies:" + tenantId;
            TenantSecurityPolicies cachedPolicies = (TenantSecurityPolicies) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedPolicies != null) {
                return cachedPolicies;
            }

            // Fetch from tenant service
            Map<String, Object> config = getTenantConfiguration();
            TenantSecurityPolicies policies = mapToSecurityPolicies(config);
            
            // Cache the policies
            redisTemplate.opsForValue().set(cacheKey, policies, usageCacheTtlMinutes, TimeUnit.MINUTES);
            
            return policies;
            
        } catch (Exception e) {
            logger.error("Error retrieving tenant security policies for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to retrieve tenant security policies", e);
        }
    }

    /**
     * Update tenant security policies
     */
    public void updateTenantSecurityPolicies(TenantSecurityPolicies policies) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            Map<String, Object> config = mapFromSecurityPolicies(policies);
            updateTenantConfiguration(config);
            
            // Clear cache
            String cacheKey = "tenant_security_policies:" + tenantId;
            redisTemplate.delete(cacheKey);
            
            auditService.logSecurityEvent(null, tenantId, "TENANT_SECURITY_POLICIES_UPDATED",
                "Tenant security policies updated",
                SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
                
        } catch (Exception e) {
            logger.error("Error updating tenant security policies for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to update tenant security policies", e);
        }
    }

    // Tenant Usage Monitoring

    /**
     * Get tenant usage statistics
     */
    public TenantUsageStatistics getTenantUsageStatistics() {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            // Check cache first
            String cacheKey = "tenant_usage:" + tenantId;
            TenantUsageStatistics cachedUsage = (TenantUsageStatistics) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedUsage != null) {
                return cachedUsage;
            }

            TenantUsageStatistics usage = new TenantUsageStatistics();
            usage.setTenantId(tenantId);
            usage.setTimestamp(LocalDateTime.now());
            
            // Get user count from users service
            usage.setUserCount(getUserCount(tenantId));
            
            // Get active sessions count
            usage.setActiveSessionsCount(getActiveSessionsCount(tenantId));
            
            // Get storage usage (placeholder - would integrate with storage service)
            usage.setStorageUsageGB(getStorageUsage(tenantId));
            
            // Get API usage statistics
            usage.setApiCallsToday(getApiCallsCount(tenantId, LocalDateTime.now().toLocalDate().atStartOfDay()));
            usage.setApiCallsThisMonth(getApiCallsCount(tenantId, LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)));
            
            // Get tenant limits
            TenantValidationService.TenantInfo tenantInfo = tenantValidationService.validateTenant(tenantId).getTenantInfo();
            if (tenantInfo != null) {
                usage.setMaxUsers(tenantInfo.getMaxUsers());
                usage.setMaxStorageGB(100); // Default or from tenant info
            }
            
            // Cache the usage statistics
            redisTemplate.opsForValue().set(cacheKey, usage, usageCacheTtlMinutes, TimeUnit.MINUTES);
            
            return usage;
            
        } catch (Exception e) {
            logger.error("Error retrieving tenant usage statistics for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to retrieve tenant usage statistics", e);
        }
    }

    /**
     * Check if tenant is within usage limits
     */
    public TenantLimitCheckResult checkTenantLimits() {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            TenantUsageStatistics usage = getTenantUsageStatistics();
            TenantLimitCheckResult result = new TenantLimitCheckResult();
            result.setTenantId(tenantId);
            result.setWithinLimits(true);
            
            List<String> violations = new ArrayList<>();
            
            // Check user limit
            if (usage.getMaxUsers() != null && usage.getUserCount() >= usage.getMaxUsers()) {
                violations.add("User limit exceeded: " + usage.getUserCount() + "/" + usage.getMaxUsers());
                result.setWithinLimits(false);
            }
            
            // Check storage limit
            if (usage.getMaxStorageGB() != null && usage.getStorageUsageGB() >= usage.getMaxStorageGB()) {
                violations.add("Storage limit exceeded: " + usage.getStorageUsageGB() + "GB/" + usage.getMaxStorageGB() + "GB");
                result.setWithinLimits(false);
            }
            
            // Check API rate limits (placeholder)
            if (usage.getApiCallsToday() > 10000) { // Example limit
                violations.add("Daily API limit exceeded: " + usage.getApiCallsToday());
                result.setWithinLimits(false);
            }
            
            result.setViolations(violations);
            
            if (!result.isWithinLimits()) {
                auditService.logSecurityEvent(null, tenantId, "TENANT_LIMITS_EXCEEDED",
                    "Tenant limits exceeded: " + String.join(", ", violations),
                    SecurityAuditLog.AuditEventStatus.WARNING, null, null, null);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error checking tenant limits for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to check tenant limits", e);
        }
    }

    // Tenant Role and Permission Management

    /**
     * Get tenant-specific roles
     */
    public List<Map<String, Object>> getTenantRoles() {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            String url = usersServiceUrl + "/api/v1/roles?tenantId=" + tenantId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> roles = (List<Map<String, Object>>) response.getBody().get("data");
                return roles != null ? roles : new ArrayList<>();
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error retrieving tenant roles for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to retrieve tenant roles", e);
        }
    }

    /**
     * Create tenant-specific role
     */
    public Map<String, Object> createTenantRole(Map<String, Object> roleData) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            // Ensure tenant ID is set
            roleData.put("tenantId", tenantId.toString());
            
            String url = usersServiceUrl + "/api/v1/roles";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, roleData, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> createdRole = (Map<String, Object>) response.getBody().get("data");
                
                auditService.logSecurityEvent(null, tenantId, "TENANT_ROLE_CREATED",
                    "Tenant role created: " + roleData.get("name"),
                    SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, null);
                
                return createdRole;
            }
            
            throw new RuntimeException("Failed to create tenant role");
        } catch (Exception e) {
            logger.error("Error creating tenant role for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to create tenant role", e);
        }
    }

    // Tenant Data Export and Migration

    /**
     * Export tenant data
     */
    public TenantDataExport exportTenantData(TenantDataExportRequest request) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            TenantDataExport export = new TenantDataExport();
            export.setTenantId(tenantId);
            export.setExportId(UUID.randomUUID().toString());
            export.setRequestedAt(LocalDateTime.now());
            export.setDataTypes(request.getDataTypes());
            
            Map<String, Object> exportData = new HashMap<>();
            
            // Export configuration if requested
            if (request.getDataTypes().contains("configuration")) {
                exportData.put("configuration", getTenantConfiguration());
            }
            
            // Export roles if requested
            if (request.getDataTypes().contains("roles")) {
                exportData.put("roles", getTenantRoles());
            }
            
            // Export usage statistics if requested
            if (request.getDataTypes().contains("usage")) {
                exportData.put("usage", getTenantUsageStatistics());
            }
            
            // Export security policies if requested
            if (request.getDataTypes().contains("security_policies")) {
                exportData.put("security_policies", getTenantSecurityPolicies());
            }
            
            export.setData(exportData);
            export.setCompletedAt(LocalDateTime.now());
            export.setStatus("COMPLETED");
            
            // Store export record (in real implementation, this would be persisted)
            String cacheKey = "tenant_export:" + export.getExportId();
            redisTemplate.opsForValue().set(cacheKey, export, 24, TimeUnit.HOURS);
            
            auditService.logSecurityEvent(null, tenantId, "TENANT_DATA_EXPORTED",
                "Tenant data exported: " + request.getDataTypes(),
                SecurityAuditLog.AuditEventStatus.SUCCESS, null, null, export.getExportId());
            
            return export;
            
        } catch (Exception e) {
            logger.error("Error exporting tenant data for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to export tenant data", e);
        }
    }

    /**
     * Get tenant data export status
     */
    public TenantDataExport getDataExportStatus(String exportId) {
        UUID tenantId = tenantContextService.requireTenantContext();
        tenantContextService.requireValidatedTenant();

        try {
            String cacheKey = "tenant_export:" + exportId;
            TenantDataExport export = (TenantDataExport) redisTemplate.opsForValue().get(cacheKey);
            
            if (export == null) {
                throw new RuntimeException("Export not found: " + exportId);
            }
            
            // Validate tenant access
            if (!tenantId.equals(export.getTenantId())) {
                throw new RuntimeException("Access denied to export: " + exportId);
            }
            
            return export;
            
        } catch (Exception e) {
            logger.error("Error retrieving export status: {}", exportId, e);
            throw new RuntimeException("Failed to retrieve export status", e);
        }
    }

    // Private helper methods

    private int getUserCount(UUID tenantId) {
        try {
            String url = usersServiceUrl + "/api/v1/users/count?tenantId=" + tenantId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object count = response.getBody().get("data");
                return count instanceof Number ? ((Number) count).intValue() : 0;
            }
            
            return 0;
        } catch (Exception e) {
            logger.debug("Error getting user count for tenant: {}", tenantId, e);
            return 0;
        }
    }

    private int getActiveSessionsCount(UUID tenantId) {
        // This would integrate with session management
        return 0; // Placeholder
    }

    private double getStorageUsage(UUID tenantId) {
        // This would integrate with storage service
        return 0.0; // Placeholder
    }

    private long getApiCallsCount(UUID tenantId, LocalDateTime since) {
        // This would integrate with API gateway metrics
        return 0L; // Placeholder
    }

    private void clearTenantConfigurationCache(UUID tenantId) {
        try {
            String cacheKey = "tenant_security_policies:" + tenantId;
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            logger.error("Error clearing tenant configuration cache", e);
        }
    }

    private TenantSecurityPolicies mapToSecurityPolicies(Map<String, Object> config) {
        TenantSecurityPolicies policies = new TenantSecurityPolicies();
        
        // Map configuration to security policies
        policies.setPasswordMinLength((Integer) config.getOrDefault("password_min_length", 8));
        policies.setPasswordRequireSpecialChars((Boolean) config.getOrDefault("password_require_special_chars", true));
        policies.setPasswordRequireNumbers((Boolean) config.getOrDefault("password_require_numbers", true));
        policies.setPasswordRequireUppercase((Boolean) config.getOrDefault("password_require_uppercase", true));
        policies.setPasswordHistoryCount((Integer) config.getOrDefault("password_history_count", 5));
        policies.setSessionTimeoutMinutes((Integer) config.getOrDefault("session_timeout_minutes", 30));
        policies.setMfaRequired((Boolean) config.getOrDefault("mfa_required", false));
        policies.setMaxFailedLoginAttempts((Integer) config.getOrDefault("max_failed_login_attempts", 5));
        policies.setAccountLockoutDurationMinutes((Integer) config.getOrDefault("account_lockout_duration_minutes", 30));
        policies.setApiRateLimitPerMinute((Integer) config.getOrDefault("api_rate_limit_per_minute", 100));
        
        return policies;
    }

    private Map<String, Object> mapFromSecurityPolicies(TenantSecurityPolicies policies) {
        Map<String, Object> config = new HashMap<>();
        
        config.put("password_min_length", policies.getPasswordMinLength());
        config.put("password_require_special_chars", policies.isPasswordRequireSpecialChars());
        config.put("password_require_numbers", policies.isPasswordRequireNumbers());
        config.put("password_require_uppercase", policies.isPasswordRequireUppercase());
        config.put("password_history_count", policies.getPasswordHistoryCount());
        config.put("session_timeout_minutes", policies.getSessionTimeoutMinutes());
        config.put("mfa_required", policies.isMfaRequired());
        config.put("max_failed_login_attempts", policies.getMaxFailedLoginAttempts());
        config.put("account_lockout_duration_minutes", policies.getAccountLockoutDurationMinutes());
        config.put("api_rate_limit_per_minute", policies.getApiRateLimitPerMinute());
        
        return config;
    }

    // Inner classes for data structures

    public static class TenantSecurityPolicies {
        private int passwordMinLength = 8;
        private boolean passwordRequireSpecialChars = true;
        private boolean passwordRequireNumbers = true;
        private boolean passwordRequireUppercase = true;
        private int passwordHistoryCount = 5;
        private int sessionTimeoutMinutes = 30;
        private boolean mfaRequired = false;
        private int maxFailedLoginAttempts = 5;
        private int accountLockoutDurationMinutes = 30;
        private int apiRateLimitPerMinute = 100;

        // Getters and Setters
        public int getPasswordMinLength() { return passwordMinLength; }
        public void setPasswordMinLength(int passwordMinLength) { this.passwordMinLength = passwordMinLength; }

        public boolean isPasswordRequireSpecialChars() { return passwordRequireSpecialChars; }
        public void setPasswordRequireSpecialChars(boolean passwordRequireSpecialChars) { this.passwordRequireSpecialChars = passwordRequireSpecialChars; }

        public boolean isPasswordRequireNumbers() { return passwordRequireNumbers; }
        public void setPasswordRequireNumbers(boolean passwordRequireNumbers) { this.passwordRequireNumbers = passwordRequireNumbers; }

        public boolean isPasswordRequireUppercase() { return passwordRequireUppercase; }
        public void setPasswordRequireUppercase(boolean passwordRequireUppercase) { this.passwordRequireUppercase = passwordRequireUppercase; }

        public int getPasswordHistoryCount() { return passwordHistoryCount; }
        public void setPasswordHistoryCount(int passwordHistoryCount) { this.passwordHistoryCount = passwordHistoryCount; }

        public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }

        public boolean isMfaRequired() { return mfaRequired; }
        public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }

        public int getMaxFailedLoginAttempts() { return maxFailedLoginAttempts; }
        public void setMaxFailedLoginAttempts(int maxFailedLoginAttempts) { this.maxFailedLoginAttempts = maxFailedLoginAttempts; }

        public int getAccountLockoutDurationMinutes() { return accountLockoutDurationMinutes; }
        public void setAccountLockoutDurationMinutes(int accountLockoutDurationMinutes) { this.accountLockoutDurationMinutes = accountLockoutDurationMinutes; }

        public int getApiRateLimitPerMinute() { return apiRateLimitPerMinute; }
        public void setApiRateLimitPerMinute(int apiRateLimitPerMinute) { this.apiRateLimitPerMinute = apiRateLimitPerMinute; }
    }

    public static class TenantUsageStatistics {
        private UUID tenantId;
        private LocalDateTime timestamp;
        private int userCount;
        private Integer maxUsers;
        private int activeSessionsCount;
        private double storageUsageGB;
        private Integer maxStorageGB;
        private long apiCallsToday;
        private long apiCallsThisMonth;

        // Getters and Setters
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public int getUserCount() { return userCount; }
        public void setUserCount(int userCount) { this.userCount = userCount; }

        public Integer getMaxUsers() { return maxUsers; }
        public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

        public int getActiveSessionsCount() { return activeSessionsCount; }
        public void setActiveSessionsCount(int activeSessionsCount) { this.activeSessionsCount = activeSessionsCount; }

        public double getStorageUsageGB() { return storageUsageGB; }
        public void setStorageUsageGB(double storageUsageGB) { this.storageUsageGB = storageUsageGB; }

        public Integer getMaxStorageGB() { return maxStorageGB; }
        public void setMaxStorageGB(Integer maxStorageGB) { this.maxStorageGB = maxStorageGB; }

        public long getApiCallsToday() { return apiCallsToday; }
        public void setApiCallsToday(long apiCallsToday) { this.apiCallsToday = apiCallsToday; }

        public long getApiCallsThisMonth() { return apiCallsThisMonth; }
        public void setApiCallsThisMonth(long apiCallsThisMonth) { this.apiCallsThisMonth = apiCallsThisMonth; }
    }

    public static class TenantLimitCheckResult {
        private UUID tenantId;
        private boolean withinLimits;
        private List<String> violations;

        // Getters and Setters
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

        public boolean isWithinLimits() { return withinLimits; }
        public void setWithinLimits(boolean withinLimits) { this.withinLimits = withinLimits; }

        public List<String> getViolations() { return violations; }
        public void setViolations(List<String> violations) { this.violations = violations; }
    }

    public static class TenantDataExportRequest {
        private List<String> dataTypes;
        private String format = "JSON";
        private boolean includeMetadata = true;

        // Getters and Setters
        public List<String> getDataTypes() { return dataTypes; }
        public void setDataTypes(List<String> dataTypes) { this.dataTypes = dataTypes; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public boolean isIncludeMetadata() { return includeMetadata; }
        public void setIncludeMetadata(boolean includeMetadata) { this.includeMetadata = includeMetadata; }
    }

    public static class TenantDataExport {
        private String exportId;
        private UUID tenantId;
        private LocalDateTime requestedAt;
        private LocalDateTime completedAt;
        private String status;
        private List<String> dataTypes;
        private Map<String, Object> data;

        // Getters and Setters
        public String getExportId() { return exportId; }
        public void setExportId(String exportId) { this.exportId = exportId; }

        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

        public LocalDateTime getRequestedAt() { return requestedAt; }
        public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<String> getDataTypes() { return dataTypes; }
        public void setDataTypes(List<String> dataTypes) { this.dataTypes = dataTypes; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}