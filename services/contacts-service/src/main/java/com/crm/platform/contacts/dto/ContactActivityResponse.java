package com.crm.platform.contacts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for contact activities
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactActivityResponse {
    
    @JsonProperty("id")
    private UUID id;
    
    @JsonProperty("contactId")
    private UUID contactId;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("subject")
    private String subject;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("priority")
    private String priority;
    
    @JsonProperty("direction")
    private String direction;
    
    @JsonProperty("outcome")
    private String outcome;
    
    @JsonProperty("duration")
    private Integer duration;
    
    @JsonProperty("scheduledAt")
    private Instant scheduledAt;
    
    @JsonProperty("completedAt")
    private Instant completedAt;
    
    @JsonProperty("dueAt")
    private Instant dueAt;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("updatedAt")
    private Instant updatedAt;
    
    @JsonProperty("ownerId")
    private UUID ownerId;
    
    @JsonProperty("ownerName")
    private String ownerName;
    
    @JsonProperty("assignedToId")
    private UUID assignedToId;
    
    @JsonProperty("assignedToName")
    private String assignedToName;
    
    @JsonProperty("relatedRecords")
    private Map<String, UUID> relatedRecords;
    
    @JsonProperty("customFields")
    private Map<String, Object> customFields;
    
    @JsonProperty("attachments")
    private Integer attachmentCount;
    
    @JsonProperty("isOverdue")
    private Boolean isOverdue;
    
    @JsonProperty("reminderSet")
    private Boolean reminderSet;
    
    @JsonProperty("reminderAt")
    private Instant reminderAt;
    
    // Constructors
    public ContactActivityResponse() {}
    
    public ContactActivityResponse(UUID id, UUID contactId, String type, String subject) {
        this.id = id;
        this.contactId = contactId;
        this.type = type;
        this.subject = subject;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getContactId() {
        return contactId;
    }
    
    public void setContactId(UUID contactId) {
        this.contactId = contactId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public String getOutcome() {
        return outcome;
    }
    
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Instant getScheduledAt() {
        return scheduledAt;
    }
    
    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    public Instant getDueAt() {
        return dueAt;
    }
    
    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
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
    
    public UUID getAssignedToId() {
        return assignedToId;
    }
    
    public void setAssignedToId(UUID assignedToId) {
        this.assignedToId = assignedToId;
    }
    
    public String getAssignedToName() {
        return assignedToName;
    }
    
    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }
    
    public Map<String, UUID> getRelatedRecords() {
        return relatedRecords;
    }
    
    public void setRelatedRecords(Map<String, UUID> relatedRecords) {
        this.relatedRecords = relatedRecords;
    }
    
    public Map<String, Object> getCustomFields() {
        return customFields;
    }
    
    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
    
    public Integer getAttachmentCount() {
        return attachmentCount;
    }
    
    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }
    
    public Boolean getIsOverdue() {
        return isOverdue;
    }
    
    public void setIsOverdue(Boolean isOverdue) {
        this.isOverdue = isOverdue;
    }
    
    public Boolean getReminderSet() {
        return reminderSet;
    }
    
    public void setReminderSet(Boolean reminderSet) {
        this.reminderSet = reminderSet;
    }
    
    public Instant getReminderAt() {
        return reminderAt;
    }
    
    public void setReminderAt(Instant reminderAt) {
        this.reminderAt = reminderAt;
    }
}