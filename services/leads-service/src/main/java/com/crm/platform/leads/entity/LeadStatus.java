package com.crm.platform.leads.entity;

public enum LeadStatus {
    NEW("New"),
    CONTACTED("Contacted"),
    QUALIFIED("Qualified"),
    UNQUALIFIED("Unqualified"),
    NURTURING("Nurturing"),
    CONVERTED("Converted"),
    LOST("Lost"),
    INACTIVE("Inactive");

    private final String displayName;

    LeadStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this != CONVERTED && this != LOST && this != INACTIVE;
    }

    public boolean isConvertible() {
        return this == QUALIFIED;
    }
}