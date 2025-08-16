package com.crm.platform.accounts.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_accounts_parent_id", columnList = "parent_account_id"),
    @Index(name = "idx_accounts_name", columnList = "tenant_id, name"),
    @Index(name = "idx_accounts_owner", columnList = "owner_id"),
    @Index(name = "idx_accounts_type", columnList = "tenant_id, account_type"),
    @Index(name = "idx_accounts_industry", columnList = "tenant_id, industry"),
    @Index(name = "idx_accounts_territory", columnList = "territory_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 100)
    @Column(name = "account_number")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    @Size(max = 100)
    @Column(name = "industry")
    private String industry;

    @Size(max = 50)
    @Column(name = "annual_revenue")
    private BigDecimal annualRevenue;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Size(max = 255)
    @Column(name = "website")
    private String website;

    @Size(max = 50)
    @Column(name = "phone")
    private String phone;

    @Size(max = 50)
    @Column(name = "fax")
    private String fax;

    @Column(name = "billing_address", columnDefinition = "jsonb")
    private String billingAddress;

    @Column(name = "shipping_address", columnDefinition = "jsonb")
    private String shippingAddress;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private String customFields;

    // Hierarchy support
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    @JsonIgnore
    private Account parentAccount;

    @OneToMany(mappedBy = "parentAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Account> childAccounts = new ArrayList<>();

    @Column(name = "hierarchy_level")
    private Integer hierarchyLevel = 0;

    @Column(name = "hierarchy_path")
    private String hierarchyPath;

    // Territory management
    @Column(name = "territory_id")
    private UUID territoryId;

    // Ownership and audit fields
    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotNull
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @NotNull
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    // Constructors
    public Account() {}

    public Account(String name, UUID tenantId, UUID ownerId, UUID createdBy) {
        this.name = name;
        this.tenantId = tenantId;
        this.ownerId = ownerId;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    // Helper methods for hierarchy
    public boolean isRootAccount() {
        return parentAccount == null;
    }

    public boolean hasChildren() {
        return !childAccounts.isEmpty();
    }

    public void addChildAccount(Account child) {
        childAccounts.add(child);
        child.setParentAccount(this);
        child.setHierarchyLevel(this.hierarchyLevel + 1);
        child.updateHierarchyPath();
    }

    public void removeChildAccount(Account child) {
        childAccounts.remove(child);
        child.setParentAccount(null);
        child.setHierarchyLevel(0);
        child.updateHierarchyPath();
    }

    private void updateHierarchyPath() {
        if (parentAccount == null) {
            this.hierarchyPath = id.toString();
        } else {
            this.hierarchyPath = parentAccount.getHierarchyPath() + "/" + id.toString();
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

    public Account getParentAccount() {
        return parentAccount;
    }

    public void setParentAccount(Account parentAccount) {
        this.parentAccount = parentAccount;
    }

    public List<Account> getChildAccounts() {
        return childAccounts;
    }

    public void setChildAccounts(List<Account> childAccounts) {
        this.childAccounts = childAccounts;
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

    public UUID getTerritoryId() {
        return territoryId;
    }

    public void setTerritoryId(UUID territoryId) {
        this.territoryId = territoryId;
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
}