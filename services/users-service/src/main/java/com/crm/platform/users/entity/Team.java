package com.crm.platform.users.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a team with hierarchical support and organizational structure
 */
@Entity
@Table(name = "teams", indexes = {
    @Index(name = "idx_teams_name", columnList = "name"),
    @Index(name = "idx_teams_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_teams_manager_id", columnList = "manager_id"),
    @Index(name = "idx_teams_parent_id", columnList = "parent_team_id"),
    @Index(name = "idx_teams_department", columnList = "department"),
    @Index(name = "idx_teams_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "parent_team_id")
    private UUID parentTeamId;

    @Column(name = "hierarchy_level")
    private Integer hierarchyLevel = 0;

    @Column(name = "hierarchy_path", length = 1000)
    private String hierarchyPath; // Materialized path for efficient queries

    @Enumerated(EnumType.STRING)
    @Column(name = "team_type", nullable = false, length = 20)
    private TeamType teamType = TeamType.FUNCTIONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeamStatus status = TeamStatus.ACTIVE;

    @Size(max = 100)
    @Column(name = "department", length = 100)
    private String department;

    @Size(max = 200)
    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "cost_center", length = 50)
    private String costCenter;

    @Column(name = "budget")
    private java.math.BigDecimal budget;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "color", length = 7)
    private String color; // Hex color for UI

    @Column(name = "icon", length = 50)
    private String icon; // Icon identifier for UI

    // Team settings (JSON format)
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings; // JSON string for team-specific settings

    // Team goals and metrics
    @Column(name = "goals", columnDefinition = "TEXT")
    private String goals; // JSON string for team goals

    @Column(name = "kpis", columnDefinition = "TEXT")
    private String kpis; // JSON string for key performance indicators

    // Contact information
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "slack_channel", length = 100)
    private String slackChannel;

    @Column(name = "teams_channel", length = 100)
    private String teamsChannel;

    // Working hours and schedule
    @Column(name = "working_hours_start")
    private java.time.LocalTime workingHoursStart;

    @Column(name = "working_hours_end")
    private java.time.LocalTime workingHoursEnd;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "team_working_days", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "day_of_week")
    private Set<java.time.DayOfWeek> workingDays = new HashSet<>();

    // Team permissions and access control
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "team_roles",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"),
        indexes = {
            @Index(name = "idx_team_roles_team", columnList = "team_id"),
            @Index(name = "idx_team_roles_role", columnList = "role_id")
        }
    )
    private Set<Role> roles = new HashSet<>();

    // Statistics and metrics
    @Column(name = "member_count")
    private Integer memberCount = 0;

    @Column(name = "active_member_count")
    private Integer activeMemberCount = 0;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "performance_score")
    private java.math.BigDecimal performanceScore;

    // Audit fields
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Team() {}

    public Team(String name, String description, UUID tenantId, UUID managerId) {
        this.name = name;
        this.description = description;
        this.tenantId = tenantId;
        this.managerId = managerId;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public Integer getHierarchyLevel() { return hierarchyLevel; }
    public void setHierarchyLevel(Integer hierarchyLevel) { this.hierarchyLevel = hierarchyLevel; }

    public String getHierarchyPath() { return hierarchyPath; }
    public void setHierarchyPath(String hierarchyPath) { this.hierarchyPath = hierarchyPath; }

    public TeamType getTeamType() { return teamType; }
    public void setTeamType(TeamType teamType) { this.teamType = teamType; }

    public TeamStatus getStatus() { return status; }
    public void setStatus(TeamStatus status) { this.status = status; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    public java.math.BigDecimal getBudget() { return budget; }
    public void setBudget(java.math.BigDecimal budget) { this.budget = budget; }

    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getSettings() { return settings; }
    public void setSettings(String settings) { this.settings = settings; }

    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }

    public String getKpis() { return kpis; }
    public void setKpis(String kpis) { this.kpis = kpis; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSlackChannel() { return slackChannel; }
    public void setSlackChannel(String slackChannel) { this.slackChannel = slackChannel; }

    public String getTeamsChannel() { return teamsChannel; }
    public void setTeamsChannel(String teamsChannel) { this.teamsChannel = teamsChannel; }

    public java.time.LocalTime getWorkingHoursStart() { return workingHoursStart; }
    public void setWorkingHoursStart(java.time.LocalTime workingHoursStart) { this.workingHoursStart = workingHoursStart; }

    public java.time.LocalTime getWorkingHoursEnd() { return workingHoursEnd; }
    public void setWorkingHoursEnd(java.time.LocalTime workingHoursEnd) { this.workingHoursEnd = workingHoursEnd; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Set<java.time.DayOfWeek> getWorkingDays() { return workingDays; }
    public void setWorkingDays(Set<java.time.DayOfWeek> workingDays) { this.workingDays = workingDays; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }

    public Integer getActiveMemberCount() { return activeMemberCount; }
    public void setActiveMemberCount(Integer activeMemberCount) { this.activeMemberCount = activeMemberCount; }

    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public java.math.BigDecimal getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(java.math.BigDecimal performanceScore) { this.performanceScore = performanceScore; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean isActive() {
        return status == TeamStatus.ACTIVE;
    }

    public boolean isAtCapacity() {
        return maxMembers != null && memberCount != null && memberCount >= maxMembers;
    }

    public boolean canAddMembers() {
        return isActive() && !isAtCapacity();
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    public boolean isChildOf(Team parentTeam) {
        return this.parentTeamId != null && this.parentTeamId.equals(parentTeam.getId());
    }

    public boolean isDescendantOf(Team ancestorTeam) {
        return this.hierarchyPath != null && 
               this.hierarchyPath.contains("/" + ancestorTeam.getId() + "/");
    }

    public void updateHierarchyPath() {
        if (this.parentTeamId == null) {
            this.hierarchyPath = "/" + this.id + "/";
            this.hierarchyLevel = 0;
        }
        // Parent hierarchy path should be set by service when parent hierarchy is known
    }

    public void incrementMemberCount() {
        this.memberCount = (this.memberCount == null ? 0 : this.memberCount) + 1;
    }

    public void decrementMemberCount() {
        this.memberCount = Math.max(0, (this.memberCount == null ? 0 : this.memberCount) - 1);
    }

    public void updateActivityTimestamp() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public enum TeamType {
        FUNCTIONAL,     // Functional teams (Sales, Marketing, etc.)
        PROJECT,        // Project-based teams
        CROSS_FUNCTIONAL, // Cross-functional teams
        TEMPORARY,      // Temporary teams
        VIRTUAL,        // Virtual/remote teams
        COMMITTEE       // Committee or governance teams
    }

    public enum TeamStatus {
        ACTIVE,         // Active team
        INACTIVE,       // Inactive team
        SUSPENDED,      // Temporarily suspended
        ARCHIVED,       // Archived team
        DISBANDED       // Disbanded team
    }
}