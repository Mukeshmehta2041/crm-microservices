package com.crm.platform.users.service;

import com.crm.platform.users.dto.CreateUserRequest;
import com.crm.platform.users.dto.UpdateUserRequest;
import com.crm.platform.users.dto.UserResponse;
import com.crm.platform.users.entity.User;
import com.crm.platform.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}