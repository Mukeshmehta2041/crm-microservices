package com.crm.platform.leads.entity;

public enum QualificationStatus {
    UNQUALIFIED("Unqualified"),
    MARKETING_QUALIFIED("Marketing Qualified"),
    SALES_QUALIFIED("Sales Qualified"),
    QUALIFIED("Qualified"),
    DISQUALIFIED("Disqualified");

    private final String displayName;

    QualificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isQualified() {
        return this == MARKETING_QUALIFIED || this == SALES_QUALIFIED || this == QUALIFIED;
    }

    public boolean canProgress() {
        return this != DISQUALIFIED;
    }
}