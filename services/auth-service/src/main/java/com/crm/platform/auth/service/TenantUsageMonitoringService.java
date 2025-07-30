package com.crm.platform.auth.service;

import com.crm.platform.auth.entity.SecurityAuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for monitoring tenant usage and enforcing limits
 */
@Service
public class TenantUsageMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(TenantUsageMonitoringService.class);

    private final TenantContextService tenantContextService;
    private final TenantManagementService tenantManagementService;
    private final SecurityAuditService auditService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public TenantUsageMonitoringService(TenantContextService tenantContextService,
                                      TenantManagementService tenantManagementService,
                                      SecurityAuditService auditService,
                                      RedisTemplate<String, Object> redisTemplate) {
        this.tenantContextService = tenantContextService;
        this.tenantManagementService = tenantManagementService;
        this.auditService = auditService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Record API usage for current tenant
     */
    public void recordApiUsage(String endpoint, String method) {
        try {
            UUID tenantId = tenantContextService.getCurrentTenantId();
            if (tenantId == null) {
                return; // No tenant context
            }

            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String thisMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // Increment daily counter
            String dailyKey = "tenant_api_usage:daily:" + tenantId + ":" + today;
            redisTemplate.opsForValue().increment(dailyKey);
            redisTemplate.expire(dailyKey, 7, TimeUnit.DAYS); // Keep for 7 days
            
            // Increment monthly counter
            String monthlyKey = "tenant_api_usage:monthly:" + tenantId + ":" + thisMonth;
            redisTemplate.opsForValue().increment(monthlyKey);
            redisTemplate.expire(monthlyKey, 90, TimeUnit.DAYS); // Keep for 90 days
            
            // Increment endpoint-specific counter
            String endpointKey = "tenant_api_usage:endpoint:" + tenantId + ":" + today + ":" + endpoint + ":" + method;
            redisTemplate.opsForValue().increment(endpointKey);
            redisTemplate.expire(endpointKey, 7, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.error("Error recording API usage", e);
        }
    }

    /**
     * Check if tenant can make API call (rate limiting)
     */
    public boolean canMakeApiCall() {
        try {
            UUID tenantId = tenantContextService.getCurrentTenantId();
            if (tenantId == null) {
                return true; // No tenant context, allow
            }

            // Get tenant security policies
            TenantManagementService.TenantSecurityPolicies policies = tenantManagementService.getTenantSecurityPolicies();
            int rateLimitPerMinute = policies.getApiRateLimitPerMinute();
            
            String minuteKey = "tenant_rate_limit:" + tenantId + ":" + 
                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
            
            Long currentCount = redisTemplate.opsForValue().increment(minuteKey);
            if (currentCount == 1) {
                redisTemplate.expire(minuteKey, 1, TimeUnit.MINUTES);
            }
            
            if (currentCount > rateLimitPerMinute) {
                auditService.logSecurityEvent(null, tenantId, "TENANT_RATE_LIMIT_EXCEEDED",
                    "Tenant API rate limit exceeded: " + currentCount + "/" + rateLimitPerMinute,
                    SecurityAuditLog.AuditEventStatus.WARNING, null, null, null);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking API rate limit", e);
            return true; // Allow on error
        }
    }

    /**
     * Record user login for tenant
     */
    public void recordUserLogin(UUID userId) {
        try {
            UUID tenantId = tenantContextService.getCurrentTenantId();
            if (tenantId == null) {
                return;
            }

            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String thisMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // Record daily active user
            String dailyActiveUsersKey = "tenant_active_users:daily:" + tenantId + ":" + today;
            redisTemplate.opsForSet().add(dailyActiveUsersKey, userId.toString());
            redisTemplate.expire(dailyActiveUsersKey, 7, TimeUnit.DAYS);
            
            // Record monthly active user
            String monthlyActiveUsersKey = "tenant_active_users:monthly:" + tenantId + ":" + thisMonth;
            redisTemplate.opsForSet().add(monthlyActiveUsersKey, userId.toString());
            redisTemplate.expire(monthlyActiveUsersKey, 90, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.error("Error recording user login", e);
        }
    }

    /**
     * Get tenant usage metrics
     */
    public TenantUsageMetrics getTenantUsageMetrics(UUID tenantId) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String thisMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            TenantUsageMetrics metrics = new TenantUsageMetrics();
            metrics.setTenantId(tenantId);
            metrics.setTimestamp(LocalDateTime.now());
            
            // API usage
            String dailyApiKey = "tenant_api_usage:daily:" + tenantId + ":" + today;
            String monthlyApiKey = "tenant_api_usage:monthly:" + tenantId + ":" + thisMonth;
            
            Object dailyApiUsage = redisTemplate.opsForValue().get(dailyApiKey);
            Object monthlyApiUsage = redisTemplate.opsForValue().get(monthlyApiKey);
            
            metrics.setApiCallsToday(dailyApiUsage != null ? Long.parseLong(dailyApiUsage.toString()) : 0L);
            metrics.setApiCallsThisMonth(monthlyApiUsage != null ? Long.parseLong(monthlyApiUsage.toString()) : 0L);
            
            // Active users
            String dailyActiveUsersKey = "tenant_active_users:daily:" + tenantId + ":" + today;
            String monthlyActiveUsersKey = "tenant_active_users:monthly:" + tenantId + ":" + thisMonth;
            
            Long dailyActiveUsers = redisTemplate.opsForSet().size(dailyActiveUsersKey);
            Long monthlyActiveUsers = redisTemplate.opsForSet().size(monthlyActiveUsersKey);
            
            metrics.setDailyActiveUsers(dailyActiveUsers != null ? dailyActiveUsers.intValue() : 0);
            metrics.setMonthlyActiveUsers(monthlyActiveUsers != null ? monthlyActiveUsers.intValue() : 0);
            
            // Storage usage (placeholder - would integrate with storage service)
            metrics.setStorageUsageGB(0.0);
            
            return metrics;
            
        } catch (Exception e) {
            logger.error("Error getting tenant usage metrics for tenant: {}", tenantId, e);
            return new TenantUsageMetrics();
        }
    }

    /**
     * Get top API endpoints for tenant
     */
    public List<ApiEndpointUsage> getTopApiEndpoints(UUID tenantId, int limit) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String pattern = "tenant_api_usage:endpoint:" + tenantId + ":" + today + ":*";
            
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<ApiEndpointUsage> endpointUsages = new ArrayList<>();
            
            for (String key : keys) {
                try {
                    Object countObj = redisTemplate.opsForValue().get(key);
                    if (countObj != null) {
                        long count = Long.parseLong(countObj.toString());
                        
                        // Parse endpoint and method from key
                        String[] parts = key.split(":");
                        if (parts.length >= 6) {
                            String endpoint = parts[4];
                            String method = parts[5];
                            
                            ApiEndpointUsage usage = new ApiEndpointUsage();
                            usage.setEndpoint(endpoint);
                            usage.setMethod(method);
                            usage.setCount(count);
                            
                            endpointUsages.add(usage);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error parsing endpoint usage key: {}", key, e);
                }
            }
            
            // Sort by count descending and limit results
            return endpointUsages.stream()
                    .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                    .limit(limit)
                    .toList();
                    
        } catch (Exception e) {
            logger.error("Error getting top API endpoints for tenant: {}", tenantId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Check tenant usage limits and send alerts
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkUsageLimits() {
        try {
            // This would typically iterate through all active tenants
            // For now, we'll check if there's a current tenant context
            UUID tenantId = tenantContextService.getCurrentTenantId();
            if (tenantId == null) {
                return;
            }
            
            checkTenantUsageLimits(tenantId);
            
        } catch (Exception e) {
            logger.error("Error during scheduled usage limit check", e);
        }
    }

    /**
     * Check usage limits for specific tenant
     */
    public void checkTenantUsageLimits(UUID tenantId) {
        try {
            TenantUsageMetrics metrics = getTenantUsageMetrics(tenantId);
            TenantManagementService.TenantUsageStatistics usage = tenantManagementService.getTenantUsageStatistics();
            
            List<String> warnings = new ArrayList<>();
            
            // Check user limit (90% threshold)
            if (usage.getMaxUsers() != null) {
                double userUsagePercent = (double) usage.getUserCount() / usage.getMaxUsers() * 100;
                if (userUsagePercent >= 90) {
                    warnings.add("User limit at " + String.format("%.1f", userUsagePercent) + "%");
                }
            }
            
            // Check storage limit (90% threshold)
            if (usage.getMaxStorageGB() != null) {
                double storageUsagePercent = usage.getStorageUsageGB() / usage.getMaxStorageGB() * 100;
                if (storageUsagePercent >= 90) {
                    warnings.add("Storage limit at " + String.format("%.1f", storageUsagePercent) + "%");
                }
            }
            
            // Check API usage (daily limit based on monthly allowance)
            long dailyApiLimit = 10000; // Example daily limit
            if (metrics.getApiCallsToday() >= dailyApiLimit * 0.9) {
                warnings.add("Daily API usage at " + (metrics.getApiCallsToday() * 100 / dailyApiLimit) + "%");
            }
            
            // Send warnings if any
            if (!warnings.isEmpty()) {
                auditService.logSecurityEvent(null, tenantId, "TENANT_USAGE_WARNING",
                    "Tenant usage warnings: " + String.join(", ", warnings),
                    SecurityAuditLog.AuditEventStatus.WARNING, null, null, null);
                
                logger.warn("Tenant {} usage warnings: {}", tenantId, warnings);
            }
            
        } catch (Exception e) {
            logger.error("Error checking usage limits for tenant: {}", tenantId, e);
        }
    }

    /**
     * Generate usage report for tenant
     */
    public TenantUsageReport generateUsageReport(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            TenantUsageReport report = new TenantUsageReport();
            report.setTenantId(tenantId);
            report.setStartDate(startDate);
            report.setEndDate(endDate);
            report.setGeneratedAt(LocalDateTime.now());
            
            // Get current metrics
            TenantUsageMetrics metrics = getTenantUsageMetrics(tenantId);
            report.setCurrentMetrics(metrics);
            
            // Get top endpoints
            List<ApiEndpointUsage> topEndpoints = getTopApiEndpoints(tenantId, 10);
            report.setTopEndpoints(topEndpoints);
            
            // Calculate trends (simplified)
            report.setTrends(calculateUsageTrends(tenantId));
            
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating usage report for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to generate usage report", e);
        }
    }

    // Private helper methods

    private Map<String, String> calculateUsageTrends(UUID tenantId) {
        Map<String, String> trends = new HashMap<>();
        
        try {
            // Get current and previous day metrics for comparison
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String yesterday = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            String todayKey = "tenant_api_usage:daily:" + tenantId + ":" + today;
            String yesterdayKey = "tenant_api_usage:daily:" + tenantId + ":" + yesterday;
            
            Object todayUsage = redisTemplate.opsForValue().get(todayKey);
            Object yesterdayUsage = redisTemplate.opsForValue().get(yesterdayKey);
            
            long todayCount = todayUsage != null ? Long.parseLong(todayUsage.toString()) : 0L;
            long yesterdayCount = yesterdayUsage != null ? Long.parseLong(yesterdayUsage.toString()) : 0L;
            
            if (yesterdayCount > 0) {
                double change = ((double) (todayCount - yesterdayCount) / yesterdayCount) * 100;
                if (change > 10) {
                    trends.put("api_usage", "increasing");
                } else if (change < -10) {
                    trends.put("api_usage", "decreasing");
                } else {
                    trends.put("api_usage", "stable");
                }
            } else {
                trends.put("api_usage", "new");
            }
            
        } catch (Exception e) {
            logger.debug("Error calculating usage trends", e);
            trends.put("api_usage", "unknown");
        }
        
        return trends;
    }

    // Inner classes

    public static class TenantUsageMetrics {
        private UUID tenantId;
        private LocalDateTime timestamp;
        private long apiCallsToday;
        private long apiCallsThisMonth;
        private int dailyActiveUsers;
        private int monthlyActiveUsers;
        private double storageUsageGB;

        // Getters and Setters
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public long getApiCallsToday() { return apiCallsToday; }
        public void setApiCallsToday(long apiCallsToday) { this.apiCallsToday = apiCallsToday; }

        public long getApiCallsThisMonth() { return apiCallsThisMonth; }
        public void setApiCallsThisMonth(long apiCallsThisMonth) { this.apiCallsThisMonth = apiCallsThisMonth; }

        public int getDailyActiveUsers() { return dailyActiveUsers; }
        public void setDailyActiveUsers(int dailyActiveUsers) { this.dailyActiveUsers = dailyActiveUsers; }

        public int getMonthlyActiveUsers() { return monthlyActiveUsers; }
        public void setMonthlyActiveUsers(int monthlyActiveUsers) { this.monthlyActiveUsers = monthlyActiveUsers; }

        public double getStorageUsageGB() { return storageUsageGB; }
        public void setStorageUsageGB(double storageUsageGB) { this.storageUsageGB = storageUsageGB; }
    }

    public static class ApiEndpointUsage {
        private String endpoint;
        private String method;
        private long count;

        // Getters and Setters
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class TenantUsageReport {
        private UUID tenantId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime generatedAt;
        private TenantUsageMetrics currentMetrics;
        private List<ApiEndpointUsage> topEndpoints;
        private Map<String, String> trends;

        // Getters and Setters
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public TenantUsageMetrics getCurrentMetrics() { return currentMetrics; }
        public void setCurrentMetrics(TenantUsageMetrics currentMetrics) { this.currentMetrics = currentMetrics; }

        public List<ApiEndpointUsage> getTopEndpoints() { return topEndpoints; }
        public void setTopEndpoints(List<ApiEndpointUsage> topEndpoints) { this.topEndpoints = topEndpoints; }

        public Map<String, String> getTrends() { return trends; }
        public void setTrends(Map<String, String> trends) { this.trends = trends; }
    }
}