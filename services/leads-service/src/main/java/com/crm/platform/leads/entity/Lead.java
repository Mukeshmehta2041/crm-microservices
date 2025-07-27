package com.crm.platform.leads.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_leads_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_leads_email", columnList = "email"),
    @Index(name = "idx_leads_phone", columnList = "phone"),
    @Index(name = "idx_leads_status", columnList = "status"),
    @Index(name = "idx_leads_score", columnList = "lead_score"),
    @Index(name = "idx_leads_source", columnList = "lead_source"),
    @Index(name = "idx_leads_owner", columnList = "owner_id"),
    @Index(name = "idx_leads_created_at", columnList = "created_at"),
    @Index(name = "idx_leads_updated_at", columnList = "updated_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    @NotNull
    private UUID tenantId;

    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Column(name = "email", length = 255)
    @Email
    @Size(max = 255)
    private String email;

    @Column(name = "phone", length = 50)
    @Size(max = 50)
    private String phone;

    @Column(name = "mobile", length = 50)
    @Size(max = 50)
    private String mobile;

    @Column(name = "company", length = 255)
    @Size(max = 255)
    private String company;

    @Column(name = "title", length = 100)
    @Size(max = 100)
    private String title;

    @Column(name = "industry", length = 100)
    @Size(max = 100)
    private String industry;

    @Column(name = "website", length = 255)
    @Size(max = 255)
    private String website;

    @Column(name = "lead_source", length = 100)
    @Size(max = 100)
    private String leadSource;

    @Column(name = "lead_source_detail", length = 255)
    @Size(max = 255)
    private String leadSourceDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @NotNull
    private LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification_status", length = 50)
    private QualificationStatus qualificationStatus = QualificationStatus.UNQUALIFIED;

    @Column(name = "lead_score", nullable = false)
    @Min(0)
    @Max(100)
    private Integer leadScore = 0;

    @Column(name = "annual_revenue", precision = 15, scale = 2)
    @DecimalMin("0.00")
    private BigDecimal annualRevenue;

    @Column(name = "number_of_employees")
    @Min(0)
    private Integer numberOfEmployees;

    @Column(name = "budget", precision = 15, scale = 2)
    @DecimalMin("0.00")
    private BigDecimal budget;

    @Column(name = "purchase_timeframe", length = 50)
    @Size(max = 50)
    private String purchaseTimeframe;

    @Column(name = "decision_maker")
    private Boolean decisionMaker = false;

    @Column(name = "pain_points", columnDefinition = "TEXT")
    private String painPoints;

    @Column(name = "interests", columnDefinition = "TEXT")
    private String interests;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "do_not_call")
    private Boolean doNotCall = false;

    @Column(name = "do_not_email")
    private Boolean doNotEmail = false;

    @Column(name = "email_opt_out")
    private Boolean emailOptOut = false;

    @Column(name = "preferred_contact_method", length = 20)
    @Size(max = 20)
    private String preferredContactMethod = "email";

    @Column(name = "timezone", length = 50)
    @Size(max = 50)
    private String timezone;

    @Column(name = "language", length = 10)
    @Size(max = 10)
    private String language = "en-US";

    @Column(name = "converted_contact_id")
    private UUID convertedContactId;

    @Column(name = "converted_account_id")
    private UUID convertedAccountId;

    @Column(name = "converted_deal_id")
    private UUID convertedDealId;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "owner_id", nullable = false)
    @NotNull
    private UUID ownerId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "next_follow_up_at")
    private LocalDateTime nextFollowUpAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    @NotNull
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    @NotNull
    private UUID updatedBy;

    // Constructors
    public Lead() {}

    public Lead(UUID tenantId, String firstName, String lastName, UUID ownerId, UUID createdBy) {
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.ownerId = ownerId;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        this.assignedAt = LocalDateTime.now();
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

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLeadSource() {
        return leadSource;
    }

    public void setLeadSource(String leadSource) {
        this.leadSource = leadSource;
    }

    public String getLeadSourceDetail() {
        return leadSourceDetail;
    }

    public void setLeadSourceDetail(String leadSourceDetail) {
        this.leadSourceDetail = leadSourceDetail;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public QualificationStatus getQualificationStatus() {
        return qualificationStatus;
    }

    public void setQualificationStatus(QualificationStatus qualificationStatus) {
        this.qualificationStatus = qualificationStatus;
    }

    public Integer getLeadScore() {
        return leadScore;
    }

    public void setLeadScore(Integer leadScore) {
        this.leadScore = leadScore;
    }

    public BigDecimal getAnnualRevenue() {
        return annualRevenue;
    }

    public void setAnnualRevenue(BigDecimal annualRevenue) {
        this.annualRevenue = annualRevenue;
    }

    public Integer getNumberOfEmployees() {
        return numberOfEmployees;
    }

    public void setNumberOfEmployees(Integer numberOfEmployees) {
        this.numberOfEmployees = numberOfEmployees;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getPurchaseTimeframe() {
        return purchaseTimeframe;
    }

    public void setPurchaseTimeframe(String purchaseTimeframe) {
        this.purchaseTimeframe = purchaseTimeframe;
    }

    public Boolean getDecisionMaker() {
        return decisionMaker;
    }

    public void setDecisionMaker(Boolean decisionMaker) {
        this.decisionMaker = decisionMaker;
    }

    public String getPainPoints() {
        return painPoints;
    }

    public void setPainPoints(String painPoints) {
        this.painPoints = painPoints;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(String preferredContactMethod) {
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

    public UUID getConvertedContactId() {
        return convertedContactId;
    }

    public void setConvertedContactId(UUID convertedContactId) {
        this.convertedContactId = convertedContactId;
    }

    public UUID getConvertedAccountId() {
        return convertedAccountId;
    }

    public void setConvertedAccountId(UUID convertedAccountId) {
        this.convertedAccountId = convertedAccountId;
    }

    public UUID getConvertedDealId() {
        return convertedDealId;
    }

    public void setConvertedDealId(UUID convertedDealId) {
        this.convertedDealId = convertedDealId;
    }

    public LocalDateTime getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(LocalDateTime convertedAt) {
        this.convertedAt = convertedAt;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public LocalDateTime getNextFollowUpAt() {
        return nextFollowUpAt;
    }

    public void setNextFollowUpAt(LocalDateTime nextFollowUpAt) {
        this.nextFollowUpAt = nextFollowUpAt;
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

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isConverted() {
        return convertedContactId != null || convertedAccountId != null || convertedDealId != null;
    }

    public boolean isQualified() {
        return qualificationStatus == QualificationStatus.QUALIFIED;
    }

    public boolean isHotLead() {
        return leadScore >= 80;
    }

    public boolean isWarmLead() {
        return leadScore >= 50 && leadScore < 80;
    }

    public boolean isColdLead() {
        return leadScore < 50;
    }
}