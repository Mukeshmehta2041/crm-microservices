package com.crm.platform.auth.client;

import com.crm.platform.auth.dto.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceClient(RestTemplate restTemplate, 
                           @Value("${services.user-service.url:http://localhost:8082}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @SuppressWarnings("unchecked")
    public UserInfo getUserById(UUID userId) {
        try {
            logger.debug("Fetching user info for ID: {}", userId);
            
            String url = userServiceUrl + "/api/v1/users/" + userId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> apiResponse = response.getBody();
                Boolean success = (Boolean) apiResponse.get("success");
                
                if (Boolean.TRUE.equals(success)) {
                    Map<String, Object> userData = (Map<String, Object>) apiResponse.get("data");
                    if (userData != null) {
                        return mapFromUserData(userData);
                    }
                }
            }
            
            logger.warn("User not found with ID: {}", userId);
            return null;
            
        } catch (RestClientException e) {
            logger.error("Error fetching user info for ID: {}", userId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public UserInfo createUser(com.crm.platform.auth.dto.CreateUserRequest request) {
        try {
            logger.debug("Creating user with email: {}", request.getEmail());
            
            String url = userServiceUrl + "/api/v1/users";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> apiResponse = response.getBody();
                Boolean success = (Boolean) apiResponse.get("success");
                
                if (Boolean.TRUE.equals(success)) {
                    Map<String, Object> userData = (Map<String, Object>) apiResponse.get("data");
                    if (userData != null) {
                        String idStr = (String) userData.get("id");
                        UUID userId = UUID.fromString(idStr);
                        String email = (String) userData.get("email");
                        String firstName = (String) userData.get("firstName");
                        String lastName = (String) userData.get("lastName");
                        String phoneNumber = (String) userData.get("phoneNumber");
                        String jobTitle = (String) userData.get("jobTitle");
                        String department = (String) userData.get("department");
                        String profileImageUrl = (String) userData.get("profileImageUrl");
                        String tenantIdStr = (String) userData.get("tenantId");
                        UUID tenantId = UUID.fromString(tenantIdStr);
                        
                        UserInfo userInfo = new UserInfo(userId, email, firstName, lastName, 
                                                       phoneNumber, jobTitle, department, 
                                                       profileImageUrl, Set.of(), tenantId);
                        logger.info("User created successfully with ID: {}", userId);
                        return userInfo;
                    }
                }
            }
            
            logger.error("Failed to create user with email: {}. Status: {}, Response: {}", 
                        request.getEmail(), response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to create user: HTTP " + response.getStatusCode());
            
        } catch (Exception e) {
            logger.error("Error creating user with email: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public UserInfo getUserByEmail(String email) {
        try {
            logger.debug("Fetching user info for email: {}", email);
            
            String url = userServiceUrl + "/api/v1/users/email/" + email;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> apiResponse = response.getBody();
                Boolean success = (Boolean) apiResponse.get("success");
                
                if (Boolean.TRUE.equals(success)) {
                    Map<String, Object> userData = (Map<String, Object>) apiResponse.get("data");
                    if (userData != null) {
                        return mapFromUserData(userData);
                    }
                }
            }
            
            logger.warn("User not found with email: {}", email);
            return null;
            
        } catch (RestClientException e) {
            logger.error("Error fetching user info for email: {}", email, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private UserInfo mapFromUserData(Map<String, Object> userData) {
        try {
            UUID id = UUID.fromString((String) userData.get("id"));
            String email = (String) userData.get("email");
            String firstName = (String) userData.get("firstName");
            String lastName = (String) userData.get("lastName");
            String phoneNumber = (String) userData.get("phoneNumber");
            String jobTitle = (String) userData.get("jobTitle");
            String department = (String) userData.get("department");
            String profileImageUrl = (String) userData.get("profileImageUrl");
            UUID tenantId = UUID.fromString((String) userData.get("tenantId"));
            
            // Handle roles - they might be a list of strings or objects
            Set<String> roleStrings = Set.of();
            Object rolesObj = userData.get("roles");
            if (rolesObj instanceof java.util.List) {
                java.util.List<?> rolesList = (java.util.List<?>) rolesObj;
                roleStrings = rolesList.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            }
            
            return new UserInfo(id, email, firstName, lastName, phoneNumber, 
                              jobTitle, department, profileImageUrl, roleStrings, tenantId);
                              
        } catch (Exception e) {
            logger.error("Error mapping user data: {}", userData, e);
            return null;
        }
    }

    private UserInfo mapToUserInfo(UserServiceResponse userResponse) {
        Set<String> roleStrings = userResponse.getRoles() != null ? 
            userResponse.getRoles().stream()
                .map(role -> role.name())
                .collect(Collectors.toSet()) : 
            Set.of();

        return new UserInfo(
            userResponse.getId(),
            userResponse.getEmail(),
            userResponse.getFirstName(),
            userResponse.getLastName(),
            userResponse.getPhoneNumber(),
            userResponse.getJobTitle(),
            userResponse.getDepartment(),
            userResponse.getProfileImageUrl(),
            roleStrings,
            userResponse.getTenantId()
        );
    }

    // Inner class to handle ApiResponse wrapper
    public static class ApiResponseWrapper {
        private boolean success;
        private UserServiceResponse data;
        private Object meta;
        private Object errors;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public UserServiceResponse getData() { return data; }
        public void setData(UserServiceResponse data) { this.data = data; }

        public Object getMeta() { return meta; }
        public void setMeta(Object meta) { this.meta = meta; }

        public Object getErrors() { return errors; }
        public void setErrors(Object errors) { this.errors = errors; }
    }

    // Inner class to match the UserResponse from Users Service
    public static class UserServiceResponse {
        private UUID id;
        private String email;
        private UUID tenantId;
        private String status;
        private String firstName;
        private String lastName;
        private String fullName;
        private String phoneNumber;
        private String jobTitle;
        private String department;
        private String profileImageUrl;
        private String timezone;
        private String language;
        private String dateFormat;
        private String timeFormat;
        private Set<UserRole> roles;
        private UUID managerId;
        private UUID teamId;
        private String lastActivityAt;
        private Boolean onboardingCompleted;
        private Boolean emailNotificationsEnabled;
        private Boolean pushNotificationsEnabled;
        private Boolean smsNotificationsEnabled;
        private String createdAt;
        private String updatedAt;

        // Getters and Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

        public String getTimeFormat() { return timeFormat; }
        public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }

        public Set<UserRole> getRoles() { return roles; }
        public void setRoles(Set<UserRole> roles) { this.roles = roles; }

        public UUID getManagerId() { return managerId; }
        public void setManagerId(UUID managerId) { this.managerId = managerId; }

        public UUID getTeamId() { return teamId; }
        public void setTeamId(UUID teamId) { this.teamId = teamId; }

        public String getLastActivityAt() { return lastActivityAt; }
        public void setLastActivityAt(String lastActivityAt) { this.lastActivityAt = lastActivityAt; }

        public Boolean getOnboardingCompleted() { return onboardingCompleted; }
        public void setOnboardingCompleted(Boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }

        public Boolean getEmailNotificationsEnabled() { return emailNotificationsEnabled; }
        public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }

        public Boolean getPushNotificationsEnabled() { return pushNotificationsEnabled; }
        public void setPushNotificationsEnabled(Boolean pushNotificationsEnabled) { this.pushNotificationsEnabled = pushNotificationsEnabled; }

        public Boolean getSmsNotificationsEnabled() { return smsNotificationsEnabled; }
        public void setSmsNotificationsEnabled(Boolean smsNotificationsEnabled) { this.smsNotificationsEnabled = smsNotificationsEnabled; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public enum UserRole {
            ADMIN, MANAGER, SALES_REP, MARKETING_USER, SUPPORT_AGENT, VIEWER
        }
    }
}