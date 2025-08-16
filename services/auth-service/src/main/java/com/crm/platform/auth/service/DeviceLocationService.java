package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.DeviceInfo;
import com.crm.platform.auth.dto.LocationInfo;
import com.crm.platform.auth.dto.SuspiciousActivityAlert;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.entity.UserSession;
import com.crm.platform.auth.repository.UserSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Enhanced device and location tracking service providing sophisticated
 * device detection, IP-based location services, device fingerprinting,
 * and suspicious activity detection.
 */
@Service
public class DeviceLocationService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceLocationService.class);

    // Enhanced device detection patterns
    private static final Map<String, Pattern> DEVICE_PATTERNS = Map.of(
        "iPhone", Pattern.compile("(?i).*iPhone.*"),
        "iPad", Pattern.compile("(?i).*iPad.*"),
        "Android Phone", Pattern.compile("(?i).*Android.*(Mobile|Phone).*"),
        "Android Tablet", Pattern.compile("(?i).*Android(?!.*Mobile).*"),
        "Windows Phone", Pattern.compile("(?i).*Windows Phone.*"),
        "BlackBerry", Pattern.compile("(?i).*BlackBerry.*"),
        "Desktop", Pattern.compile("(?i).*(Windows|Macintosh|Linux|X11).*"),
        "Smart TV", Pattern.compile("(?i).*(Smart-TV|SmartTV|TV).*"),
        "Gaming Console", Pattern.compile("(?i).*(PlayStation|Xbox|Nintendo).*")
    );

    // Browser detection patterns with version extraction
    private static final Map<String, Pattern> BROWSER_PATTERNS = Map.of(
        "Chrome", Pattern.compile("(?i).*Chrome/([\\d.]+).*"),
        "Firefox", Pattern.compile("(?i).*Firefox/([\\d.]+).*"),
        "Safari", Pattern.compile("(?i).*Version/([\\d.]+).*Safari.*"),
        "Edge", Pattern.compile("(?i).*Edg/([\\d.]+).*"),
        "Opera", Pattern.compile("(?i).*Opera/([\\d.]+).*"),
        "Internet Explorer", Pattern.compile("(?i).*MSIE ([\\d.]+).*")
    );

    // Operating system detection patterns
    private static final Map<String, Pattern> OS_PATTERNS = Map.of(
        "Windows 11", Pattern.compile("(?i).*Windows NT 10\\.0.*"),
        "Windows 10", Pattern.compile("(?i).*Windows NT 10\\.0.*"),
        "Windows 8.1", Pattern.compile("(?i).*Windows NT 6\\.3.*"),
        "Windows 8", Pattern.compile("(?i).*Windows NT 6\\.2.*"),
        "Windows 7", Pattern.compile("(?i).*Windows NT 6\\.1.*"),
        "macOS", Pattern.compile("(?i).*Mac OS X ([\\d_]+).*"),
        "iOS", Pattern.compile("(?i).*OS ([\\d_]+) like Mac OS X.*"),
        "Android", Pattern.compile("(?i).*Android ([\\d.]+).*"),
        "Linux", Pattern.compile("(?i).*Linux.*"),
        "Ubuntu", Pattern.compile("(?i).*Ubuntu.*")
    );

    // Suspicious activity thresholds
    private static final int MAX_LOCATIONS_PER_HOUR = 3;
    private static final int MAX_DEVICES_PER_DAY = 5;
    private static final int MAX_FAILED_ATTEMPTS_PER_IP = 10;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private SecurityAuditService auditService;

    @Value("${auth.location.api-key:}")
    private String locationApiKey;

    @Value("${auth.location.service-url:http://ip-api.com/json/}")
    private String locationServiceUrl;

    @Value("${auth.device.enable-advanced-fingerprinting:true}")
    private boolean enableAdvancedFingerprinting;

    @Value("${auth.security.enable-suspicious-detection:true}")
    private boolean enableSuspiciousDetection;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Detect device type with enhanced accuracy
     */
    public String detectDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }

        // Check specific device patterns first
        for (Map.Entry<String, Pattern> entry : DEVICE_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(userAgent).matches()) {
                return entry.getKey();
            }
        }

        // Fallback to basic detection
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile Device";
        } else if (userAgent.toLowerCase().contains("tablet")) {
            return "Tablet";
        }

        return "Desktop";
    }

    /**
     * Extract browser information with version
     */
    public DeviceInfo.BrowserInfo extractBrowserInfo(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return new DeviceInfo.BrowserInfo("Unknown", "Unknown");
        }

        for (Map.Entry<String, Pattern> entry : BROWSER_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(userAgent);
            if (matcher.find()) {
                String version = matcher.groupCount() > 0 ? matcher.group(1) : "Unknown";
                return new DeviceInfo.BrowserInfo(entry.getKey(), version);
            }
        }

        return new DeviceInfo.BrowserInfo("Unknown", "Unknown");
    }

    /**
     * Extract operating system information with version
     */
    public DeviceInfo.OSInfo extractOSInfo(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return new DeviceInfo.OSInfo("Unknown", "Unknown");
        }

        for (Map.Entry<String, Pattern> entry : OS_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(userAgent);
            if (matcher.find()) {
                String version = matcher.groupCount() > 0 ? 
                    matcher.group(1).replace("_", ".") : "Unknown";
                return new DeviceInfo.OSInfo(entry.getKey(), version);
            }
        }

        return new DeviceInfo.OSInfo("Unknown", "Unknown");
    }

    /**
     * Generate advanced device fingerprint
     */
    public String generateAdvancedDeviceFingerprint(HttpServletRequest request) {
        try {
            Map<String, String> fingerprintData = new HashMap<>();
            
            // Basic headers
            fingerprintData.put("user_agent", request.getHeader("User-Agent"));
            fingerprintData.put("accept", request.getHeader("Accept"));
            fingerprintData.put("accept_language", request.getHeader("Accept-Language"));
            fingerprintData.put("accept_encoding", request.getHeader("Accept-Encoding"));
            
            // Additional headers for enhanced fingerprinting
            if (enableAdvancedFingerprinting) {
                fingerprintData.put("accept_charset", request.getHeader("Accept-Charset"));
                fingerprintData.put("connection", request.getHeader("Connection"));
                fingerprintData.put("upgrade_insecure_requests", request.getHeader("Upgrade-Insecure-Requests"));
                fingerprintData.put("sec_fetch_dest", request.getHeader("Sec-Fetch-Dest"));
                fingerprintData.put("sec_fetch_mode", request.getHeader("Sec-Fetch-Mode"));
                fingerprintData.put("sec_fetch_site", request.getHeader("Sec-Fetch-Site"));
                fingerprintData.put("cache_control", request.getHeader("Cache-Control"));
            }

            // Screen and timezone information (would come from client-side JavaScript)
            // For now, we'll use placeholder values
            fingerprintData.put("timezone_offset", "placeholder");
            fingerprintData.put("screen_resolution", "placeholder");
            fingerprintData.put("color_depth", "placeholder");

            // Create fingerprint hash
            String fingerprintString = fingerprintData.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("|"));

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fingerprintString.getBytes());
            
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            logger.error("Error generating device fingerprint", e);
            return "unknown";
        }
    }

    /**
     * Get location information from IP address
     */
    public LocationInfo getLocationFromIP(String ipAddress) {
        if (!StringUtils.hasText(ipAddress) || isLocalIP(ipAddress)) {
            return new LocationInfo("Local", "Local", "Local", "Local", 0.0, 0.0);
        }

        try {
            String url = locationServiceUrl + ipAddress;
            if (StringUtils.hasText(locationApiKey)) {
                url += "?key=" + locationApiKey;
            }

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && "success".equals(response.get("status"))) {
                return new LocationInfo(
                    (String) response.get("country"),
                    (String) response.get("regionName"),
                    (String) response.get("city"),
                    (String) response.get("timezone"),
                    ((Number) response.getOrDefault("lat", 0.0)).doubleValue(),
                    ((Number) response.getOrDefault("lon", 0.0)).doubleValue()
                );
            }

        } catch (Exception e) {
            logger.debug("Error getting location for IP: {}", ipAddress, e);
        }

        return new LocationInfo("Unknown", "Unknown", "Unknown", "Unknown", 0.0, 0.0);
    }

    /**
     * Create comprehensive device information
     */
    public DeviceInfo createDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = extractClientIpAddress(request);
        
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceType(detectDeviceType(userAgent));
        deviceInfo.setBrowserInfo(extractBrowserInfo(userAgent));
        deviceInfo.setOsInfo(extractOSInfo(userAgent));
        deviceInfo.setIpAddress(ipAddress);
        deviceInfo.setUserAgent(userAgent);
        deviceInfo.setFingerprint(generateAdvancedDeviceFingerprint(request));
        deviceInfo.setLocationInfo(getLocationFromIP(ipAddress));
        deviceInfo.setTimestamp(LocalDateTime.now());
        
        return deviceInfo;
    }

    /**
     * Detect suspicious login activity
     */
    public List<SuspiciousActivityAlert> detectSuspiciousActivity(UUID userId, UUID tenantId, 
                                                                DeviceInfo currentDevice) {
        if (!enableSuspiciousDetection) {
            return Collections.emptyList();
        }

        List<SuspiciousActivityAlert> alerts = new ArrayList<>();
        LocalDateTime recentTime = LocalDateTime.now().minusHours(24);

        try {
            // Get recent sessions for the user
            List<UserSession> recentSessions = sessionRepository.findByUserIdAndStatus(
                userId, UserSession.SessionStatus.ACTIVE)
                .stream()
                .filter(session -> session.getCreatedAt().isAfter(recentTime))
                .collect(Collectors.toList());

            // Check for multiple locations
            Set<String> recentLocations = recentSessions.stream()
                .map(UserSession::getLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            if (recentLocations.size() > MAX_LOCATIONS_PER_HOUR) {
                alerts.add(new SuspiciousActivityAlert(
                    SuspiciousActivityAlert.AlertType.MULTIPLE_LOCATIONS,
                    "Multiple locations detected: " + recentLocations.size(),
                    SuspiciousActivityAlert.RiskLevel.HIGH,
                    Map.of("locations", recentLocations, "count", recentLocations.size())
                ));
            }

            // Check for multiple devices
            Set<String> recentDevices = recentSessions.stream()
                .map(UserSession::getDeviceFingerprint)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            if (recentDevices.size() > MAX_DEVICES_PER_DAY) {
                alerts.add(new SuspiciousActivityAlert(
                    SuspiciousActivityAlert.AlertType.MULTIPLE_DEVICES,
                    "Multiple devices detected: " + recentDevices.size(),
                    SuspiciousActivityAlert.RiskLevel.MEDIUM,
                    Map.of("device_count", recentDevices.size())
                ));
            }

            // Check for unusual location
            if (isUnusualLocation(userId, currentDevice.getLocationInfo(), recentSessions)) {
                alerts.add(new SuspiciousActivityAlert(
                    SuspiciousActivityAlert.AlertType.UNUSUAL_LOCATION,
                    "Login from unusual location: " + currentDevice.getLocationInfo().getCity(),
                    SuspiciousActivityAlert.RiskLevel.MEDIUM,
                    Map.of("location", currentDevice.getLocationInfo())
                ));
            }

            // Check for new device
            if (isNewDevice(userId, currentDevice.getFingerprint(), recentSessions)) {
                alerts.add(new SuspiciousActivityAlert(
                    SuspiciousActivityAlert.AlertType.NEW_DEVICE,
                    "Login from new device: " + currentDevice.getDeviceType(),
                    SuspiciousActivityAlert.RiskLevel.LOW,
                    Map.of("device_type", currentDevice.getDeviceType(),
                           "browser", currentDevice.getBrowserInfo().getName())
                ));
            }

        } catch (Exception e) {
            logger.error("Error detecting suspicious activity for user: {}", userId, e);
        }

        return alerts;
    }

    /**
     * Send security notifications for suspicious activity
     */
    public void sendSecurityNotifications(UUID userId, UUID tenantId, 
                                        List<SuspiciousActivityAlert> alerts, 
                                        DeviceInfo deviceInfo) {
        if (alerts.isEmpty()) {
            return;
        }

        try {
            for (SuspiciousActivityAlert alert : alerts) {
                // Log security event
                auditService.logSecurityEvent(userId, tenantId, 
                    "SUSPICIOUS_ACTIVITY_" + alert.getType().name(),
                    alert.getMessage(),
                    SecurityAuditLog.AuditEventStatus.WARNING,
                    deviceInfo.getIpAddress(),
                    deviceInfo.getUserAgent(),
                    null,
                    objectMapper.writeValueAsString(alert.getDetails())
                );

                // Send notification (placeholder - would integrate with notification service)
                sendNotificationToUser(userId, alert, deviceInfo);
            }

            logger.warn("Suspicious activity detected for user: {} - {} alerts", userId, alerts.size());

        } catch (Exception e) {
            logger.error("Error sending security notifications", e);
        }
    }

    /**
     * Get device usage statistics for a user
     */
    public Map<String, Object> getDeviceStatistics(UUID userId, UUID tenantId) {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            List<UserSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(session -> session.getCreatedAt().isAfter(thirtyDaysAgo))
                .collect(Collectors.toList());

            Map<String, Long> deviceTypes = sessions.stream()
                .collect(Collectors.groupingBy(
                    session -> session.getDeviceType() != null ? session.getDeviceType() : "Unknown",
                    Collectors.counting()
                ));

            Map<String, Long> browsers = sessions.stream()
                .collect(Collectors.groupingBy(
                    session -> session.getBrowser() != null ? session.getBrowser() : "Unknown",
                    Collectors.counting()
                ));

            Map<String, Long> locations = sessions.stream()
                .filter(session -> session.getLocation() != null)
                .collect(Collectors.groupingBy(
                    UserSession::getLocation,
                    Collectors.counting()
                ));

            Set<String> uniqueIPs = sessions.stream()
                .map(UserSession::getIpAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            return Map.of(
                "total_sessions", sessions.size(),
                "device_types", deviceTypes,
                "browsers", browsers,
                "locations", locations,
                "unique_ips", uniqueIPs.size(),
                "period_days", 30
            );

        } catch (Exception e) {
            logger.error("Error getting device statistics for user: {}", userId, e);
            return Map.of("error", "Failed to retrieve statistics");
        }
    }

    // Private helper methods

    private String extractClientIpAddress(HttpServletRequest request) {
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

    private boolean isLocalIP(String ipAddress) {
        return "127.0.0.1".equals(ipAddress) || 
               "localhost".equals(ipAddress) || 
               "::1".equals(ipAddress) ||
               ipAddress.startsWith("192.168.") ||
               ipAddress.startsWith("10.") ||
               ipAddress.startsWith("172.");
    }

    private boolean isUnusualLocation(UUID userId, LocationInfo currentLocation, 
                                    List<UserSession> recentSessions) {
        if (currentLocation == null || "Unknown".equals(currentLocation.getCountry())) {
            return false;
        }

        // Get historical locations for the user
        Set<String> historicalCountries = recentSessions.stream()
            .map(UserSession::getLocation)
            .filter(Objects::nonNull)
            .map(location -> {
                // Extract country from location string (simplified)
                return location.split(",")[0].trim();
            })
            .collect(Collectors.toSet());

        // If user has never logged in from this country, it's unusual
        return !historicalCountries.contains(currentLocation.getCountry());
    }

    private boolean isNewDevice(UUID userId, String deviceFingerprint, 
                              List<UserSession> recentSessions) {
        if (!StringUtils.hasText(deviceFingerprint)) {
            return false;
        }

        // Check if this device fingerprint has been seen before
        return recentSessions.stream()
            .noneMatch(session -> deviceFingerprint.equals(session.getDeviceFingerprint()));
    }

    private void sendNotificationToUser(UUID userId, SuspiciousActivityAlert alert, 
                                      DeviceInfo deviceInfo) {
        // Placeholder for notification service integration
        // This would send email, SMS, or push notifications to the user
        logger.info("Security notification sent to user: {} - {}", userId, alert.getMessage());
        
        // In a real implementation, this would:
        // 1. Get user notification preferences
        // 2. Format notification message
        // 3. Send via appropriate channel (email, SMS, push)
        // 4. Log notification delivery
    }
}