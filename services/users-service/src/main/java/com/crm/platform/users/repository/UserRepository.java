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

    // Enhanced search and filtering methods

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.middleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.jobTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersAdvanced(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:department IS NULL OR LOWER(u.department) = LOWER(:department)) AND " +
           "(:jobTitle IS NULL OR LOWER(u.jobTitle) LIKE LOWER(CONCAT('%', :jobTitle, '%'))) AND " +
           "(:managerId IS NULL OR u.managerId = :managerId) AND " +
           "(:teamId IS NULL OR u.teamId = :teamId) AND " +
           "(:officeLocation IS NULL OR LOWER(u.officeLocation) LIKE LOWER(CONCAT('%', :officeLocation, '%'))) AND " +
           "(:onboardingCompleted IS NULL OR u.onboardingCompleted = :onboardingCompleted)")
    Page<User> findUsersWithFilters(@Param("tenantId") UUID tenantId,
                                   @Param("status") User.UserStatus status,
                                   @Param("department") String department,
                                   @Param("jobTitle") String jobTitle,
                                   @Param("managerId") UUID managerId,
                                   @Param("teamId") UUID teamId,
                                   @Param("officeLocation") String officeLocation,
                                   @Param("onboardingCompleted") Boolean onboardingCompleted,
                                   Pageable pageable);

    // User statistics and metrics

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId")
    long countTotalUsersByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE'")
    long countActiveUsersByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.onboardingCompleted = false")
    long countUsersWithIncompleteOnboarding(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.lastLoginAt > :since")
    long countActiveUsersSince(@Param("tenantId") UUID tenantId, @Param("since") LocalDateTime since);

    @Query("SELECT u.department, COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE' " +
           "GROUP BY u.department ORDER BY COUNT(u) DESC")
    List<Object[]> getUserCountByDepartment(@Param("tenantId") UUID tenantId);

    @Query("SELECT u.jobTitle, COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE' " +
           "GROUP BY u.jobTitle ORDER BY COUNT(u) DESC")
    List<Object[]> getUserCountByJobTitle(@Param("tenantId") UUID tenantId);

    @Query("SELECT u.officeLocation, COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE' " +
           "GROUP BY u.officeLocation ORDER BY COUNT(u) DESC")
    List<Object[]> getUserCountByOfficeLocation(@Param("tenantId") UUID tenantId);

    // User hierarchy and relationships

    @Query("SELECT u FROM User u WHERE u.managerId = :managerId AND u.status = 'ACTIVE'")
    List<User> findDirectReports(@Param("managerId") UUID managerId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.managerId = :managerId AND u.status = 'ACTIVE'")
    long countDirectReports(@Param("managerId") UUID managerId);

    @Query("SELECT u FROM User u WHERE u.teamId = :teamId AND u.status = 'ACTIVE' ORDER BY u.firstName, u.lastName")
    List<User> findTeamMembers(@Param("teamId") UUID teamId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.teamId = :teamId AND u.status = 'ACTIVE'")
    long countTeamMembers(@Param("teamId") UUID teamId);

    // Activity and engagement queries

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.lastActivityAt BETWEEN :startDate AND :endDate " +
           "ORDER BY u.lastActivityAt DESC")
    List<User> findUsersActiveInPeriod(@Param("tenantId") UUID tenantId, 
                                      @Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.lastLoginAt BETWEEN :startDate AND :endDate " +
           "ORDER BY u.lastLoginAt DESC")
    List<User> findUsersLoggedInPeriod(@Param("tenantId") UUID tenantId, 
                                      @Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedInPeriod(@Param("tenantId") UUID tenantId, 
                                       @Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    // Bulk operations

    @Modifying
    @Query("UPDATE User u SET u.status = :newStatus WHERE u.id IN :userIds")
    void bulkUpdateStatus(@Param("userIds") List<UUID> userIds, @Param("newStatus") User.UserStatus newStatus);

    @Modifying
    @Query("UPDATE User u SET u.department = :department WHERE u.id IN :userIds")
    void bulkUpdateDepartment(@Param("userIds") List<UUID> userIds, @Param("department") String department);

    @Modifying
    @Query("UPDATE User u SET u.managerId = :managerId WHERE u.id IN :userIds")
    void bulkUpdateManager(@Param("userIds") List<UUID> userIds, @Param("managerId") UUID managerId);

    @Modifying
    @Query("UPDATE User u SET u.teamId = :teamId WHERE u.id IN :userIds")
    void bulkUpdateTeam(@Param("userIds") List<UUID> userIds, @Param("teamId") UUID teamId);

    // GDPR and compliance queries

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.gdprConsentGiven = false")
    List<User> findUsersWithoutGdprConsent(@Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.dataExportRequested = true AND u.dataExportRequestedAt IS NOT NULL")
    List<User> findUsersWithDataExportRequests(@Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.deletionRequested = true AND u.deletionScheduledAt <= :now")
    List<User> findUsersScheduledForDeletion(@Param("tenantId") UUID tenantId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.gdprConsentGiven = :consent, u.gdprConsentDate = :consentDate WHERE u.id = :userId")
    void updateGdprConsent(@Param("userId") UUID userId, @Param("consent") Boolean consent, @Param("consentDate") LocalDateTime consentDate);

    @Modifying
    @Query("UPDATE User u SET u.marketingConsentGiven = :consent, u.marketingConsentDate = :consentDate WHERE u.id = :userId")
    void updateMarketingConsent(@Param("userId") UUID userId, @Param("consent") Boolean consent, @Param("consentDate") LocalDateTime consentDate);

    // Custom fields queries

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.customFields LIKE CONCAT('%', :fieldValue, '%')")
    List<User> findUsersByCustomField(@Param("tenantId") UUID tenantId, @Param("fieldValue") String fieldValue);

    // Skills and certifications

    @Query("SELECT DISTINCT s FROM User u JOIN u.skills s WHERE u.tenantId = :tenantId ORDER BY s")
    List<String> findAllSkillsByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT DISTINCT c FROM User u JOIN u.certifications c WHERE u.tenantId = :tenantId ORDER BY c")
    List<String> findAllCertificationsByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM User u JOIN u.skills s WHERE u.tenantId = :tenantId AND LOWER(s) LIKE LOWER(CONCAT('%', :skill, '%'))")
    List<User> findUsersBySkill(@Param("tenantId") UUID tenantId, @Param("skill") String skill);

    @Query("SELECT u FROM User u JOIN u.certifications c WHERE u.tenantId = :tenantId AND LOWER(c) LIKE LOWER(CONCAT('%', :certification, '%'))")
    List<User> findUsersByCertification(@Param("tenantId") UUID tenantId, @Param("certification") String certification);
}