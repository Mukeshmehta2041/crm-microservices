package com.crm.platform.users.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_users_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(unique = true, nullable = false)
    private String email;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "language", length = 10)
    private String language = "en";

    @Column(name = "date_format", length = 20)
    private String dateFormat = "MM/dd/yyyy";

    @Column(name = "time_format", length = 10)
    private String timeFormat = "12h";

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<UserRole> roles;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted = false;

    @Column(name = "email_notifications_enabled")
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "push_notifications_enabled")
    private Boolean pushNotificationsEnabled = true;

    @Column(name = "sms_notifications_enabled")
    private Boolean smsNotificationsEnabled = false;

    // Extended profile fields
    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "twitter_handle", length = 100)
    private String twitterHandle;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(name = "hire_date")
    private java.time.LocalDate hireDate;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "cost_center", length = 100)
    private String costCenter;

    @Column(name = "office_location", length = 200)
    private String officeLocation;

    @Column(name = "work_phone", length = 20)
    private String workPhone;

    @Column(name = "mobile_phone", length = 20)
    private String mobilePhone;

    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 100)
    private String emergencyContactRelationship;

    // Skills and expertise
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_skills", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "skill", length = 100)
    private Set<String> skills;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_certifications", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "certification", length = 200)
    private Set<String> certifications;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_languages", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "language_code", length = 10)
    private Set<String> spokenLanguages;

    // Custom fields support using JSONB
    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields; // JSON string for flexible custom fields

    // User preferences and settings
    @Column(name = "theme_preference", length = 20)
    private String themePreference = "light"; // light, dark, auto

    @Column(name = "currency_preference", length = 10)
    private String currencyPreference = "USD";

    @Column(name = "number_format", length = 20)
    private String numberFormat = "1,234.56";

    @Column(name = "week_start_day")
    private Integer weekStartDay = 1; // 1 = Monday, 0 = Sunday

    @Column(name = "working_hours_start")
    private java.time.LocalTime workingHoursStart;

    @Column(name = "working_hours_end")
    private java.time.LocalTime workingHoursEnd;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_working_days", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "day_of_week")
    private Set<java.time.DayOfWeek> workingDays;

    // Security and privacy settings
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "profile_visibility", length = 20)
    private String profileVisibility = "TEAM"; // PUBLIC, TEAM, PRIVATE

    @Column(name = "activity_visibility", length = 20)
    private String activityVisibility = "TEAM"; // PUBLIC, TEAM, PRIVATE

    @Column(name = "email_visibility", length = 20)
    private String emailVisibility = "TEAM"; // PUBLIC, TEAM, PRIVATE

    @Column(name = "phone_visibility", length = 20)
    private String phoneVisibility = "TEAM"; // PUBLIC, TEAM, PRIVATE

    // Activity and engagement metrics
    @Column(name = "login_count")
    private Long loginCount = 0L;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_password_change_at")
    private LocalDateTime lastPasswordChangeAt;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    // Onboarding and training
    @Column(name = "onboarding_step")
    private Integer onboardingStep = 0;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_completed_trainings", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "training_id")
    private Set<UUID> completedTrainings;

    // Data retention and compliance
    @Column(name = "data_retention_policy", length = 50)
    private String dataRetentionPolicy;

    @Column(name = "gdpr_consent_given")
    private Boolean gdprConsentGiven = false;

    @Column(name = "gdpr_consent_date")
    private LocalDateTime gdprConsentDate;

    @Column(name = "marketing_consent_given")
    private Boolean marketingConsentGiven = false;

    @Column(name = "marketing_consent_date")
    private LocalDateTime marketingConsentDate;

    @Column(name = "data_export_requested")
    private Boolean dataExportRequested = false;

    @Column(name = "data_export_requested_at")
    private LocalDateTime dataExportRequestedAt;

    @Column(name = "deletion_requested")
    private Boolean deletionRequested = false;

    @Column(name = "deletion_requested_at")
    private LocalDateTime deletionRequestedAt;

    @Column(name = "deletion_scheduled_at")
    private LocalDateTime deletionScheduledAt;

    // Audit trail
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "version")
    private Long version = 1L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public User() {}

    public User(String email, UUID tenantId) {
        this.email = email;
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

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

    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(Boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }

    public Boolean getEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }

    public Boolean getPushNotificationsEnabled() { return pushNotificationsEnabled; }
    public void setPushNotificationsEnabled(Boolean pushNotificationsEnabled) { this.pushNotificationsEnabled = pushNotificationsEnabled; }

    public Boolean getSmsNotificationsEnabled() { return smsNotificationsEnabled; }
    public void setSmsNotificationsEnabled(Boolean smsNotificationsEnabled) { this.smsNotificationsEnabled = smsNotificationsEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Extended profile fields getters and setters
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getTwitterHandle() { return twitterHandle; }
    public void setTwitterHandle(String twitterHandle) { this.twitterHandle = twitterHandle; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateProvince() { return stateProvince; }
    public void setStateProvince(String stateProvince) { this.stateProvince = stateProvince; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public java.time.LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(java.time.LocalDate birthDate) { this.birthDate = birthDate; }

    public java.time.LocalDate getHireDate() { return hireDate; }
    public void setHireDate(java.time.LocalDate hireDate) { this.hireDate = hireDate; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }

    public String getWorkPhone() { return workPhone; }
    public void setWorkPhone(String workPhone) { this.workPhone = workPhone; }

    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String emergencyContactRelationship) { this.emergencyContactRelationship = emergencyContactRelationship; }

    public Set<String> getSkills() { return skills; }
    public void setSkills(Set<String> skills) { this.skills = skills; }

    public Set<String> getCertifications() { return certifications; }
    public void setCertifications(Set<String> certifications) { this.certifications = certifications; }

    public Set<String> getSpokenLanguages() { return spokenLanguages; }
    public void setSpokenLanguages(Set<String> spokenLanguages) { this.spokenLanguages = spokenLanguages; }

    public String getCustomFields() { return customFields; }
    public void setCustomFields(String customFields) { this.customFields = customFields; }

    public String getThemePreference() { return themePreference; }
    public void setThemePreference(String themePreference) { this.themePreference = themePreference; }

    public String getCurrencyPreference() { return currencyPreference; }
    public void setCurrencyPreference(String currencyPreference) { this.currencyPreference = currencyPreference; }

    public String getNumberFormat() { return numberFormat; }
    public void setNumberFormat(String numberFormat) { this.numberFormat = numberFormat; }

    public Integer getWeekStartDay() { return weekStartDay; }
    public void setWeekStartDay(Integer weekStartDay) { this.weekStartDay = weekStartDay; }

    public java.time.LocalTime getWorkingHoursStart() { return workingHoursStart; }
    public void setWorkingHoursStart(java.time.LocalTime workingHoursStart) { this.workingHoursStart = workingHoursStart; }

    public java.time.LocalTime getWorkingHoursEnd() { return workingHoursEnd; }
    public void setWorkingHoursEnd(java.time.LocalTime workingHoursEnd) { this.workingHoursEnd = workingHoursEnd; }

    public Set<java.time.DayOfWeek> getWorkingDays() { return workingDays; }
    public void setWorkingDays(Set<java.time.DayOfWeek> workingDays) { this.workingDays = workingDays; }

    public Boolean getTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getProfileVisibility() { return profileVisibility; }
    public void setProfileVisibility(String profileVisibility) { this.profileVisibility = profileVisibility; }

    public String getActivityVisibility() { return activityVisibility; }
    public void setActivityVisibility(String activityVisibility) { this.activityVisibility = activityVisibility; }

    public String getEmailVisibility() { return emailVisibility; }
    public void setEmailVisibility(String emailVisibility) { this.emailVisibility = emailVisibility; }

    public String getPhoneVisibility() { return phoneVisibility; }
    public void setPhoneVisibility(String phoneVisibility) { this.phoneVisibility = phoneVisibility; }

    public Long getLoginCount() { return loginCount; }
    public void setLoginCount(Long loginCount) { this.loginCount = loginCount; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getLastPasswordChangeAt() { return lastPasswordChangeAt; }
    public void setLastPasswordChangeAt(LocalDateTime lastPasswordChangeAt) { this.lastPasswordChangeAt = lastPasswordChangeAt; }

    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }

    public Integer getOnboardingStep() { return onboardingStep; }
    public void setOnboardingStep(Integer onboardingStep) { this.onboardingStep = onboardingStep; }

    public LocalDateTime getOnboardingCompletedAt() { return onboardingCompletedAt; }
    public void setOnboardingCompletedAt(LocalDateTime onboardingCompletedAt) { this.onboardingCompletedAt = onboardingCompletedAt; }

    public Set<UUID> getCompletedTrainings() { return completedTrainings; }
    public void setCompletedTrainings(Set<UUID> completedTrainings) { this.completedTrainings = completedTrainings; }

    public String getDataRetentionPolicy() { return dataRetentionPolicy; }
    public void setDataRetentionPolicy(String dataRetentionPolicy) { this.dataRetentionPolicy = dataRetentionPolicy; }

    public Boolean getGdprConsentGiven() { return gdprConsentGiven; }
    public void setGdprConsentGiven(Boolean gdprConsentGiven) { this.gdprConsentGiven = gdprConsentGiven; }

    public LocalDateTime getGdprConsentDate() { return gdprConsentDate; }
    public void setGdprConsentDate(LocalDateTime gdprConsentDate) { this.gdprConsentDate = gdprConsentDate; }

    public Boolean getMarketingConsentGiven() { return marketingConsentGiven; }
    public void setMarketingConsentGiven(Boolean marketingConsentGiven) { this.marketingConsentGiven = marketingConsentGiven; }

    public LocalDateTime getMarketingConsentDate() { return marketingConsentDate; }
    public void setMarketingConsentDate(LocalDateTime marketingConsentDate) { this.marketingConsentDate = marketingConsentDate; }

    public Boolean getDataExportRequested() { return dataExportRequested; }
    public void setDataExportRequested(Boolean dataExportRequested) { this.dataExportRequested = dataExportRequested; }

    public LocalDateTime getDataExportRequestedAt() { return dataExportRequestedAt; }
    public void setDataExportRequestedAt(LocalDateTime dataExportRequestedAt) { this.dataExportRequestedAt = dataExportRequestedAt; }

    public Boolean getDeletionRequested() { return deletionRequested; }
    public void setDeletionRequested(Boolean deletionRequested) { this.deletionRequested = deletionRequested; }

    public LocalDateTime getDeletionRequestedAt() { return deletionRequestedAt; }
    public void setDeletionRequestedAt(LocalDateTime deletionRequestedAt) { this.deletionRequestedAt = deletionRequestedAt; }

    public LocalDateTime getDeletionScheduledAt() { return deletionScheduledAt; }
    public void setDeletionScheduledAt(LocalDateTime deletionScheduledAt) { this.deletionScheduledAt = deletionScheduledAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    // Helper methods
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public String getFullName() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName);
        }
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleName);
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName);
        }
        
        return fullName.length() > 0 ? fullName.toString() : email;
    }

    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean hasCompletedOnboarding() {
        return Boolean.TRUE.equals(onboardingCompleted) && onboardingCompletedAt != null;
    }

    public String getPreferredName() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        return firstName != null && !firstName.trim().isEmpty() ? firstName : email;
    }

    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
            address.append(addressLine1);
        }
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine2);
        }
        if (city != null && !city.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(city);
        }
        if (stateProvince != null && !stateProvince.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(stateProvince);
        }
        if (postalCode != null && !postalCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(postalCode);
        }
        if (country != null && !country.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(country);
        }
        return address.toString();
    }

    public void incrementLoginCount() {
        this.loginCount = (this.loginCount == null ? 0L : this.loginCount) + 1;
        this.lastLoginAt = LocalDateTime.now();
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    public void lockAccount(int lockoutMinutes) {
        this.accountLockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, DELETED
    }

    public enum UserRole {
        ADMIN, MANAGER, SALES_REP, MARKETING_USER, SUPPORT_AGENT, VIEWER
    }
}