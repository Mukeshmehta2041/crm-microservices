package com.crm.platform.auth.service;

import com.crm.platform.auth.dto.DeviceInfo;
import com.crm.platform.auth.dto.LocationInfo;
import com.crm.platform.auth.dto.SuspiciousActivityAlert;
import com.crm.platform.auth.entity.UserSession;
import com.crm.platform.auth.repository.UserSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceLocationServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private SecurityAuditService auditService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private DeviceLocationService deviceLocationService;

    private UUID testUserId;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();

        // Set configuration values
        ReflectionTestUtils.setField(deviceLocationService, "locationApiKey", "test-api-key");
        ReflectionTestUtils.setField(deviceLocationService, "locationServiceUrl", "http://test-api.com/");
        ReflectionTestUtils.setField(deviceLocationService, "enableAdvancedFingerprinting", true);
        ReflectionTestUtils.setField(deviceLocationService, "enableSuspiciousDetection", true);
    }

    @Test
    void testDetectDeviceType_iPhone() {
        // Arrange
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15";

        // Act
        String deviceType = deviceLocationService.detectDeviceType(userAgent);

        // Assert
        assertEquals("iPhone", deviceType);
    }

    @Test
    void testDetectDeviceType_AndroidPhone() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 Mobile Safari/537.36";

        // Act
        String deviceType = deviceLocationService.detectDeviceType(userAgent);

        // Assert
        assertEquals("Android Phone", deviceType);
    }

    @Test
    void testDetectDeviceType_AndroidTablet() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Linux; Android 10; SM-T510) AppleWebKit/537.36 Safari/537.36";

        // Act
        String deviceType = deviceLocationService.detectDeviceType(userAgent);

        // Assert
        assertEquals("Android Tablet", deviceType);
    }

    @Test
    void testDetectDeviceType_Desktop() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124";

        // Act
        String deviceType = deviceLocationService.detectDeviceType(userAgent);

        // Assert
        assertEquals("Desktop", deviceType);
    }

    @Test
    void testDetectDeviceType_Unknown() {
        // Arrange
        String userAgent = null;

        // Act
        String deviceType = deviceLocationService.detectDeviceType(userAgent);

        // Assert
        assertEquals("Unknown", deviceType);
    }

    @Test
    void testExtractBrowserInfo_Chrome() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

        // Act
        DeviceInfo.BrowserInfo browserInfo = deviceLocationService.extractBrowserInfo(userAgent);

        // Assert
        assertEquals("Chrome", browserInfo.getName());
        assertEquals("91.0.4472.124", browserInfo.getVersion());
    }

    @Test
    void testExtractBrowserInfo_Firefox() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0";

        // Act
        DeviceInfo.BrowserInfo browserInfo = deviceLocationService.extractBrowserInfo(userAgent);

        // Assert
        assertEquals("Firefox", browserInfo.getName());
        assertEquals("89.0", browserInfo.getVersion());
    }

    @Test
    void testExtractBrowserInfo_Safari() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15";

        // Act
        DeviceInfo.BrowserInfo browserInfo = deviceLocationService.extractBrowserInfo(userAgent);

        // Assert
        assertEquals("Safari", browserInfo.getName());
        assertEquals("14.1.1", browserInfo.getVersion());
    }

    @Test
    void testExtractOSInfo_Windows() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

        // Act
        DeviceInfo.OSInfo osInfo = deviceLocationService.extractOSInfo(userAgent);

        // Assert
        assertEquals("Windows 11", osInfo.getName()); // Windows NT 10.0 maps to Windows 11
    }

    @Test
    void testExtractOSInfo_macOS() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15";

        // Act
        DeviceInfo.OSInfo osInfo = deviceLocationService.extractOSInfo(userAgent);

        // Assert
        assertEquals("macOS", osInfo.getName());
        assertEquals("10.15.7", osInfo.getVersion());
    }

    @Test
    void testExtractOSInfo_iOS() {
        // Arrange
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15";

        // Act
        DeviceInfo.OSInfo osInfo = deviceLocationService.extractOSInfo(userAgent);

        // Assert
        assertEquals("iOS", osInfo.getName());
        assertEquals("14.6", osInfo.getVersion());
    }

    @Test
    void testExtractOSInfo_Android() {
        // Arrange
        String userAgent = "Mozilla/5.0 (Linux; Android 11; SM-G975F) AppleWebKit/537.36";

        // Act
        DeviceInfo.OSInfo osInfo = deviceLocationService.extractOSInfo(userAgent);

        // Assert
        assertEquals("Android", osInfo.getName());
        assertEquals("11", osInfo.getVersion());
    }

    @Test
    void testGenerateAdvancedDeviceFingerprint() {
        // Arrange
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Test Browser");
        when(httpServletRequest.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml");
        when(httpServletRequest.getHeader("Accept-Language")).thenReturn("en-US,en;q=0.9");
        when(httpServletRequest.getHeader("Accept-Encoding")).thenReturn("gzip, deflate, br");

        // Act
        String fingerprint = deviceLocationService.generateAdvancedDeviceFingerprint(httpServletRequest);

        // Assert
        assertNotNull(fingerprint);
        assertNotEquals("unknown", fingerprint);
        assertTrue(fingerprint.length() > 0);
    }

    @Test
    void testCreateDeviceInfo() {
        // Arrange
        when(httpServletRequest.getHeader("User-Agent"))
            .thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        when(httpServletRequest.getHeader("Accept")).thenReturn("text/html");
        when(httpServletRequest.getHeader("Accept-Language")).thenReturn("en-US");
        when(httpServletRequest.getHeader("Accept-Encoding")).thenReturn("gzip");

        // Act
        DeviceInfo deviceInfo = deviceLocationService.createDeviceInfo(httpServletRequest);

        // Assert
        assertNotNull(deviceInfo);
        assertEquals("iPhone", deviceInfo.getDeviceType());
        assertEquals("Safari", deviceInfo.getBrowserInfo().getName());
        assertEquals("iOS", deviceInfo.getOsInfo().getName());
        assertEquals("203.0.113.1", deviceInfo.getIpAddress());
        assertNotNull(deviceInfo.getFingerprint());
        assertNotNull(deviceInfo.getLocationInfo());
        assertNotNull(deviceInfo.getTimestamp());
    }

    @Test
    void testDetectSuspiciousActivity_MultipleLocations() {
        // Arrange
        DeviceInfo currentDevice = new DeviceInfo();
        currentDevice.setLocationInfo(new LocationInfo("US", "California", "San Francisco", "PST", 37.7749, -122.4194));

        UserSession session1 = createTestSession("Location1");
        UserSession session2 = createTestSession("Location2");
        UserSession session3 = createTestSession("Location3");
        UserSession session4 = createTestSession("Location4");

        when(sessionRepository.findByUserIdAndStatus(testUserId, UserSession.SessionStatus.ACTIVE))
            .thenReturn(Arrays.asList(session1, session2, session3, session4));

        // Act
        List<SuspiciousActivityAlert> alerts = deviceLocationService.detectSuspiciousActivity(
            testUserId, testTenantId, currentDevice);

        // Assert
        assertNotNull(alerts);
        assertTrue(alerts.stream().anyMatch(alert -> 
            alert.getType() == SuspiciousActivityAlert.AlertType.MULTIPLE_LOCATIONS));
    }

    @Test
    void testDetectSuspiciousActivity_MultipleDevices() {
        // Arrange
        DeviceInfo currentDevice = new DeviceInfo();
        currentDevice.setFingerprint("new-device-fingerprint");

        UserSession session1 = createTestSession("Location1");
        session1.setDeviceFingerprint("device1");
        UserSession session2 = createTestSession("Location1");
        session2.setDeviceFingerprint("device2");
        UserSession session3 = createTestSession("Location1");
        session3.setDeviceFingerprint("device3");
        UserSession session4 = createTestSession("Location1");
        session4.setDeviceFingerprint("device4");
        UserSession session5 = createTestSession("Location1");
        session5.setDeviceFingerprint("device5");
        UserSession session6 = createTestSession("Location1");
        session6.setDeviceFingerprint("device6");

        when(sessionRepository.findByUserIdAndStatus(testUserId, UserSession.SessionStatus.ACTIVE))
            .thenReturn(Arrays.asList(session1, session2, session3, session4, session5, session6));

        // Act
        List<SuspiciousActivityAlert> alerts = deviceLocationService.detectSuspiciousActivity(
            testUserId, testTenantId, currentDevice);

        // Assert
        assertNotNull(alerts);
        assertTrue(alerts.stream().anyMatch(alert -> 
            alert.getType() == SuspiciousActivityAlert.AlertType.MULTIPLE_DEVICES));
    }

    @Test
    void testDetectSuspiciousActivity_NewDevice() {
        // Arrange
        DeviceInfo currentDevice = new DeviceInfo();
        currentDevice.setFingerprint("new-device-fingerprint");
        currentDevice.setDeviceType("iPhone");

        UserSession existingSession = createTestSession("Location1");
        existingSession.setDeviceFingerprint("existing-device-fingerprint");

        when(sessionRepository.findByUserIdAndStatus(testUserId, UserSession.SessionStatus.ACTIVE))
            .thenReturn(Arrays.asList(existingSession));

        // Act
        List<SuspiciousActivityAlert> alerts = deviceLocationService.detectSuspiciousActivity(
            testUserId, testTenantId, currentDevice);

        // Assert
        assertNotNull(alerts);
        assertTrue(alerts.stream().anyMatch(alert -> 
            alert.getType() == SuspiciousActivityAlert.AlertType.NEW_DEVICE));
    }

    @Test
    void testDetectSuspiciousActivity_NoAlerts() {
        // Arrange
        DeviceInfo currentDevice = new DeviceInfo();
        currentDevice.setFingerprint("existing-device-fingerprint");
        currentDevice.setLocationInfo(new LocationInfo("US", "California", "San Francisco", "PST", 37.7749, -122.4194));

        UserSession existingSession = createTestSession("US, California, San Francisco");
        existingSession.setDeviceFingerprint("existing-device-fingerprint");

        when(sessionRepository.findByUserIdAndStatus(testUserId, UserSession.SessionStatus.ACTIVE))
            .thenReturn(Arrays.asList(existingSession));

        // Act
        List<SuspiciousActivityAlert> alerts = deviceLocationService.detectSuspiciousActivity(
            testUserId, testTenantId, currentDevice);

        // Assert
        assertNotNull(alerts);
        // Should have no high-risk alerts, but might have low-risk ones
        assertTrue(alerts.stream().noneMatch(alert -> 
            alert.getRiskLevel() == SuspiciousActivityAlert.RiskLevel.HIGH));
    }

    @Test
    void testGetDeviceStatistics() {
        // Arrange
        UserSession session1 = createTestSession("Location1");
        session1.setDeviceType("iPhone");
        session1.setBrowser("Safari");
        session1.setIpAddress("192.168.1.1");

        UserSession session2 = createTestSession("Location2");
        session2.setDeviceType("Desktop");
        session2.setBrowser("Chrome");
        session2.setIpAddress("192.168.1.2");

        UserSession session3 = createTestSession("Location1");
        session3.setDeviceType("iPhone");
        session3.setBrowser("Safari");
        session3.setIpAddress("192.168.1.1");

        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
            .thenReturn(Arrays.asList(session1, session2, session3));

        // Act
        Map<String, Object> statistics = deviceLocationService.getDeviceStatistics(testUserId, testTenantId);

        // Assert
        assertNotNull(statistics);
        assertEquals(3, statistics.get("total_sessions"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> deviceTypes = (Map<String, Long>) statistics.get("device_types");
        assertEquals(2L, deviceTypes.get("iPhone"));
        assertEquals(1L, deviceTypes.get("Desktop"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> browsers = (Map<String, Long>) statistics.get("browsers");
        assertEquals(2L, browsers.get("Safari"));
        assertEquals(1L, browsers.get("Chrome"));
        
        assertEquals(2, statistics.get("unique_ips"));
        assertEquals(30, statistics.get("period_days"));
    }

    @Test
    void testSendSecurityNotifications() {
        // Arrange
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setIpAddress("203.0.113.1");
        deviceInfo.setUserAgent("Test User Agent");

        SuspiciousActivityAlert alert = new SuspiciousActivityAlert(
            SuspiciousActivityAlert.AlertType.MULTIPLE_LOCATIONS,
            "Multiple locations detected",
            SuspiciousActivityAlert.RiskLevel.HIGH,
            Map.of("count", 5)
        );

        List<SuspiciousActivityAlert> alerts = Arrays.asList(alert);

        // Act
        deviceLocationService.sendSecurityNotifications(testUserId, testTenantId, alerts, deviceInfo);

        // Assert
        verify(auditService).logSecurityEvent(
            eq(testUserId), 
            eq(testTenantId), 
            eq("SUSPICIOUS_ACTIVITY_MULTIPLE_LOCATIONS"),
            eq("Multiple locations detected"),
            any(),
            eq("203.0.113.1"),
            eq("Test User Agent"),
            isNull(),
            anyString()
        );
    }

    // Helper methods
    private UserSession createTestSession(String location) {
        UserSession session = new UserSession();
        session.setId(UUID.randomUUID());
        session.setUserId(testUserId);
        session.setTenantId(testTenantId);
        session.setTokenId("session-" + UUID.randomUUID());
        session.setStatus(UserSession.SessionStatus.ACTIVE);
        session.setLocation(location);
        session.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        session.setDeviceFingerprint("device-fingerprint-" + UUID.randomUUID());
        return session;
    }
}