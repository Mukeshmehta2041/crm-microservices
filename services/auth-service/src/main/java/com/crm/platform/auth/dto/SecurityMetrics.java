package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for security metrics and monitoring data
 */
public class SecurityMetrics {

    @JsonProperty("total_login_attempts")
    private long totalLoginAttempts;

    @JsonProperty("successful_logins")
    private long successfulLogins;

    @JsonProperty("failed_logins")
    private long failedLogins;

    @JsonProperty("account_lockouts")
    private long accountLockouts;

    @JsonProperty("brute_force_attempts")
    private long bruteForceAttempts;

    @JsonProperty("suspicious_activities")
    private long suspiciousActivities;

    @JsonProperty("mfa_challenges")
    private long mfaChallenges;

    @JsonProperty("mfa_successes")
    private long mfaSuccesses;

    @JsonProperty("mfa_failures")
    private long mfaFailures;

    @JsonProperty("password_resets")
    private long passwordResets;

    @JsonProperty("token_refreshes")
    private long tokenRefreshes;

    @JsonProperty("oauth2_authorizations")
    private long oauth2Authorizations;

    @JsonProperty("unique_users")
    private long uniqueUsers;

    @JsonProperty("unique_ip_addresses")
    private long uniqueIpAddresses;

    @JsonProperty("active_sessions")
    private long activeSessions;

    @JsonProperty("period_start")
    private LocalDateTime periodStart;

    @JsonProperty("period_end")
    private LocalDateTime periodEnd;

    @JsonProperty("top_failure_reasons")
    private Map<String, Long> topFailureReasons;

    @JsonProperty("geographic_distribution")
    private Map<String, Long> geographicDistribution;

    @JsonProperty("device_type_distribution")
    private Map<String, Long> deviceTypeDistribution;

    @JsonProperty("hourly_activity")
    private Map<Integer, Long> hourlyActivity;

    // Constructors
    public SecurityMetrics() {}

    public SecurityMetrics(LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters and Setters
    public long getTotalLoginAttempts() { return totalLoginAttempts; }
    public void setTotalLoginAttempts(long totalLoginAttempts) { this.totalLoginAttempts = totalLoginAttempts; }

    public long getSuccessfulLogins() { return successfulLogins; }
    public void setSuccessfulLogins(long successfulLogins) { this.successfulLogins = successfulLogins; }

    public long getFailedLogins() { return failedLogins; }
    public void setFailedLogins(long failedLogins) { this.failedLogins = failedLogins; }

    public long getAccountLockouts() { return accountLockouts; }
    public void setAccountLockouts(long accountLockouts) { this.accountLockouts = accountLockouts; }

    public long getBruteForceAttempts() { return bruteForceAttempts; }
    public void setBruteForceAttempts(long bruteForceAttempts) { this.bruteForceAttempts = bruteForceAttempts; }

    public long getSuspiciousActivities() { return suspiciousActivities; }
    public void setSuspiciousActivities(long suspiciousActivities) { this.suspiciousActivities = suspiciousActivities; }

    public long getMfaChallenges() { return mfaChallenges; }
    public void setMfaChallenges(long mfaChallenges) { this.mfaChallenges = mfaChallenges; }

    public long getMfaSuccesses() { return mfaSuccesses; }
    public void setMfaSuccesses(long mfaSuccesses) { this.mfaSuccesses = mfaSuccesses; }

    public long getMfaFailures() { return mfaFailures; }
    public void setMfaFailures(long mfaFailures) { this.mfaFailures = mfaFailures; }

    public long getPasswordResets() { return passwordResets; }
    public void setPasswordResets(long passwordResets) { this.passwordResets = passwordResets; }

    public long getTokenRefreshes() { return tokenRefreshes; }
    public void setTokenRefreshes(long tokenRefreshes) { this.tokenRefreshes = tokenRefreshes; }

    public long getOauth2Authorizations() { return oauth2Authorizations; }
    public void setOauth2Authorizations(long oauth2Authorizations) { this.oauth2Authorizations = oauth2Authorizations; }

    public long getUniqueUsers() { return uniqueUsers; }
    public void setUniqueUsers(long uniqueUsers) { this.uniqueUsers = uniqueUsers; }

    public long getUniqueIpAddresses() { return uniqueIpAddresses; }
    public void setUniqueIpAddresses(long uniqueIpAddresses) { this.uniqueIpAddresses = uniqueIpAddresses; }

    public long getActiveSessions() { return activeSessions; }
    public void setActiveSessions(long activeSessions) { this.activeSessions = activeSessions; }

    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }

    public Map<String, Long> getTopFailureReasons() { return topFailureReasons; }
    public void setTopFailureReasons(Map<String, Long> topFailureReasons) { this.topFailureReasons = topFailureReasons; }

    public Map<String, Long> getGeographicDistribution() { return geographicDistribution; }
    public void setGeographicDistribution(Map<String, Long> geographicDistribution) { this.geographicDistribution = geographicDistribution; }

    public Map<String, Long> getDeviceTypeDistribution() { return deviceTypeDistribution; }
    public void setDeviceTypeDistribution(Map<String, Long> deviceTypeDistribution) { this.deviceTypeDistribution = deviceTypeDistribution; }

    public Map<Integer, Long> getHourlyActivity() { return hourlyActivity; }
    public void setHourlyActivity(Map<Integer, Long> hourlyActivity) { this.hourlyActivity = hourlyActivity; }

    // Calculated properties
    @JsonProperty("success_rate")
    public double getSuccessRate() {
        if (totalLoginAttempts == 0) return 0.0;
        return (double) successfulLogins / totalLoginAttempts * 100;
    }

    @JsonProperty("failure_rate")
    public double getFailureRate() {
        if (totalLoginAttempts == 0) return 0.0;
        return (double) failedLogins / totalLoginAttempts * 100;
    }

    @JsonProperty("mfa_success_rate")
    public double getMfaSuccessRate() {
        if (mfaChallenges == 0) return 0.0;
        return (double) mfaSuccesses / mfaChallenges * 100;
    }
}