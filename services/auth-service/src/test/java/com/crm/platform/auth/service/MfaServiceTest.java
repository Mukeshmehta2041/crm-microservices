package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.*;
import com.crm.platform.auth.entity.UserCredentials;
import com.crm.platform.auth.repository.UserCredentialsRepository;
import com.crm.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private UserCredentialsRepository userCredentialsRepository;

    @Mock
    private SecurityAuditService auditService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private MfaService mfaService;

    private UUID testUserId;
    private UUID testTenantId;
    private UserCredentials testCredentials;
    private String testAuthorization;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        testAuthorization = "Bearer test-token";

        testCredentials = new UserCredentials();
        testCredentials.setUserId(testUserId);
        testCredentials.setTenantId(testTenantId);
        testCredentials.setEmail("test@example.com");
        testCredentials.setMfaEnabled(false);

        // Set configuration values
        ReflectionTestUtils.setField(mfaService, "appName", "Test App");
        ReflectionTestUtils.setField(mfaService, "issuer", "test-issuer");

        // Mock HTTP request
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Test User Agent");
    }

    @Test
    void testSetupMfa_Success() {
        // Arrange
        MfaSetupRequest request = new MfaSetupRequest();
        request.setMethod("TOTP");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));
        when(userCredentialsRepository.save(any(UserCredentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MfaSetupResponse response = mfaService.setupMfa(testAuthorization, request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertEquals("TOTP", response.getMethod());
        assertNotNull(response.getSecret());
        assertNotNull(response.getQrCode());
        assertNotNull(response.getQrCodeUrl());
        assertNotNull(response.getBackupCodes());
        assertEquals(10, response.getBackupCodes().size()); // Default backup codes count
        assertTrue(response.getQrCodeUrl().contains("otpauth://totp/"));
        assertTrue(response.getQrCodeUrl().contains("test@example.com"));

        verify(userCredentialsRepository).save(any(UserCredentials.class));
        verify(auditService).logSecurityEvent(any(UUID.class), any(UUID.class), eq("MFA_SETUP_INITIATED"), 
            anyString(), any(), anyString(), anyString(), isNull());
    }

    @Test
    void testSetupMfa_AlreadyEnabled() {
        // Arrange
        testCredentials.setMfaEnabled(true);
        MfaSetupRequest request = new MfaSetupRequest();
        request.setMethod("TOTP");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> mfaService.setupMfa(testAuthorization, request, httpServletRequest));
        
        assertTrue(exception.getMessage().contains("MFA is already enabled"));
    }

    @Test
    void testSetupMfa_UserNotFound() {
        // Arrange
        MfaSetupRequest request = new MfaSetupRequest();
        request.setMethod("TOTP");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> mfaService.setupMfa(testAuthorization, request, httpServletRequest));
        
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void testVerifyMfaSetup_Success() {
        // Arrange
        String testSecret = "JBSWY3DPEHPK3PXP"; // Base32 encoded test secret
        testCredentials.setMfaSecret(testSecret);
        testCredentials.setMfaMethod(UserCredentials.MfaMethod.TOTP);
        testCredentials.setBackupCodes("[\"12345678\",\"87654321\"]");

        MfaVerifySetupRequest request = new MfaVerifySetupRequest();
        // Generate a valid TOTP code for the test secret
        String validCode = generateValidTotpCode(testSecret);
        request.setCode(validCode);
        request.setMethod("TOTP");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));
        when(userCredentialsRepository.save(any(UserCredentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var response = mfaService.verifyMfaSetup(testAuthorization, request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));
        assertEquals("TOTP", response.get("method"));
        assertNotNull(response.get("enabled_at"));

        verify(userCredentialsRepository).save(any(UserCredentials.class));
        verify(auditService).logSecurityEvent(any(UUID.class), any(UUID.class), eq("MFA_SETUP_COMPLETED"), 
            anyString(), any(), anyString(), anyString(), isNull());
    }

    @Test
    void testVerifyMfaSetup_InvalidCode() {
        // Arrange
        String testSecret = "JBSWY3DPEHPK3PXP";
        testCredentials.setMfaSecret(testSecret);
        testCredentials.setMfaMethod(UserCredentials.MfaMethod.TOTP);

        MfaVerifySetupRequest request = new MfaVerifySetupRequest();
        request.setCode("000000"); // Invalid code
        request.setMethod("TOTP");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> mfaService.verifyMfaSetup(testAuthorization, request, httpServletRequest));
        
        assertTrue(exception.getMessage().contains("Invalid verification code"));
        verify(auditService).logSecurityEvent(any(UUID.class), any(UUID.class), eq("MFA_SETUP_VERIFICATION_FAILED"), 
            anyString(), any(), anyString(), anyString(), isNull());
    }

    @Test
    void testVerifyMfa_Success() {
        // Arrange
        String testSecret = "JBSWY3DPEHPK3PXP";
        testCredentials.setMfaEnabled(true);
        testCredentials.setMfaSecret(testSecret);
        testCredentials.setMfaMethod(UserCredentials.MfaMethod.TOTP);

        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setMfaToken("test-mfa-token");
        String validCode = generateValidTotpCode(testSecret);
        request.setCode(validCode);

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));

        // Act
        LoginResponse response = mfaService.verifyMfa(request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertTrue(response.isMfaVerified());
        assertEquals("Bearer", response.getTokenType());

        verify(auditService).logSecurityEvent(any(UUID.class), any(UUID.class), eq("MFA_VERIFICATION_SUCCESS"), 
            anyString(), any(), anyString(), anyString(), isNull());
    }

    @Test
    void testVerifyMfa_WithBackupCode() {
        // Arrange
        testCredentials.setMfaEnabled(true);
        testCredentials.setMfaSecret("JBSWY3DPEHPK3PXP");
        testCredentials.setBackupCodes("[\"12345678\",\"87654321\"]");

        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setMfaToken("test-mfa-token");
        request.setBackupCode("12345678");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));
        when(userCredentialsRepository.save(any(UserCredentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LoginResponse response = mfaService.verifyMfa(request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.isMfaVerified());

        verify(userCredentialsRepository).save(any(UserCredentials.class));
        verify(auditService).logSecurityEvent(any(UUID.class), any(UUID.class), eq("MFA_VERIFICATION_SUCCESS"), 
            contains("BACKUP_CODE"), any(), anyString(), anyString(), isNull());
    }

    @Test
    void testVerifyMfa_InvalidCode() {
        // Arrange
        testCredentials.setMfaEnabled(true);
        testCredentials.setMfaSecret("JBSWY3DPEHPK3PXP");

        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setMfaToken("test-mfa-token");
        request.setCode("000000"); // Invalid code

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> mfaService.verifyMfa(request, httpServletRequest));
        
        assertTrue(exception.getMessage().contains("Invalid MFA code"));
        verify(auditService).logSecurityEvent(any(UUID.class), any(UUID.class), eq("MFA_VERIFICATION_FAILED"), 
            anyString(), any(), anyString(), anyString(), isNull());
    }

    @Test
    void testDisableMfa_Success() {
        // Arrange
        String testSecret = "JBSWY3DPEHPK3PXP";
        testCredentials.setMfaEnabled(true);
        testCredentials.setMfaSecret(testSecret);
        testCredentials.setBackupCodes("[\"12345678\"]");
        testCredentials.setMfaMethod(UserCredentials.MfaMethod.TOTP);

        MfaDisableRequest request = new MfaDisableRequest();
        request.setCurrentPassword("password123");
        String validCode = generateValidTotpCode(testSecret);
        request.setMfaCode(validCode);

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));
        when(userCredentialsRepository.save(any(UserCredentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var response = mfaService.disableMfa(testAuthorization, request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));
        assertNotNull(response.get("disabled_at"));

        verify(userCredentialsRepository).save(any(UserCredentials.class));
        verify(auditService).logSecurityEvent(any(UUID.class), any(UUID.class), eq("MFA_DISABLED"), 
            anyString(), any(), anyString(), anyString(), isNull());
    }

    @Test
    void testGetBackupCodes_Success() {
        // Arrange
        testCredentials.setMfaEnabled(true);
        testCredentials.setBackupCodes("[\"12345678\",\"87654321\",\"11111111\"]");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));

        // Act
        MfaBackupCodesResponse response = mfaService.getBackupCodes(testAuthorization);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getBackupCodes());
        assertEquals(3, response.getBackupCodes().size());
        assertEquals(3, response.getRemainingCodes());
        assertTrue(response.getBackupCodes().contains("12345678"));
        assertTrue(response.getBackupCodes().contains("87654321"));
        assertTrue(response.getBackupCodes().contains("11111111"));
    }

    @Test
    void testGetBackupCodes_MfaNotEnabled() {
        // Arrange
        testCredentials.setMfaEnabled(false);

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> mfaService.getBackupCodes(testAuthorization));
        
        assertTrue(exception.getMessage().contains("MFA is not enabled"));
    }

    @Test
    void testRegenerateBackupCodes_Success() {
        // Arrange
        testCredentials.setMfaEnabled(true);
        testCredentials.setBackupCodes("[\"12345678\"]");

        MfaBackupCodesRegenerateRequest request = new MfaBackupCodesRegenerateRequest();
        request.setCurrentPassword("password123");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));
        when(userCredentialsRepository.save(any(UserCredentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MfaBackupCodesResponse response = mfaService.regenerateBackupCodes(testAuthorization, request, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getBackupCodes());
        assertEquals(10, response.getBackupCodes().size()); // Default count
        assertEquals(10, response.getRemainingCodes());
        assertNotNull(response.getGeneratedAt());

        verify(userCredentialsRepository).save(any(UserCredentials.class));
        verify(auditService).logSecurityEvent(any(UUID.class), any(UUID.class), eq("MFA_BACKUP_CODES_REGENERATED"), 
            anyString(), any(), anyString(), anyString(), isNull());
    }

    @Test
    void testGetMfaStatus_Enabled() {
        // Arrange
        testCredentials.setMfaEnabled(true);
        testCredentials.setMfaMethod(UserCredentials.MfaMethod.TOTP);
        testCredentials.setBackupCodes("[\"12345678\",\"87654321\"]");
        testCredentials.setTrustedDevices("[{\"id\":\"device1\",\"name\":\"Test Device\"}]");

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));

        // Act
        MfaStatusResponse response = mfaService.getMfaStatus(testAuthorization);

        // Assert
        assertNotNull(response);
        assertTrue(response.isMfaEnabled());
        assertEquals("TOTP", response.getMethod());
        assertEquals(2, response.getBackupCodesCount());
        assertEquals(1, response.getTrustedDevicesCount());
        assertNotNull(response.getAvailableMethods());
        assertNotNull(response.getRecoveryOptions());
        assertTrue(response.getAvailableMethods().contains("TOTP"));
        assertTrue(response.getRecoveryOptions().contains("BACKUP_CODES"));
        assertTrue(response.getRecoveryOptions().contains("TRUSTED_DEVICE"));
    }

    @Test
    void testGetMfaStatus_Disabled() {
        // Arrange
        testCredentials.setMfaEnabled(false);

        when(userCredentialsRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(testCredentials));

        // Act
        MfaStatusResponse response = mfaService.getMfaStatus(testAuthorization);

        // Assert
        assertNotNull(response);
        assertFalse(response.isMfaEnabled());
        assertNull(response.getMethod());
        assertNull(response.getBackupCodesCount());
        assertNull(response.getTrustedDevicesCount());
    }

    // Helper method to generate a valid TOTP code for testing
    private String generateValidTotpCode(String secret) {
        try {
            // This is a simplified version - in real tests you might want to use the actual TOTP algorithm
            // For now, we'll return a predictable code that the service can validate
            long timeWindow = System.currentTimeMillis() / 1000L / 30;
            
            byte[] secretBytes = new Base32().decode(secret);
            byte[] timeBytes = new byte[8];
            for (int i = 7; i >= 0; i--) {
                timeBytes[i] = (byte) (timeWindow & 0xFF);
                timeWindow >>= 8;
            }

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(secretBytes, "HmacSHA1");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(timeBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int truncatedHash = ((hash[offset] & 0x7F) << 24) |
                               ((hash[offset + 1] & 0xFF) << 16) |
                               ((hash[offset + 2] & 0xFF) << 8) |
                               (hash[offset + 3] & 0xFF);

            int code = truncatedHash % 1000000;
            return String.format("%06d", code);

        } catch (Exception e) {
            // Fallback for testing
            return "123456";
        }
    }
}