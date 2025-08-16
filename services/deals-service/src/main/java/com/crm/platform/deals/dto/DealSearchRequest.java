package com.crm.platform.deals.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DealSearchRequest {

    private String name;
    private List<UUID> pipelineIds;
    private List<UUID> stageIds;
    private List<UUID> ownerIds;
    private List<UUID> accountIds;
    private List<UUID> contactIds;
    
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String currency;
    
    private BigDecimal minProbability;
    private BigDecimal maxProbability;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedCloseDateFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedCloseDateTo;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualCloseDateFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualCloseDateTo;
    
    private String dealType;
    private String leadSource;
    private Boolean isClosed;
    private Boolean isWon;
    private List<String> tags;
    
    private Map<String, Object> customFields;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDate createdAfter;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDate createdBefore;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDate updatedAfter;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDate updatedBefore;

    // Sorting
    private String sortBy = "createdAt";
    private String sortDirection = "desc";

    // Pagination
    private int page = 0;
    private int size = 20;

    // Constructors
    public DealSearchRequest() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<UUID> getPipelineIds() { return pipelineIds; }
    public void setPipelineIds(List<UUID> pipelineIds) { this.pipelineIds = pipelineIds; }

    public List<UUID> getStageIds() { return stageIds; }
    public void setStageIds(List<UUID> stageIds) { this.stageIds = stageIds; }

    public List<UUID> getOwnerIds() { return ownerIds; }
    public void setOwnerIds(List<UUID> ownerIds) { this.ownerIds = ownerIds; }

    public List<UUID> getAccountIds() { return accountIds; }
    public void setAccountIds(List<UUID> accountIds) { this.accountIds = accountIds; }

    public List<UUID> getContactIds() { return contactIds; }
    public void setContactIds(List<UUID> contactIds) { this.contactIds = contactIds; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getMinProbability() { return minProbability; }
    public void setMinProbability(BigDecimal minProbability) { this.minProbability = minProbability; }

    public BigDecimal getMaxProbability() { return maxProbability; }
    public void setMaxProbability(BigDecimal maxProbability) { this.maxProbability = maxProbability; }

    public LocalDate getExpectedCloseDateFrom() { return expectedCloseDateFrom; }
    public void setExpectedCloseDateFrom(LocalDate expectedCloseDateFrom) { this.expectedCloseDateFrom = expectedCloseDateFrom; }

    public LocalDate getExpectedCloseDateTo() { return expectedCloseDateTo; }
    public void setExpectedCloseDateTo(LocalDate expectedCloseDateTo) { this.expectedCloseDateTo = expectedCloseDateTo; }

    public LocalDate getActualCloseDateFrom() { return actualCloseDateFrom; }
    public void setActualCloseDateFrom(LocalDate actualCloseDateFrom) { this.actualCloseDateFrom = actualCloseDateFrom; }

    public LocalDate getActualCloseDateTo() { return actualCloseDateTo; }
    public void setActualCloseDateTo(LocalDate actualCloseDateTo) { this.actualCloseDateTo = actualCloseDateTo; }

    public String getDealType() { return dealType; }
    public void setDealType(String dealType) { this.dealType = dealType; }

    public String getLeadSource() { return leadSource; }
    public void setLeadSource(String leadSource) { this.leadSource = leadSource; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }

    public Boolean getIsWon() { return isWon; }
    public void setIsWon(Boolean isWon) { this.isWon = isWon; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }

    public LocalDate getCreatedAfter() { return createdAfter; }
    public void setCreatedAfter(LocalDate createdAfter) { this.createdAfter = createdAfter; }

    public LocalDate getCreatedBefore() { return createdBefore; }
    public void setCreatedBefore(LocalDate createdBefore) { this.createdBefore = createdBefore; }

    public LocalDate getUpdatedAfter() { return updatedAfter; }
    public void setUpdatedAfter(LocalDate updatedAfter) { this.updatedAfter = updatedAfter; }

    public LocalDate getUpdatedBefore() { return updatedBefore; }
    public void setUpdatedBefore(LocalDate updatedBefore) { this.updatedBefore = updatedBefore; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}