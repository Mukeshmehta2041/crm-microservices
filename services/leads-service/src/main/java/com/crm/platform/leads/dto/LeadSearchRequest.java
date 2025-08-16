package com.crm.platform.leads.dto;

import com.crm.platform.leads.entity.LeadStatus;
import com.crm.platform.leads.entity.QualificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class LeadSearchRequest {

    private String searchTerm;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String company;
    private String title;
    private String industry;
    private List<String> leadSources;
    private List<LeadStatus> statuses;
    private List<QualificationStatus> qualificationStatuses;
    private Integer minLeadScore;
    private Integer maxLeadScore;
    private BigDecimal minAnnualRevenue;
    private BigDecimal maxAnnualRevenue;
    private Integer minEmployees;
    private Integer maxEmployees;
    private BigDecimal minBudget;
    private BigDecimal maxBudget;
    private List<String> purchaseTimeframes;
    private Boolean decisionMaker;
    private Boolean converted;
    private List<UUID> ownerIds;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime updatedAfter;
    private LocalDateTime updatedBefore;
    private LocalDateTime lastActivityAfter;
    private LocalDateTime lastActivityBefore;
    private LocalDateTime nextFollowUpAfter;
    private LocalDateTime nextFollowUpBefore;
    private Boolean hasNextFollowUp;
    private Boolean overdue;
    private String timezone;
    private String language;
    private Boolean doNotCall;
    private Boolean doNotEmail;
    private Boolean emailOptOut;

    // Sorting
    private String sortBy = "createdAt";
    private String sortDirection = "desc";

    // Pagination
    private int page = 0;
    private int size = 20;

    // Constructors
    public LeadSearchRequest() {}

    // Getters and Setters
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
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

    public List<String> getLeadSources() {
        return leadSources;
    }

    public void setLeadSources(List<String> leadSources) {
        this.leadSources = leadSources;
    }

    public List<LeadStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<LeadStatus> statuses) {
        this.statuses = statuses;
    }

    public List<QualificationStatus> getQualificationStatuses() {
        return qualificationStatuses;
    }

    public void setQualificationStatuses(List<QualificationStatus> qualificationStatuses) {
        this.qualificationStatuses = qualificationStatuses;
    }

    public Integer getMinLeadScore() {
        return minLeadScore;
    }

    public void setMinLeadScore(Integer minLeadScore) {
        this.minLeadScore = minLeadScore;
    }

    public Integer getMaxLeadScore() {
        return maxLeadScore;
    }

    public void setMaxLeadScore(Integer maxLeadScore) {
        this.maxLeadScore = maxLeadScore;
    }

    public BigDecimal getMinAnnualRevenue() {
        return minAnnualRevenue;
    }

    public void setMinAnnualRevenue(BigDecimal minAnnualRevenue) {
        this.minAnnualRevenue = minAnnualRevenue;
    }

    public BigDecimal getMaxAnnualRevenue() {
        return maxAnnualRevenue;
    }

    public void setMaxAnnualRevenue(BigDecimal maxAnnualRevenue) {
        this.maxAnnualRevenue = maxAnnualRevenue;
    }

    public Integer getMinEmployees() {
        return minEmployees;
    }

    public void setMinEmployees(Integer minEmployees) {
        this.minEmployees = minEmployees;
    }

    public Integer getMaxEmployees() {
        return maxEmployees;
    }

    public void setMaxEmployees(Integer maxEmployees) {
        this.maxEmployees = maxEmployees;
    }

    public BigDecimal getMinBudget() {
        return minBudget;
    }

    public void setMinBudget(BigDecimal minBudget) {
        this.minBudget = minBudget;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }

    public List<String> getPurchaseTimeframes() {
        return purchaseTimeframes;
    }

    public void setPurchaseTimeframes(List<String> purchaseTimeframes) {
        this.purchaseTimeframes = purchaseTimeframes;
    }

    public Boolean getDecisionMaker() {
        return decisionMaker;
    }

    public void setDecisionMaker(Boolean decisionMaker) {
        this.decisionMaker = decisionMaker;
    }

    public Boolean getConverted() {
        return converted;
    }

    public void setConverted(Boolean converted) {
        this.converted = converted;
    }

    public List<UUID> getOwnerIds() {
        return ownerIds;
    }

    public void setOwnerIds(List<UUID> ownerIds) {
        this.ownerIds = ownerIds;
    }

    public LocalDateTime getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(LocalDateTime createdAfter) {
        this.createdAfter = createdAfter;
    }

    public LocalDateTime getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(LocalDateTime createdBefore) {
        this.createdBefore = createdBefore;
    }

    public LocalDateTime getUpdatedAfter() {
        return updatedAfter;
    }

    public void setUpdatedAfter(LocalDateTime updatedAfter) {
        this.updatedAfter = updatedAfter;
    }

    public LocalDateTime getUpdatedBefore() {
        return updatedBefore;
    }

    public void setUpdatedBefore(LocalDateTime updatedBefore) {
        this.updatedBefore = updatedBefore;
    }

    public LocalDateTime getLastActivityAfter() {
        return lastActivityAfter;
    }

    public void setLastActivityAfter(LocalDateTime lastActivityAfter) {
        this.lastActivityAfter = lastActivityAfter;
    }

    public LocalDateTime getLastActivityBefore() {
        return lastActivityBefore;
    }

    public void setLastActivityBefore(LocalDateTime lastActivityBefore) {
        this.lastActivityBefore = lastActivityBefore;
    }

    public LocalDateTime getNextFollowUpAfter() {
        return nextFollowUpAfter;
    }

    public void setNextFollowUpAfter(LocalDateTime nextFollowUpAfter) {
        this.nextFollowUpAfter = nextFollowUpAfter;
    }

    public LocalDateTime getNextFollowUpBefore() {
        return nextFollowUpBefore;
    }

    public void setNextFollowUpBefore(LocalDateTime nextFollowUpBefore) {
        this.nextFollowUpBefore = nextFollowUpBefore;
    }

    public Boolean getHasNextFollowUp() {
        return hasNextFollowUp;
    }

    public void setHasNextFollowUp(Boolean hasNextFollowUp) {
        this.hasNextFollowUp = hasNextFollowUp;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
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

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}