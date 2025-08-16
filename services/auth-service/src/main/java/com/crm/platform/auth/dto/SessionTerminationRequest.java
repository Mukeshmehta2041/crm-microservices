package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class SessionTerminationRequest {

    @JsonProperty("session_ids")
    private List<String> sessionIds;

    @JsonProperty("termination_reason")
    private String terminationReason;

    @JsonProperty("notify_user")
    private boolean notifyUser = true;

    @JsonProperty("force_termination")
    private boolean forceTermination = false;

    // Criteria-based termination fields
    @JsonProperty("device_type")
    private String deviceType;

    private String location;

    @JsonProperty("older_than_hours")
    private Integer olderThanHours;

    @JsonProperty("inactive_for_hours")
    private Integer inactiveForHours;

    // Constructors
    public SessionTerminationRequest() {}

    public SessionTerminationRequest(List<String> sessionIds, String terminationReason) {
        this.sessionIds = sessionIds;
        this.terminationReason = terminationReason;
    }

    // Getters and Setters
    public List<String> getSessionIds() { return sessionIds; }
    public void setSessionIds(List<String> sessionIds) { this.sessionIds = sessionIds; }

    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }

    public boolean isNotifyUser() { return notifyUser; }
    public void setNotifyUser(boolean notifyUser) { this.notifyUser = notifyUser; }

    public boolean isForceTermination() { return forceTermination; }
    public void setForceTermination(boolean forceTermination) { this.forceTermination = forceTermination; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getOlderThanHours() { return olderThanHours; }
    public void setOlderThanHours(Integer olderThanHours) { this.olderThanHours = olderThanHours; }

    public Integer getInactiveForHours() { return inactiveForHours; }
    public void setInactiveForHours(Integer inactiveForHours) { this.inactiveForHours = inactiveForHours; }

    // Helper methods
    public boolean hasCriteria() {
        return deviceType != null || location != null || 
               olderThanHours != null || inactiveForHours != null;
    }

    public boolean hasSpecificSessions() {
        return sessionIds != null && !sessionIds.isEmpty();
    }
}