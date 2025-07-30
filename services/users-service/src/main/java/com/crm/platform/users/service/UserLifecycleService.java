package com.crm.platform.users.service;

import com.crm.platform.users.entity.User;
import com.crm.platform.users.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing user lifecycle including onboarding, data transfer, 
 * anonymization, and activity tracking
 */
@Service
@Transactional
public class UserLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(UserLifecycleService.class);

    private final UserRepository userRepository;
    private final UserAuditService userAuditService;
    private final ObjectMapper objectMapper;

    @Value("${app.user.onboarding.steps:5}")
    private int totalOnboardingSteps;

    @Value("${app.user.data-retention.days:2555}") // 7 years default
    private int dataRetentionDays;

    @Autowired
    public UserLifecycleService(UserRepository userRepository, UserAuditService userAuditService) {
        this.userRepository = userRepository;
        this.userAuditService = userAuditService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initialize user onboarding workflow
     */
    public void initializeOnboarding(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Initializing onboarding for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setOnboardingStep(1);
        user.setOnboardingCompleted(false);
        user.setUpdatedBy(performedBy);
        
        userRepository.save(user);
        
        // Log onboarding start
        userAuditService.logUserUpdate(user, user, performedBy, request);
        
        logger.info("Onboarding initialized for user: {}", userId);
    }

    /**
     * Update onboarding progress
     */
    public void updateOnboardingProgress(UUID userId, int step, UUID performedBy, HttpServletRequest request) {
        logger.info("Updating onboarding progress for user: {} to step: {}", userId, step);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        if (step < 1 || step > totalOnboardingSteps) {
            throw new IllegalArgumentException("Invalid onboarding step: " + step);
        }
        
        user.setOnboardingStep(step);
        user.setUpdatedBy(performedBy);
        
        // Check if onboarding is completed
        if (step >= totalOnboardingSteps) {
            user.setOnboardingCompleted(true);
            user.setOnboardingCompletedAt(LocalDateTime.now());
            logger.info("Onboarding completed for user: {}", userId);
        }
        
        userRepository.save(user);
        
        // Log onboarding progress
        userAuditService.logUserUpdate(user, user, performedBy, request);
        
        logger.info("Onboarding progress updated for user: {}", userId);
    }

    /**
     * Complete user onboarding
     */
    public void completeOnboarding(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Completing onboarding for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setOnboardingCompleted(true);
        user.setOnboardingCompletedAt(LocalDateTime.now());
        user.setOnboardingStep(totalOnboardingSteps);
        user.setUpdatedBy(performedBy);
        
        userRepository.save(user);
        
        // Log onboarding completion
        userAuditService.logUserUpdate(user, user, performedBy, request);
        
        logger.info("Onboarding completed for user: {}", userId);
    }

    /**
     * Add completed training to user
     */
    public void addCompletedTraining(UUID userId, UUID trainingId, UUID performedBy, HttpServletRequest request) {
        logger.info("Adding completed training: {} for user: {}", trainingId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        Set<UUID> completedTrainings = user.getCompletedTrainings();
        if (completedTrainings == null) {
            completedTrainings = new HashSet<>();
        }
        
        completedTrainings.add(trainingId);
        user.setCompletedTrainings(completedTrainings);
        user.setUpdatedBy(performedBy);
        
        userRepository.save(user);
        
        logger.info("Training {} added to completed trainings for user: {}", trainingId, userId);
    }

    /**
     * Update user activity tracking
     */
    @Async
    public void trackUserActivity(UUID userId, String activityType, Map<String, Object> activityData) {
        logger.debug("Tracking activity for user: {} - type: {}", userId, activityType);
        
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setLastActivityAt(LocalDateTime.now());
                userRepository.save(user);
            }
        } catch (Exception e) {
            logger.error("Error tracking user activity for user: {}", userId, e);
        }
    }

    /**
     * Prepare user data for transfer (before deletion)
     */
    public Map<String, Object> prepareUserDataTransfer(UUID userId) {
        logger.info("Preparing data transfer for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        Map<String, Object> userData = new HashMap<>();
        
        // Basic information
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());
        userData.put("middleName", user.getMiddleName());
        userData.put("displayName", user.getDisplayName());
        userData.put("phoneNumber", user.getPhoneNumber());
        userData.put("mobilePhone", user.getMobilePhone());
        userData.put("workPhone", user.getWorkPhone());
        
        // Professional information
        userData.put("jobTitle", user.getJobTitle());
        userData.put("department", user.getDepartment());
        userData.put("employeeId", user.getEmployeeId());
        userData.put("managerId", user.getManagerId());
        userData.put("teamId", user.getTeamId());
        userData.put("hireDate", user.getHireDate());
        userData.put("officeLocation", user.getOfficeLocation());
        
        // Contact and address information
        userData.put("addressLine1", user.getAddressLine1());
        userData.put("addressLine2", user.getAddressLine2());
        userData.put("city", user.getCity());
        userData.put("stateProvince", user.getStateProvince());
        userData.put("postalCode", user.getPostalCode());
        userData.put("country", user.getCountry());
        
        // Skills and certifications
        userData.put("skills", user.getSkills());
        userData.put("certifications", user.getCertifications());
        userData.put("spokenLanguages", user.getSpokenLanguages());
        
        // Custom fields
        if (user.getCustomFields() != null) {
            try {
                userData.put("customFields", objectMapper.readValue(user.getCustomFields(), Map.class));
            } catch (Exception e) {
                logger.warn("Failed to parse custom fields for user: {}", userId, e);
            }
        }
        
        // Preferences
        userData.put("timezone", user.getTimezone());
        userData.put("language", user.getLanguage());
        userData.put("themePreference", user.getThemePreference());
        userData.put("currencyPreference", user.getCurrencyPreference());
        
        // Activity data
        userData.put("lastActivityAt", user.getLastActivityAt());
        userData.put("lastLoginAt", user.getLastLoginAt());
        userData.put("loginCount", user.getLoginCount());
        userData.put("onboardingCompleted", user.getOnboardingCompleted());
        userData.put("onboardingCompletedAt", user.getOnboardingCompletedAt());
        userData.put("completedTrainings", user.getCompletedTrainings());
        
        // Timestamps
        userData.put("createdAt", user.getCreatedAt());
        userData.put("updatedAt", user.getUpdatedAt());
        
        logger.info("Data transfer prepared for user: {}", userId);
        return userData;
    }

    /**
     * Anonymize user data for GDPR compliance
     */
    public void anonymizeUserData(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Anonymizing user data for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Store original data for audit
        User originalUser = new User();
        // Copy original data for audit logging
        
        // Anonymize personal information
        String anonymizedId = "anon_" + UUID.randomUUID().toString().substring(0, 8);
        user.setEmail(anonymizedId + "@anonymized.local");
        user.setFirstName("Anonymized");
        user.setLastName("User");
        user.setMiddleName(null);
        user.setDisplayName("Anonymized User");
        user.setPhoneNumber(null);
        user.setMobilePhone(null);
        user.setWorkPhone(null);
        user.setBio(null);
        
        // Anonymize address information
        user.setAddressLine1(null);
        user.setAddressLine2(null);
        user.setCity(null);
        user.setStateProvince(null);
        user.setPostalCode(null);
        user.setCountry(null);
        
        // Anonymize personal details
        user.setBirthDate(null);
        user.setEmergencyContactName(null);
        user.setEmergencyContactPhone(null);
        user.setEmergencyContactRelationship(null);
        
        // Clear social profiles
        user.setWebsiteUrl(null);
        user.setLinkedinUrl(null);
        user.setTwitterHandle(null);
        
        // Clear custom fields
        user.setCustomFields(null);
        
        // Update status and metadata
        user.setStatus(User.UserStatus.DELETED);
        user.setUpdatedBy(performedBy);
        
        userRepository.save(user);
        
        // Log anonymization
        userAuditService.logUserUpdate(originalUser, user, performedBy, request);
        
        logger.info("User data anonymized for user: {}", userId);
    }

    /**
     * Schedule user for deletion
     */
    public void scheduleUserDeletion(UUID userId, int gracePeriodDays, UUID performedBy, HttpServletRequest request) {
        logger.info("Scheduling user deletion for user: {} with grace period: {} days", userId, gracePeriodDays);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setDeletionRequested(true);
        user.setDeletionRequestedAt(LocalDateTime.now());
        user.setDeletionScheduledAt(LocalDateTime.now().plusDays(gracePeriodDays));
        user.setUpdatedBy(performedBy);
        
        userRepository.save(user);
        
        logger.info("User deletion scheduled for user: {} on: {}", userId, user.getDeletionScheduledAt());
    }

    /**
     * Cancel scheduled user deletion
     */
    public void cancelUserDeletion(UUID userId, UUID performedBy, HttpServletRequest request) {
        logger.info("Canceling user deletion for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setDeletionRequested(false);
        user.setDeletionRequestedAt(null);
        user.setDeletionScheduledAt(null);
        user.setUpdatedBy(performedBy);
        
        userRepository.save(user);
        
        logger.info("User deletion canceled for user: {}", userId);
    }

    /**
     * Process users scheduled for deletion
     */
    @Async
    public void processScheduledDeletions() {
        logger.info("Processing users scheduled for deletion");
        
        LocalDateTime now = LocalDateTime.now();
        List<User> usersToDelete = userRepository.findUsersScheduledForDeletion(null, now);
        
        for (User user : usersToDelete) {
            try {
                // Prepare data transfer before anonymization
                Map<String, Object> userData = prepareUserDataTransfer(user.getId());
                
                // TODO: Export user data to external system or file
                
                // Anonymize user data
                anonymizeUserData(user.getId(), null, null);
                
                logger.info("Processed scheduled deletion for user: {}", user.getId());
                
            } catch (Exception e) {
                logger.error("Error processing scheduled deletion for user: {}", user.getId(), e);
            }
        }
        
        logger.info("Completed processing {} scheduled deletions", usersToDelete.size());
    }

    /**
     * Get user onboarding status
     */
    public Map<String, Object> getOnboardingStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("currentStep", user.getOnboardingStep());
        status.put("totalSteps", totalOnboardingSteps);
        status.put("isCompleted", user.getOnboardingCompleted());
        status.put("completedAt", user.getOnboardingCompletedAt());
        status.put("progressPercentage", (double) user.getOnboardingStep() / totalOnboardingSteps * 100);
        status.put("completedTrainings", user.getCompletedTrainings());
        
        return status;
    }

    /**
     * Get user activity summary
     */
    public Map<String, Object> getUserActivitySummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("lastActivityAt", user.getLastActivityAt());
        summary.put("lastLoginAt", user.getLastLoginAt());
        summary.put("loginCount", user.getLoginCount());
        summary.put("accountCreatedAt", user.getCreatedAt());
        summary.put("profileLastUpdatedAt", user.getUpdatedAt());
        summary.put("isActive", user.isActive());
        summary.put("onboardingCompleted", user.getOnboardingCompleted());
        
        // Calculate activity metrics
        if (user.getLastActivityAt() != null) {
            long daysSinceLastActivity = java.time.temporal.ChronoUnit.DAYS.between(
                user.getLastActivityAt().toLocalDate(), LocalDateTime.now().toLocalDate());
            summary.put("daysSinceLastActivity", daysSinceLastActivity);
        }
        
        if (user.getCreatedAt() != null) {
            long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                user.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate());
            summary.put("daysSinceCreation", daysSinceCreation);
        }
        
        return summary;
    }
}