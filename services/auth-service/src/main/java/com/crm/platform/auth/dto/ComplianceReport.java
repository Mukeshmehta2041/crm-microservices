package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for compliance reporting and audit trails
 */
public class ComplianceReport {

    @JsonProperty("report_id")
    private String reportId;

    @JsonProperty("report_type")
    private ReportType reportType;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;

    @JsonProperty("period_start")
    private LocalDateTime periodStart;

    @JsonProperty("period_end")
    private LocalDateTime periodEnd;

    @JsonProperty("total_events")
    private long totalEvents;

    @JsonProperty("security_events")
    private SecurityEventsSummary securityEvents;

    @JsonProperty("access_patterns")
    private AccessPatternsSummary accessPatterns;

    @JsonProperty("policy_violations")
    private List<PolicyViolation> policyViolations;

    @JsonProperty("risk_indicators")
    private List<RiskIndicator> riskIndicators;

    @JsonProperty("recommendations")
    private List<String> recommendations;

    @JsonProperty("compliance_score")
    private double complianceScore;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public ComplianceReport() {
        this.generatedAt = LocalDateTime.now();
    }

    public ComplianceReport(ReportType reportType, String tenantId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        this();
        this.reportType = reportType;
        this.tenantId = tenantId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Nested classes
    public static class SecurityEventsSummary {
        @JsonProperty("authentication_events")
        private long authenticationEvents;

        @JsonProperty("authorization_events")
        private long authorizationEvents;

        @JsonProperty("failed_attempts")
        private long failedAttempts;

        @JsonProperty("account_lockouts")
        private long accountLockouts;

        @JsonProperty("suspicious_activities")
        private long suspiciousActivities;

        @JsonProperty("privilege_escalations")
        private long privilegeEscalations;

        // Getters and Setters
        public long getAuthenticationEvents() { return authenticationEvents; }
        public void setAuthenticationEvents(long authenticationEvents) { this.authenticationEvents = authenticationEvents; }

        public long getAuthorizationEvents() { return authorizationEvents; }
        public void setAuthorizationEvents(long authorizationEvents) { this.authorizationEvents = authorizationEvents; }

        public long getFailedAttempts() { return failedAttempts; }
        public void setFailedAttempts(long failedAttempts) { this.failedAttempts = failedAttempts; }

        public long getAccountLockouts() { return accountLockouts; }
        public void setAccountLockouts(long accountLockouts) { this.accountLockouts = accountLockouts; }

        public long getSuspiciousActivities() { return suspiciousActivities; }
        public void setSuspiciousActivities(long suspiciousActivities) { this.suspiciousActivities = suspiciousActivities; }

        public long getPrivilegeEscalations() { return privilegeEscalations; }
        public void setPrivilegeEscalations(long privilegeEscalations) { this.privilegeEscalations = privilegeEscalations; }
    }

    public static class AccessPatternsSummary {
        @JsonProperty("unique_users")
        private long uniqueUsers;

        @JsonProperty("peak_concurrent_sessions")
        private long peakConcurrentSessions;

        @JsonProperty("average_session_duration")
        private double averageSessionDuration;

        @JsonProperty("geographic_locations")
        private Map<String, Long> geographicLocations;

        @JsonProperty("device_types")
        private Map<String, Long> deviceTypes;

        @JsonProperty("access_times")
        private Map<String, Long> accessTimes;

        // Getters and Setters
        public long getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(long uniqueUsers) { this.uniqueUsers = uniqueUsers; }

        public long getPeakConcurrentSessions() { return peakConcurrentSessions; }
        public void setPeakConcurrentSessions(long peakConcurrentSessions) { this.peakConcurrentSessions = peakConcurrentSessions; }

        public double getAverageSessionDuration() { return averageSessionDuration; }
        public void setAverageSessionDuration(double averageSessionDuration) { this.averageSessionDuration = averageSessionDuration; }

        public Map<String, Long> getGeographicLocations() { return geographicLocations; }
        public void setGeographicLocations(Map<String, Long> geographicLocations) { this.geographicLocations = geographicLocations; }

        public Map<String, Long> getDeviceTypes() { return deviceTypes; }
        public void setDeviceTypes(Map<String, Long> deviceTypes) { this.deviceTypes = deviceTypes; }

        public Map<String, Long> getAccessTimes() { return accessTimes; }
        public void setAccessTimes(Map<String, Long> accessTimes) { this.accessTimes = accessTimes; }
    }

    public static class PolicyViolation {
        @JsonProperty("violation_type")
        private String violationType;

        @JsonProperty("description")
        private String description;

        @JsonProperty("severity")
        private Severity severity;

        @JsonProperty("count")
        private long count;

        @JsonProperty("first_occurrence")
        private LocalDateTime firstOccurrence;

        @JsonProperty("last_occurrence")
        private LocalDateTime lastOccurrence;

        @JsonProperty("affected_users")
        private List<String> affectedUsers;

        // Constructors
        public PolicyViolation() {}

        public PolicyViolation(String violationType, String description, Severity severity, long count) {
            this.violationType = violationType;
            this.description = description;
            this.severity = severity;
            this.count = count;
        }

        // Getters and Setters
        public String getViolationType() { return violationType; }
        public void setViolationType(String violationType) { this.violationType = violationType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Severity getSeverity() { return severity; }
        public void setSeverity(Severity severity) { this.severity = severity; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }

        public LocalDateTime getFirstOccurrence() { return firstOccurrence; }
        public void setFirstOccurrence(LocalDateTime firstOccurrence) { this.firstOccurrence = firstOccurrence; }

        public LocalDateTime getLastOccurrence() { return lastOccurrence; }
        public void setLastOccurrence(LocalDateTime lastOccurrence) { this.lastOccurrence = lastOccurrence; }

        public List<String> getAffectedUsers() { return affectedUsers; }
        public void setAffectedUsers(List<String> affectedUsers) { this.affectedUsers = affectedUsers; }
    }

    public static class RiskIndicator {
        @JsonProperty("indicator_type")
        private String indicatorType;

        @JsonProperty("risk_level")
        private RiskLevel riskLevel;

        @JsonProperty("description")
        private String description;

        @JsonProperty("value")
        private double value;

        @JsonProperty("threshold")
        private double threshold;

        @JsonProperty("trend")
        private Trend trend;

        // Constructors
        public RiskIndicator() {}

        public RiskIndicator(String indicatorType, RiskLevel riskLevel, String description, double value, double threshold) {
            this.indicatorType = indicatorType;
            this.riskLevel = riskLevel;
            this.description = description;
            this.value = value;
            this.threshold = threshold;
        }

        // Getters and Setters
        public String getIndicatorType() { return indicatorType; }
        public void setIndicatorType(String indicatorType) { this.indicatorType = indicatorType; }

        public RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }

        public Trend getTrend() { return trend; }
        public void setTrend(Trend trend) { this.trend = trend; }
    }

    // Enums
    public enum ReportType {
        SECURITY_AUDIT,
        COMPLIANCE_SUMMARY,
        ACCESS_REVIEW,
        INCIDENT_REPORT,
        RISK_ASSESSMENT
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Trend {
        INCREASING, DECREASING, STABLE, UNKNOWN
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

    public SecurityEventsSummary getSecurityEvents() { return securityEvents; }
    public void setSecurityEvents(SecurityEventsSummary securityEvents) { this.securityEvents = securityEvents; }

    public AccessPatternsSummary getAccessPatterns() { return accessPatterns; }
    public void setAccessPatterns(AccessPatternsSummary accessPatterns) { this.accessPatterns = accessPatterns; }

    public List<PolicyViolation> getPolicyViolations() { return policyViolations; }
    public void setPolicyViolations(List<PolicyViolation> policyViolations) { this.policyViolations = policyViolations; }

    public List<RiskIndicator> getRiskIndicators() { return riskIndicators; }
    public void setRiskIndicators(List<RiskIndicator> riskIndicators) { this.riskIndicators = riskIndicators; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public double getComplianceScore() { return complianceScore; }
    public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}