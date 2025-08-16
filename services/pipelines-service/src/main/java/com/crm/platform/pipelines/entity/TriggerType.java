package com.crm.platform.pipelines.entity;

public enum TriggerType {
    DEAL_CREATED("Deal Created", "Triggered when a new deal is created"),
    DEAL_UPDATED("Deal Updated", "Triggered when a deal is updated"),
    STAGE_CHANGED("Stage Changed", "Triggered when a deal moves to a different stage"),
    PROBABILITY_CHANGED("Probability Changed", "Triggered when deal probability changes"),
    AMOUNT_CHANGED("Amount Changed", "Triggered when deal amount changes"),
    DATE_CHANGED("Date Changed", "Triggered when deal dates change"),
    FIELD_UPDATED("Field Updated", "Triggered when specific fields are updated"),
    TIME_BASED("Time Based", "Triggered based on time conditions"),
    ACTIVITY_COMPLETED("Activity Completed", "Triggered when an activity is completed"),
    EMAIL_OPENED("Email Opened", "Triggered when an email is opened"),
    EMAIL_CLICKED("Email Clicked", "Triggered when an email link is clicked");

    private final String displayName;
    private final String description;

    TriggerType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static TriggerType fromString(String value) {
        for (TriggerType type : TriggerType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown trigger type: " + value);
    }
}