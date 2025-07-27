package com.crm.platform.contacts.dto;

import java.util.List;
import java.util.UUID;

public class BulkContactRequest {

    private List<ContactRequest> contacts;
    private Boolean validateOnly = false;
    private Boolean skipDuplicates = true;
    private Boolean updateExisting = false;
    private String duplicateMatchField = "email"; // email, phone, or fullName
    private UUID batchId;

    public List<ContactRequest> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactRequest> contacts) {
        this.contacts = contacts;
    }

    public Boolean getValidateOnly() {
        return validateOnly;
    }

    public void setValidateOnly(Boolean validateOnly) {
        this.validateOnly = validateOnly;
    }

    public Boolean getSkipDuplicates() {
        return skipDuplicates;
    }

    public void setSkipDuplicates(Boolean skipDuplicates) {
        this.skipDuplicates = skipDuplicates;
    }

    public Boolean getUpdateExisting() {
        return updateExisting;
    }

    public void setUpdateExisting(Boolean updateExisting) {
        this.updateExisting = updateExisting;
    }

    public String getDuplicateMatchField() {
        return duplicateMatchField;
    }

    public void setDuplicateMatchField(String duplicateMatchField) {
        this.duplicateMatchField = duplicateMatchField;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }
}

