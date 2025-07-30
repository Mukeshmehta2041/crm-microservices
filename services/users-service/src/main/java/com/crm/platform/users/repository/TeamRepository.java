package com.crm.platform.users.repository;

import com.crm.platform.users.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for team operations with hierarchical support
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * Find team by name and tenant
     */
    Optional<Team> findByNameAndTenantId(String name, UUID tenantId);

    /**
     * Find all active teams for a tenant
     */
    List<Team> findByTenantIdAndStatus(UUID tenantId, Team.TeamStatus status);

    /**
     * Find all teams for a tenant with pagination
     */
    Page<Team> findByTenantIdAndStatus(UUID tenantId, Team.TeamStatus status, Pageable pageable);

    /**
     * Find teams by manager
     */
    List<Team> findByManagerIdAndStatus(UUID managerId, Team.TeamStatus status);

    /**
     * Find teams by department
     */
    List<Team> findByTenantIdAndDepartmentAndStatus(UUID tenantId, String department, Team.TeamStatus status);

    /**
     * Find teams by type
     */
    List<Team> findByTenantIdAndTeamTypeAndStatus(UUID tenantId, Team.TeamType teamType, Team.TeamStatus status);

    /**
     * Find child teams of a parent team
     */
    List<Team> findByParentTeamIdAndStatus(UUID parentTeamId, Team.TeamStatus status);

    /**
     * Find all descendant teams using hierarchy path
     */
    @Query("SELECT t FROM Team t WHERE t.hierarchyPath LIKE CONCAT(:hierarchyPath, '%') AND t.status = :status")
    List<Team> findDescendantTeams(@Param("hierarchyPath") String hierarchyPath, @Param("status") Team.TeamStatus status);

    /**
     * Find teams by hierarchy level
     */
    List<Team> findByTenantIdAndHierarchyLevelAndStatus(UUID tenantId, Integer hierarchyLevel, Team.TeamStatus status);

    /**
     * Find top-level teams (no parent)
     */
    List<Team> findByTenantIdAndParentTeamIdIsNullAndStatus(UUID tenantId, Team.TeamStatus status);

    /**
     * Search teams by name
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.status = :status AND " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Team> searchTeams(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status, 
                          @Param("search") String search, Pageable pageable);

    /**
     * Find teams by location
     */
    List<Team> findByTenantIdAndLocationAndStatus(UUID tenantId, String location, Team.TeamStatus status);

    /**
     * Find teams with budget range
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.status = :status AND " +
           "t.budget BETWEEN :minBudget AND :maxBudget")
    List<Team> findTeamsByBudgetRange(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status,
                                     @Param("minBudget") java.math.BigDecimal minBudget, 
                                     @Param("maxBudget") java.math.BigDecimal maxBudget);

    /**
     * Count teams by tenant
     */
    long countByTenantIdAndStatus(UUID tenantId, Team.TeamStatus status);

    /**
     * Count teams by department
     */
    long countByTenantIdAndDepartmentAndStatus(UUID tenantId, String department, Team.TeamStatus status);

    /**
     * Count teams by manager
     */
    long countByManagerIdAndStatus(UUID managerId, Team.TeamStatus status);

    /**
     * Find teams with member count range
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.status = :status AND " +
           "t.memberCount BETWEEN :minMembers AND :maxMembers")
    List<Team> findTeamsByMemberCountRange(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status,
                                          @Param("minMembers") Integer minMembers, 
                                          @Param("maxMembers") Integer maxMembers);

    /**
     * Find teams at capacity
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.status = :status AND " +
           "t.maxMembers IS NOT NULL AND t.memberCount >= t.maxMembers")
    List<Team> findTeamsAtCapacity(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status);

    /**
     * Find teams with recent activity
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.status = :status AND " +
           "t.lastActivityAt > :since ORDER BY t.lastActivityAt DESC")
    List<Team> findTeamsWithRecentActivity(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status,
                                          @Param("since") LocalDateTime since);

    /**
     * Find inactive teams
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.status = :status AND " +
           "(t.lastActivityAt IS NULL OR t.lastActivityAt < :cutoffDate)")
    List<Team> findInactiveTeams(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status,
                                @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Update team member count
     */
    @Modifying
    @Query("UPDATE Team t SET t.memberCount = :memberCount, t.activeMemberCount = :activeMemberCount WHERE t.id = :teamId")
    void updateMemberCounts(@Param("teamId") UUID teamId, @Param("memberCount") Integer memberCount, 
                           @Param("activeMemberCount") Integer activeMemberCount);

    /**
     * Update team activity timestamp
     */
    @Modifying
    @Query("UPDATE Team t SET t.lastActivityAt = :activityTime WHERE t.id = :teamId")
    void updateLastActivityTime(@Param("teamId") UUID teamId, @Param("activityTime") LocalDateTime activityTime);

    /**
     * Update team performance score
     */
    @Modifying
    @Query("UPDATE Team t SET t.performanceScore = :score WHERE t.id = :teamId")
    void updatePerformanceScore(@Param("teamId") UUID teamId, @Param("score") java.math.BigDecimal score);

    /**
     * Check if team name exists in tenant
     */
    boolean existsByNameAndTenantIdAndStatus(String name, UUID tenantId, Team.TeamStatus status);

    /**
     * Get team statistics by department
     */
    @Query("SELECT t.department, COUNT(t), AVG(t.memberCount), SUM(t.budget) FROM Team t " +
           "WHERE t.tenantId = :tenantId AND t.status = :status AND t.department IS NOT NULL " +
           "GROUP BY t.department ORDER BY COUNT(t) DESC")
    List<Object[]> getTeamStatsByDepartment(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status);

    /**
     * Get team statistics by type
     */
    @Query("SELECT t.teamType, COUNT(t), AVG(t.memberCount) FROM Team t " +
           "WHERE t.tenantId = :tenantId AND t.status = :status " +
           "GROUP BY t.teamType ORDER BY COUNT(t) DESC")
    List<Object[]> getTeamStatsByType(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status);

    /**
     * Get team statistics by location
     */
    @Query("SELECT t.location, COUNT(t), AVG(t.memberCount) FROM Team t " +
           "WHERE t.tenantId = :tenantId AND t.status = :status AND t.location IS NOT NULL " +
           "GROUP BY t.location ORDER BY COUNT(t) DESC")
    List<Object[]> getTeamStatsByLocation(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status);

    /**
     * Find teams with specific role
     */
    @Query("SELECT DISTINCT t FROM Team t JOIN t.roles r WHERE t.tenantId = :tenantId AND " +
           "t.status = :status AND r.id = :roleId")
    List<Team> findTeamsWithRole(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status,
                                @Param("roleId") UUID roleId);

    /**
     * Find teams managed by user (including inherited management)
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.status = :status AND " +
           "(t.managerId = :userId OR t.hierarchyPath LIKE CONCAT('%/', " +
           "(SELECT t2.id FROM Team t2 WHERE t2.managerId = :userId), '/%'))")
    List<Team> findTeamsManagedByUser(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status,
                                     @Param("userId") UUID userId);

    /**
     * Get teams ordered by performance score
     */
    List<Team> findByTenantIdAndStatusOrderByPerformanceScoreDesc(UUID tenantId, Team.TeamStatus status);

    /**
     * Get teams ordered by member count
     */
    List<Team> findByTenantIdAndStatusOrderByMemberCountDesc(UUID tenantId, Team.TeamStatus status);

    /**
     * Find teams created in date range
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.status = :status AND " +
           "t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Team> findTeamsCreatedInPeriod(@Param("tenantId") UUID tenantId, @Param("status") Team.TeamStatus status,
                                       @Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
}