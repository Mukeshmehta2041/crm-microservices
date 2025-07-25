package com.programming.auth.entity;

import com.programming.common.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "oauth_applications", indexes = {
    @Index(name = "idx_oauth_applications_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_oauth_applications_client_id", columnList = "clientId"),
    @Index(name = "idx_oauth_applications_is_active", columnList = "isActive")
})
public class OAuthApplication extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "client_secret_hash", nullable = false)
    private String clientSecretHash;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_type", nullable = false, length = 50)
    private ApplicationType applicationType;

    @Column(name = "redirect_uris", columnDefinition = "TEXT")
    private String redirectUris;

    @Column(name = "scopes", columnDefinition = "TEXT")
    private String scopes;

    @Column(name = "is_trusted", nullable = false)
    private Boolean isTrusted = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Pattern(regexp = "^https?://.*", message = "Invalid URL format")
    @Size(max = 500)
    @Column(name = "logo_url")
    private String logoUrl;

    @Pattern(regexp = "^https?://.*", message = "Invalid URL format")
    @Size(max = 500)
    @Column(name = "website_url")
    private String websiteUrl;

    @Pattern(regexp = "^https?://.*", message = "Invalid URL format")
    @Size(max = 500)
    @Column(name = "privacy_policy_url")
    private String privacyPolicyUrl;

    @Pattern(regexp = "^https?://.*", message = "Invalid URL format")
    @Size(max = 500)
    @Column(name = "terms_of_service_url")
    private String termsOfServiceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false, insertable = false)
    private User createdByUser;

    public enum ApplicationType {
        WEB, NATIVE, SERVICE
    }

    // Constructors
    public OAuthApplication() {
        super();
    }

    public OAuthApplication(String clientId, String name, ApplicationType applicationType, Tenant tenant) {
        super(tenant.getId());
        this.clientId = clientId;
        this.name = name;
        this.applicationType = applicationType;
        this.tenant = tenant;
    }

    // Getters and Setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public Boolean getIsTrusted() {
        return isTrusted;
    }

    public void setIsTrusted(Boolean isTrusted) {
        this.isTrusted = isTrusted;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getPrivacyPolicyUrl() {
        return privacyPolicyUrl;
    }

    public void setPrivacyPolicyUrl(String privacyPolicyUrl) {
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    public String getTermsOfServiceUrl() {
        return termsOfServiceUrl;
    }

    public void setTermsOfServiceUrl(String termsOfServiceUrl) {
        this.termsOfServiceUrl = termsOfServiceUrl;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
        if (tenant != null) {
            setTenantId(tenant.getId());
        }
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }}
