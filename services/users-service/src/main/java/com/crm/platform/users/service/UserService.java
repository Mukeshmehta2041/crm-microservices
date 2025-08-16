package com.crm.platform.users.service;

import com.crm.platform.users.dto.CreateUserRequest;
import com.crm.platform.users.dto.UpdateUserRequest;
import com.crm.platform.users.dto.UserResponse;
import com.crm.platform.users.dto.UserStatisticsResponse;
import com.crm.platform.users.entity.User;
import com.crm.platform.users.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserAuditService userAuditService;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserService(UserRepository userRepository, UserAuditService userAuditService) {
        this.userRepository = userRepository;
        this.userAuditService = userAuditService;
        this.objectMapper = new ObjectMapper();
    }

    public UserResponse createUser(CreateUserRequest request) {
        logger.info("Creating user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmailAndTenantId(request.getEmail(), request.getTenantId())) {
            throw new IllegalArgumentException("User with email already exists in this tenant");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setTenantId(request.getTenantId());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setJobTitle(request.getJobTitle());
        user.setDepartment(request.getDepartment());
        user.setProfileImageUrl(request.getProfileImageUrl());
        user.setTimezone(request.getTimezone());
        user.setLanguage(request.getLanguage());
        user.setDateFormat(request.getDateFormat());
        user.setTimeFormat(request.getTimeFormat());
        user.setRoles(request.getRoles());
        user.setManagerId(request.getManagerId());
        user.setTeamId(request.getTeamId());
        user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        user.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        user.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());

        User savedUser = userRepository.save(user);
        logger.info("User created successfully with ID: {}", savedUser.getId());

        return new UserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(UUID userId) {
        logger.debug("Fetching user by ID: {}", userId);
        return userRepository.findById(userId)
                .map(UserResponse::new);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .map(UserResponse::new);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmailAndTenant(String email, UUID tenantId) {
        logger.debug("Fetching user by email: {} and tenant: {}", email, tenantId);
        return userRepository.findByEmailAndTenantId(email, tenantId)
                .map(UserResponse::new);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByTenant(UUID tenantId) {
        logger.debug("Fetching users for tenant: {}", tenantId);
        return userRepository.findByTenantId(tenantId)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByTenant(UUID tenantId, Pageable pageable) {
        logger.debug("Fetching users for tenant: {} with pagination", tenantId);
        return userRepository.findByTenantId(tenantId, pageable)
                .map(UserResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(UUID tenantId, String search, Pageable pageable) {
        logger.debug("Searching users for tenant: {} with query: {}", tenantId, search);
        return userRepository.searchUsers(tenantId, search, pageable)
                .map(UserResponse::new);
    }

    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        logger.info("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Update fields if provided
        if (request.getEmail() != null) {
            // Check if email is already taken by another user
            Optional<User> existingUser = userRepository.findByEmailAndTenantId(request.getEmail(), user.getTenantId());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Email already exists for another user");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getJobTitle() != null) user.setJobTitle(request.getJobTitle());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
        if (request.getTimezone() != null) user.setTimezone(request.getTimezone());
        if (request.getLanguage() != null) user.setLanguage(request.getLanguage());
        if (request.getDateFormat() != null) user.setDateFormat(request.getDateFormat());
        if (request.getTimeFormat() != null) user.setTimeFormat(request.getTimeFormat());
        if (request.getRoles() != null) user.setRoles(request.getRoles());
        if (request.getManagerId() != null) user.setManagerId(request.getManagerId());
        if (request.getTeamId() != null) user.setTeamId(request.getTeamId());
        if (request.getOnboardingCompleted() != null) user.setOnboardingCompleted(request.getOnboardingCompleted());
        if (request.getEmailNotificationsEnabled() != null) user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        if (request.getPushNotificationsEnabled() != null) user.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        if (request.getSmsNotificationsEnabled() != null) user.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully with ID: {}", updatedUser.getId());

        return new UserResponse(updatedUser);
    }

    public void updateLastActivity(UUID userId) {
        logger.debug("Updating last activity for user: {}", userId);
        userRepository.updateLastActivityTime(userId, LocalDateTime.now());
    }

    public void updateUserStatus(UUID userId, User.UserStatus status) {
        logger.info("Updating status for user: {} to: {}", userId, status);
        userRepository.updateUserStatus(userId, status);
    }

    public void deleteUser(UUID userId) {
        logger.info("Deleting user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Soft delete by updating status
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);
        
        logger.info("User soft deleted successfully with ID: {}", userId);
    }

    @Transactional(readOnly = true)
    public long countActiveUsersByTenant(UUID tenantId) {
        return userRepository.countByTenantIdAndStatus(tenantId, User.UserStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersWithIncompleteOnboarding(UUID tenantId) {
        return userRepository.findUsersWithIncompleteOnboarding(tenantId)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    // Enhanced search and filtering methods

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsersAdvanced(UUID tenantId, String search, Pageable pageable) {
        logger.debug("Advanced search for users in tenant: {} with query: {}", tenantId, search);
        return userRepository.searchUsersAdvanced(tenantId, search, pageable)
                .map(UserResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findUsersWithFilters(UUID tenantId, User.UserStatus status, String department,
                                                  String jobTitle, UUID managerId, UUID teamId, 
                                                  String officeLocation, Boolean onboardingCompleted, 
                                                  Pageable pageable) {
        logger.debug("Finding users with filters for tenant: {}", tenantId);
        return userRepository.findUsersWithFilters(tenantId, status, department, jobTitle, 
                                                  managerId, teamId, officeLocation, onboardingCompleted, pageable)
                .map(UserResponse::new);
    }

    // User activation and deactivation

    public UserResponse activateUser(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Activating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        User.UserStatus oldStatus = user.getStatus();
        user.setStatus(User.UserStatus.ACTIVE);
        user.setUpdatedBy(performedBy);
        
        User updatedUser = userRepository.save(user);
        
        // Log audit trail
        userAuditService.logUserStatusChange(user, oldStatus, User.UserStatus.ACTIVE, performedBy, request);
        
        logger.info("User activated successfully: {}", userId);
        return new UserResponse(updatedUser);
    }

    public UserResponse deactivateUser(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Deactivating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        User.UserStatus oldStatus = user.getStatus();
        user.setStatus(User.UserStatus.INACTIVE);
        user.setUpdatedBy(performedBy);
        
        User updatedUser = userRepository.save(user);
        
        // Log audit trail
        userAuditService.logUserStatusChange(user, oldStatus, User.UserStatus.INACTIVE, performedBy, request);
        
        logger.info("User deactivated successfully: {}", userId);
        return new UserResponse(updatedUser);
    }

    public UserResponse suspendUser(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Suspending user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        User.UserStatus oldStatus = user.getStatus();
        user.setStatus(User.UserStatus.SUSPENDED);
        user.setUpdatedBy(performedBy);
        
        User updatedUser = userRepository.save(user);
        
        // Log audit trail
        userAuditService.logUserStatusChange(user, oldStatus, User.UserStatus.SUSPENDED, performedBy, request);
        
        logger.info("User suspended successfully: {}", userId);
        return new UserResponse(updatedUser);
    }

    // User statistics and analytics

    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics(UUID tenantId) {
        logger.debug("Generating user statistics for tenant: {}", tenantId);
        
        UserStatisticsResponse stats = new UserStatisticsResponse();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        // Basic counts
        stats.setTotalUsers(userRepository.countTotalUsersByTenant(tenantId));
        stats.setActiveUsers(userRepository.countActiveUsersByTenant(tenantId));
        stats.setInactiveUsers(userRepository.countByTenantIdAndStatus(tenantId, User.UserStatus.INACTIVE));
        stats.setSuspendedUsers(userRepository.countByTenantIdAndStatus(tenantId, User.UserStatus.SUSPENDED));
        stats.setUsersWithIncompleteOnboarding(userRepository.countUsersWithIncompleteOnboarding(tenantId));
        
        // Activity metrics
        stats.setUsersActiveLast30Days(userRepository.countActiveUsersSince(tenantId, thirtyDaysAgo));
        stats.setUsersLoggedInLast30Days(userRepository.countActiveUsersSince(tenantId, thirtyDaysAgo));
        stats.setNewUsersLast30Days(userRepository.findUsersCreatedInPeriod(tenantId, thirtyDaysAgo, LocalDateTime.now()).size());
        
        // Department statistics
        List<Object[]> deptStats = userRepository.getUserCountByDepartment(tenantId);
        stats.setUsersByDepartment(deptStats.stream()
                .map(row -> new UserStatisticsResponse.DepartmentStats((String) row[0], (Long) row[1]))
                .collect(Collectors.toList()));
        
        // Job title statistics
        List<Object[]> jobStats = userRepository.getUserCountByJobTitle(tenantId);
        stats.setUsersByJobTitle(jobStats.stream()
                .map(row -> new UserStatisticsResponse.JobTitleStats((String) row[0], (Long) row[1]))
                .collect(Collectors.toList()));
        
        // Office location statistics
        List<Object[]> locationStats = userRepository.getUserCountByOfficeLocation(tenantId);
        stats.setUsersByOfficeLocation(locationStats.stream()
                .map(row -> new UserStatisticsResponse.OfficeLocationStats((String) row[0], (Long) row[1]))
                .collect(Collectors.toList()));
        
        // Calculate onboarding completion rate
        long totalUsers = stats.getTotalUsers();
        if (totalUsers > 0) {
            stats.setOnboardingCompletionRate(
                ((double) (totalUsers - stats.getUsersWithIncompleteOnboarding()) / totalUsers) * 100);
        }
        
        return stats;
    }

    // Bulk operations

    public void bulkUpdateStatus(List<UUID> userIds, User.UserStatus status, UUID performedBy, HttpServletRequest request) {
        logger.info("Bulk updating status for {} users to: {}", userIds.size(), status);
        
        // Get users before update for audit logging
        List<User> users = userRepository.findAllById(userIds);
        
        userRepository.bulkUpdateStatus(userIds, status);
        
        // Log audit trail for each user
        for (User user : users) {
            userAuditService.logUserStatusChange(user, user.getStatus(), status, performedBy, request);
        }
        
        logger.info("Bulk status update completed for {} users", userIds.size());
    }

    public void bulkUpdateDepartment(List<UUID> userIds, String department, UUID performedBy, HttpServletRequest request) {
        logger.info("Bulk updating department for {} users to: {}", userIds.size(), department);
        userRepository.bulkUpdateDepartment(userIds, department);
        logger.info("Bulk department update completed for {} users", userIds.size());
    }

    public void bulkUpdateManager(List<UUID> userIds, UUID managerId, UUID performedBy, HttpServletRequest request) {
        logger.info("Bulk updating manager for {} users to: {}", userIds.size(), managerId);
        userRepository.bulkUpdateManager(userIds, managerId);
        logger.info("Bulk manager update completed for {} users", userIds.size());
    }

    public void bulkUpdateTeam(List<UUID> userIds, UUID teamId, UUID performedBy, HttpServletRequest request) {
        logger.info("Bulk updating team for {} users to: {}", userIds.size(), teamId);
        userRepository.bulkUpdateTeam(userIds, teamId);
        logger.info("Bulk team update completed for {} users", userIds.size());
    }

    // User hierarchy and relationships

    @Transactional(readOnly = true)
    public List<UserResponse> getDirectReports(UUID managerId) {
        logger.debug("Getting direct reports for manager: {}", managerId);
        return userRepository.findDirectReports(managerId)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getTeamMembers(UUID teamId) {
        logger.debug("Getting team members for team: {}", teamId);
        return userRepository.findTeamMembers(teamId)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countDirectReports(UUID managerId) {
        return userRepository.countDirectReports(managerId);
    }

    @Transactional(readOnly = true)
    public long countTeamMembers(UUID teamId) {
        return userRepository.countTeamMembers(teamId);
    }

    // Skills and certifications

    @Transactional(readOnly = true)
    public List<String> getAllSkills(UUID tenantId) {
        return userRepository.findAllSkillsByTenant(tenantId);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCertifications(UUID tenantId) {
        return userRepository.findAllCertificationsByTenant(tenantId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findUsersBySkill(UUID tenantId, String skill) {
        return userRepository.findUsersBySkill(tenantId, skill)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findUsersByCertification(UUID tenantId, String certification) {
        return userRepository.findUsersByCertification(tenantId, certification)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    // GDPR and compliance

    public void updateGdprConsent(UUID userId, boolean consent, UUID performedBy, HttpServletRequest request) {
        logger.info("Updating GDPR consent for user: {} to: {}", userId, consent);
        
        LocalDateTime now = LocalDateTime.now();
        userRepository.updateGdprConsent(userId, consent, now);
        
        // Log audit trail
        userAuditService.logGdprConsentChange(userId, null, consent, performedBy, request);
        
        logger.info("GDPR consent updated for user: {}", userId);
    }

    public void updateMarketingConsent(UUID userId, boolean consent, UUID performedBy, HttpServletRequest request) {
        logger.info("Updating marketing consent for user: {} to: {}", userId, consent);
        
        LocalDateTime now = LocalDateTime.now();
        userRepository.updateMarketingConsent(userId, consent, now);
        
        logger.info("Marketing consent updated for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersWithoutGdprConsent(UUID tenantId) {
        return userRepository.findUsersWithoutGdprConsent(tenantId)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    public void requestDataExport(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Data export requested for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setDataExportRequested(true);
        user.setDataExportRequestedAt(LocalDateTime.now());
        user.setUpdatedBy(performedBy);
        
        userRepository.save(user);
        
        // Log audit trail
        userAuditService.logDataExportRequest(userId, user.getTenantId(), performedBy, request);
        
        logger.info("Data export request logged for user: {}", userId);
    }

    public void requestDataDeletion(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Data deletion requested for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setDeletionRequested(true);
        user.setDeletionRequestedAt(LocalDateTime.now());
        user.setDeletionScheduledAt(LocalDateTime.now().plusDays(30)); // 30-day grace period
        user.setUpdatedBy(performedBy);
        
        userRepository.save(user);
        
        logger.info("Data deletion request logged for user: {}", userId);
    }

    // Custom fields management

    public UserResponse updateCustomFields(UUID userId, Map<String, Object> customFields, UUID performedBy, HttpServletRequest request) {
        logger.info("Updating custom fields for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        try {
            String customFieldsJson = objectMapper.writeValueAsString(customFields);
            user.setCustomFields(customFieldsJson);
            user.setUpdatedBy(performedBy);
            
            User updatedUser = userRepository.save(user);
            
            logger.info("Custom fields updated for user: {}", userId);
            return new UserResponse(updatedUser);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize custom fields for user: {}", userId, e);
            throw new IllegalArgumentException("Invalid custom fields format");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomFields(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        if (user.getCustomFields() == null || user.getCustomFields().trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(user.getCustomFields(), Map.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize custom fields for user: {}", userId, e);
            return new HashMap<>();
        }
    }
}