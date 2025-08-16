package com.crm.platform.users.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing team membership with roles and permissions
 */
@Entity
@Table(name = "team_members", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_id"}),
       indexes = {
    @Index(name = "idx_team_members_team", columnList = "team_id"),
    @Index(name = "idx_team_members_user", columnList = "user_id"),
    @Index(name = "idx_team_members_tenant", columnList = "tenant_id"),
    @Index(name = "idx_team_members_role", columnList = "team_role"),
    @Index(name = "idx_team_members_status", columnList = "status"),
    @Index(name = "idx_team_members_joined", columnList = "joined_at")
})
@EntityListeners(AuditingEntityListener.class)
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false, length = 20)
    private TeamRole teamRole = TeamRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_primary_team")
    private Boolean isPrimaryTeam = false;

    @Column(name = "allocation_percentage")
    private Integer allocationPercentage = 100; // Percentage of time allocated to this team

    @Column(name = "reporting_manager_id")
    private UUID reportingManagerId; // Can be different from team manager

    @Column(name = "position_title", length = 150)
    private String positionTitle; // Role/position within the team

    @Column(name = "responsibilities", columnDefinition = "TEXT")
    private String responsibilities; // JSON array of responsibilities

    @Column(name = "skills_required", columnDefinition = "TEXT")
    private String skillsRequired; // JSON array of required skills

    @Column(name = "performance_rating")
    private java.math.BigDecimal performanceRating;

    @Column(name = "last_performance_review")
    private LocalDateTime lastPerformanceReview;

    @Column(name = "billable_rate")
    private java.math.BigDecimal billableRate;

    @Column(name = "cost_rate")
    private java.math.BigDecimal costRate;

    // Access and permissions
    @Column(name = "can_manage_team")
    private Boolean canManageTeam = false;

    @Column(name = "can_add_members")
    private Boolean canAddMembers = false;

    @Column(name = "can_remove_members")
    private Boolean canRemoveMembers = false;

    @Column(name = "can_view_team_analytics")
    private Boolean canViewTeamAnalytics = false;

    @Column(name = "can_edit_team_settings")
    private Boolean canEditTeamSettings = false;

    // Activity tracking
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "activity_score")
    private java.math.BigDecimal activityScore;

    @Column(name = "contribution_score")
    private java.math.BigDecimal contributionScore;

    // Notification preferences
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    @Column(name = "slack_notifications")
    private Boolean slackNotifications = true;

    @Column(name = "teams_notifications")
    private Boolean teamsNotifications = true;

    // Audit fields
    @Column(name = "added_by")
    private UUID addedBy;

    @Column(name = "removed_by")
    private UUID removedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // JPA relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // Constructors
    public TeamMember() {}

    public TeamMember(UUID teamId, UUID userId, UUID tenantId, TeamRole teamRole, UUID addedBy) {
        this.teamId = teamId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.teamRole = teamRole;
        this.addedBy = addedBy;
        this.joinedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public TeamRole getTeamRole() { return teamRole; }
    public void setTeamRole(TeamRole teamRole) { this.teamRole = teamRole; }

    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }

    public Boolean getIsPrimaryTeam() { return isPrimaryTeam; }
    public void setIsPrimaryTeam(Boolean isPrimaryTeam) { this.isPrimaryTeam = isPrimaryTeam; }

    public Integer getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(Integer allocationPercentage) { this.allocationPercentage = allocationPercentage; }

    public UUID getReportingManagerId() { return reportingManagerId; }
    public void setReportingManagerId(UUID reportingManagerId) { this.reportingManagerId = reportingManagerId; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }

    public String getSkillsRequired() { return skillsRequired; }
    public void setSkillsRequired(String skillsRequired) { this.skillsRequired = skillsRequired; }

    public java.math.BigDecimal getPerformanceRating() { return performanceRating; }
    public void setPerformanceRating(java.math.BigDecimal performanceRating) { this.performanceRating = performanceRating; }

    public LocalDateTime getLastPerformanceReview() { return lastPerformanceReview; }
    public void setLastPerformanceReview(LocalDateTime lastPerformanceReview) { this.lastPerformanceReview = lastPerformanceReview; }

    public java.math.BigDecimal getBillableRate() { return billableRate; }
    public void setBillableRate(java.math.BigDecimal billableRate) { this.billableRate = billableRate; }

    public java.math.BigDecimal getCostRate() { return costRate; }
    public void setCostRate(java.math.BigDecimal costRate) { this.costRate = costRate; }

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

    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public java.math.BigDecimal getActivityScore() { return activityScore; }
    public void setActivityScore(java.math.BigDecimal activityScore) { this.activityScore = activityScore; }

    public java.math.BigDecimal getContributionScore() { return contributionScore; }
    public void setContributionScore(java.math.BigDecimal contributionScore) { this.contributionScore = contributionScore; }

    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public Boolean getSlackNotifications() { return slackNotifications; }
    public void setSlackNotifications(Boolean slackNotifications) { this.slackNotifications = slackNotifications; }

    public Boolean getTeamsNotifications() { return teamsNotifications; }
    public void setTeamsNotifications(Boolean teamsNotifications) { this.teamsNotifications = teamsNotifications; }

    public UUID getAddedBy() { return addedBy; }
    public void setAddedBy(UUID addedBy) { this.addedBy = addedBy; }

    public UUID getRemovedBy() { return removedBy; }
    public void setRemovedBy(UUID removedBy) { this.removedBy = removedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // Helper methods
    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isManager() {
        return teamRole == TeamRole.MANAGER || teamRole == TeamRole.LEAD;
    }

    public boolean canManage() {
        return Boolean.TRUE.equals(canManageTeam) || isManager();
    }

    public void leave() {
        this.status = MemberStatus.LEFT;
        this.leftAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
        this.leftAt = null;
    }

    public void suspend() {
        this.status = MemberStatus.SUSPENDED;
    }

    public long getDaysInTeam() {
        LocalDateTime endDate = leftAt != null ? leftAt : LocalDateTime.now();
        return java.time.temporal.ChronoUnit.DAYS.between(joinedAt, endDate);
    }

    public boolean isFullTimeAllocation() {
        return allocationPercentage != null && allocationPercentage >= 100;
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public enum TeamRole {
        MANAGER,        // Team manager
        LEAD,           // Team lead
        SENIOR_MEMBER,  // Senior team member
        MEMBER,         // Regular team member
        JUNIOR_MEMBER,  // Junior team member
        INTERN,         // Intern
        CONTRACTOR,     // External contractor
        CONSULTANT,     // External consultant
        OBSERVER        // Observer (read-only access)
    }

    public enum MemberStatus {
        ACTIVE,         // Active member
        INACTIVE,       // Inactive member
        SUSPENDED,      // Temporarily suspended
        LEFT,           // Left the team
        ON_LEAVE        // On leave
    }
}