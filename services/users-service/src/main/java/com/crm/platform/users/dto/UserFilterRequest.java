package com.crm.platform.users.dto;

import com.crm.platform.users.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Request DTO for filtering users
 */
public class UserFilterRequest {
    
    @JsonProperty("status")
    private User.UserStatus status;
    
    @JsonProperty("department")
    private String department;
    
    @JsonProperty("job_title")
    private String jobTitle;
    
    @JsonProperty("manager_id")
    private UUID managerId;
    
    @JsonProperty("team_id")
    private UUID teamId;
    
    @JsonProperty("office_location")
    private String officeLocation;
    
    @JsonProperty("onboarding_completed")
    private Boolean onboardingCompleted;
    
    @JsonProperty("search_query")
    private String searchQuery;

    public UserFilterRequest() {}

    // Getters and Setters
    public User.UserStatus getStatus() { return status; }
    public void setStatus(User.UserStatus status) { this.status = status; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID managerId) { this.managerId = managerId; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }

    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(Boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
}