package com.crm.platform.users.service;

import com.crm.platform.users.entity.User;
import com.crm.platform.users.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for comprehensive profile management including avatar upload,
 * preferences, social profiles, and privacy settings
 */
@Service
@Transactional
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;
    private final UserAuditService userAuditService;
    private final ObjectMapper objectMapper;

    // Configuration properties
    @Value("${app.profile.avatar.upload-path:/uploads/avatars}")
    private String avatarUploadPath;

    @Value("${app.profile.avatar.max-size:5242880}") // 5MB
    private long maxAvatarSize;

    @Value("${app.profile.avatar.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedAvatarTypes;

    @Value("${app.profile.social.validate-urls:true}")
    private boolean validateSocialUrls;

    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]\\d{1,14}$");
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
        "^https?://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-]+/?$");
    private static final Pattern TWITTER_PATTERN = Pattern.compile(
        "^@?[a-zA-Z0-9_]{1,15}$");

    @Autowired
    public ProfileService(UserRepository userRepository, UserAuditService userAuditService) {
        this.userRepository = userRepository;
        this.userAuditService = userAuditService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== Profile Management ====================

    /**
     * Get user profile with privacy filtering
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserProfile(UUID userId, UUID requestingUserId, boolean includePrivateFields) {
        logger.debug("Getting profile for user: {} requested by: {}", userId, requestingUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Map<String, Object> profile = new HashMap<>();

        // Basic information (always visible)
        profile.put("id", user.getId());
        profile.put("displayName", user.getDisplayName());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("fullName", user.getFullName());
        profile.put("profileImageUrl", user.getProfileImageUrl());
        profile.put("jobTitle", user.getJobTitle());
        profile.put("department", user.getDepartment());

        // Apply privacy filtering
        boolean canViewPrivateInfo = canViewPrivateProfile(user, requestingUserId);
        boolean canViewContactInfo = canViewContactInfo(user, requestingUserId);

        // Contact information (based on privacy settings)
        if (canViewContactInfo || includePrivateFields) {
            if (shouldShowField(user.getEmailVisibility(), requestingUserId, userId)) {
                profile.put("email", user.getEmail());
            }
            if (shouldShowField(user.getPhoneVisibility(), requestingUserId, userId)) {
                profile.put("phoneNumber", user.getPhoneNumber());
                profile.put("mobilePhone", user.getMobilePhone());
                profile.put("workPhone", user.getWorkPhone());
            }
        }

        // Professional information
        profile.put("employeeId", user.getEmployeeId());
        profile.put("managerId", user.getManagerId());
        profile.put("teamId", user.getTeamId());
        profile.put("officeLocation", user.getOfficeLocation());
        profile.put("costCenter", user.getCostCenter());
        profile.put("hireDate", user.getHireDate());

        // Skills and expertise
        profile.put("skills", user.getSkills());
        profile.put("certifications", user.getCertifications());
        profile.put("spokenLanguages", user.getSpokenLanguages());

        // Social profiles
        profile.put("websiteUrl", user.getWebsiteUrl());
        profile.put("linkedinUrl", user.getLinkedinUrl());
        profile.put("twitterHandle", user.getTwitterHandle());

        // Bio and personal information
        if (canViewPrivateInfo || includePrivateFields) {
            profile.put("bio", user.getBio());
            profile.put("middleName", user.getMiddleName());
        }

        // Address information (private)
        if (canViewPrivateInfo || includePrivateFields) {
            profile.put("address", Map.of(
                "line1", user.getAddressLine1(),
                "line2", user.getAddressLine2(),
                "city", user.getCity(),
                "stateProvince", user.getStateProvince(),
                "postalCode", user.getPostalCode(),
                "country", user.getCountry(),
                "fullAddress", user.getFullAddress()
            ));
        }

        // Preferences and settings
        if (canViewPrivateInfo || includePrivateFields) {
            profile.put("preferences", getUserPreferences(userId));
        }

        // Activity information
        if (shouldShowField(user.getActivityVisibility(), requestingUserId, userId)) {
            profile.put("lastActivityAt", user.getLastActivityAt());
            profile.put("onboardingCompleted", user.getOnboardingCompleted());
        }

        // Custom fields
        if (user.getCustomFields() != null) {
            try {
                Map<String, Object> customFields = objectMapper.readValue(user.getCustomFields(), Map.class);
                profile.put("customFields", customFields);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse custom fields for user: {}", userId, e);
            }
        }

        // Metadata
        profile.put("profileVisibility", user.getProfileVisibility());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("updatedAt", user.getUpdatedAt());

        return profile;
    }

    /**
     * Update user profile with validation
     */
    public User updateProfile(UUID userId, Map<String, Object> updates, UUID updatedBy, HttpServletRequest request) {
        logger.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        User originalUser = cloneUser(user); // For audit logging

        // Update basic information
        updateIfPresent(updates, "firstName", value -> user.setFirstName((String) value));
        updateIfPresent(updates, "lastName", value -> user.setLastName((String) value));
        updateIfPresent(updates, "middleName", value -> user.setMiddleName((String) value));
        updateIfPresent(updates, "displayName", value -> user.setDisplayName((String) value));
        updateIfPresent(updates, "bio", value -> user.setBio((String) value));

        // Update contact information with validation
        updateIfPresent(updates, "phoneNumber", value -> {
            validatePhoneNumber((String) value);
            user.setPhoneNumber((String) value);
        });
        updateIfPresent(updates, "mobilePhone", value -> {
            validatePhoneNumber((String) value);
            user.setMobilePhone((String) value);
        });
        updateIfPresent(updates, "workPhone", value -> {
            validatePhoneNumber((String) value);
            user.setWorkPhone((String) value);
        });

        // Update professional information
        updateIfPresent(updates, "jobTitle", value -> user.setJobTitle((String) value));
        updateIfPresent(updates, "department", value -> user.setDepartment((String) value));
        updateIfPresent(updates, "employeeId", value -> user.setEmployeeId((String) value));
        updateIfPresent(updates, "costCenter", value -> user.setCostCenter((String) value));
        updateIfPresent(updates, "officeLocation", value -> user.setOfficeLocation((String) value));

        // Update address information
        updateIfPresent(updates, "addressLine1", value -> user.setAddressLine1((String) value));
        updateIfPresent(updates, "addressLine2", value -> user.setAddressLine2((String) value));
        updateIfPresent(updates, "city", value -> user.setCity((String) value));
        updateIfPresent(updates, "stateProvince", value -> user.setStateProvince((String) value));
        updateIfPresent(updates, "postalCode", value -> user.setPostalCode((String) value));
        updateIfPresent(updates, "country", value -> user.setCountry((String) value));

        // Update social profiles with validation
        updateIfPresent(updates, "websiteUrl", value -> {
            validateUrl((String) value);
            user.setWebsiteUrl((String) value);
        });
        updateIfPresent(updates, "linkedinUrl", value -> {
            validateLinkedInUrl((String) value);
            user.setLinkedinUrl((String) value);
        });
        updateIfPresent(updates, "twitterHandle", value -> {
            validateTwitterHandle((String) value);
            user.setTwitterHandle((String) value);
        });

        // Update skills and certifications
        updateIfPresent(updates, "skills", value -> {
            @SuppressWarnings("unchecked")
            Set<String> skills = new HashSet<>((List<String>) value);
            user.setSkills(skills);
        });
        updateIfPresent(updates, "certifications", value -> {
            @SuppressWarnings("unchecked")
            Set<String> certifications = new HashSet<>((List<String>) value);
            user.setCertifications(certifications);
        });
        updateIfPresent(updates, "spokenLanguages", value -> {
            @SuppressWarnings("unchecked")
            Set<String> languages = new HashSet<>((List<String>) value);
            user.setSpokenLanguages(languages);
        });

        // Update privacy settings
        updateIfPresent(updates, "profileVisibility", value -> user.setProfileVisibility((String) value));
        updateIfPresent(updates, "activityVisibility", value -> user.setActivityVisibility((String) value));
        updateIfPresent(updates, "emailVisibility", value -> user.setEmailVisibility((String) value));
        updateIfPresent(updates, "phoneVisibility", value -> user.setPhoneVisibility((String) value));

        user.setUpdatedBy(updatedBy);
        User updatedUser = userRepository.save(user);

        // Log audit trail
        userAuditService.logUserUpdate(originalUser, updatedUser, updatedBy, request);

        logger.info("Profile updated successfully for user: {}", userId);
        return updatedUser;
    }

    // ==================== Avatar Management ====================

    /**
     * Upload and process avatar image
     */
    public String uploadAvatar(UUID userId, MultipartFile file, UUID uploadedBy, HttpServletRequest request) {
        logger.info("Uploading avatar for user: {}", userId);

        // Validate file
        validateAvatarFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        try {
            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(avatarUploadPath);
            Files.createDirectories(uploadDir);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf('.')) : ".jpg";
            String filename = userId + "_" + System.currentTimeMillis() + extension;
            Path filePath = uploadDir.resolve(filename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update user profile
            String avatarUrl = "/uploads/avatars/" + filename;
            String oldAvatarUrl = user.getProfileImageUrl();
            user.setProfileImageUrl(avatarUrl);
            user.setUpdatedBy(uploadedBy);
            userRepository.save(user);

            // Delete old avatar if exists
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                deleteOldAvatar(oldAvatarUrl);
            }

            logger.info("Avatar uploaded successfully for user: {}", userId);
            return avatarUrl;

        } catch (IOException e) {
            logger.error("Failed to upload avatar for user: {}", userId, e);
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage());
        }
    }

    /**
     * Delete user avatar
     */
    public void deleteAvatar(UUID userId, UUID deletedBy, HttpServletRequest request) {
        logger.info("Deleting avatar for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        String avatarUrl = user.getProfileImageUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            user.setProfileImageUrl(null);
            user.setUpdatedBy(deletedBy);
            userRepository.save(user);

            deleteOldAvatar(avatarUrl);
            logger.info("Avatar deleted successfully for user: {}", userId);
        }
    }

    // ==================== Preferences Management ====================

    /**
     * Get user preferences
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Map<String, Object> preferences = new HashMap<>();

        // UI preferences
        preferences.put("theme", user.getThemePreference());
        preferences.put("language", user.getLanguage());
        preferences.put("timezone", user.getTimezone());
        preferences.put("dateFormat", user.getDateFormat());
        preferences.put("timeFormat", user.getTimeFormat());
        preferences.put("currency", user.getCurrencyPreference());
        preferences.put("numberFormat", user.getNumberFormat());
        preferences.put("weekStartDay", user.getWeekStartDay());

        // Working hours
        preferences.put("workingHours", Map.of(
            "start", user.getWorkingHoursStart(),
            "end", user.getWorkingHoursEnd(),
            "days", user.getWorkingDays()
        ));

        // Notification preferences
        preferences.put("notifications", Map.of(
            "email", user.getEmailNotificationsEnabled(),
            "push", user.getPushNotificationsEnabled(),
            "sms", user.getSmsNotificationsEnabled()
        ));

        // Privacy preferences
        preferences.put("privacy", Map.of(
            "profile", user.getProfileVisibility(),
            "activity", user.getActivityVisibility(),
            "email", user.getEmailVisibility(),
            "phone", user.getPhoneVisibility()
        ));

        return preferences;
    }

    /**
     * Update user preferences
     */
    public Map<String, Object> updatePreferences(UUID userId, Map<String, Object> preferences, 
                                                UUID updatedBy, HttpServletRequest request) {
        logger.info("Updating preferences for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Update UI preferences
        updateIfPresent(preferences, "theme", value -> user.setThemePreference((String) value));
        updateIfPresent(preferences, "language", value -> user.setLanguage((String) value));
        updateIfPresent(preferences, "timezone", value -> user.setTimezone((String) value));
        updateIfPresent(preferences, "dateFormat", value -> user.setDateFormat((String) value));
        updateIfPresent(preferences, "timeFormat", value -> user.setTimeFormat((String) value));
        updateIfPresent(preferences, "currency", value -> user.setCurrencyPreference((String) value));
        updateIfPresent(preferences, "numberFormat", value -> user.setNumberFormat((String) value));
        updateIfPresent(preferences, "weekStartDay", value -> user.setWeekStartDay((Integer) value));

        // Update working hours
        if (preferences.containsKey("workingHours")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> workingHours = (Map<String, Object>) preferences.get("workingHours");
            updateIfPresent(workingHours, "start", value -> 
                user.setWorkingHoursStart(java.time.LocalTime.parse((String) value)));
            updateIfPresent(workingHours, "end", value -> 
                user.setWorkingHoursEnd(java.time.LocalTime.parse((String) value)));
            updateIfPresent(workingHours, "days", value -> {
                @SuppressWarnings("unchecked")
                List<String> days = (List<String>) value;
                Set<java.time.DayOfWeek> workingDays = days.stream()
                    .map(java.time.DayOfWeek::valueOf)
                    .collect(java.util.stream.Collectors.toSet());
                user.setWorkingDays(workingDays);
            });
        }

        // Update notification preferences
        if (preferences.containsKey("notifications")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> notifications = (Map<String, Object>) preferences.get("notifications");
            updateIfPresent(notifications, "email", value -> user.setEmailNotificationsEnabled((Boolean) value));
            updateIfPresent(notifications, "push", value -> user.setPushNotificationsEnabled((Boolean) value));
            updateIfPresent(notifications, "sms", value -> user.setSmsNotificationsEnabled((Boolean) value));
        }

        // Update privacy preferences
        if (preferences.containsKey("privacy")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> privacy = (Map<String, Object>) preferences.get("privacy");
            updateIfPresent(privacy, "profile", value -> user.setProfileVisibility((String) value));
            updateIfPresent(privacy, "activity", value -> user.setActivityVisibility((String) value));
            updateIfPresent(privacy, "email", value -> user.setEmailVisibility((String) value));
            updateIfPresent(privacy, "phone", value -> user.setPhoneVisibility((String) value));
        }

        user.setUpdatedBy(updatedBy);
        userRepository.save(user);

        logger.info("Preferences updated successfully for user: {}", userId);
        return getUserPreferences(userId);
    }

    // ==================== Privacy and Visibility Control ====================

    /**
     * Update privacy settings
     */
    public Map<String, Object> updatePrivacySettings(UUID userId, Map<String, String> privacySettings, 
                                                    UUID updatedBy, HttpServletRequest request) {
        logger.info("Updating privacy settings for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Validate privacy values
        Set<String> validValues = Set.of("PUBLIC", "TEAM", "PRIVATE");

        updateIfPresent(privacySettings, "profile", value -> {
            if (!validValues.contains(value)) {
                throw new IllegalArgumentException("Invalid profile visibility: " + value);
            }
            user.setProfileVisibility((String) value);
        });

        updateIfPresent(privacySettings, "activity", value -> {
            if (!validValues.contains(value)) {
                throw new IllegalArgumentException("Invalid activity visibility: " + value);
            }
            user.setActivityVisibility((String) value);
        });

        updateIfPresent(privacySettings, "email", value -> {
            if (!validValues.contains(value)) {
                throw new IllegalArgumentException("Invalid email visibility: " + value);
            }
            user.setEmailVisibility((String) value);
        });

        updateIfPresent(privacySettings, "phone", value -> {
            if (!validValues.contains(value)) {
                throw new IllegalArgumentException("Invalid phone visibility: " + value);
            }
            user.setPhoneVisibility((String) value);
        });

        user.setUpdatedBy(updatedBy);
        userRepository.save(user);

        logger.info("Privacy settings updated successfully for user: {}", userId);

        return Map.of(
            "profile", user.getProfileVisibility(),
            "activity", user.getActivityVisibility(),
            "email", user.getEmailVisibility(),
            "phone", user.getPhoneVisibility()
        );
    }

    // ==================== Private Helper Methods ====================

    private void validateAvatarFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file cannot be empty");
        }

        if (file.getSize() > maxAvatarSize) {
            throw new IllegalArgumentException("Avatar file size exceeds maximum allowed size");
        }

        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(allowedAvatarTypes.split(",")).contains(contentType)) {
            throw new IllegalArgumentException("Invalid avatar file type. Allowed types: " + allowedAvatarTypes);
        }
    }

    private void deleteOldAvatar(String avatarUrl) {
        try {
            if (avatarUrl.startsWith("/uploads/avatars/")) {
                String filename = avatarUrl.substring("/uploads/avatars/".length());
                Path filePath = Paths.get(avatarUploadPath, filename);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete old avatar: {}", avatarUrl, e);
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty() && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }

    private void validateUrl(String url) {
        if (validateSocialUrls && url != null && !url.isEmpty() && !URL_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException("Invalid URL format");
        }
    }

    private void validateLinkedInUrl(String linkedinUrl) {
        if (validateSocialUrls && linkedinUrl != null && !linkedinUrl.isEmpty() && 
            !LINKEDIN_PATTERN.matcher(linkedinUrl).matches()) {
            throw new IllegalArgumentException("Invalid LinkedIn URL format");
        }
    }

    private void validateTwitterHandle(String twitterHandle) {
        if (twitterHandle != null && !twitterHandle.isEmpty() && !TWITTER_PATTERN.matcher(twitterHandle).matches()) {
            throw new IllegalArgumentException("Invalid Twitter handle format");
        }
    }

    private boolean canViewPrivateProfile(User user, UUID requestingUserId) {
        if (user.getId().equals(requestingUserId)) {
            return true; // User can always view their own profile
        }

        String visibility = user.getProfileVisibility();
        if ("PUBLIC".equals(visibility)) {
            return true;
        }

        if ("TEAM".equals(visibility)) {
            // Check if users are in the same team (simplified logic)
            return user.getTeamId() != null && 
                   userRepository.findById(requestingUserId)
                       .map(requestingUser -> user.getTeamId().equals(requestingUser.getTeamId()))
                       .orElse(false);
        }

        return false; // PRIVATE visibility
    }

    private boolean canViewContactInfo(User user, UUID requestingUserId) {
        return canViewPrivateProfile(user, requestingUserId);
    }

    private boolean shouldShowField(String fieldVisibility, UUID requestingUserId, UUID profileUserId) {
        if (profileUserId.equals(requestingUserId)) {
            return true; // User can always see their own fields
        }

        if ("PUBLIC".equals(fieldVisibility)) {
            return true;
        }

        if ("TEAM".equals(fieldVisibility)) {
            // Simplified team check - in real implementation, check team membership
            return true; // For now, allow team visibility
        }

        return false; // PRIVATE visibility
    }

    private void updateIfPresent(Map<String, Object> updates, String key, java.util.function.Consumer<Object> updater) {
        if (updates.containsKey(key)) {
            Object value = updates.get(key);
            if (value != null) {
                updater.accept(value);
            }
        }
    }

    private User cloneUser(User user) {
        // Create a shallow clone for audit purposes
        User clone = new User();
        clone.setId(user.getId());
        clone.setFirstName(user.getFirstName());
        clone.setLastName(user.getLastName());
        clone.setEmail(user.getEmail());
        // Add other fields as needed for audit
        return clone;
    }
}