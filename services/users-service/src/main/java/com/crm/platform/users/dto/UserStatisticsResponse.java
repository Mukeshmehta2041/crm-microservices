package com.crm.platform.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for user statistics and metrics
 */
public class UserStatisticsResponse {
    
    @JsonProperty("total_users")
    private long totalUsers;
    
    @JsonProperty("active_users")
    private long activeUsers;
    
    @JsonProperty("inactive_users")
    private long inactiveUsers;
    
    @JsonProperty("suspended_users")
    private long suspendedUsers;
    
    @JsonProperty("users_with_incomplete_onboarding")
    private long usersWithIncompleteOnboarding;
    
    @JsonProperty("users_active_last_30_days")
    private long usersActiveLast30Days;
    
    @JsonProperty("users_logged_in_last_30_days")
    private long usersLoggedInLast30Days;
    
    @JsonProperty("new_users_last_30_days")
    private long newUsersLast30Days;
    
    @JsonProperty("users_by_department")
    private List<DepartmentStats> usersByDepartment;
    
    @JsonProperty("users_by_job_title")
    private List<JobTitleStats> usersByJobTitle;
    
    @JsonProperty("users_by_office_location")
    private List<OfficeLocationStats> usersByOfficeLocation;
    
    @JsonProperty("onboarding_completion_rate")
    private double onboardingCompletionRate;
    
    @JsonProperty("average_login_frequency")
    private double averageLoginFrequency;
    
    @JsonProperty("most_active_users")
    private List<UserActivityStats> mostActiveUsers;
    
    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;

    public UserStatisticsResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getInactiveUsers() { return inactiveUsers; }
    public void setInactiveUsers(long inactiveUsers) { this.inactiveUsers = inactiveUsers; }

    public long getSuspendedUsers() { return suspendedUsers; }
    public void setSuspendedUsers(long suspendedUsers) { this.suspendedUsers = suspendedUsers; }

    public long getUsersWithIncompleteOnboarding() { return usersWithIncompleteOnboarding; }
    public void setUsersWithIncompleteOnboarding(long usersWithIncompleteOnboarding) { this.usersWithIncompleteOnboarding = usersWithIncompleteOnboarding; }

    public long getUsersActiveLast30Days() { return usersActiveLast30Days; }
    public void setUsersActiveLast30Days(long usersActiveLast30Days) { this.usersActiveLast30Days = usersActiveLast30Days; }

    public long getUsersLoggedInLast30Days() { return usersLoggedInLast30Days; }
    public void setUsersLoggedInLast30Days(long usersLoggedInLast30Days) { this.usersLoggedInLast30Days = usersLoggedInLast30Days; }

    public long getNewUsersLast30Days() { return newUsersLast30Days; }
    public void setNewUsersLast30Days(long newUsersLast30Days) { this.newUsersLast30Days = newUsersLast30Days; }

    public List<DepartmentStats> getUsersByDepartment() { return usersByDepartment; }
    public void setUsersByDepartment(List<DepartmentStats> usersByDepartment) { this.usersByDepartment = usersByDepartment; }

    public List<JobTitleStats> getUsersByJobTitle() { return usersByJobTitle; }
    public void setUsersByJobTitle(List<JobTitleStats> usersByJobTitle) { this.usersByJobTitle = usersByJobTitle; }

    public List<OfficeLocationStats> getUsersByOfficeLocation() { return usersByOfficeLocation; }
    public void setUsersByOfficeLocation(List<OfficeLocationStats> usersByOfficeLocation) { this.usersByOfficeLocation = usersByOfficeLocation; }

    public double getOnboardingCompletionRate() { return onboardingCompletionRate; }
    public void setOnboardingCompletionRate(double onboardingCompletionRate) { this.onboardingCompletionRate = onboardingCompletionRate; }

    public double getAverageLoginFrequency() { return averageLoginFrequency; }
    public void setAverageLoginFrequency(double averageLoginFrequency) { this.averageLoginFrequency = averageLoginFrequency; }

    public List<UserActivityStats> getMostActiveUsers() { return mostActiveUsers; }
    public void setMostActiveUsers(List<UserActivityStats> mostActiveUsers) { this.mostActiveUsers = mostActiveUsers; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    // Nested classes for statistics
    public static class DepartmentStats {
        @JsonProperty("department")
        private String department;
        
        @JsonProperty("user_count")
        private long userCount;

        public DepartmentStats() {}

        public DepartmentStats(String department, long userCount) {
            this.department = department;
            this.userCount = userCount;
        }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public long getUserCount() { return userCount; }
        public void setUserCount(long userCount) { this.userCount = userCount; }
    }

    public static class JobTitleStats {
        @JsonProperty("job_title")
        private String jobTitle;
        
        @JsonProperty("user_count")
        private long userCount;

        public JobTitleStats() {}

        public JobTitleStats(String jobTitle, long userCount) {
            this.jobTitle = jobTitle;
            this.userCount = userCount;
        }

        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

        public long getUserCount() { return userCount; }
        public void setUserCount(long userCount) { this.userCount = userCount; }
    }

    public static class OfficeLocationStats {
        @JsonProperty("office_location")
        private String officeLocation;
        
        @JsonProperty("user_count")
        private long userCount;

        public OfficeLocationStats() {}

        public OfficeLocationStats(String officeLocation, long userCount) {
            this.officeLocation = officeLocation;
            this.userCount = userCount;
        }

        public String getOfficeLocation() { return officeLocation; }
        public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }

        public long getUserCount() { return userCount; }
        public void setUserCount(long userCount) { this.userCount = userCount; }
    }

    public static class UserActivityStats {
        @JsonProperty("user_id")
        private java.util.UUID userId;
        
        @JsonProperty("full_name")
        private String fullName;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("login_count")
        private long loginCount;
        
        @JsonProperty("last_activity_at")
        private LocalDateTime lastActivityAt;

        public UserActivityStats() {}

        public UserActivityStats(java.util.UUID userId, String fullName, String email, 
                               long loginCount, LocalDateTime lastActivityAt) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.loginCount = loginCount;
            this.lastActivityAt = lastActivityAt;
        }

        public java.util.UUID getUserId() { return userId; }
        public void setUserId(java.util.UUID userId) { this.userId = userId; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public long getLoginCount() { return loginCount; }
        public void setLoginCount(long loginCount) { this.loginCount = loginCount; }

        public LocalDateTime getLastActivityAt() { return lastActivityAt; }
        public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    }
}