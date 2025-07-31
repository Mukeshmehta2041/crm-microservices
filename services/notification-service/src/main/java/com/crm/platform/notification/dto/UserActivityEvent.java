package com.crm.platform.notification.dto;

import java.util.UUID;

public class UserActivityEvent extends BaseEvent {
    private String activityType;
    private String description;
    private UUID targetEntityId;
    private String targetEntityType;

    public UserActivityEvent() {
        super();
    }

    public UserActivityEvent(UUID tenantId, UUID userId, String activityType, String description) {
        super("userActivity", tenantId, userId, "user-service");
        this.activityType = activityType;
        this.description = description;
    }

    // Getters and Setters
    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(UUID targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public void setTargetEntityType(String targetEntityType) {
        this.targetEntityType = targetEntityType;
    }
}