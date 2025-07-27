package com.crm.platform.users.repository;

import com.crm.platform.users.entity.User;
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

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    List<User> findByTenantId(UUID tenantId);

    Page<User> findByTenantId(UUID tenantId, Pageable pageable);

    List<User> findByTenantIdAndStatus(UUID tenantId, User.UserStatus status);

    Page<User> findByTenantIdAndStatus(UUID tenantId, User.UserStatus status, Pageable pageable);

    List<User> findByManagerId(UUID managerId);

    List<User> findByTeamId(UUID teamId);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.lastActivityAt = :activityTime WHERE u.id = :userId")
    void updateLastActivityTime(@Param("userId") UUID userId, @Param("activityTime") LocalDateTime activityTime);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    void updateUserStatus(@Param("userId") UUID userId, @Param("status") User.UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") User.UserStatus status);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.onboardingCompleted = false")
    List<User> findUsersWithIncompleteOnboarding(@Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.lastActivityAt < :cutoffDate AND u.status = 'ACTIVE'")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
}