package com.crm.platform.users.dto;

import com.crm.platform.users.entity.Team;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for creating a team
 */
public class CreateTeamRequest {
    
    @NotBlank(message = "Team name is required")
    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    @JsonProperty("name")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;
    
    @NotNull(message = "Tenant ID is required")
    @JsonProperty("tenant_id")
    private UUID tenantId;
    
    @JsonProperty("manager_id")
    private UUID managerId;
    
    @JsonProperty("parent_team_id")
    private UUID parentTeamId;
    
    @JsonProperty("team_type")
    private Team.TeamType teamType = Team.TeamType.FUNCTIONAL;
    
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    @JsonProperty("department")
    private String department;
    
    @Size(max = 200, message = "Location cannot exceed 200 characters")
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("cost_center")
    private String costCenter;
    
    @JsonProperty("budget")
    private BigDecimal budget;
    
    @JsonProperty("max_members")
    private Integer maxMembers;
    
    @JsonProperty("color")
    private String color;
    
    @JsonProperty("icon")
    private String icon;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("phone")
    private String phone;
    
    @JsonProperty("slack_channel")
    private String slackChannel;
    
    @JsonProperty("teams_channel")
    private String teamsChannel;
    
    @JsonProperty("working_hours_start")
    private LocalTime workingHoursStart;
    
    @JsonProperty("working_hours_end")
    private LocalTime workingHoursEnd;
    
    @JsonProperty("timezone")
    private String timezone;
    
    @JsonProperty("working_days")
    private Set<DayOfWeek> workingDays;
    
    @JsonProperty("settings")
    private Object settings; // Will be converted to JSON
    
    @JsonProperty("goals")
    private Object goals; // Will be converted to JSON
    
    @JsonProperty("kpis")
    private Object kpis; // Will be converted to JSON

    public CreateTeamRequest() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID managerId) { this.managerId = managerId; }

    public UUID getParentTeamId() { return parentTeamId; }
    public void setParentTeamId(UUID parentTeamId) { this.parentTeamId = parentTeamId; }

    public Team.TeamType getTeamType() { return teamType; }
    public void setTeamType(Team.TeamType teamType) { this.teamType = teamType; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSlackChannel() { return slackChannel; }
    public void setSlackChannel(String slackChannel) { this.slackChannel = slackChannel; }

    public String getTeamsChannel() { return teamsChannel; }
    public void setTeamsChannel(String teamsChannel) { this.teamsChannel = teamsChannel; }

    public LocalTime getWorkingHoursStart() { return workingHoursStart; }
    public void setWorkingHoursStart(LocalTime workingHoursStart) { this.workingHoursStart = workingHoursStart; }

    public LocalTime getWorkingHoursEnd() { return workingHoursEnd; }
    public void setWorkingHoursEnd(LocalTime workingHoursEnd) { this.workingHoursEnd = workingHoursEnd; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Set<DayOfWeek> getWorkingDays() { return workingDays; }
    public void setWorkingDays(Set<DayOfWeek> workingDays) { this.workingDays = workingDays; }

    public Object getSettings() { return settings; }
    public void setSettings(Object settings) { this.settings = settings; }

    public Object getGoals() { return goals; }
    public void setGoals(Object goals) { this.goals = goals; }

    public Object getKpis() { return kpis; }
    public void setKpis(Object kpis) { this.kpis = kpis; }
}