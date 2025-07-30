package com.crm.platform.auth.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityAuditService auditService;
    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    
    // IP blocking and whitelisting
    private final Set<String> blockedIPs = ConcurrentHashMap.newKeySet();
    private final Set<String> whitelistedIPs = ConcurrentHashMap.newKeySet();
    private final Map<String, LocalDateTime> temporaryBlocks = new ConcurrentHashMap<>();

    // Rate limiting configurations - configurable via properties
    @Value("${rate-limit.login.per-minute:5}")
    private int LOGIN_ATTEMPTS_PER_MINUTE;
    
    @Value("${rate-limit.login.per-hour:20}")
    private int LOGIN_ATTEMPTS_PER_HOUR;
    
    // Token endpoint rate limits - configurable via properties
    @Value("${rate-limit.token.per-minute:10}")
    private int TOKEN_REQUESTS_PER_MINUTE;
    
    @Value("${rate-limit.token.per-hour:100}")
    private int TOKEN_REQUESTS_PER_HOUR;
    
    @Value("${rate-limit.refresh-token.per-minute:5}")
    private int REFRESH_TOKEN_REQUESTS_PER_MINUTE;
    
    @Value("${rate-limit.refresh-token.per-hour:50}")
    private int REFRESH_TOKEN_REQUESTS_PER_HOUR;
    
    @Value("${rate-limit.revoke-token.per-minute:20}")
    private int REVOKE_TOKEN_REQUESTS_PER_MINUTE;
    
    @Value("${rate-limit.introspect.per-minute:30}")
    private int INTROSPECT_REQUESTS_PER_MINUTE;

    // DDoS protection settings
    @Value("${rate-limit.ddos.requests-per-second:100}")
    private int DDOS_REQUESTS_PER_SECOND;
    
    @Value("${rate-limit.ddos.burst-capacity:200}")
    private int DDOS_BURST_CAPACITY;
    
    @Value("${rate-limit.ip-block.duration-minutes:60}")
    private int IP_BLOCK_DURATION_MINUTES;
    
    @Value("${rate-limit.suspicious.threshold:50}")
    private int SUSPICIOUS_ACTIVITY_THRESHOLD;

    @Autowired
    public RateLimitingService(RedisTemplate<String, Object> redisTemplate, SecurityAuditService auditService) {
        this.redisTemplate = redisTemplate;
        this.auditService = auditService;
        
        // Initialize default whitelisted IPs (localhost, private networks)
        initializeDefaultWhitelist();
    }

    public boolean isAllowed(String identifier, String operation) {
        return isAllowed(identifier, operation, null);
    }

    public boolean isAllowed(String identifier, String operation, String ipAddress) {
        // Check IP blocking first
        if (ipAddress != null && isIPBlocked(ipAddress)) {
            logger.warn("Request blocked from IP: {} for operation: {}", ipAddress, operation);
            auditService.logSecurityEvent(null, null, "IP_BLOCKED_REQUEST",
                "Request blocked from IP: " + ipAddress + " for operation: " + operation,
                com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
                ipAddress, null, null);
            return false;
        }

        String key = operation + ":" + identifier;
        Bucket bucket = bucketCache.computeIfAbsent(key, k -> createBucket(operation));
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            logger.warn("Rate limit exceeded for identifier: {} operation: {}", identifier, operation);
            
            // Check if this should trigger IP blocking
            if (ipAddress != null && shouldBlockIP(ipAddress, operation)) {
                blockIPTemporarily(ipAddress, "Rate limit exceeded");
            }
            
            auditService.logSecurityEvent(null, null, "RATE_LIMIT_EXCEEDED",
                "Rate limit exceeded for identifier: " + identifier + " operation: " + operation,
                com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
                ipAddress, null, null);
        }

        return allowed;
    }

    private Bucket createBucket(String operation) {
        switch (operation) {
            case "login":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(LOGIN_ATTEMPTS_PER_MINUTE, Refill.intervally(LOGIN_ATTEMPTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(LOGIN_ATTEMPTS_PER_HOUR, Refill.intervally(LOGIN_ATTEMPTS_PER_HOUR, Duration.ofHours(1))))
                    .build();
            case "oauth2_token":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(TOKEN_REQUESTS_PER_MINUTE, Refill.intervally(TOKEN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(TOKEN_REQUESTS_PER_HOUR, Refill.intervally(TOKEN_REQUESTS_PER_HOUR, Duration.ofHours(1))))
                    .build();
            case "refresh_token":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(REFRESH_TOKEN_REQUESTS_PER_MINUTE, Refill.intervally(REFRESH_TOKEN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(REFRESH_TOKEN_REQUESTS_PER_HOUR, Refill.intervally(REFRESH_TOKEN_REQUESTS_PER_HOUR, Duration.ofHours(1))))
                    .build();
            case "revoke_token":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(REVOKE_TOKEN_REQUESTS_PER_MINUTE, Refill.intervally(REVOKE_TOKEN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .build();
            case "token_introspect":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(INTROSPECT_REQUESTS_PER_MINUTE, Refill.intervally(INTROSPECT_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                    .build();
            case "ddos_protection":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(DDOS_REQUESTS_PER_SECOND, Refill.intervally(DDOS_REQUESTS_PER_SECOND, Duration.ofSeconds(1))))
                    .addLimit(Bandwidth.classic(DDOS_BURST_CAPACITY, Refill.intervally(DDOS_BURST_CAPACITY, Duration.ofMinutes(1))))
                    .build();
            case "password_reset":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1))))
                    .build();
            case "email_verification":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1))))
                    .build();
            case "mfa_verification":
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                    .addLimit(Bandwidth.classic(50, Refill.intervally(50, Duration.ofHours(1))))
                    .build();
            default:
                // Default rate limit: 100 requests per minute
                return Bucket.builder()
                    .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                    .build();
        }
    }

    public void resetRateLimit(String identifier, String operation) {
        String key = operation + ":" + identifier;
        bucketCache.remove(key);
    }

    /**
     * Check rate limit for OAuth2 token requests
     */
    public boolean isTokenRequestAllowed(String clientId) {
        return isAllowed(clientId, "oauth2_token");
    }

    /**
     * Check rate limit for refresh token requests
     */
    public boolean isRefreshTokenAllowed(String identifier) {
        return isAllowed(identifier, "refresh_token");
    }

    /**
     * Check rate limit for token revocation requests
     */
    public boolean isRevokeTokenAllowed(String identifier) {
        return isAllowed(identifier, "revoke_token");
    }

    /**
     * Check rate limit for token introspection requests
     */
    public boolean isTokenIntrospectionAllowed(String identifier) {
        return isAllowed(identifier, "token_introspect");
    }

    /**
     * Get remaining tokens for a specific operation
     */
    public long getRemainingTokens(String identifier, String operation) {
        String key = operation + ":" + identifier;
        Bucket bucket = bucketCache.get(key);
        if (bucket != null) {
            return bucket.getAvailableTokens();
        }
        return createBucket(operation).getAvailableTokens();
    }

    /**
     * Get rate limit information for monitoring
     */
    public RateLimitInfo getRateLimitInfo(String identifier, String operation) {
        String key = operation + ":" + identifier;
        Bucket bucket = bucketCache.computeIfAbsent(key, k -> createBucket(operation));
        
        return new RateLimitInfo(
            bucket.getAvailableTokens(),
            getMaxTokensForOperation(operation),
            System.currentTimeMillis()
        );
    }

    private long getMaxTokensForOperation(String operation) {
        switch (operation) {
            case "login": return LOGIN_ATTEMPTS_PER_MINUTE;
            case "oauth2_token": return TOKEN_REQUESTS_PER_MINUTE;
            case "refresh_token": return REFRESH_TOKEN_REQUESTS_PER_MINUTE;
            case "revoke_token": return REVOKE_TOKEN_REQUESTS_PER_MINUTE;
            case "token_introspect": return INTROSPECT_REQUESTS_PER_MINUTE;
            case "ddos_protection": return DDOS_REQUESTS_PER_SECOND;
            case "password_reset": return 3;
            case "email_verification": return 5;
            case "mfa_verification": return 10;
            default: return 100;
        }
    }

    // IP Blocking and Whitelisting Methods

    /**
     * Initialize default whitelisted IPs
     */
    private void initializeDefaultWhitelist() {
        // Add localhost and private network ranges to whitelist
        whitelistedIPs.add("127.0.0.1");
        whitelistedIPs.add("::1");
        whitelistedIPs.add("0:0:0:0:0:0:0:1");
        
        // Load additional whitelisted IPs from Redis if available
        loadWhitelistFromRedis();
        loadBlocklistFromRedis();
    }

    /**
     * Check if an IP address is blocked
     */
    public boolean isIPBlocked(String ipAddress) {
        if (isIPWhitelisted(ipAddress)) {
            return false;
        }

        // Check permanent blocks
        if (blockedIPs.contains(ipAddress)) {
            return true;
        }

        // Check temporary blocks
        LocalDateTime blockExpiry = temporaryBlocks.get(ipAddress);
        if (blockExpiry != null) {
            if (LocalDateTime.now().isBefore(blockExpiry)) {
                return true;
            } else {
                // Block has expired, remove it
                temporaryBlocks.remove(ipAddress);
            }
        }

        return false;
    }

    /**
     * Check if an IP address is whitelisted
     */
    public boolean isIPWhitelisted(String ipAddress) {
        return whitelistedIPs.contains(ipAddress) || isPrivateIP(ipAddress);
    }

    /**
     * Block an IP address permanently
     */
    public void blockIPPermanently(String ipAddress, String reason) {
        if (isIPWhitelisted(ipAddress)) {
            logger.warn("Cannot block whitelisted IP: {}", ipAddress);
            return;
        }

        blockedIPs.add(ipAddress);
        saveBlocklistToRedis();
        
        logger.warn("IP {} blocked permanently. Reason: {}", ipAddress, reason);
        auditService.logSecurityEvent(null, null, "IP_BLOCKED_PERMANENT",
            "IP " + ipAddress + " blocked permanently. Reason: " + reason,
            com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
            ipAddress, null, null);
    }

    /**
     * Block an IP address temporarily
     */
    public void blockIPTemporarily(String ipAddress, String reason) {
        if (isIPWhitelisted(ipAddress)) {
            logger.warn("Cannot block whitelisted IP: {}", ipAddress);
            return;
        }

        LocalDateTime blockUntil = LocalDateTime.now().plusMinutes(IP_BLOCK_DURATION_MINUTES);
        temporaryBlocks.put(ipAddress, blockUntil);
        
        logger.warn("IP {} blocked temporarily until {}. Reason: {}", ipAddress, blockUntil, reason);
        auditService.logSecurityEvent(null, null, "IP_BLOCKED_TEMPORARY",
            "IP " + ipAddress + " blocked temporarily until " + blockUntil + ". Reason: " + reason,
            com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
            ipAddress, null, null);
    }

    /**
     * Unblock an IP address
     */
    public void unblockIP(String ipAddress) {
        boolean wasBlocked = blockedIPs.remove(ipAddress) || temporaryBlocks.remove(ipAddress) != null;
        
        if (wasBlocked) {
            saveBlocklistToRedis();
            logger.info("IP {} unblocked", ipAddress);
            auditService.logSecurityEvent(null, null, "IP_UNBLOCKED",
                "IP " + ipAddress + " unblocked",
                com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.SUCCESS,
                ipAddress, null, null);
        }
    }

    /**
     * Add IP to whitelist
     */
    public void addToWhitelist(String ipAddress) {
        whitelistedIPs.add(ipAddress);
        saveWhitelistToRedis();
        
        // Remove from blocklist if present
        unblockIP(ipAddress);
        
        logger.info("IP {} added to whitelist", ipAddress);
        auditService.logSecurityEvent(null, null, "IP_WHITELISTED",
            "IP " + ipAddress + " added to whitelist",
            com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.SUCCESS,
            ipAddress, null, null);
    }

    /**
     * Remove IP from whitelist
     */
    public void removeFromWhitelist(String ipAddress) {
        if (whitelistedIPs.remove(ipAddress)) {
            saveWhitelistToRedis();
            logger.info("IP {} removed from whitelist", ipAddress);
            auditService.logSecurityEvent(null, null, "IP_WHITELIST_REMOVED",
                "IP " + ipAddress + " removed from whitelist",
                com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.SUCCESS,
                ipAddress, null, null);
        }
    }

    /**
     * Check if IP should be blocked based on suspicious activity
     */
    private boolean shouldBlockIP(String ipAddress, String operation) {
        if (isIPWhitelisted(ipAddress)) {
            return false;
        }

        // Count recent failed attempts from this IP
        String key = "suspicious:" + ipAddress;
        Bucket suspiciousBucket = bucketCache.computeIfAbsent(key, k -> 
            Bucket.builder()
                .addLimit(Bandwidth.classic(SUSPICIOUS_ACTIVITY_THRESHOLD, 
                    Refill.intervally(SUSPICIOUS_ACTIVITY_THRESHOLD, Duration.ofHours(1))))
                .build()
        );

        return !suspiciousBucket.tryConsume(1);
    }

    /**
     * Check if IP is in private network range
     */
    private boolean isPrivateIP(String ipAddress) {
        try {
            // Simple check for common private IP ranges
            return ipAddress.startsWith("192.168.") ||
                   ipAddress.startsWith("10.") ||
                   ipAddress.startsWith("172.16.") ||
                   ipAddress.startsWith("172.17.") ||
                   ipAddress.startsWith("172.18.") ||
                   ipAddress.startsWith("172.19.") ||
                   ipAddress.startsWith("172.2") ||
                   ipAddress.startsWith("172.30.") ||
                   ipAddress.startsWith("172.31.") ||
                   ipAddress.equals("127.0.0.1") ||
                   ipAddress.equals("::1");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * DDoS Protection - check if request should be allowed based on global rate limits
     */
    public boolean isDDoSProtectionTriggered(String ipAddress) {
        if (isIPWhitelisted(ipAddress)) {
            return false;
        }

        String key = "ddos:" + ipAddress;
        Bucket ddosBucket = bucketCache.computeIfAbsent(key, k -> createBucket("ddos_protection"));
        
        boolean allowed = ddosBucket.tryConsume(1);
        if (!allowed) {
            logger.warn("DDoS protection triggered for IP: {}", ipAddress);
            auditService.logSecurityEvent(null, null, "DDOS_PROTECTION_TRIGGERED",
                "DDoS protection triggered for IP: " + ipAddress,
                com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
                ipAddress, null, null);
        }
        
        return !allowed;
    }

    /**
     * Get blocked IPs list
     */
    public Set<String> getBlockedIPs() {
        Set<String> allBlocked = new HashSet<>(blockedIPs);
        
        // Add temporarily blocked IPs that haven't expired
        LocalDateTime now = LocalDateTime.now();
        temporaryBlocks.entrySet().stream()
            .filter(entry -> now.isBefore(entry.getValue()))
            .forEach(entry -> allBlocked.add(entry.getKey()));
            
        return allBlocked;
    }

    /**
     * Get whitelisted IPs list
     */
    public Set<String> getWhitelistedIPs() {
        return new HashSet<>(whitelistedIPs);
    }

    /**
     * Get IP blocking status
     */
    public IPBlockingStatus getIPBlockingStatus(String ipAddress) {
        boolean isWhitelisted = isIPWhitelisted(ipAddress);
        boolean isPermanentlyBlocked = blockedIPs.contains(ipAddress);
        LocalDateTime temporaryBlockExpiry = temporaryBlocks.get(ipAddress);
        boolean isTemporarilyBlocked = temporaryBlockExpiry != null && LocalDateTime.now().isBefore(temporaryBlockExpiry);
        
        return new IPBlockingStatus(ipAddress, isWhitelisted, isPermanentlyBlocked, 
                                  isTemporarilyBlocked, temporaryBlockExpiry);
    }

    // Redis persistence methods
    private void saveWhitelistToRedis() {
        try {
            redisTemplate.opsForSet().getOperations().delete("rate_limit:whitelist");
            if (!whitelistedIPs.isEmpty()) {
                redisTemplate.opsForSet().add("rate_limit:whitelist", whitelistedIPs.toArray());
            }
        } catch (Exception e) {
            logger.error("Error saving whitelist to Redis", e);
        }
    }

    private void saveBlocklistToRedis() {
        try {
            redisTemplate.opsForSet().getOperations().delete("rate_limit:blocklist");
            if (!blockedIPs.isEmpty()) {
                redisTemplate.opsForSet().add("rate_limit:blocklist", blockedIPs.toArray());
            }
        } catch (Exception e) {
            logger.error("Error saving blocklist to Redis", e);
        }
    }

    private void loadWhitelistFromRedis() {
        try {
            Set<Object> whitelistFromRedis = redisTemplate.opsForSet().members("rate_limit:whitelist");
            if (whitelistFromRedis != null) {
                whitelistFromRedis.forEach(ip -> whitelistedIPs.add(ip.toString()));
            }
        } catch (Exception e) {
            logger.error("Error loading whitelist from Redis", e);
        }
    }

    private void loadBlocklistFromRedis() {
        try {
            Set<Object> blocklistFromRedis = redisTemplate.opsForSet().members("rate_limit:blocklist");
            if (blocklistFromRedis != null) {
                blocklistFromRedis.forEach(ip -> blockedIPs.add(ip.toString()));
            }
        } catch (Exception e) {
            logger.error("Error loading blocklist from Redis", e);
        }
    }

    // CAPTCHA Integration Methods

    /**
     * Check if CAPTCHA is required for this IP/operation
     */
    public boolean isCaptchaRequired(String ipAddress, String operation) {
        if (isIPWhitelisted(ipAddress)) {
            return false;
        }

        // CAPTCHA required if IP has exceeded suspicious activity threshold
        String key = "captcha_required:" + ipAddress + ":" + operation;
        return redisTemplate.hasKey(key);
    }

    /**
     * Mark that CAPTCHA is required for this IP/operation
     */
    public void requireCaptcha(String ipAddress, String operation, int durationMinutes) {
        String key = "captcha_required:" + ipAddress + ":" + operation;
        redisTemplate.opsForValue().set(key, "true", durationMinutes, TimeUnit.MINUTES);
        
        logger.info("CAPTCHA required for IP: {} operation: {} for {} minutes", 
                   ipAddress, operation, durationMinutes);
        auditService.logSecurityEvent(null, null, "CAPTCHA_REQUIRED",
            "CAPTCHA required for IP: " + ipAddress + " operation: " + operation,
            com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.WARNING,
            ipAddress, null, null);
    }

    /**
     * Clear CAPTCHA requirement after successful verification
     */
    public void clearCaptchaRequirement(String ipAddress, String operation) {
        String key = "captcha_required:" + ipAddress + ":" + operation;
        redisTemplate.delete(key);
        
        logger.info("CAPTCHA requirement cleared for IP: {} operation: {}", ipAddress, operation);
        auditService.logSecurityEvent(null, null, "CAPTCHA_CLEARED",
            "CAPTCHA requirement cleared for IP: " + ipAddress + " operation: " + operation,
            com.crm.platform.auth.entity.SecurityAuditLog.AuditEventStatus.SUCCESS,
            ipAddress, null, null);
    }

    /**
     * Enhanced rate limiting with CAPTCHA integration
     */
    public RateLimitResult checkRateLimit(String identifier, String operation, String ipAddress) {
        // Check DDoS protection first
        if (isDDoSProtectionTriggered(ipAddress)) {
            return new RateLimitResult(false, true, true, "DDoS protection triggered");
        }

        // Check if IP is blocked
        if (isIPBlocked(ipAddress)) {
            return new RateLimitResult(false, true, false, "IP address is blocked");
        }

        // Check CAPTCHA requirement
        boolean captchaRequired = isCaptchaRequired(ipAddress, operation);

        // Check rate limit
        boolean allowed = isAllowed(identifier, operation, ipAddress);
        
        if (!allowed && !captchaRequired) {
            // First time exceeding rate limit - require CAPTCHA
            requireCaptcha(ipAddress, operation, 30); // 30 minutes
            captchaRequired = true;
        }

        return new RateLimitResult(allowed, false, captchaRequired, 
                                 allowed ? "Request allowed" : "Rate limit exceeded");
    }

    /**
     * Clean up expired temporary blocks and CAPTCHA requirements
     */
    public void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up expired temporary blocks
        temporaryBlocks.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        
        logger.debug("Cleaned up expired rate limiting entries");
    }

    /**
     * Get comprehensive rate limiting statistics
     */
    public Map<String, Object> getRateLimitingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("blocked_ips_count", blockedIPs.size());
        stats.put("whitelisted_ips_count", whitelistedIPs.size());
        stats.put("temporary_blocks_count", temporaryBlocks.size());
        stats.put("active_buckets_count", bucketCache.size());
        
        // Count expired temporary blocks
        LocalDateTime now = LocalDateTime.now();
        long expiredBlocks = temporaryBlocks.values().stream()
            .mapToLong(expiry -> now.isAfter(expiry) ? 1 : 0)
            .sum();
        stats.put("expired_temporary_blocks", expiredBlocks);
        
        return stats;
    }

    /**
     * Rate limit information holder
     */
    public static class RateLimitInfo {
        private final long remainingTokens;
        private final long maxTokens;
        private final long timestamp;

        public RateLimitInfo(long remainingTokens, long maxTokens, long timestamp) {
            this.remainingTokens = remainingTokens;
            this.maxTokens = maxTokens;
            this.timestamp = timestamp;
        }

        public long getRemainingTokens() { return remainingTokens; }
        public long getMaxTokens() { return maxTokens; }
        public long getTimestamp() { return timestamp; }
        public boolean isLimitExceeded() { return remainingTokens <= 0; }
    }

    /**
     * IP blocking status holder
     */
    public static class IPBlockingStatus {
        private final String ipAddress;
        private final boolean isWhitelisted;
        private final boolean isPermanentlyBlocked;
        private final boolean isTemporarilyBlocked;
        private final LocalDateTime temporaryBlockExpiry;

        public IPBlockingStatus(String ipAddress, boolean isWhitelisted, boolean isPermanentlyBlocked,
                              boolean isTemporarilyBlocked, LocalDateTime temporaryBlockExpiry) {
            this.ipAddress = ipAddress;
            this.isWhitelisted = isWhitelisted;
            this.isPermanentlyBlocked = isPermanentlyBlocked;
            this.isTemporarilyBlocked = isTemporarilyBlocked;
            this.temporaryBlockExpiry = temporaryBlockExpiry;
        }

        public String getIpAddress() { return ipAddress; }
        public boolean isWhitelisted() { return isWhitelisted; }
        public boolean isPermanentlyBlocked() { return isPermanentlyBlocked; }
        public boolean isTemporarilyBlocked() { return isTemporarilyBlocked; }
        public LocalDateTime getTemporaryBlockExpiry() { return temporaryBlockExpiry; }
        public boolean isBlocked() { return isPermanentlyBlocked || isTemporarilyBlocked; }
    }

    /**
     * Rate limit result holder
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final boolean blocked;
        private final boolean captchaRequired;
        private final String message;

        public RateLimitResult(boolean allowed, boolean blocked, boolean captchaRequired, String message) {
            this.allowed = allowed;
            this.blocked = blocked;
            this.captchaRequired = captchaRequired;
            this.message = message;
        }

        public boolean isAllowed() { return allowed; }
        public boolean isBlocked() { return blocked; }
        public boolean isCaptchaRequired() { return captchaRequired; }
        public String getMessage() { return message; }
    }
}