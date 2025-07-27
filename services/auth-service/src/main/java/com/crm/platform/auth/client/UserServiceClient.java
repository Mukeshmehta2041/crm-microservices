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

    public UserInfo getUserById(UUID userId) {
        try {
            logger.debug("Fetching user info for ID: {}", userId);
            
            String url = userServiceUrl + "/api/v1/users/" + userId;
            ResponseEntity<UserServiceResponse> response = restTemplate.getForEntity(url, UserServiceResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                UserServiceResponse userResponse = response.getBody();
                return mapToUserInfo(userResponse);
            }
            
            logger.warn("User not found with ID: {}", userId);
            return null;
            
        } catch (RestClientException e) {
            logger.error("Error fetching user info for ID: {}", userId, e);
            return null;
        }
    }

    public UserInfo getUserByEmail(String email) {
        try {
            logger.debug("Fetching user info for email: {}", email);
            
            String url = userServiceUrl + "/api/v1/users/email/" + email;
            ResponseEntity<UserServiceResponse> response = restTemplate.getForEntity(url, UserServiceResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                UserServiceResponse userResponse = response.getBody();
                return mapToUserInfo(userResponse);
            }
            
            logger.warn("User not found with email: {}", email);
            return null;
            
        } catch (RestClientException e) {
            logger.error("Error fetching user info for email: {}", email, e);
            return null;
        }
    }

    private UserInfo mapToUserInfo(UserServiceResponse userResponse) {
        Set<String> roleStrings = userResponse.getRoles() != null ? 
            userResponse.getRoles().stream()
                .map(Enum::name)
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

    // Inner class to match the UserResponse from Users Service
    public static class UserServiceResponse {
        private UUID id;
        private String email;
        private UUID tenantId;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String jobTitle;
        private String department;
        private String profileImageUrl;
        private Set<UserRole> roles;

        // Getters and Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

        public Set<UserRole> getRoles() { return roles; }
        public void setRoles(Set<UserRole> roles) { this.roles = roles; }

        public enum UserRole {
            ADMIN, MANAGER, SALES_REP, MARKETING_USER, SUPPORT_AGENT, VIEWER
        }
    }
}