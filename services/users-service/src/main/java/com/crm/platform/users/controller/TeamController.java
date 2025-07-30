package com.crm.platform.users.controller;

import com.crm.platform.users.dto.CreateTeamRequest;
import com.crm.platform.users.dto.TeamMemberRequest;
import com.crm.platform.users.entity.Permission;
import com.crm.platform.users.entity.Team;
import com.crm.platform.users.entity.TeamMember;
import com.crm.platform.users.service.TeamService;
import com.crm.platform.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controller for team management operations
 */
@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Team Management", description = "Team and team member management endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TeamController {

    private final TeamService teamService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TeamController(TeamService teamService) {
        this.teamService = teamService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== Team CRUD Operations ====================

    @PostMapping
    @Operation(summary = "Create Team", description = "Create a new team")
    public ResponseEntity<ApiResponse<Team>> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID createdBy,
            HttpServletRequest httpRequest) {
        
        Team team = teamService.createTeam(
            request.getName(),
            request.getDescription(),
            request.getTenantId(),
            request.getManagerId(),
            request.getTeamType(),
            request.getParentTeamId(),
            request.getDepartment(),
            request.getLocation(),
            createdBy,
            httpRequest
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(team));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Team", description = "Get team by ID")
    public ResponseEntity<ApiResponse<Team>> getTeamById(@PathVariable UUID id) {
        return teamService.getTeamById(id)
                .map(team -> ResponseEntity.ok(ApiResponse.success(team)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get Tenant Teams", description = "Get all teams for a tenant")
    public ResponseEntity<ApiResponse<List<Team>>> getTenantTeams(@PathVariable UUID tenantId) {
        List<Team> teams = teamService.getAllTeams(tenantId);
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    @GetMapping("/tenant/{tenantId}/paginated")
    @Operation(summary = "Get Tenant Teams Paginated", description = "Get teams for tenant with pagination")
    public ResponseEntity<ApiResponse<Page<Team>>> getTenantTeamsPaginated(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        Page<Team> teams = teamService.getTeams(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    @GetMapping("/tenant/{tenantId}/search")
    @Operation(summary = "Search Teams", description = "Search teams by name")
    public ResponseEntity<ApiResponse<Page<Team>>> searchTeams(
            @PathVariable UUID tenantId,
            @RequestParam String query,
            Pageable pageable) {
        Page<Team> teams = teamService.searchTeams(tenantId, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Team", description = "Update team details")
    public ResponseEntity<ApiResponse<Team>> updateTeam(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "X-User-ID", required = false) UUID updatedBy,
            HttpServletRequest request) {
        
        Team team = teamService.updateTeam(
            id,
            (String) updates.get("name"),
            (String) updates.get("description"),
            (String) updates.get("department"),
            (String) updates.get("location"),
            (String) updates.get("color"),
            (String) updates.get("icon"),
            (Integer) updates.get("maxMembers"),
            updates.get("budget") != null ? new java.math.BigDecimal(updates.get("budget").toString()) : null,
            updatedBy,
            request
        );
        
        return ResponseEntity.ok(ApiResponse.success(team));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Team", description = "Delete a team")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteTeam(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ID", required = false) UUID deletedBy,
            HttpServletRequest request) {
        
        teamService.deleteTeam(id, deletedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "deleted", true,
            "teamId", id,
            "deletedAt", LocalDateTime.now()
        )));
    }

    // ==================== Team Member Management ====================

    @PostMapping("/{id}/members")
    @Operation(summary = "Add Team Member", description = "Add member to team")
    public ResponseEntity<ApiResponse<TeamMember>> addTeamMember(
            @PathVariable UUID id,
            @Valid @RequestBody TeamMemberRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID addedBy,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            HttpServletRequest httpRequest) {
        
        TeamMember teamMember = teamService.addTeamMember(
            id,
            request.getUserId(),
            tenantId,
            request.getTeamRole(),
            request.getAllocationPercentage(),
            request.getIsPrimaryTeam(),
            addedBy,
            httpRequest
        );
        
        return ResponseEntity.ok(ApiResponse.success(teamMember));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove Team Member", description = "Remove member from team")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeTeamMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-ID", required = false) UUID removedBy,
            HttpServletRequest request) {
        
        teamService.removeTeamMember(id, userId, removedBy, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "removed", true,
            "teamId", id,
            "userId", userId,
            "removedAt", LocalDateTime.now()
        )));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get Team Members", description = "Get all members of a team")
    public ResponseEntity<ApiResponse<List<TeamMember>>> getTeamMembers(@PathVariable UUID id) {
        List<TeamMember> members = teamService.getTeamMembers(id);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PutMapping("/{id}/members/{userId}/role")
    @Operation(summary = "Update Member Role", description = "Update team member role")
    public ResponseEntity<ApiResponse<TeamMember>> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam TeamMember.TeamRole role,
            @RequestHeader(value = "X-User-ID", required = false) UUID updatedBy,
            HttpServletRequest request) {
        
        TeamMember teamMember = teamService.updateTeamMemberRole(id, userId, role, updatedBy, request);
        return ResponseEntity.ok(ApiResponse.success(teamMember));
    }

    @PutMapping("/{id}/members/{userId}/allocation")
    @Operation(summary = "Update Member Allocation", description = "Update team member allocation percentage")
    public ResponseEntity<ApiResponse<TeamMember>> updateMemberAllocation(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam Integer allocationPercentage,
            @RequestHeader(value = "X-User-ID", required = false) UUID updatedBy,
            HttpServletRequest request) {
        
        TeamMember teamMember = teamService.updateMemberAllocation(id, userId, allocationPercentage, updatedBy, request);
        return ResponseEntity.ok(ApiResponse.success(teamMember));
    }

    // ==================== Team Hierarchy ====================

    @GetMapping("/tenant/{tenantId}/hierarchy")
    @Operation(summary = "Get Team Hierarchy", description = "Get team hierarchy for tenant")
    public ResponseEntity<ApiResponse<List<Team>>> getTeamHierarchy(@PathVariable UUID tenantId) {
        List<Team> hierarchy = teamService.getTeamHierarchy(tenantId);
        return ResponseEntity.ok(ApiResponse.success(hierarchy));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get Child Teams", description = "Get child teams")
    public ResponseEntity<ApiResponse<List<Team>>> getChildTeams(@PathVariable UUID id) {
        List<Team> childTeams = teamService.getChildTeams(id);
        return ResponseEntity.ok(ApiResponse.success(childTeams));
    }

    @GetMapping("/{id}/descendants")
    @Operation(summary = "Get Descendant Teams", description = "Get all descendant teams")
    public ResponseEntity<ApiResponse<List<Team>>> getDescendantTeams(@PathVariable UUID id) {
        List<Team> descendants = teamService.getDescendantTeams(id);
        return ResponseEntity.ok(ApiResponse.success(descendants));
    }

    // ==================== User Team Queries ====================

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Teams", description = "Get teams where user is a member")
    public ResponseEntity<ApiResponse<List<TeamMember>>> getUserTeams(@PathVariable UUID userId) {
        List<TeamMember> userTeams = teamService.getUserTeams(userId);
        return ResponseEntity.ok(ApiResponse.success(userTeams));
    }

    @GetMapping("/user/{userId}/managed")
    @Operation(summary = "Get Teams Managed by User", description = "Get teams managed by user")
    public ResponseEntity<ApiResponse<List<Team>>> getTeamsManagedByUser(
            @PathVariable UUID userId,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        List<Team> managedTeams = teamService.getTeamsManagedByUser(tenantId, userId);
        return ResponseEntity.ok(ApiResponse.success(managedTeams));
    }

    // ==================== Team Permissions ====================

    @GetMapping("/{id}/members/{userId}/permissions")
    @Operation(summary = "Get Member Permissions", description = "Get effective permissions for team member")
    public ResponseEntity<ApiResponse<Set<Permission>>> getTeamMemberPermissions(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        Set<Permission> permissions = teamService.calculateTeamMemberPermissions(id, userId);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/{id}/members/{userId}/permissions/check")
    @Operation(summary = "Check Member Permission", description = "Check if team member has specific permission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkTeamMemberPermission(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam String resource,
            @RequestParam String action) {
        
        boolean hasPermission = teamService.teamMemberHasPermission(id, userId, resource, action);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "teamId", id,
            "userId", userId,
            "resource", resource,
            "action", action,
            "hasPermission", hasPermission,
            "checkedAt", LocalDateTime.now()
        )));
    }

    // ==================== Team Statistics and Analytics ====================

    @GetMapping("/{id}/statistics")
    @Operation(summary = "Get Team Statistics", description = "Get comprehensive team statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeamStatistics(@PathVariable UUID id) {
        Map<String, Object> statistics = teamService.getTeamStatistics(id);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @GetMapping("/tenant/{tenantId}/statistics")
    @Operation(summary = "Get Tenant Team Statistics", description = "Get tenant-wide team statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantTeamStatistics(@PathVariable UUID tenantId) {
        Map<String, Object> statistics = teamService.getTenantTeamStatistics(tenantId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @GetMapping("/{id}/analytics/performance")
    @Operation(summary = "Get Team Performance Analytics", description = "Get team performance metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeamPerformanceAnalytics(@PathVariable UUID id) {
        Map<String, Object> analytics = teamService.getTeamStatistics(id);
        
        // Extract performance-specific metrics
        Map<String, Object> performanceMetrics = Map.of(
            "performanceScore", analytics.get("performanceScore"),
            "averageTenure", analytics.get("averageTenure"),
            "recentlyActiveMembers", analytics.get("recentlyActiveMembers"),
            "annualTurnover", analytics.get("annualTurnover"),
            "membersByRole", analytics.get("membersByRole"),
            "membersByAllocation", analytics.get("membersByAllocation")
        );
        
        return ResponseEntity.ok(ApiResponse.success(performanceMetrics));
    }

    @GetMapping("/{id}/analytics/activity")
    @Operation(summary = "Get Team Activity Analytics", description = "Get team activity and engagement metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeamActivityAnalytics(@PathVariable UUID id) {
        Map<String, Object> analytics = teamService.getTeamStatistics(id);
        
        // Extract activity-specific metrics
        Map<String, Object> activityMetrics = Map.of(
            "totalMembers", analytics.get("totalMembers"),
            "recentlyActiveMembers", analytics.get("recentlyActiveMembers"),
            "averageTenure", analytics.get("averageTenure"),
            "annualTurnover", analytics.get("annualTurnover")
        );
        
        return ResponseEntity.ok(ApiResponse.success(activityMetrics));
    }

    @GetMapping("/{id}/analytics/composition")
    @Operation(summary = "Get Team Composition Analytics", description = "Get team composition and structure metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeamCompositionAnalytics(@PathVariable UUID id) {
        Map<String, Object> analytics = teamService.getTeamStatistics(id);
        
        // Extract composition-specific metrics
        Map<String, Object> compositionMetrics = Map.of(
            "totalMembers", analytics.get("totalMembers"),
            "managersCount", analytics.get("managersCount"),
            "leadsCount", analytics.get("leadsCount"),
            "regularMembersCount", analytics.get("regularMembersCount"),
            "membersByRole", analytics.get("membersByRole"),
            "membersByAllocation", analytics.get("membersByAllocation"),
            "isAtCapacity", analytics.get("isAtCapacity")
        );
        
        return ResponseEntity.ok(ApiResponse.success(compositionMetrics));
    }

    // ==================== Team Filters and Queries ====================

    @GetMapping("/tenant/{tenantId}/by-department")
    @Operation(summary = "Get Teams by Department", description = "Get teams filtered by department")
    public ResponseEntity<ApiResponse<List<Team>>> getTeamsByDepartment(
            @PathVariable UUID tenantId,
            @RequestParam String department) {
        // This would need to be implemented in TeamService
        List<Team> teams = teamService.getAllTeams(tenantId).stream()
                .filter(team -> department.equals(team.getDepartment()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    @GetMapping("/tenant/{tenantId}/by-location")
    @Operation(summary = "Get Teams by Location", description = "Get teams filtered by location")
    public ResponseEntity<ApiResponse<List<Team>>> getTeamsByLocation(
            @PathVariable UUID tenantId,
            @RequestParam String location) {
        List<Team> teams = teamService.getAllTeams(tenantId).stream()
                .filter(team -> location.equals(team.getLocation()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    @GetMapping("/tenant/{tenantId}/by-type")
    @Operation(summary = "Get Teams by Type", description = "Get teams filtered by team type")
    public ResponseEntity<ApiResponse<List<Team>>> getTeamsByType(
            @PathVariable UUID tenantId,
            @RequestParam Team.TeamType teamType) {
        List<Team> teams = teamService.getAllTeams(tenantId).stream()
                .filter(team -> teamType.equals(team.getTeamType()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "healthy",
            "service", "team-service",
            "timestamp", LocalDateTime.now()
        )));
    }
}