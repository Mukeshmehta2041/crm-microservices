package com.crm.platform.users.service;

import com.crm.platform.users.entity.Team;
import com.crm.platform.users.entity.TeamMember;
import com.crm.platform.users.entity.Permission;
import com.crm.platform.users.repository.TeamRepository;
import com.crm.platform.users.repository.TeamMemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for team management including creation, member management, 
 * permissions, and analytics
 */
@Service
@Transactional
public class TeamService {

    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RoleService roleService;
    private final UserAuditService userAuditService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TeamService(TeamRepository teamRepository,
                      TeamMemberRepository teamMemberRepository,
                      RoleService roleService,
                      UserAuditService userAuditService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.roleService = roleService;
        this.userAuditService = userAuditService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== Team CRUD Operations ====================

    /**
     * Create a new team
     */
    public Team createTeam(String name, String description, UUID tenantId, UUID managerId, 
                          Team.TeamType teamType, UUID parentTeamId, String department, 
                          String location, UUID createdBy, HttpServletRequest request) {
        logger.info("Creating team: {} for tenant: {}", name, tenantId);

        // Check if team name already exists in tenant
        if (teamRepository.existsByNameAndTenantIdAndStatus(name, tenantId, Team.TeamStatus.ACTIVE)) {
            throw new IllegalArgumentException("Team with name '" + name + "' already exists in this tenant");
        }

        Team team = new Team(name, description, tenantId, managerId);
        team.setTeamType(teamType);
        team.setDepartment(department);
        team.setLocation(location);
        team.setCreatedBy(createdBy);

        // Handle hierarchy
        if (parentTeamId != null) {
            Team parentTeam = teamRepository.findById(parentTeamId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent team not found"));
            
            if (!parentTeam.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Parent team must be in the same tenant");
            }

            team.setParentTeamId(parentTeamId);
            team.setHierarchyLevel(parentTeam.getHierarchyLevel() + 1);
            team.setHierarchyPath(parentTeam.getHierarchyPath() + team.getId() + "/");
        } else {
            team.setHierarchyLevel(0);
        }

        Team savedTeam = teamRepository.save(team);

        // Update hierarchy path after save (when ID is available)
        if (parentTeamId == null) {
            savedTeam.setHierarchyPath("/" + savedTeam.getId() + "/");
            savedTeam = teamRepository.save(savedTeam);
        }

        // Add manager as team member if specified
        if (managerId != null) {
            addTeamMember(savedTeam.getId(), managerId, tenantId, TeamMember.TeamRole.MANAGER, 
                         100, true, createdBy, request);
        }

        logger.info("Team created successfully: {} with ID: {}", name, savedTeam.getId());
        return savedTeam;
    }

    /**
     * Update team
     */
    public Team updateTeam(UUID teamId, String name, String description, String department, 
                          String location, String color, String icon, Integer maxMembers,
                          java.math.BigDecimal budget, UUID updatedBy, HttpServletRequest request) {
        logger.info("Updating team: {}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));

        // Check if new name conflicts with existing teams
        if (name != null && !name.equals(team.getName())) {
            if (teamRepository.existsByNameAndTenantIdAndStatus(name, team.getTenantId(), Team.TeamStatus.ACTIVE)) {
                throw new IllegalArgumentException("Team with name '" + name + "' already exists in this tenant");
            }
            team.setName(name);
        }

        if (description != null) team.setDescription(description);
        if (department != null) team.setDepartment(department);
        if (location != null) team.setLocation(location);
        if (color != null) team.setColor(color);
        if (icon != null) team.setIcon(icon);
        if (maxMembers != null) team.setMaxMembers(maxMembers);
        if (budget != null) team.setBudget(budget);
        team.setUpdatedBy(updatedBy);

        Team updatedTeam = teamRepository.save(team);
        logger.info("Team updated successfully: {}", teamId);
        return updatedTeam;
    }

    /**
     * Delete team (soft delete)
     */
    public void deleteTeam(UUID teamId, UUID deletedBy, HttpServletRequest request) {
        logger.info("Deleting team: {}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));

        // Check if team has active members
        long activeMemberCount = teamMemberRepository.countByTeamIdAndStatus(teamId, TeamMember.MemberStatus.ACTIVE);
        if (activeMemberCount > 0) {
            throw new IllegalArgumentException("Cannot delete team with active members. " +
                    "Please remove all members first.");
        }

        // Check if team has child teams
        List<Team> childTeams = teamRepository.findByParentTeamIdAndStatus(teamId, Team.TeamStatus.ACTIVE);
        if (!childTeams.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete team with child teams. " +
                    "Please delete or reassign child teams first.");
        }

        team.setStatus(Team.TeamStatus.ARCHIVED);
        team.setUpdatedBy(deletedBy);
        teamRepository.save(team);

        logger.info("Team deleted successfully: {}", teamId);
    }

    // ==================== Team Member Management ====================

    /**
     * Add member to team
     */
    public TeamMember addTeamMember(UUID teamId, UUID userId, UUID tenantId, TeamMember.TeamRole teamRole,
                                   Integer allocationPercentage, Boolean isPrimaryTeam, UUID addedBy, 
                                   HttpServletRequest request) {
        logger.info("Adding member: {} to team: {}", userId, teamId);

        // Validate team exists and is active
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));

        if (!team.isActive()) {
            throw new IllegalArgumentException("Cannot add members to inactive team");
        }

        if (!team.canAddMembers()) {
            throw new IllegalArgumentException("Team is at capacity and cannot accept new members");
        }

        // Check if user is already a member
        if (teamMemberRepository.existsByTeamIdAndUserIdAndStatus(teamId, userId, TeamMember.MemberStatus.ACTIVE)) {
            throw new IllegalArgumentException("User is already a member of this team");
        }

        TeamMember teamMember = new TeamMember(teamId, userId, tenantId, teamRole, addedBy);
        teamMember.setAllocationPercentage(allocationPercentage);
        teamMember.setIsPrimaryTeam(isPrimaryTeam);

        // Set permissions based on role
        setMemberPermissionsByRole(teamMember, teamRole);

        TeamMember savedMember = teamMemberRepository.save(teamMember);

        // Update team member count
        updateTeamMemberCounts(teamId);

        // Set as primary team if specified
        if (Boolean.TRUE.equals(isPrimaryTeam)) {
            teamMemberRepository.unsetPrimaryTeamForUser(userId);
            teamMemberRepository.setPrimaryTeamForUser(teamId, userId);
        }

        // Update team activity
        teamRepository.updateLastActivityTime(teamId, LocalDateTime.now());

        logger.info("Member added successfully: {} to team: {}", userId, teamId);
        return savedMember;
    }

    /**
     * Remove member from team
     */
    public void removeTeamMember(UUID teamId, UUID userId, UUID removedBy, HttpServletRequest request) {
        logger.info("Removing member: {} from team: {}", userId, teamId);

        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found"));

        if (!teamMember.isActive()) {
            throw new IllegalArgumentException("Member is already inactive");
        }

        teamMember.leave();
        teamMember.setRemovedBy(removedBy);
        teamMemberRepository.save(teamMember);

        // Update team member count
        updateTeamMemberCounts(teamId);

        // Update team activity
        teamRepository.updateLastActivityTime(teamId, LocalDateTime.now());

        logger.info("Member removed successfully: {} from team: {}", userId, teamId);
    }

    /**
     * Update team member role
     */
    public TeamMember updateTeamMemberRole(UUID teamId, UUID userId, TeamMember.TeamRole newRole, 
                                          UUID updatedBy, HttpServletRequest request) {
        logger.info("Updating member role: {} in team: {} to: {}", userId, teamId, newRole);

        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found"));

        if (!teamMember.isActive()) {
            throw new IllegalArgumentException("Cannot update role for inactive member");
        }

        teamMember.setTeamRole(newRole);
        setMemberPermissionsByRole(teamMember, newRole);

        TeamMember updatedMember = teamMemberRepository.save(teamMember);
        logger.info("Member role updated successfully: {} in team: {}", userId, teamId);
        return updatedMember;
    }

    /**
     * Update member allocation percentage
     */
    public TeamMember updateMemberAllocation(UUID teamId, UUID userId, Integer allocationPercentage, 
                                            UUID updatedBy, HttpServletRequest request) {
        logger.info("Updating member allocation: {} in team: {} to: {}%", userId, teamId, allocationPercentage);

        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found"));

        if (!teamMember.isActive()) {
            throw new IllegalArgumentException("Cannot update allocation for inactive member");
        }

        teamMember.setAllocationPercentage(allocationPercentage);
        TeamMember updatedMember = teamMemberRepository.save(teamMember);
        
        logger.info("Member allocation updated successfully: {} in team: {}", userId, teamId);
        return updatedMember;
    }

    // ==================== Team Permission Management ====================

    /**
     * Calculate effective permissions for team member
     */
    public Set<Permission> calculateTeamMemberPermissions(UUID teamId, UUID userId) {
        logger.debug("Calculating team permissions for user: {} in team: {}", userId, teamId);

        Set<Permission> permissions = new HashSet<>();

        // Get user's individual permissions
        permissions.addAll(roleService.calculateEffectivePermissions(userId));

        // Get team-specific permissions
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team != null) {
            // Add permissions from team roles
            team.getRoles().forEach(role -> {
                permissions.addAll(role.getPermissions());
                // Add inherited permissions from role hierarchy
                permissions.addAll(getInheritedPermissionsFromRole(role));
            });

            // Add inherited permissions from parent teams
            permissions.addAll(getInheritedTeamPermissions(team));
        }

        logger.debug("Calculated {} effective permissions for user: {} in team: {}", 
                    permissions.size(), userId, teamId);
        return permissions;
    }

    /**
     * Check if team member has specific permission
     */
    public boolean teamMemberHasPermission(UUID teamId, UUID userId, String resource, String action) {
        Set<Permission> permissions = calculateTeamMemberPermissions(teamId, userId);
        return permissions.stream().anyMatch(p -> p.matches(resource, action));
    }

    // ==================== Team Hierarchy Management ====================

    /**
     * Get team hierarchy for tenant
     */
    public List<Team> getTeamHierarchy(UUID tenantId) {
        List<Team> allTeams = teamRepository.findByTenantIdAndStatus(tenantId, Team.TeamStatus.ACTIVE);
        return buildTeamHierarchy(allTeams);
    }

    /**
     * Get child teams
     */
    public List<Team> getChildTeams(UUID teamId) {
        return teamRepository.findByParentTeamIdAndStatus(teamId, Team.TeamStatus.ACTIVE);
    }

    /**
     * Get descendant teams (all levels)
     */
    public List<Team> getDescendantTeams(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));
        
        return teamRepository.findDescendantTeams(team.getHierarchyPath(), Team.TeamStatus.ACTIVE);
    }

    // ==================== Team Statistics and Analytics ====================

    /**
     * Get team statistics
     */
    public Map<String, Object> getTeamStatistics(UUID teamId) {
        logger.debug("Generating team statistics for team: {}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));

        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        long totalMembers = teamMemberRepository.countByTeamIdAndStatus(teamId, TeamMember.MemberStatus.ACTIVE);
        long managersCount = teamMemberRepository.countByTeamIdAndTeamRoleAndStatus(
            teamId, TeamMember.TeamRole.MANAGER, TeamMember.MemberStatus.ACTIVE);
        long leadsCount = teamMemberRepository.countByTeamIdAndTeamRoleAndStatus(
            teamId, TeamMember.TeamRole.LEAD, TeamMember.MemberStatus.ACTIVE);

        stats.put("totalMembers", totalMembers);
        stats.put("managersCount", managersCount);
        stats.put("leadsCount", leadsCount);
        stats.put("regularMembersCount", totalMembers - managersCount - leadsCount);

        // Member statistics by role
        List<Object[]> memberStatsByRole = teamMemberRepository.getMemberStatsByRole(
            teamId, TeamMember.MemberStatus.ACTIVE);
        stats.put("membersByRole", memberStatsByRole);

        // Member statistics by allocation
        List<Object[]> memberStatsByAllocation = teamMemberRepository.getMemberStatsByAllocation(
            teamId, TeamMember.MemberStatus.ACTIVE);
        stats.put("membersByAllocation", memberStatsByAllocation);

        // Team metrics
        stats.put("averageTenure", teamMemberRepository.getAverageTeamTenure(teamId));
        stats.put("performanceScore", team.getPerformanceScore());
        stats.put("budget", team.getBudget());
        stats.put("isAtCapacity", team.isAtCapacity());

        // Activity metrics
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<TeamMember> recentlyActive = teamMemberRepository.findMembersWithRecentActivity(
            teamId, TeamMember.MemberStatus.ACTIVE, thirtyDaysAgo);
        stats.put("recentlyActiveMembers", recentlyActive.size());

        // Turnover metrics
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        long turnoverCount = teamMemberRepository.getTeamTurnoverForPeriod(teamId, oneYearAgo, LocalDateTime.now());
        stats.put("annualTurnover", turnoverCount);

        return stats;
    }

    /**
     * Get tenant team statistics
     */
    public Map<String, Object> getTenantTeamStatistics(UUID tenantId) {
        logger.debug("Generating tenant team statistics for tenant: {}", tenantId);

        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        long totalTeams = teamRepository.countByTenantIdAndStatus(tenantId, Team.TeamStatus.ACTIVE);
        stats.put("totalTeams", totalTeams);

        // Team statistics by department
        List<Object[]> teamStatsByDept = teamRepository.getTeamStatsByDepartment(tenantId, Team.TeamStatus.ACTIVE);
        stats.put("teamsByDepartment", teamStatsByDept);

        // Team statistics by type
        List<Object[]> teamStatsByType = teamRepository.getTeamStatsByType(tenantId, Team.TeamStatus.ACTIVE);
        stats.put("teamsByType", teamStatsByType);

        // Team statistics by location
        List<Object[]> teamStatsByLocation = teamRepository.getTeamStatsByLocation(tenantId, Team.TeamStatus.ACTIVE);
        stats.put("teamsByLocation", teamStatsByLocation);

        // Performance metrics
        List<Team> topPerformingTeams = teamRepository.findByTenantIdAndStatusOrderByPerformanceScoreDesc(
            tenantId, Team.TeamStatus.ACTIVE).stream().limit(10).collect(Collectors.toList());
        stats.put("topPerformingTeams", topPerformingTeams);

        // Size metrics
        List<Team> largestTeams = teamRepository.findByTenantIdAndStatusOrderByMemberCountDesc(
            tenantId, Team.TeamStatus.ACTIVE).stream().limit(10).collect(Collectors.toList());
        stats.put("largestTeams", largestTeams);

        return stats;
    }

    // ==================== Query Methods ====================

    /**
     * Get all teams for tenant
     */
    @Transactional(readOnly = true)
    public List<Team> getAllTeams(UUID tenantId) {
        return teamRepository.findByTenantIdAndStatus(tenantId, Team.TeamStatus.ACTIVE);
    }

    /**
     * Get teams with pagination
     */
    @Transactional(readOnly = true)
    public Page<Team> getTeams(UUID tenantId, Pageable pageable) {
        return teamRepository.findByTenantIdAndStatus(tenantId, Team.TeamStatus.ACTIVE, pageable);
    }

    /**
     * Search teams
     */
    @Transactional(readOnly = true)
    public Page<Team> searchTeams(UUID tenantId, String search, Pageable pageable) {
        return teamRepository.searchTeams(tenantId, Team.TeamStatus.ACTIVE, search, pageable);
    }

    /**
     * Get team by ID
     */
    @Transactional(readOnly = true)
    public Optional<Team> getTeamById(UUID teamId) {
        return teamRepository.findById(teamId);
    }

    /**
     * Get team members
     */
    @Transactional(readOnly = true)
    public List<TeamMember> getTeamMembers(UUID teamId) {
        return teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMember.MemberStatus.ACTIVE);
    }

    /**
     * Get user teams
     */
    @Transactional(readOnly = true)
    public List<TeamMember> getUserTeams(UUID userId) {
        return teamMemberRepository.findByUserIdAndStatus(userId, TeamMember.MemberStatus.ACTIVE);
    }

    /**
     * Get teams managed by user
     */
    @Transactional(readOnly = true)
    public List<Team> getTeamsManagedByUser(UUID tenantId, UUID userId) {
        return teamRepository.findTeamsManagedByUser(tenantId, Team.TeamStatus.ACTIVE, userId);
    }

    // ==================== Private Helper Methods ====================

    private void setMemberPermissionsByRole(TeamMember teamMember, TeamMember.TeamRole role) {
        switch (role) {
            case MANAGER:
                teamMember.setCanManageTeam(true);
                teamMember.setCanAddMembers(true);
                teamMember.setCanRemoveMembers(true);
                teamMember.setCanViewTeamAnalytics(true);
                teamMember.setCanEditTeamSettings(true);
                break;
            case LEAD:
                teamMember.setCanManageTeam(true);
                teamMember.setCanAddMembers(true);
                teamMember.setCanRemoveMembers(false);
                teamMember.setCanViewTeamAnalytics(true);
                teamMember.setCanEditTeamSettings(false);
                break;
            case SENIOR_MEMBER:
                teamMember.setCanManageTeam(false);
                teamMember.setCanAddMembers(false);
                teamMember.setCanRemoveMembers(false);
                teamMember.setCanViewTeamAnalytics(true);
                teamMember.setCanEditTeamSettings(false);
                break;
            default:
                teamMember.setCanManageTeam(false);
                teamMember.setCanAddMembers(false);
                teamMember.setCanRemoveMembers(false);
                teamMember.setCanViewTeamAnalytics(false);
                teamMember.setCanEditTeamSettings(false);
                break;
        }
    }

    private void updateTeamMemberCounts(UUID teamId) {
        long totalMembers = teamMemberRepository.countByTeamIdAndStatus(teamId, TeamMember.MemberStatus.ACTIVE);
        teamRepository.updateMemberCounts(teamId, (int) totalMembers, (int) totalMembers);
    }

    private List<Team> buildTeamHierarchy(List<Team> teams) {
        return teams.stream()
                .filter(team -> team.getParentTeamId() == null)
                .sorted(Comparator.comparing(Team::getName))
                .collect(Collectors.toList());
    }

    private Set<Permission> getInheritedPermissionsFromRole(com.crm.platform.users.entity.Role role) {
        Set<Permission> inheritedPermissions = new HashSet<>();
        
        if (role.getParentRoleId() != null) {
            roleService.getRoleById(role.getParentRoleId()).ifPresent(parentRole -> {
                inheritedPermissions.addAll(parentRole.getPermissions());
                inheritedPermissions.addAll(getInheritedPermissionsFromRole(parentRole));
            });
        }
        
        return inheritedPermissions;
    }

    private Set<Permission> getInheritedTeamPermissions(Team team) {
        Set<Permission> inheritedPermissions = new HashSet<>();
        
        if (team.getParentTeamId() != null) {
            teamRepository.findById(team.getParentTeamId()).ifPresent(parentTeam -> {
                parentTeam.getRoles().forEach(role -> {
                    inheritedPermissions.addAll(role.getPermissions());
                });
                inheritedPermissions.addAll(getInheritedTeamPermissions(parentTeam));
            });
        }
        
        return inheritedPermissions;
    }
}