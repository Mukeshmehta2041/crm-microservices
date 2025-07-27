package com.crm.platform.leads.dto;

import com.crm.platform.leads.entity.LeadStatus;
import com.crm.platform.leads.entity.QualificationStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LeadRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Size(max = 50, message = "Mobile must not exceed 50 characters")
    private String mobile;

    @Size(max = 255, message = "Company must not exceed 255 characters")
    private String company;

    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 100, message = "Lead source must not exceed 100 characters")
    private String leadSource;

    @Size(max = 255, message = "Lead source detail must not exceed 255 characters")
    private String leadSourceDetail;

    private LeadStatus status;

    private QualificationStatus qualificationStatus;

    @Min(value = 0, message = "Lead score must be at least 0")
    @Max(value = 100, message = "Lead score must not exceed 100")
    private Integer leadScore;

    @DecimalMin(value = "0.00", message = "Annual revenue must be positive")
    private BigDecimal annualRevenue;

    @Min(value = 0, message = "Number of employees must be positive")
    private Integer numberOfEmployees;

    @DecimalMin(value = "0.00", message = "Budget must be positive")
    private BigDecimal budget;

    @Size(max = 50, message = "Purchase timeframe must not exceed 50 characters")
    private String purchaseTimeframe;

    private Boolean decisionMaker;

    private String painPoints;

    private String interests;

    private String notes;

    private Boolean doNotCall;

    private Boolean doNotEmail;

    private Boolean emailOptOut;

    @Size(max = 20, message = "Preferred contact method must not exceed 20 characters")
    private String preferredContactMethod;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    @Size(max = 10, message = "Language must not exceed 10 characters")
    private String language;

    private UUID ownerId;

    private LocalDateTime nextFollowUpAt;

    // Constructors
    public LeadRequest() {}

    // Getters and Setters
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

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getNextFollowUpAt() {
        return nextFollowUpAt;
    }

    public void setNextFollowUpAt(LocalDateTime nextFollowUpAt) {
        this.nextFollowUpAt = nextFollowUpAt;
    }
}