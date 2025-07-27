package com.crm.platform.activities.repository;

import com.crm.platform.activities.entity.Activity;
import com.crm.platform.activities.entity.ActivityStatus;
import com.crm.platform.activities.entity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {

    // Basic queries by tenant
    Page<Activity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    
    List<Activity> findByTenantIdAndStatusOrderByDueDateAsc(UUID tenantId, ActivityStatus status);
    
    // Timeline queries
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "(a.relatedEntityType = :entityType AND a.relatedEntityId = :entityId) " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> findTimelineByEntity(@Param("tenantId") UUID tenantId,
                                       @Param("entityType") String entityType,
                                       @Param("entityId") UUID entityId,
                                       Pageable pageable);

    // User-specific queries
    Page<Activity> findByTenantIdAndOwnerIdOrderByCreatedAtDesc(UUID tenantId, UUID ownerId, Pageable pageable);
    
    Page<Activity> findByTenantIdAndAssignedToOrderByDueDateAsc(UUID tenantId, UUID assignedTo, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "(a.ownerId = :userId OR a.assignedTo = :userId) " +
           "ORDER BY a.dueDate ASC")
    Page<Activity> findByUserInvolved(@Param("tenantId") UUID tenantId,
                                     @Param("userId") UUID userId,
                                     Pageable pageable);

    // Status and type queries
    Page<Activity> findByTenantIdAndStatusOrderByDueDateAsc(UUID tenantId, ActivityStatus status, Pageable pageable);
    
    Page<Activity> findByTenantIdAndActivityTypeOrderByCreatedAtDesc(UUID tenantId, ActivityType activityType, Pageable pageable);

    // Date range queries
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "a.dueDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.dueDate ASC")
    List<Activity> findByDueDateRange(@Param("tenantId") UUID tenantId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "a.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> findByCreatedDateRange(@Param("tenantId") UUID tenantId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);

    // Overdue activities
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "a.dueDate < :currentTime AND a.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "ORDER BY a.dueDate ASC")
    List<Activity> findOverdueActivities(@Param("tenantId") UUID tenantId,
                                        @Param("currentTime") LocalDateTime currentTime);

    // Today's activities
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "DATE(a.dueDate) = DATE(:today) " +
           "ORDER BY a.dueDate ASC")
    List<Activity> findTodaysActivities(@Param("tenantId") UUID tenantId,
                                       @Param("today") LocalDateTime today);

    // Upcoming activities
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "a.dueDate BETWEEN :startTime AND :endTime AND " +
           "a.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "ORDER BY a.dueDate ASC")
    List<Activity> findUpcomingActivities(@Param("tenantId") UUID tenantId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    // Calendar integration queries
    List<Activity> findByTenantIdAndExternalCalendarIdIsNotNull(UUID tenantId);
    
    Optional<Activity> findByTenantIdAndExternalEventId(UUID tenantId, String externalEventId);

    // Recurring activities
    List<Activity> findByTenantIdAndIsRecurringTrue(UUID tenantId);

    // Analytics queries
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "a.status = :status AND a.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("tenantId") UUID tenantId,
                                  @Param("status") ActivityStatus status,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a.activityType, COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "a.createdAt BETWEEN :startDate AND :endDate GROUP BY a.activityType")
    List<Object[]> countByTypeAndDateRange(@Param("tenantId") UUID tenantId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a.ownerId, COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "a.createdAt BETWEEN :startDate AND :endDate GROUP BY a.ownerId")
    List<Object[]> countByOwnerAndDateRange(@Param("tenantId") UUID tenantId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // Productivity metrics
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (a.completedDate - a.createdAt))/3600) FROM Activity a " +
           "WHERE a.tenantId = :tenantId AND a.status = 'COMPLETED' AND " +
           "a.completedDate BETWEEN :startDate AND :endDate")
    Double getAverageCompletionTimeHours(@Param("tenantId") UUID tenantId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Search by tags
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           ":tag = ANY(a.tags) ORDER BY a.createdAt DESC")
    Page<Activity> findByTag(@Param("tenantId") UUID tenantId,
                            @Param("tag") String tag,
                            Pageable pageable);

    // Participants queries
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           ":participantId = ANY(a.participants) ORDER BY a.createdAt DESC")
    Page<Activity> findByParticipant(@Param("tenantId") UUID tenantId,
                                    @Param("participantId") UUID participantId,
                                    Pageable pageable);

    // Delegation queries
    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND " +
           "a.ownerId = :ownerId AND a.assignedTo != :ownerId AND a.assignedTo IS NOT NULL " +
           "ORDER BY a.createdAt DESC")
    Page<Activity> findDelegatedActivities(@Param("tenantId") UUID tenantId,
                                          @Param("ownerId") UUID ownerId,
                                          Pageable pageable);
}