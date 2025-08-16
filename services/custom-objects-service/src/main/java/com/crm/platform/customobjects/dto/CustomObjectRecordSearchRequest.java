package com.crm.platform.customobjects.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Search request DTO for custom object records
 */
public class CustomObjectRecordSearchRequest {

    @JsonProperty("custom_object_id")
    private UUID customObjectId;

    @JsonProperty("search_term")
    private String searchTerm;

    @JsonProperty("field_filters")
    private Map<String, Object> fieldFilters;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("owner_id")
    private UUID ownerId;

    @JsonProperty("created_after")
    private LocalDateTime createdAfter;

    @JsonProperty("created_before")
    private LocalDateTime createdBefore;

    @JsonProperty("updated_after")
    private LocalDateTime updatedAfter;

    @JsonProperty("updated_before")
    private LocalDateTime updatedBefore;

    @JsonProperty("sort_by")
    private String sortBy = "createdAt";

    @JsonProperty("sort_direction")
    private String sortDirection = "desc";

    private Integer page = 0;

    private Integer size = 20;

    @JsonProperty("include_custom_object")
    private Boolean includeCustomObject = false;

    // Constructors
    public CustomObjectRecordSearchRequest() {}

    public CustomObjectRecordSearchRequest(UUID customObjectId) {
        this.customObjectId = customObjectId;
    }

    // Getters and Setters
    public UUID getCustomObjectId() {
        return customObjectId;
    }

    public void setCustomObjectId(UUID customObjectId) {
        this.customObjectId = customObjectId;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Map<String, Object> getFieldFilters() {
        return fieldFilters;
    }

    public void setFieldFilters(Map<String, Object> fieldFilters) {
        this.fieldFilters = fieldFilters;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
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

    public Boolean getIncludeCustomObject() {
        return includeCustomObject;
    }

    public void setIncludeCustomObject(Boolean includeCustomObject) {
        this.includeCustomObject = includeCustomObject;
    }

    @Override
    public String toString() {
        return "CustomObjectRecordSearchRequest{" +
                "customObjectId=" + customObjectId +
                ", searchTerm='" + searchTerm + '\'' +
                ", isActive=" + isActive +
                ", ownerId=" + ownerId +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}