package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.*;
import com.crm.platform.auth.entity.SecurityAuditLog;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.exception.OAuth2Exception;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.common.exception.BusinessException;
import com.crm.platform.common.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-Factor Authentication service providing TOTP authentication,
 * backup code management, and device trust functionality.
 */
@Service
@Transactional
public class MfaService {

    private static final Logger logger = LoggerFactory.getLogger(MfaService.class);

    private static final int SECRET_LENGTH = 32; // 160 bits
    private static final int TOTP_DIGITS = 6;
    private static final int TOTP_PERIOD = 30; // seconds
    private static final int TIME_WINDOW = 1; // Allow 1 period before/after current
    private static final int BACKUP_CODES_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int DEFAULT_TRUST_DURATION_DAYS = 30;

    @Autowired
    private UserCredentialsRepository userCredentialsRepository;

    @Autowired
    private SecurityAuditService auditService;

    @Value("${app.name:CRM Platform}")
    private String appName;

    @Value("${mfa.issuer:crm-platform}")
    private String issuer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Setup MFA for a user
     */
    public MfaSetupResponse setupMfa(String authorization, MfaSetupRequest request, 
                                   jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            // Extract user ID from authorization (simplified - in real implementation would use JWT)
            UUID userId = extractUserIdFromAuth(authorization);
            
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            // Check if MFA is already enabled
            if (Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                throw new BusinessException("MFA is already enabled for this user");
            }

            // Generate TOTP secret
            String secret = generateTotpSecret();
            
            // Generate backup codes
            List<String> backupCodes = generateBackupCodes();
            
            // Create QR code
            String qrCodeUrl = generateQrCodeUrl(credentials.getEmail(), secret);
            byte[] qrCodeImage = generateQrCodeImage(qrCodeUrl);

            // Store secret temporarily (not enabled until verified)
            credentials.setMfaSecret(secret);
            credentials.setBackupCodes(objectMapper.writeValueAsString(backupCodes));
            credentials.setMfaMethod(UserCredentials.MfaMethod.TOTP);
            userCredentialsRepository.save(credentials);

            // Audit log
            auditService.logSecurityEvent(userId, credentials.getTenantId(), "MFA_SETUP_INITIATED", 
                "MFA setup initiated for user", 
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);

            // Create response
            MfaSetupResponse response = new MfaSetupResponse();
            response.setMethod("TOTP");
            response.setSecret(secret);
            response.setQrCode(Base64.getEncoder().encodeToString(qrCodeImage));
            response.setQrCodeUrl(qrCodeUrl);
            response.setBackupCodes(backupCodes);
            response.setInstructions("Scan the QR code with your authenticator app and enter the verification code to complete setup");

            logger.info("MFA setup initiated for user: {}", userId);
            return response;

        } catch (Exception e) {
            logger.error("Error setting up MFA", e);
            throw new BusinessException("Failed to setup MFA: " + e.getMessage());
        }
    }

    /**
     * Verify MFA setup
     */
    public Map<String, Object> verifyMfaSetup(String authorization, MfaVerifySetupRequest request, 
                                            jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            UUID userId = extractUserIdFromAuth(authorization);
            
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            // Check if secret exists but MFA not yet enabled
            if (!StringUtils.hasText(credentials.getMfaSecret()) || Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                throw new BusinessException("Invalid MFA setup state");
            }

            // Verify TOTP code
            boolean isValid = verifyTotpCode(credentials.getMfaSecret(), request.getCode());
            
            if (!isValid) {
                // Audit failed attempt
                auditService.logSecurityEvent(userId, credentials.getTenantId(), "MFA_SETUP_VERIFICATION_FAILED", 
                    "MFA setup verification failed - invalid code", 
                    SecurityAuditLog.AuditEventStatus.FAILURE, 
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
                
                throw new BusinessException("Invalid verification code");
            }

            // Enable MFA
            credentials.setMfaEnabled(true);
            userCredentialsRepository.save(credentials);

            // Audit successful setup
            auditService.logSecurityEvent(userId, credentials.getTenantId(), "MFA_SETUP_COMPLETED", 
                "MFA setup completed successfully", 
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);

            logger.info("MFA setup completed for user: {}", userId);
            
            return Map.of(
                "success", true,
                "message", "MFA has been successfully enabled",
                "method", "TOTP",
                "enabled_at", LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("Error verifying MFA setup", e);
            throw new BusinessException("Failed to verify MFA setup: " + e.getMessage());
        }
    }

    /**
     * Verify MFA code during login
     */
    public LoginResponse verifyMfa(MfaVerificationRequest request, 
                                 jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            // Extract user info from MFA token (simplified)
            UUID userId = extractUserIdFromMfaToken(request.getMfaToken());
            
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            // Check if MFA is enabled
            if (!Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                throw new BusinessException("MFA is not enabled for this user");
            }

            boolean isValid = false;
            String verificationMethod = null;

            // Try TOTP code first
            if (StringUtils.hasText(request.getCode())) {
                isValid = verifyTotpCode(credentials.getMfaSecret(), request.getCode());
                verificationMethod = "TOTP";
            }

            // Try backup code if TOTP failed
            if (!isValid && StringUtils.hasText(request.getBackupCode())) {
                isValid = verifyAndConsumeBackupCode(credentials, request.getBackupCode());
                verificationMethod = "BACKUP_CODE";
            }

            if (!isValid) {
                // Audit failed attempt
                auditService.logSecurityEvent(userId, credentials.getTenantId(), "MFA_VERIFICATION_FAILED", 
                    "MFA verification failed during login", 
                    SecurityAuditLog.AuditEventStatus.FAILURE, 
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
                
                throw new BusinessException("Invalid MFA code");
            }

            // Update last used timestamp
            // This would typically be stored in a separate MFA usage tracking table
            
            // Audit successful verification
            auditService.logSecurityEvent(userId, credentials.getTenantId(), "MFA_VERIFICATION_SUCCESS", 
                "MFA verification successful using " + verificationMethod, 
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);

            // Generate login response (simplified)
            LoginResponse response = new LoginResponse();
            response.setAccessToken("access-token-placeholder"); // Would generate real token
            response.setRefreshToken("refresh-token-placeholder");
            response.setExpiresIn(3600);
            response.setTokenType("Bearer");
            response.setMfaVerified(true);

            logger.info("MFA verification successful for user: {} using {}", userId, verificationMethod);
            return response;

        } catch (Exception e) {
            logger.error("Error verifying MFA", e);
            throw new BusinessException("Failed to verify MFA: " + e.getMessage());
        }
    }

    /**
     * Disable MFA for a user
     */
    public Map<String, Object> disableMfa(String authorization, MfaDisableRequest request, 
                                        jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            UUID userId = extractUserIdFromAuth(authorization);
            
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            // Check if MFA is enabled
            if (!Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                throw new BusinessException("MFA is not enabled for this user");
            }

            // Verify current password
            // This would typically use PasswordEncoder to verify
            // For now, we'll assume it's verified

            // Verify MFA code
            boolean isValid = verifyTotpCode(credentials.getMfaSecret(), request.getMfaCode());
            
            if (!isValid) {
                auditService.logSecurityEvent(userId, credentials.getTenantId(), "MFA_DISABLE_FAILED", 
                    "MFA disable attempt failed - invalid code", 
                    SecurityAuditLog.AuditEventStatus.FAILURE, 
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
                
                throw new BusinessException("Invalid MFA code");
            }

            // Disable MFA
            credentials.setMfaEnabled(false);
            credentials.setMfaSecret(null);
            credentials.setBackupCodes(null);
            credentials.setMfaMethod(null);
            credentials.setTrustedDevices(null);
            userCredentialsRepository.save(credentials);

            // Audit successful disable
            auditService.logSecurityEvent(userId, credentials.getTenantId(), "MFA_DISABLED", 
                "MFA disabled successfully", 
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);

            logger.info("MFA disabled for user: {}", userId);
            
            return Map.of(
                "success", true,
                "message", "MFA has been successfully disabled",
                "disabled_at", LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("Error disabling MFA", e);
            throw new BusinessException("Failed to disable MFA: " + e.getMessage());
        }
    }

    /**
     * Get MFA backup codes
     */
    public MfaBackupCodesResponse getBackupCodes(String authorization) {
        try {
            UUID userId = extractUserIdFromAuth(authorization);
            
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            if (!Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                throw new BusinessException("MFA is not enabled for this user");
            }

            List<String> backupCodes = parseBackupCodes(credentials.getBackupCodes());
            
            MfaBackupCodesResponse response = new MfaBackupCodesResponse();
            response.setBackupCodes(backupCodes);
            response.setRemainingCodes(backupCodes.size());
            response.setGeneratedAt(LocalDateTime.now()); // Would store actual generation time

            return response;

        } catch (Exception e) {
            logger.error("Error getting backup codes", e);
            throw new BusinessException("Failed to get backup codes: " + e.getMessage());
        }
    }

    /**
     * Regenerate MFA backup codes
     */
    public MfaBackupCodesResponse regenerateBackupCodes(String authorization, 
                                                      MfaBackupCodesRegenerateRequest request,
                                                      jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            UUID userId = extractUserIdFromAuth(authorization);
            
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            if (!Boolean.TRUE.equals(credentials.getMfaEnabled())) {
                throw new BusinessException("MFA is not enabled for this user");
            }

            // Verify current password (simplified)
            // In real implementation, would verify password with PasswordEncoder

            // Generate new backup codes
            List<String> newBackupCodes = generateBackupCodes();
            credentials.setBackupCodes(objectMapper.writeValueAsString(newBackupCodes));
            userCredentialsRepository.save(credentials);

            // Audit log
            auditService.logSecurityEvent(userId, credentials.getTenantId(), "MFA_BACKUP_CODES_REGENERATED", 
                "MFA backup codes regenerated", 
                SecurityAuditLog.AuditEventStatus.SUCCESS, 
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);

            MfaBackupCodesResponse response = new MfaBackupCodesResponse();
            response.setBackupCodes(newBackupCodes);
            response.setRemainingCodes(newBackupCodes.size());
            response.setGeneratedAt(LocalDateTime.now());

            logger.info("MFA backup codes regenerated for user: {}", userId);
            return response;

        } catch (Exception e) {
            logger.error("Error regenerating backup codes", e);
            throw new BusinessException("Failed to regenerate backup codes: " + e.getMessage());
        }
    }

    /**
     * Get MFA status for a user
     */
    public MfaStatusResponse getMfaStatus(String authorization) {
        try {
            UUID userId = extractUserIdFromAuth(authorization);
            
            UserCredentials credentials = userCredentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

            MfaStatusResponse response = new MfaStatusResponse();
            response.setMfaEnabled(Boolean.TRUE.equals(credentials.getMfaEnabled()));
            
            if (response.isMfaEnabled()) {
                response.setMethod(credentials.getMfaMethod() != null ? credentials.getMfaMethod().name() : null);
                
                // Count backup codes
                List<String> backupCodes = parseBackupCodes(credentials.getBackupCodes());
                response.setBackupCodesCount(backupCodes.size());
                
                // Count trusted devices
                List<Map<String, Object>> trustedDevices = parseTrustedDevices(credentials.getTrustedDevices());
                response.setTrustedDevicesCount(trustedDevices.size());
                
                // Set available methods and recovery options
                response.setAvailableMethods(List.of("TOTP"));
                response.setRecoveryOptions(List.of("BACKUP_CODES", "TRUSTED_DEVICE"));
            }

            return response;

        } catch (Exception e) {
            logger.error("Error getting MFA status", e);
            throw new BusinessException("Failed to get MFA status: " + e.getMessage());
        }
    }

    // Private helper methods

    private String generateTotpSecret() {
        byte[] secretBytes = new byte[SECRET_LENGTH];
        secureRandom.nextBytes(secretBytes);
        return new Base32().encodeToString(secretBytes);
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            codes.add(generateBackupCode());
        }
        return codes;
    }

    private String generateBackupCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    private String generateQrCodeUrl(String email, String secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
            issuer, email, secret, issuer, TOTP_DIGITS, TOTP_PERIOD);
    }

    private byte[] generateQrCodeImage(String qrCodeUrl) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 200, 200);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }



    private String generateTotpCode(String secret, long timeWindow) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        byte[] secretBytes = new Base32().decode(secret);
        byte[] timeBytes = longToBytes(timeWindow);

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "HmacSHA1");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(timeBytes);

        int offset = hash[hash.length - 1] & 0x0F;
        int truncatedHash = ((hash[offset] & 0x7F) << 24) |
                           ((hash[offset + 1] & 0xFF) << 16) |
                           ((hash[offset + 2] & 0xFF) << 8) |
                           (hash[offset + 3] & 0xFF);

        int code = truncatedHash % (int) Math.pow(10, TOTP_DIGITS);
        return String.format("%0" + TOTP_DIGITS + "d", code);
    }

    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private boolean verifyAndConsumeBackupCode(UserCredentials credentials, String backupCode) {
        try {
            List<String> backupCodes = parseBackupCodes(credentials.getBackupCodes());
            
            if (backupCodes.remove(backupCode)) {
                // Update backup codes in database
                credentials.setBackupCodes(objectMapper.writeValueAsString(backupCodes));
                userCredentialsRepository.save(credentials);
                return true;
            }
            return false;

        } catch (Exception e) {
            logger.error("Error verifying backup code", e);
            return false;
        }
    }

    private List<String> parseBackupCodes(String backupCodesJson) {
        try {
            if (!StringUtils.hasText(backupCodesJson)) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(backupCodesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error parsing backup codes", e);
            return new ArrayList<>();
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

    private UUID extractUserIdFromAuth(String authorization) {
        // Simplified - in real implementation would parse JWT token
        // For now, return a placeholder
        return UUID.randomUUID();
    }

    private UUID extractUserIdFromMfaToken(String mfaToken) {
        // Simplified - in real implementation would parse MFA token
        // For now, return a placeholder
        return UUID.randomUUID();
    }

    /**
     * Public method to verify TOTP code (for use by other services)
     */
    public boolean verifyTotpCode(String secret, String code) {
        try {
            long currentTime = System.currentTimeMillis() / 1000L;
            long timeWindow = currentTime / TOTP_PERIOD;

            // Check current time window and adjacent windows
            for (int i = -TIME_WINDOW; i <= TIME_WINDOW; i++) {
                String expectedCode = generateTotpCode(secret, timeWindow + i);
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            logger.error("Error verifying TOTP code", e);
            return false;
        }
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}