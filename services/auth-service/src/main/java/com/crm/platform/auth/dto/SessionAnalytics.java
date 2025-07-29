package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public class SessionAnalytics {

    @JsonProperty("total_sessions")
    private long totalSessions;

    @JsonProperty("active_sessions")
    private long activeSessions;

    @JsonProperty("expired_sessions")
    private long expiredSessions;

    @JsonProperty("revoked_sessions")
    private long revokedSessions;

    @JsonProperty("device_breakdown")
    private Map<String, Long> deviceBreakdown;

    @JsonProperty("browser_breakdown")
    private Map<String, Long> browserBreakdown;

    @JsonProperty("location_breakdown")
    private Map<String, Long> locationBreakdown;

    @JsonProperty("os_breakdown")
    private Map<String, Long> osBreakdown;

    @JsonProperty("unique_ips")
    private long uniqueIps;

    @JsonProperty("unique_devices")
    private long uniqueDevices;

    @JsonProperty("average_session_duration_minutes")
    private double averageSessionDurationMinutes;

    @JsonProperty("peak_concurrent_sessions")
    private long peakConcurrentSessions;

    @JsonProperty("analysis_period_days")
    private int analysisPeriodDays;

    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;

    @JsonProperty("security_alerts_count")
    private long securityAlertsCount;

    @JsonProperty("suspicious_activity_detected")
    private boolean suspiciousActivityDetected;

    // Constructors
    public SessionAnalytics() {
        this.generatedAt = LocalDateTime.now();
    }

    public SessionAnalytics(long totalSessions, long activeSessions, 
                           Map<String, Long> deviceBreakdown, 
                           Map<String, Long> browserBreakdown,
                           Map<String, Long> locationBreakdown,
                           int analysisPeriodDays) {
        this.totalSessions = totalSessions;
        this.activeSessions = activeSessions;
        this.deviceBreakdown = deviceBreakdown;
        this.browserBreakdown = browserBreakdown;
        this.locationBreakdown = locationBreakdown;
        this.analysisPeriodDays = analysisPeriodDays;
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }

    public long getActiveSessions() { return activeSessions; }
    public void setActiveSessions(long activeSessions) { this.activeSessions = activeSessions; }

    public long getExpiredSessions() { return expiredSessions; }
    public void setExpiredSessions(long expiredSessions) { this.expiredSessions = expiredSessions; }

    public long getRevokedSessions() { return revokedSessions; }
    public void setRevokedSessions(long revokedSessions) { this.revokedSessions = revokedSessions; }

    public Map<String, Long> getDeviceBreakdown() { return deviceBreakdown; }
    public void setDeviceBreakdown(Map<String, Long> deviceBreakdown) { this.deviceBreakdown = deviceBreakdown; }

    public Map<String, Long> getBrowserBreakdown() { return browserBreakdown; }
    public void setBrowserBreakdown(Map<String, Long> browserBreakdown) { this.browserBreakdown = browserBreakdown; }

    public Map<String, Long> getLocationBreakdown() { return locationBreakdown; }
    public void setLocationBreakdown(Map<String, Long> locationBreakdown) { this.locationBreakdown = locationBreakdown; }

    public Map<String, Long> getOsBreakdown() { return osBreakdown; }
    public void setOsBreakdown(Map<String, Long> osBreakdown) { this.osBreakdown = osBreakdown; }

    public long getUniqueIps() { return uniqueIps; }
    public void setUniqueIps(long uniqueIps) { this.uniqueIps = uniqueIps; }

    public long getUniqueDevices() { return uniqueDevices; }
    public void setUniqueDevices(long uniqueDevices) { this.uniqueDevices = uniqueDevices; }

    public double getAverageSessionDurationMinutes() { return averageSessionDurationMinutes; }
    public void setAverageSessionDurationMinutes(double averageSessionDurationMinutes) { 
        this.averageSessionDurationMinutes = averageSessionDurationMinutes; 
    }

    public long getPeakConcurrentSessions() { return peakConcurrentSessions; }
    public void setPeakConcurrentSessions(long peakConcurrentSessions) { 
        this.peakConcurrentSessions = peakConcurrentSessions; 
    }

    public int getAnalysisPeriodDays() { return analysisPeriodDays; }
    public void setAnalysisPeriodDays(int analysisPeriodDays) { this.analysisPeriodDays = analysisPeriodDays; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public long getSecurityAlertsCount() { return securityAlertsCount; }
    public void setSecurityAlertsCount(long securityAlertsCount) { this.securityAlertsCount = securityAlertsCount; }

    public boolean isSuspiciousActivityDetected() { return suspiciousActivityDetected; }
    public void setSuspiciousActivityDetected(boolean suspiciousActivityDetected) { 
        this.suspiciousActivityDetected = suspiciousActivityDetected; 
    }

    // Helper methods
    public double getActiveSessionPercentage() {
        return totalSessions > 0 ? (double) activeSessions / totalSessions * 100 : 0.0;
    }

    public String getMostUsedDevice() {
        return deviceBreakdown != null && !deviceBreakdown.isEmpty() ?
            deviceBreakdown.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown") : "Unknown";
    }

    public String getMostUsedBrowser() {
        return browserBreakdown != null && !browserBreakdown.isEmpty() ?
            browserBreakdown.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown") : "Unknown";
    }

    public String getMostCommonLocation() {
        return locationBreakdown != null && !locationBreakdown.isEmpty() ?
            locationBreakdown.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown") : "Unknown";
    }
}