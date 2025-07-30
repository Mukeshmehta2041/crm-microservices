package com.crm.platform.users.dto;

import com.crm.platform.users.entity.TeamMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for team member operations
 */
public class TeamMemberRequest {
    
    @NotNull(message = "User ID is required")
    @JsonProperty("user_id")
    private UUID userId;
    
    @JsonProperty("team_role")
    private TeamMember.TeamRole teamRole = TeamMember.TeamRole.MEMBER;
    
    @Min(value = 1, message = "Allocation percentage must be at least 1%")
    @Max(value = 100, message = "Allocation percentage cannot exceed 100%")
    @JsonProperty("allocation_percentage")
    private Integer allocationPercentage = 100;
    
    @JsonProperty("is_primary_team")
    private Boolean isPrimaryTeam = false;
    
    @JsonProperty("reporting_manager_id")
    private UUID reportingManagerId;
    
    @JsonProperty("position_title")
    private String positionTitle;
    
    @JsonProperty("responsibilities")
    private Object responsibilities; // Will be converted to JSON array
    
    @JsonProperty("skills_required")
    private Object skillsRequired; // Will be converted to JSON array
    
    @JsonProperty("billable_rate")
    private BigDecimal billableRate;
    
    @JsonProperty("cost_rate")
    private BigDecimal costRate;
    
    @JsonProperty("can_manage_team")
    private Boolean canManageTeam;
    
    @JsonProperty("can_add_members")
    private Boolean canAddMembers;
    
    @JsonProperty("can_remove_members")
    private Boolean canRemoveMembers;
    
    @JsonProperty("can_view_team_analytics")
    private Boolean canViewTeamAnalytics;
    
    @JsonProperty("can_edit_team_settings")
    private Boolean canEditTeamSettings;
    
    @JsonProperty("email_notifications")
    private Boolean emailNotifications = true;
    
    @JsonProperty("slack_notifications")
    private Boolean slackNotifications = true;
    
    @JsonProperty("teams_notifications")
    private Boolean teamsNotifications = true;

    public TeamMemberRequest() {}

    public TeamMemberRequest(UUID userId, TeamMember.TeamRole teamRole, Integer allocationPercentage, Boolean isPrimaryTeam) {
        this.userId = userId;
        this.teamRole = teamRole;
        this.allocationPercentage = allocationPercentage;
        this.isPrimaryTeam = isPrimaryTeam;
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public TeamMember.TeamRole getTeamRole() { return teamRole; }
    public void setTeamRole(TeamMember.TeamRole teamRole) { this.teamRole = teamRole; }

    public Integer getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(Integer allocationPercentage) { this.allocationPercentage = allocationPercentage; }

    public Boolean getIsPrimaryTeam() { return isPrimaryTeam; }
    public void setIsPrimaryTeam(Boolean isPrimaryTeam) { this.isPrimaryTeam = isPrimaryTeam; }

    public UUID getReportingManagerId() { return reportingManagerId; }
    public void setReportingManagerId(UUID reportingManagerId) { this.reportingManagerId = reportingManagerId; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public Object getResponsibilities() { return responsibilities; }
    public void setResponsibilities(Object responsibilities) { this.responsibilities = responsibilities; }

    public Object getSkillsRequired() { return skillsRequired; }
    public void setSkillsRequired(Object skillsRequired) { this.skillsRequired = skillsRequired; }

    public BigDecimal getBillableRate() { return billableRate; }
    public void setBillableRate(BigDecimal billableRate) { this.billableRate = billableRate; }

    public BigDecimal getCostRate() { return costRate; }
    public void setCostRate(BigDecimal costRate) { this.costRate = costRate; }

    public Boolean getCanManageTeam() { return canManageTeam; }
    public void setCanManageTeam(Boolean canManageTeam) { this.canManageTeam = canManageTeam; }

    public Boolean getCanAddMembers() { return canAddMembers; }
    public void setCanAddMembers(Boolean canAddMembers) { this.canAddMembers = canAddMembers; }

    public Boolean getCanRemoveMembers() { return canRemoveMembers; }
    public void setCanRemoveMembers(Boolean canRemoveMembers) { this.canRemoveMembers = canRemoveMembers; }

    public Boolean getCanViewTeamAnalytics() { return canViewTeamAnalytics; }
    public void setCanViewTeamAnalytics(Boolean canViewTeamAnalytics) { this.canViewTeamAnalytics = canViewTeamAnalytics; }

    public Boolean getCanEditTeamSettings() { return canEditTeamSettings; }
    public void setCanEditTeamSettings(Boolean canEditTeamSettings) { this.canEditTeamSettings = canEditTeamSettings; }

    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public Boolean getSlackNotifications() { return slackNotifications; }
    public void setSlackNotifications(Boolean slackNotifications) { this.slackNotifications = slackNotifications; }

    public Boolean getTeamsNotifications() { return teamsNotifications; }
    public void setTeamsNotifications(Boolean teamsNotifications) { this.teamsNotifications = teamsNotifications; }
}