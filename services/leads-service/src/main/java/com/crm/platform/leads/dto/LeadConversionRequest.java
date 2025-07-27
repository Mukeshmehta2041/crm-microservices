package com.crm.platform.leads.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class LeadConversionRequest {

    @NotNull(message = "Lead ID is required")
    private UUID leadId;

    private boolean createContact = true;
    private boolean createAccount = false;
    private boolean createDeal = false;

    // Contact creation details
    private String contactNotes;

    // Account creation details
    private String accountName;
    private String accountType;
    private String accountIndustry;
    private String accountWebsite;
    private String accountNotes;

    // Deal creation details
    private String dealName;
    private String dealDescription;
    private UUID pipelineId;
    private UUID stageId;
    private String dealAmount;
    private String expectedCloseDate;
    private String dealNotes;

    // Additional options
    private boolean keepLead = false;
    private String conversionNotes;

    // Constructors
    public LeadConversionRequest() {}

    public LeadConversionRequest(UUID leadId) {
        this.leadId = leadId;
    }

    // Getters and Setters
    public UUID getLeadId() {
        return leadId;
    }

    public void setLeadId(UUID leadId) {
        this.leadId = leadId;
    }

    public boolean isCreateContact() {
        return createContact;
    }

    public void setCreateContact(boolean createContact) {
        this.createContact = createContact;
    }

    public boolean isCreateAccount() {
        return createAccount;
    }

    public void setCreateAccount(boolean createAccount) {
        this.createAccount = createAccount;
    }

    public boolean isCreateDeal() {
        return createDeal;
    }

    public void setCreateDeal(boolean createDeal) {
        this.createDeal = createDeal;
    }

    public String getContactNotes() {
        return contactNotes;
    }

    public void setContactNotes(String contactNotes) {
        this.contactNotes = contactNotes;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountIndustry() {
        return accountIndustry;
    }

    public void setAccountIndustry(String accountIndustry) {
        this.accountIndustry = accountIndustry;
    }

    public String getAccountWebsite() {
        return accountWebsite;
    }

    public void setAccountWebsite(String accountWebsite) {
        this.accountWebsite = accountWebsite;
    }

    public String getAccountNotes() {
        return accountNotes;
    }

    public void setAccountNotes(String accountNotes) {
        this.accountNotes = accountNotes;
    }

    public String getDealName() {
        return dealName;
    }

    public void setDealName(String dealName) {
        this.dealName = dealName;
    }

    public String getDealDescription() {
        return dealDescription;
    }

    public void setDealDescription(String dealDescription) {
        this.dealDescription = dealDescription;
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
    }

    public UUID getStageId() {
        return stageId;
    }

    public void setStageId(UUID stageId) {
        this.stageId = stageId;
    }

    public String getDealAmount() {
        return dealAmount;
    }

    public void setDealAmount(String dealAmount) {
        this.dealAmount = dealAmount;
    }

    public String getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(String expectedCloseDate) {
        this.expectedCloseDate = expectedCloseDate;
    }

    public String getDealNotes() {
        return dealNotes;
    }

    public void setDealNotes(String dealNotes) {
        this.dealNotes = dealNotes;
    }

    public boolean isKeepLead() {
        return keepLead;
    }

    public void setKeepLead(boolean keepLead) {
        this.keepLead = keepLead;
    }

    public String getConversionNotes() {
        return conversionNotes;
    }

    public void setConversionNotes(String conversionNotes) {
        this.conversionNotes = conversionNotes;
    }
}