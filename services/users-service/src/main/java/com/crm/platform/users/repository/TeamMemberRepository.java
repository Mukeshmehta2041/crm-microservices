package com.crm.platform.users.repository;

import com.crm.platform.users.entity.TeamMember;
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
 * Repository for team member operations
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    /**
     * Find team member by team and user
     */
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Find all active members of a team
     */
    List<TeamMember> findByTeamIdAndStatus(UUID teamId, TeamMember.MemberStatus status);

    /**
     * Find all teams for a user
     */
    List<TeamMember> findByUserIdAndStatus(UUID userId, TeamMember.MemberStatus status);

    /**
     * Find user's primary team
     */
    Optional<TeamMember> findByUserIdAndIsPrimaryTeamTrueAndStatus(UUID userId, TeamMember.MemberStatus status);

    /**
     * Find team members with pagination
     */
    Page<TeamMember> findByTeamIdAndStatus(UUID teamId, TeamMember.MemberStatus status, Pageable pageable);

    /**
     * Find team members by role
     */
    List<TeamMember> findByTeamIdAndTeamRoleAndStatus(UUID teamId, TeamMember.TeamRole teamRole, 
                                                     TeamMember.MemberStatus status);

    /**
     * Find team managers
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "(tm.teamRole = 'MANAGER' OR tm.teamRole = 'LEAD')")
    List<TeamMember> findTeamManagers(@Param("teamId") UUID teamId, @Param("status") TeamMember.MemberStatus status);

    /**
     * Find members by tenant
     */
    List<TeamMember> findByTenantIdAndStatus(UUID tenantId, TeamMember.MemberStatus status);

    /**
     * Find members by tenant with pagination
     */
    Page<TeamMember> findByTenantIdAndStatus(UUID tenantId, TeamMember.MemberStatus status, Pageable pageable);

    /**
     * Count active members in team
     */
    long countByTeamIdAndStatus(UUID teamId, TeamMember.MemberStatus status);

    /**
     * Count teams for user
     */
    long countByUserIdAndStatus(UUID userId, TeamMember.MemberStatus status);

    /**
     * Count members by role in team
     */
    long countByTeamIdAndTeamRoleAndStatus(UUID teamId, TeamMember.TeamRole teamRole, 
                                          TeamMember.MemberStatus status);

    /**
     * Check if user is member of team
     */
    boolean existsByTeamIdAndUserIdAndStatus(UUID teamId, UUID userId, TeamMember.MemberStatus status);

    /**
     * Check if user is manager of team
     */
    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.userId = :userId AND " +
           "tm.status = :status AND (tm.teamRole = 'MANAGER' OR tm.teamRole = 'LEAD')")
    boolean isUserTeamManager(@Param("teamId") UUID teamId, @Param("userId") UUID userId, 
                             @Param("status") TeamMember.MemberStatus status);

    /**
     * Find members with specific permissions
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "(:canManage IS NULL OR tm.canManageTeam = :canManage) AND " +
           "(:canAddMembers IS NULL OR tm.canAddMembers = :canAddMembers) AND " +
           "(:canRemoveMembers IS NULL OR tm.canRemoveMembers = :canRemoveMembers)")
    List<TeamMember> findMembersWithPermissions(@Param("teamId") UUID teamId, 
                                               @Param("status") TeamMember.MemberStatus status,
                                               @Param("canManage") Boolean canManage,
                                               @Param("canAddMembers") Boolean canAddMembers,
                                               @Param("canRemoveMembers") Boolean canRemoveMembers);

    /**
     * Find members by allocation percentage range
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "tm.allocationPercentage BETWEEN :minAllocation AND :maxAllocation")
    List<TeamMember> findMembersByAllocationRange(@Param("teamId") UUID teamId, 
                                                  @Param("status") TeamMember.MemberStatus status,
                                                  @Param("minAllocation") Integer minAllocation,
                                                  @Param("maxAllocation") Integer maxAllocation);

    /**
     * Find full-time team members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "tm.allocationPercentage >= 100")
    List<TeamMember> findFullTimeMembers(@Param("teamId") UUID teamId, @Param("status") TeamMember.MemberStatus status);

    /**
     * Find part-time team members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "tm.allocationPercentage < 100")
    List<TeamMember> findPartTimeMembers(@Param("teamId") UUID teamId, @Param("status") TeamMember.MemberStatus status);

    /**
     * Find members with recent activity
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "tm.lastActivityAt > :since ORDER BY tm.lastActivityAt DESC")
    List<TeamMember> findMembersWithRecentActivity(@Param("teamId") UUID teamId, 
                                                   @Param("status") TeamMember.MemberStatus status,
                                                   @Param("since") LocalDateTime since);

    /**
     * Find inactive members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "(tm.lastActivityAt IS NULL OR tm.lastActivityAt < :cutoffDate)")
    List<TeamMember> findInactiveMembers(@Param("teamId") UUID teamId, 
                                        @Param("status") TeamMember.MemberStatus status,
                                        @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find members joined in date range
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "tm.joinedAt BETWEEN :startDate AND :endDate ORDER BY tm.joinedAt DESC")
    List<TeamMember> findMembersJoinedInPeriod(@Param("teamId") UUID teamId, 
                                              @Param("status") TeamMember.MemberStatus status,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Find members who left in date range
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = 'LEFT' AND " +
           "tm.leftAt BETWEEN :startDate AND :endDate ORDER BY tm.leftAt DESC")
    List<TeamMember> findMembersWhoLeftInPeriod(@Param("teamId") UUID teamId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Find long-term members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "tm.joinedAt < :cutoffDate ORDER BY tm.joinedAt ASC")
    List<TeamMember> findLongTermMembers(@Param("teamId") UUID teamId, 
                                        @Param("status") TeamMember.MemberStatus status,
                                        @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Update member activity timestamp
     */
    @Modifying
    @Query("UPDATE TeamMember tm SET tm.lastActivityAt = :activityTime WHERE tm.id = :memberId")
    void updateLastActivityTime(@Param("memberId") UUID memberId, @Param("activityTime") LocalDateTime activityTime);

    /**
     * Update member performance rating
     */
    @Modifying
    @Query("UPDATE TeamMember tm SET tm.performanceRating = :rating, tm.lastPerformanceReview = :reviewDate " +
           "WHERE tm.id = :memberId")
    void updatePerformanceRating(@Param("memberId") UUID memberId, @Param("rating") java.math.BigDecimal rating,
                                @Param("reviewDate") LocalDateTime reviewDate);

    /**
     * Update member allocation percentage
     */
    @Modifying
    @Query("UPDATE TeamMember tm SET tm.allocationPercentage = :allocation WHERE tm.id = :memberId")
    void updateAllocationPercentage(@Param("memberId") UUID memberId, @Param("allocation") Integer allocation);

    /**
     * Set primary team for user (unset others first)
     */
    @Modifying
    @Query("UPDATE TeamMember tm SET tm.isPrimaryTeam = false WHERE tm.userId = :userId")
    void unsetPrimaryTeamForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE TeamMember tm SET tm.isPrimaryTeam = true WHERE tm.teamId = :teamId AND tm.userId = :userId")
    void setPrimaryTeamForUser(@Param("teamId") UUID teamId, @Param("userId") UUID userId);

    /**
     * Get team member statistics by role
     */
    @Query("SELECT tm.teamRole, COUNT(tm), AVG(tm.allocationPercentage), AVG(tm.performanceRating) " +
           "FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status " +
           "GROUP BY tm.teamRole ORDER BY COUNT(tm) DESC")
    List<Object[]> getMemberStatsByRole(@Param("teamId") UUID teamId, @Param("status") TeamMember.MemberStatus status);

    /**
     * Get team member statistics by allocation
     */
    @Query("SELECT " +
           "CASE WHEN tm.allocationPercentage >= 100 THEN 'Full-time' " +
           "WHEN tm.allocationPercentage >= 50 THEN 'Part-time' " +
           "ELSE 'Minimal' END as allocationType, " +
           "COUNT(tm), AVG(tm.performanceRating) " +
           "FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status " +
           "GROUP BY allocationType ORDER BY COUNT(tm) DESC")
    List<Object[]> getMemberStatsByAllocation(@Param("teamId") UUID teamId, @Param("status") TeamMember.MemberStatus status);

    /**
     * Find members with high performance ratings
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "tm.performanceRating >= :minRating ORDER BY tm.performanceRating DESC")
    List<TeamMember> findHighPerformers(@Param("teamId") UUID teamId, @Param("status") TeamMember.MemberStatus status,
                                       @Param("minRating") java.math.BigDecimal minRating);

    /**
     * Find members needing performance review
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = :status AND " +
           "(tm.lastPerformanceReview IS NULL OR tm.lastPerformanceReview < :cutoffDate)")
    List<TeamMember> findMembersNeedingReview(@Param("teamId") UUID teamId, @Param("status") TeamMember.MemberStatus status,
                                             @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get average team tenure (simplified for H2 compatibility)
     */
    @Query(value = "SELECT AVG(DATEDIFF('DAY', tm.joined_at, COALESCE(tm.left_at, CURRENT_TIMESTAMP))) " +
           "FROM team_members tm WHERE tm.team_id = :teamId AND tm.joined_at IS NOT NULL", 
           nativeQuery = true)
    Double getAverageTeamTenure(@Param("teamId") UUID teamId);

    /**
     * Get team turnover rate for period
     */
    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = 'LEFT' AND " +
           "tm.leftAt BETWEEN :startDate AND :endDate")
    long getTeamTurnoverForPeriod(@Param("teamId") UUID teamId, @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Find teams where user has management permissions
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.userId = :userId AND tm.status = :status AND " +
           "(tm.canManageTeam = true OR tm.teamRole IN ('MANAGER', 'LEAD'))")
    List<TeamMember> findTeamsUserCanManage(@Param("userId") UUID userId, @Param("status") TeamMember.MemberStatus status);
}