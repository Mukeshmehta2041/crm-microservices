package com.crm.platform.contacts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for merging contacts
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactMergeRequest {
    
    @JsonProperty("duplicateContactIds")
    @NotEmpty(message = "At least one duplicate contact ID is required")
    private List<UUID> duplicateContactIds;
    
    @JsonProperty("fieldMergeStrategy")
    private Map<String, MergeStrategy> fieldMergeStrategy;
    
    @JsonProperty("preserveActivities")
    private Boolean preserveActivities = true;
    
    @JsonProperty("preserveRelationships")
    private Boolean preserveRelationships = true;
    
    @JsonProperty("notifyOwners")
    private Boolean notifyOwners = true;
    
    public enum MergeStrategy {
        KEEP_PRIMARY,
        KEEP_DUPLICATE,
        CONCATENATE,
        MERGE_ARRAYS,
        KEEP_NEWEST,
        KEEP_OLDEST,
        MANUAL
    }
    
    // Constructors
    public ContactMergeRequest() {}
    
    public ContactMergeRequest(List<UUID> duplicateContactIds) {
        this.duplicateContactIds = duplicateContactIds;
    }
    
    // Getters and setters
    public List<UUID> getDuplicateContactIds() {
        return duplicateContactIds;
    }
    
    public void setDuplicateContactIds(List<UUID> duplicateContactIds) {
        this.duplicateContactIds = duplicateContactIds;
    }
    
    public Map<String, MergeStrategy> getFieldMergeStrategy() {
        return fieldMergeStrategy;
    }
    
    public void setFieldMergeStrategy(Map<String, MergeStrategy> fieldMergeStrategy) {
        this.fieldMergeStrategy = fieldMergeStrategy;
    }
    
    public Boolean getPreserveActivities() {
        return preserveActivities;
    }
    
    public void setPreserveActivities(Boolean preserveActivities) {
        this.preserveActivities = preserveActivities;
    }
    
    public Boolean getPreserveRelationships() {
        return preserveRelationships;
    }
    
    public void setPreserveRelationships(Boolean preserveRelationships) {
        this.preserveRelationships = preserveRelationships;
    }
    
    public Boolean getNotifyOwners() {
        return notifyOwners;
    }
    
    public void setNotifyOwners(Boolean notifyOwners) {
        this.notifyOwners = notifyOwners;
    }
}