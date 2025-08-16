package com.crm.platform.notification.dto;

import java.util.UUID;

public class ContactEvent extends BaseEvent {
    private String action; // CREATED, UPDATED, DELETED
    private UUID contactId;
    private String contactName;
    private String contactEmail;

    public ContactEvent() {
        super();
    }

    public ContactEvent(UUID tenantId, UUID userId, String action, UUID contactId, String contactName) {
        super("contactEvent", tenantId, userId, "contact-service");
        this.action = action;
        this.contactId = contactId;
        this.contactName = contactName;
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UUID getContactId() {
        return contactId;
    }

    public void setContactId(UUID contactId) {
        this.contactId = contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}