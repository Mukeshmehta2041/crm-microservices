package com.crm.platform.customobjects.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Search request DTO for custom objects
 */
public class CustomObjectSearchRequest {

    @JsonProperty("search_term")
    private String searchTerm;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("allow_reports")
    private Boolean allowReports;

    @JsonProperty("allow_activities")
    private Boolean allowActivities;

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

    @JsonProperty("include_fields")
    private Boolean includeFields = false;

    @JsonProperty("include_record_count")
    private Boolean includeRecordCount = false;

    // Constructors
    public CustomObjectSearchRequest() {}

    // Getters and Setters
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getAllowReports() {
        return allowReports;
    }

    public void setAllowReports(Boolean allowReports) {
        this.allowReports = allowReports;
    }

    public Boolean getAllowActivities() {
        return allowActivities;
    }

    public void setAllowActivities(Boolean allowActivities) {
        this.allowActivities = allowActivities;
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

    public Boolean getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(Boolean includeFields) {
        this.includeFields = includeFields;
    }

    public Boolean getIncludeRecordCount() {
        return includeRecordCount;
    }

    public void setIncludeRecordCount(Boolean includeRecordCount) {
        this.includeRecordCount = includeRecordCount;
    }

    @Override
    public String toString() {
        return "CustomObjectSearchRequest{" +
                "searchTerm='" + searchTerm + '\'' +
                ", isActive=" + isActive +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}