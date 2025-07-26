package com.crm.platform.tenant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenants_subdomain", columnList = "subdomain", unique = true),
    @Index(name = "idx_tenants_status", columnList = "status"),
    @Index(name = "idx_tenants_plan_type", columnList = "plan_type")
})
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 255)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Size(min = 3, max = 100)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", message = "Subdomain must contain only lowercase letters, numbers, and hyphens")
    @Column(unique = true, nullable = false, length = 100)
    private String subdomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 50)
    private PlanType planType = PlanType.BASIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "max_users")
    private Integer maxUsers = 10;

    @Column(name = "max_storage_gb")
    private Integer maxStorageGb = 100;

    @Column(name = "custom_domain", length = 255)
    private String customDomain;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "primary_color", length = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Primary color must be a valid hex color")
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Secondary color must be a valid hex color")
    private String secondaryColor;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "is_trial")
    private Boolean isTrial = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Tenant() {}

    public Tenant(String name, String subdomain) {
        this.name = name;
        this.subdomain = subdomain;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

    public PlanType getPlanType() { return planType; }
    public void setPlanType(PlanType planType) { this.planType = planType; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

    public Integer getMaxStorageGb() { return maxStorageGb; }
    public void setMaxStorageGb(Integer maxStorageGb) { this.maxStorageGb = maxStorageGb; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(String billingEmail) { this.billingEmail = billingEmail; }

    public LocalDateTime getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
    public void setSubscriptionExpiresAt(LocalDateTime subscriptionExpiresAt) { this.subscriptionExpiresAt = subscriptionExpiresAt; }

    public LocalDateTime getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(LocalDateTime trialEndsAt) { this.trialEndsAt = trialEndsAt; }

    public Boolean getIsTrial() { return isTrial; }
    public void setIsTrial(Boolean isTrial) { this.isTrial = isTrial; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public boolean isSubscriptionExpired() {
        return subscriptionExpiresAt != null && subscriptionExpiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isTrialExpired() {
        return trialEndsAt != null && trialEndsAt.isBefore(LocalDateTime.now());
    }

    public boolean canCreateUsers(int currentUserCount) {
        return maxUsers == null || currentUserCount < maxUsers;
    }

    public enum TenantStatus {
        ACTIVE, SUSPENDED, CANCELLED, PENDING_ACTIVATION
    }

    public enum PlanType {
        BASIC, PROFESSIONAL, ENTERPRISE, CUSTOM
    }
}