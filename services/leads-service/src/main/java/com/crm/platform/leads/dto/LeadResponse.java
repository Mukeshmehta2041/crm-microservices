package com.crm.platform.leads.dto;

import com.crm.platform.leads.entity.Lead;
import com.crm.platform.leads.entity.LeadStatus;
import com.crm.platform.leads.entity.QualificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LeadResponse {

    private UUID id;
    private UUID tenantId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String mobile;
    private String company;
    private String title;
    private String industry;
    private String website;
    private String leadSource;
    private String leadSourceDetail;
    private LeadStatus status;
    private QualificationStatus qualificationStatus;
    private Integer leadScore;
    private BigDecimal annualRevenue;
    private Integer numberOfEmployees;
    private BigDecimal budget;
    private String purchaseTimeframe;
    private Boolean decisionMaker;
    private String painPoints;
    private String interests;
    private String notes;
    private Boolean doNotCall;
    private Boolean doNotEmail;
    private Boolean emailOptOut;
    private String preferredContactMethod;
    private String timezone;
    private String language;
    private UUID convertedContactId;
    private UUID convertedAccountId;
    private UUID convertedDealId;
    private LocalDateTime convertedAt;
    private UUID ownerId;
    private LocalDateTime assignedAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime nextFollowUpAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    // Computed fields
    private boolean converted;
    private boolean qualified;
    private boolean hotLead;
    private boolean warmLead;
    private boolean coldLead;

    // Constructors
    public LeadResponse() {}

    public LeadResponse(Lead lead) {
        this.id = lead.getId();
        this.tenantId = lead.getTenantId();
        this.firstName = lead.getFirstName();
        this.lastName = lead.getLastName();
        this.fullName = lead.getFullName();
        this.email = lead.getEmail();
        this.phone = lead.getPhone();
        this.mobile = lead.getMobile();
        this.company = lead.getCompany();
        this.title = lead.getTitle();
        this.industry = lead.getIndustry();
        this.website = lead.getWebsite();
        this.leadSource = lead.getLeadSource();
        this.leadSourceDetail = lead.getLeadSourceDetail();
        this.status = lead.getStatus();
        this.qualificationStatus = lead.getQualificationStatus();
        this.leadScore = lead.getLeadScore();
        this.annualRevenue = lead.getAnnualRevenue();
        this.numberOfEmployees = lead.getNumberOfEmployees();
        this.budget = lead.getBudget();
        this.purchaseTimeframe = lead.getPurchaseTimeframe();
        this.decisionMaker = lead.getDecisionMaker();
        this.painPoints = lead.getPainPoints();
        this.interests = lead.getInterests();
        this.notes = lead.getNotes();
        this.doNotCall = lead.getDoNotCall();
        this.doNotEmail = lead.getDoNotEmail();
        this.emailOptOut = lead.getEmailOptOut();
        this.preferredContactMethod = lead.getPreferredContactMethod();
        this.timezone = lead.getTimezone();
        this.language = lead.getLanguage();
        this.convertedContactId = lead.getConvertedContactId();
        this.convertedAccountId = lead.getConvertedAccountId();
        this.convertedDealId = lead.getConvertedDealId();
        this.convertedAt = lead.getConvertedAt();
        this.ownerId = lead.getOwnerId();
        this.assignedAt = lead.getAssignedAt();
        this.lastActivityAt = lead.getLastActivityAt();
        this.nextFollowUpAt = lead.getNextFollowUpAt();
        this.createdAt = lead.getCreatedAt();
        this.updatedAt = lead.getUpdatedAt();
        this.createdBy = lead.getCreatedBy();
        this.updatedBy = lead.getUpdatedBy();

        // Set computed fields
        this.converted = lead.isConverted();
        this.qualified = lead.isQualified();
        this.hotLead = lead.isHotLead();
        this.warmLead = lead.isWarmLead();
        this.coldLead = lead.isColdLead();
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public boolean isConverted() {
        return converted;
    }

    public void setConverted(boolean converted) {
        this.converted = converted;
    }

    public boolean isQualified() {
        return qualified;
    }

    public void setQualified(boolean qualified) {
        this.qualified = qualified;
    }

    public boolean isHotLead() {
        return hotLead;
    }

    public void setHotLead(boolean hotLead) {
        this.hotLead = hotLead;
    }

    public boolean isWarmLead() {
        return warmLead;
    }

    public void setWarmLead(boolean warmLead) {
        this.warmLead = warmLead;
    }

    public boolean isColdLead() {
        return coldLead;
    }

    public void setColdLead(boolean coldLead) {
        this.coldLead = coldLead;
    }
}