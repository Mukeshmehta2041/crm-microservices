package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.TrustedDeviceRequest;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Device trust management service for MFA bypass functionality
 */
@Service
@Transactional
public class DeviceTrustService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceTrustService.class);
    private static final int DEFAULT_TRUST_DURATION_DAYS = 30;

    @Autowired
    private UserCredentialsRepository userCredentialsRepository;

    @Autowired
    private SecurityAuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Add a trusted device for MFA bypass
     */
    public Map<String, Object> addTrustedDevice(UUID userId, TrustedDeviceRequest request, 
                                              HttpServletRequest httpRequest) {
        try {
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            // Check if MFA is enabled
            if (!Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                throw new BusinessException("MFA must be enabled to add trusted devices");
            }

            // Generate device fingerprint
            String deviceFingerprint = generateDeviceFingerprint(httpRequest, request);
            
            // Check if device is already trusted
            List<Map<String, Object>> trustedDevices = parseTrustedDevices(credentials.getTrustedDevices());
            boolean deviceExists = trustedDevices.stream()
                .anyMatch(device -> deviceFingerprint.equals(device.get("fingerprint")));

            if (deviceExists) {
                throw new BusinessException("Device is already trusted");
            }

            // Create trusted device entry
            Map<String, Object> trustedDevice = new HashMap<>();
            trustedDevice.put("id", UUID.randomUUID().toString());
            trustedDevice.put("name", request.getDeviceName());
            trustedDevice.put("type", request.getDeviceType());
            trustedDevice.put("fingerprint", deviceFingerprint);
            trustedDevice.put("userAgent", request.getUserAgent());
            trustedDevice.put("ipAddress", getClientIp(httpRequest));
            trustedDevice.put("addedAt", LocalDateTime.now().toString());
            
            int trustDurationDays = request.getTrustDurationDays() != null ? 
                request.getTrustDurationDays() : DEFAULT_TRUST_DURATION_DAYS;
            trustedDevice.put("expiresAt", LocalDateTime.now().plusDays(trustDurationDays).toString());
            trustedDevice.put("trustDurationDays", trustDurationDays);

            // Add to trusted devices list
            trustedDevices.add(trustedDevice);
            credentials.setTrustedDevices(objectMapper.writeValueAsString(trustedDevices));
            userCredentialsRepository.save(credentials);

            // Audit log
            auditService.logSecurityEvent(userId, credentials.getTenantId(), "TRUSTED_DEVICE_ADDED", 
                "Trusted device added: " + request.getDeviceName(), 
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);

            logger.info("Trusted device added for user: {}, device: {}", userId, request.getDeviceName());

            return Map.of(
                "success", true,
                "deviceId", trustedDevice.get("id"),
                "deviceName", request.getDeviceName(),
                "expiresAt", trustedDevice.get("expiresAt"),
                "message", "Device has been added to trusted devices list"
            );

        } catch (Exception e) {
            logger.error("Error adding trusted device for user: {}", userId, e);
            throw new BusinessException("Failed to add trusted device: " + e.getMessage());
        }
    }

    /**
     * Check if current device is trusted
     */
    public boolean isDeviceTrusted(UUID userId, HttpServletRequest httpRequest) {
        try {
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElse(null);

            if (credentials == null || !Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                return false;
            }

            String currentDeviceFingerprint = generateDeviceFingerprint(httpRequest, null);
            List<Map<String, Object>> trustedDevices = parseTrustedDevices(credentials.getTrustedDevices());

            // Check if current device matches any trusted device and is not expired
            LocalDateTime now = LocalDateTime.now();
            return trustedDevices.stream()
                .anyMatch(device -> {
                    String fingerprint = (String) device.get("fingerprint");
                    String expiresAtStr = (String) device.get("expiresAt");
                    
                    if (!currentDeviceFingerprint.equals(fingerprint)) {
                        return false;
                    }
                    
                    try {
                        LocalDateTime expiresAt = LocalDateTime.parse(expiresAtStr);
                        return now.isBefore(expiresAt);
                    } catch (Exception e) {
                        logger.warn("Error parsing expiration date for trusted device", e);
                        return false;
                    }
                });

        } catch (Exception e) {
            logger.error("Error checking device trust for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Get list of trusted devices for a user
     */
    public List<Map<String, Object>> getTrustedDevices(UUID userId) {
        try {
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            List<Map<String, Object>> trustedDevices = parseTrustedDevices(credentials.getTrustedDevices());
            
            // Remove sensitive information and add status
            LocalDateTime now = LocalDateTime.now();
            return trustedDevices.stream()
                .map(device -> {
                    Map<String, Object> publicDevice = new HashMap<>();
                    publicDevice.put("id", device.get("id"));
                    publicDevice.put("name", device.get("name"));
                    publicDevice.put("type", device.get("type"));
                    publicDevice.put("addedAt", device.get("addedAt"));
                    publicDevice.put("expiresAt", device.get("expiresAt"));
                    publicDevice.put("trustDurationDays", device.get("trustDurationDays"));
                    
                    // Add status
                    try {
                        String expiresAtStr = (String) device.get("expiresAt");
                        LocalDateTime expiresAt = LocalDateTime.parse(expiresAtStr);
                        publicDevice.put("isExpired", now.isAfter(expiresAt));
                        publicDevice.put("status", now.isAfter(expiresAt) ? "EXPIRED" : "ACTIVE");
                    } catch (Exception e) {
                        publicDevice.put("isExpired", true);
                        publicDevice.put("status", "INVALID");
                    }
                    
                    return publicDevice;
                })
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting trusted devices for user: {}", userId, e);
            throw new BusinessException("Failed to get trusted devices: " + e.getMessage());
        }
    }

    /**
     * Remove a trusted device
     */
    public Map<String, Object> removeTrustedDevice(UUID userId, String deviceId, 
                                                 HttpServletRequest httpRequest) {
        try {
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            List<Map<String, Object>> trustedDevices = parseTrustedDevices(credentials.getTrustedDevices());
            
            // Find and remove the device
            Optional<Map<String, Object>> deviceToRemove = trustedDevices.stream()
                .filter(device -> deviceId.equals(device.get("id")))
                .findFirst();

            if (deviceToRemove.isEmpty()) {
                throw new BusinessException("Trusted device not found");
            }

            trustedDevices.removeIf(device -> deviceId.equals(device.get("id")));
            credentials.setTrustedDevices(objectMapper.writeValueAsString(trustedDevices));
            userCredentialsRepository.save(credentials);

            // Audit log
            String deviceName = (String) deviceToRemove.get().get("name");
            auditService.logSecurityEvent(userId, credentials.getTenantId(), "TRUSTED_DEVICE_REMOVED", 
                "Trusted device removed: " + deviceName, 
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);

            logger.info("Trusted device removed for user: {}, device: {}", userId, deviceName);

            return Map.of(
                "success", true,
                "deviceId", deviceId,
                "deviceName", deviceName,
                "message", "Device has been removed from trusted devices list"
            );

        } catch (Exception e) {
            logger.error("Error removing trusted device for user: {}", userId, e);
            throw new BusinessException("Failed to remove trusted device: " + e.getMessage());
        }
    }

    /**
     * Clean up expired trusted devices
     */
    public void cleanupExpiredTrustedDevices() {
        try {
            List<UserCredentials> usersWithMfa = userCredentialsRepository.findAll().stream()
                .filter(cred -> Boolean.TRUE.equals(cred.getMfaEnabled()) && 
                               StringUtils.hasText(cred.getTrustedDevices()))
                .collect(java.util.stream.Collectors.toList());

            LocalDateTime now = LocalDateTime.now();
            int totalCleaned = 0;

            for (UserCredentials credentials : usersWithMfa) {
                List<Map<String, Object>> trustedDevices = parseTrustedDevices(credentials.getTrustedDevices());
                int originalSize = trustedDevices.size();

                // Remove expired devices
                trustedDevices.removeIf(device -> {
                    try {
                        String expiresAtStr = (String) device.get("expiresAt");
                        LocalDateTime expiresAt = LocalDateTime.parse(expiresAtStr);
                        return now.isAfter(expiresAt);
                    } catch (Exception e) {
                        // Remove devices with invalid expiration dates
                        return true;
                    }
                });

                if (trustedDevices.size() < originalSize) {
                    credentials.setTrustedDevices(objectMapper.writeValueAsString(trustedDevices));
                    userCredentialsRepository.save(credentials);
                    totalCleaned += (originalSize - trustedDevices.size());
                }
            }

            if (totalCleaned > 0) {
                logger.info("Cleaned up {} expired trusted devices", totalCleaned);
            }

        } catch (Exception e) {
            logger.error("Error cleaning up expired trusted devices", e);
        }
    }

    // Private helper methods

    private String generateDeviceFingerprint(HttpServletRequest request, TrustedDeviceRequest deviceRequest) {
        try {
            StringBuilder fingerprint = new StringBuilder();
            
            // Add IP address
            fingerprint.append(getClientIp(request));
            
            // Add User-Agent
            String userAgent = request.getHeader("User-Agent");
            if (StringUtils.hasText(userAgent)) {
                fingerprint.append("|").append(userAgent);
            }
            
            // Add device-specific information if provided
            if (deviceRequest != null) {
                if (StringUtils.hasText(deviceRequest.getDeviceType())) {
                    fingerprint.append("|").append(deviceRequest.getDeviceType());
                }
            }

            // Hash the fingerprint for privacy and consistency
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprint.toString().getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            logger.error("Error generating device fingerprint", e);
            throw new RuntimeException("Failed to generate device fingerprint", e);
        }
    }

    private List<Map<String, Object>> parseTrustedDevices(String trustedDevicesJson) {
        try {
            if (!StringUtils.hasText(trustedDevicesJson)) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(trustedDevicesJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error parsing trusted devices", e);
            return new ArrayList<>();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}