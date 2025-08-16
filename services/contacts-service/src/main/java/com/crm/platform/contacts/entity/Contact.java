package com.crm.platform.contacts.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contacts_tenant_email", columnList = "tenant_id, email"),
    @Index(name = "idx_contacts_tenant_name", columnList = "tenant_id, last_name, first_name"),
    @Index(name = "idx_contacts_account_id", columnList = "account_id"),
    @Index(name = "idx_contacts_owner", columnList = "owner_id"),
    @Index(name = "idx_contacts_status", columnList = "tenant_id, contact_status"),
    @Index(name = "idx_contacts_lead_score", columnList = "tenant_id, lead_score"),
    @Index(name = "idx_contacts_created_at", columnList = "tenant_id, created_at"),
    @Index(name = "idx_contacts_updated_at", columnList = "tenant_id, updated_at")
})
public class Contact {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "account_id")
    private UUID accountId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Email
    @Size(max = 255)
    @Column(name = "email", length = 255)
    private String email;

    @Size(max = 50)
    @Column(name = "phone", length = 50)
    private String phone;

    @Size(max = 50)
    @Column(name = "mobile", length = 50)
    private String mobile;

    @Size(max = 100)
    @Column(name = "title", length = 100)
    private String title;

    @Size(max = 100)
    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "mailing_address", columnDefinition = "jsonb")
    private Map<String, Object> mailingAddress;

    @Column(name = "social_profiles", columnDefinition = "jsonb")
    private Map<String, Object> socialProfiles;

    @Size(max = 100)
    @Column(name = "lead_source", length = 100)
    private String leadSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_status", length = 50)
    private ContactStatus contactStatus = ContactStatus.ACTIVE;

    @Column(name = "lead_score")
    private Integer leadScore = 0;

    @Column(name = "do_not_call")
    private Boolean doNotCall = false;

    @Column(name = "do_not_email")
    private Boolean doNotEmail = false;

    @Column(name = "email_opt_out")
    private Boolean emailOptOut = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact_method", length = 20)
    private ContactMethod preferredContactMethod = ContactMethod.EMAIL;

    @Size(max = 50)
    @Column(name = "timezone", length = 50)
    private String timezone;

    @Size(max = 10)
    @Column(name = "language", length = 10)
    private String language = "en-US";

    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags = new ArrayList<>();

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private Map<String, Object> customFields;

    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotNull
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @NotNull
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ContactRelationship> relationships = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (leadScore == null) {
            leadScore = 0;
        }
        if (contactStatus == null) {
            contactStatus = ContactStatus.ACTIVE;
        }
        if (preferredContactMethod == null) {
            preferredContactMethod = ContactMethod.EMAIL;
        }
        if (doNotCall == null) {
            doNotCall = false;
        }
        if (doNotEmail == null) {
            doNotEmail = false;
        }
        if (emailOptOut == null) {
            emailOptOut = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Map<String, Object> getMailingAddress() {
        return mailingAddress;
    }

    public void setMailingAddress(Map<String, Object> mailingAddress) {
        this.mailingAddress = mailingAddress;
    }

    public Map<String, Object> getSocialProfiles() {
        return socialProfiles;
    }

    public void setSocialProfiles(Map<String, Object> socialProfiles) {
        this.socialProfiles = socialProfiles;
    }

    public String getLeadSource() {
        return leadSource;
    }

    public void setLeadSource(String leadSource) {
        this.leadSource = leadSource;
    }

    public ContactStatus getContactStatus() {
        return contactStatus;
    }

    public void setContactStatus(ContactStatus contactStatus) {
        this.contactStatus = contactStatus;
    }

    public Integer getLeadScore() {
        return leadScore;
    }

    public void setLeadScore(Integer leadScore) {
        this.leadScore = leadScore;
    }

    public Boolean getDoNotCall() {
        return doNotCall;
    }

    public void setDoNotCall(Boolean doNotCall) {
        this.doNotCall = doNotCall;
    }

    public Boolean getDoNotEmail() {
        return doNotEmail;
    }

    public void setDoNotEmail(Boolean doNotEmail) {
        this.doNotEmail = doNotEmail;
    }

    public Boolean getEmailOptOut() {
        return emailOptOut;
    }

    public void setEmailOptOut(Boolean emailOptOut) {
        this.emailOptOut = emailOptOut;
    }

    public ContactMethod getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(ContactMethod preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public List<ContactRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<ContactRelationship> relationships) {
        this.relationships = relationships;
    }

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getDisplayName() {
        if (title != null && !title.isEmpty()) {
            return getFullName() + " (" + title + ")";
        }
        return getFullName();
    }
}

