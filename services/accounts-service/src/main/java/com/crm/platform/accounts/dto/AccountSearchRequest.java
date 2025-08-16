package com.crm.platform.accounts.dto;

import com.crm.platform.accounts.entity.AccountStatus;
import com.crm.platform.accounts.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AccountSearchRequest {

    private String name;
    private String accountNumber;
    private List<AccountType> accountTypes;
    private List<String> industries;
    private BigDecimal minAnnualRevenue;
    private BigDecimal maxAnnualRevenue;
    private Integer minEmployeeCount;
    private Integer maxEmployeeCount;
    private String website;
    private String phone;
    private List<AccountStatus> statuses;
    private List<String> tags;
    private String customFieldsQuery;
    
    // Hierarchy filters
    private UUID parentAccountId;
    private Boolean includeChildAccounts;
    private Integer maxHierarchyLevel;
    private Boolean rootAccountsOnly;
    
    // Territory filters
    private List<UUID> territoryIds;
    
    // Ownership filters
    private List<UUID> ownerIds;
    
    // Date filters
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime updatedAfter;
    private LocalDateTime updatedBefore;
    
    // Relationship filters
    private UUID relatedAccountId;
    private String relationshipType;
    
    // Search and sorting
    private String searchTerm;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;

    // Constructors
    public AccountSearchRequest() {}

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public List<AccountType> getAccountTypes() {
        return accountTypes;
    }

    public void setAccountTypes(List<AccountType> accountTypes) {
        this.accountTypes = accountTypes;
    }

    public List<String> getIndustries() {
        return industries;
    }

    public void setIndustries(List<String> industries) {
        this.industries = industries;
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

    public Integer getMinEmployeeCount() {
        return minEmployeeCount;
    }

    public void setMinEmployeeCount(Integer minEmployeeCount) {
        this.minEmployeeCount = minEmployeeCount;
    }

    public Integer getMaxEmployeeCount() {
        return maxEmployeeCount;
    }

    public void setMaxEmployeeCount(Integer maxEmployeeCount) {
        this.maxEmployeeCount = maxEmployeeCount;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<AccountStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<AccountStatus> statuses) {
        this.statuses = statuses;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getCustomFieldsQuery() {
        return customFieldsQuery;
    }

    public void setCustomFieldsQuery(String customFieldsQuery) {
        this.customFieldsQuery = customFieldsQuery;
    }

    public UUID getParentAccountId() {
        return parentAccountId;
    }

    public void setParentAccountId(UUID parentAccountId) {
        this.parentAccountId = parentAccountId;
    }

    public Boolean getIncludeChildAccounts() {
        return includeChildAccounts;
    }

    public void setIncludeChildAccounts(Boolean includeChildAccounts) {
        this.includeChildAccounts = includeChildAccounts;
    }

    public Integer getMaxHierarchyLevel() {
        return maxHierarchyLevel;
    }

    public void setMaxHierarchyLevel(Integer maxHierarchyLevel) {
        this.maxHierarchyLevel = maxHierarchyLevel;
    }

    public Boolean getRootAccountsOnly() {
        return rootAccountsOnly;
    }

    public void setRootAccountsOnly(Boolean rootAccountsOnly) {
        this.rootAccountsOnly = rootAccountsOnly;
    }

    public List<UUID> getTerritoryIds() {
        return territoryIds;
    }

    public void setTerritoryIds(List<UUID> territoryIds) {
        this.territoryIds = territoryIds;
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

    public UUID getRelatedAccountId() {
        return relatedAccountId;
    }

    public void setRelatedAccountId(UUID relatedAccountId) {
        this.relatedAccountId = relatedAccountId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
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

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}