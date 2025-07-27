package com.crm.platform.accounts.dto;

import com.crm.platform.accounts.entity.AccountStatus;
import com.crm.platform.accounts.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AccountResponse {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String accountNumber;
    private AccountType accountType;
    private String industry;
    private BigDecimal annualRevenue;
    private Integer employeeCount;
    private String website;
    private String phone;
    private String fax;
    private String billingAddress;
    private String shippingAddress;
    private String description;
    private AccountStatus status;
    private String[] tags;
    private String customFields;
    
    // Hierarchy information
    private UUID parentAccountId;
    private String parentAccountName;
    private Integer hierarchyLevel;
    private String hierarchyPath;
    private List<AccountHierarchyNode> childAccounts;
    
    // Territory information
    private UUID territoryId;
    private String territoryName;
    
    // Ownership information
    private UUID ownerId;
    private String ownerName;
    
    // Audit information
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private String createdByName;
    private String updatedByName;

    // Constructors
    public AccountResponse() {}

    // Static nested class for hierarchy representation
    public static class AccountHierarchyNode {
        private UUID id;
        private String name;
        private AccountType accountType;
        private Integer hierarchyLevel;
        private List<AccountHierarchyNode> children;

        public AccountHierarchyNode() {}

        public AccountHierarchyNode(UUID id, String name, AccountType accountType, Integer hierarchyLevel) {
            this.id = id;
            this.name = name;
            this.accountType = accountType;
            this.hierarchyLevel = hierarchyLevel;
        }

        // Getters and Setters
        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AccountType getAccountType() {
            return accountType;
        }

        public void setAccountType(AccountType accountType) {
            this.accountType = accountType;
        }

        public Integer getHierarchyLevel() {
            return hierarchyLevel;
        }

        public void setHierarchyLevel(Integer hierarchyLevel) {
            this.hierarchyLevel = hierarchyLevel;
        }

        public List<AccountHierarchyNode> getChildren() {
            return children;
        }

        public void setChildren(List<AccountHierarchyNode> children) {
            this.children = children;
        }
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

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public BigDecimal getAnnualRevenue() {
        return annualRevenue;
    }

    public void setAnnualRevenue(BigDecimal annualRevenue) {
        this.annualRevenue = annualRevenue;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
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

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public UUID getParentAccountId() {
        return parentAccountId;
    }

    public void setParentAccountId(UUID parentAccountId) {
        this.parentAccountId = parentAccountId;
    }

    public String getParentAccountName() {
        return parentAccountName;
    }

    public void setParentAccountName(String parentAccountName) {
        this.parentAccountName = parentAccountName;
    }

    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    public void setHierarchyLevel(Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    public String getHierarchyPath() {
        return hierarchyPath;
    }

    public void setHierarchyPath(String hierarchyPath) {
        this.hierarchyPath = hierarchyPath;
    }

    public List<AccountHierarchyNode> getChildAccounts() {
        return childAccounts;
    }

    public void setChildAccounts(List<AccountHierarchyNode> childAccounts) {
        this.childAccounts = childAccounts;
    }

    public UUID getTerritoryId() {
        return territoryId;
    }

    public void setTerritoryId(UUID territoryId) {
        this.territoryId = territoryId;
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public void setTerritoryName(String territoryName) {
        this.territoryName = territoryName;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }
}