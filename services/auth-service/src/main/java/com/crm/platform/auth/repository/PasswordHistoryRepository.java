package com.crm.platform.auth.repository;

import com.crm.platform.auth.entity.PasswordHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for password history operations
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    /**
     * Find password history for a user ordered by creation date (newest first)
     */
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find recent password history for a user with limit
     */
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Count password history entries for a user
     */
    long countByUserId(UUID userId);

    /**
     * Delete old password history entries for a user, keeping only the specified count
     */
    @Modifying
    @Query("DELETE FROM PasswordHistory ph WHERE ph.userId = :userId AND ph.id NOT IN " +
           "(SELECT ph2.id FROM PasswordHistory ph2 WHERE ph2.userId = :userId " +
           "ORDER BY ph2.createdAt DESC LIMIT :keepCount)")
    void deleteOldPasswordHistory(@Param("userId") UUID userId, @Param("keepCount") int keepCount);

    /**
     * Delete password history entries older than specified date
     */
    @Modifying
    @Query("DELETE FROM PasswordHistory ph WHERE ph.createdAt < :cutoffDate")
    void deletePasswordHistoryOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find password history entries created after a specific date
     */
    List<PasswordHistory> findByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);
}