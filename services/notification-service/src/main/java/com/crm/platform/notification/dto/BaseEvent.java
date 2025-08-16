package com.crm.platform.notification.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserActivityEvent.class, name = "userActivity"),
    @JsonSubTypes.Type(value = ContactEvent.class, name = "contactEvent"),
    @JsonSubTypes.Type(value = DealEvent.class, name = "dealEvent"),
    @JsonSubTypes.Type(value = AuthEvent.class, name = "authEvent"),
    @JsonSubTypes.Type(value = SystemAlertEvent.class, name = "systemAlert")
})
public abstract class BaseEvent {
    private String eventId;
    private String eventType;
    private UUID tenantId;
    private UUID userId;
    private LocalDateTime timestamp;
    private String source;
    private Map<String, Object> metadata;

    // Constructors
    public BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public BaseEvent(String eventType, UUID tenantId, UUID userId, String source) {
        this();
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.userId = userId;
        this.source = source;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}