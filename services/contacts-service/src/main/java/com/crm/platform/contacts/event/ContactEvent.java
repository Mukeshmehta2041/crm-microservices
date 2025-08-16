package com.crm.platform.contacts.event;

import com.crm.platform.contacts.entity.Contact;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ContactEvent {

    private UUID eventId;
    private String eventType;
    private UUID contactId;
    private UUID tenantId;
    private UUID userId;
    private LocalDateTime timestamp;
    private Contact contactData;
    private Contact previousContactData;
    private List<UUID> mergedContactIds;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public UUID getContactId() {
        return contactId;
    }

    public void setContactId(UUID contactId) {
        this.contactId = contactId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Contact getContactData() {
        return contactData;
    }

    public void setContactData(Contact contactData) {
        this.contactData = contactData;
    }

    public Contact getPreviousContactData() {
        return previousContactData;
    }

    public void setPreviousContactData(Contact previousContactData) {
        this.previousContactData = previousContactData;
    }

    public List<UUID> getMergedContactIds() {
        return mergedContactIds;
    }

    public void setMergedContactIds(List<UUID> mergedContactIds) {
        this.mergedContactIds = mergedContactIds;
    }
}