package com.crm.platform.activities.repository;

import com.crm.platform.activities.entity.ActivityReminder;
import com.crm.platform.activities.entity.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityReminderRepository extends JpaRepository<ActivityReminder, UUID> {

    List<ActivityReminder> findByActivityId(UUID activityId);
    
    List<ActivityReminder> findByUserId(UUID userId);
    
    List<ActivityReminder> findByUserIdAndStatus(UUID userId, String status);
    
    // Find reminders that need to be sent
    @Query("SELECT r FROM ActivityReminder r WHERE r.remindAt <= :currentTime AND r.status = 'PENDING'")
    List<ActivityReminder> findPendingReminders(@Param("currentTime") LocalDateTime currentTime);
    
    // Find reminders by type
    List<ActivityReminder> findByReminderTypeAndStatus(ReminderType reminderType, String status);
    
    // Find failed reminders for retry
    @Query("SELECT r FROM ActivityReminder r WHERE r.status = 'FAILED' AND r.retryCount < :maxRetries")
    List<ActivityReminder> findFailedRemindersForRetry(@Param("maxRetries") Integer maxRetries);
    
    // Find reminders for a specific activity and user
    List<ActivityReminder> findByActivityIdAndUserId(UUID activityId, UUID userId);
    
    // Find upcoming reminders for a user
    @Query("SELECT r FROM ActivityReminder r WHERE r.userId = :userId AND " +
           "r.remindAt BETWEEN :startTime AND :endTime AND r.status = 'PENDING' " +
           "ORDER BY r.remindAt ASC")
    List<ActivityReminder> findUpcomingReminders(@Param("userId") UUID userId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);
}