package com.crm.platform.tenant.dto;

import jakarta.validation.constraints.*;

public class TenantCreateRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 255, message = "Tenant name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Subdomain is required")
    @Size(min = 3, max = 100, message = "Subdomain must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", 
             message = "Subdomain must contain only lowercase letters, numbers, and hyphens")
    private String subdomain;

    @Email(message = "Valid contact email is required")
    @NotBlank(message = "Contact email is required")
    private String contactEmail;

    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    private String contactPhone;

    @Email(message = "Valid billing email format required")
    private String billingEmail;

    @Size(max = 255, message = "Custom domain must not exceed 255 characters")
    private String customDomain;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Primary color must be a valid hex color")
    private String primaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Secondary color must be a valid hex color")
    private String secondaryColor;

    @Min(value = 1, message = "Max users must be at least 1")
    @Max(value = 10000, message = "Max users cannot exceed 10000")
    private Integer maxUsers = 10;

    @Min(value = 1, message = "Max storage must be at least 1 GB")
    @Max(value = 10000, message = "Max storage cannot exceed 10000 GB")
    private Integer maxStorageGb = 100;

    // Constructors
    public TenantCreateRequest() {}

    public TenantCreateRequest(String name, String subdomain, String contactEmail) {
        this.name = name;
        this.subdomain = subdomain;
        this.contactEmail = contactEmail;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(String billingEmail) { this.billingEmail = billingEmail; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

    public Integer getMaxStorageGb() { return maxStorageGb; }
    public void setMaxStorageGb(Integer maxStorageGb) { this.maxStorageGb = maxStorageGb; }
}