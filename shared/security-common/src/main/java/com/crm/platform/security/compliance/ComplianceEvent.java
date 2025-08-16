package com.crm.platform.security.compliance;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Compliance event for audit trail
 */
public class ComplianceEvent {
    
    private final ComplianceEventType type;
    private final UUID tenantId;
    private final UUID userId;
    private final String resource;
    private final String action;
    private final String description;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;
    
    public ComplianceEvent(ComplianceEventType type, UUID tenantId, UUID userId,
                          String resource, String action, String description,
                          Map<String, Object> metadata) {
        this.type = type;
        this.tenantId = tenantId;
        this.userId = userId;
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.metadata = metadata;
    }
    
    public ComplianceEventType getType() {
        return type;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getAction() {
        return action;
    }
    
    public String getDescription() {
        return description;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    @Override
    public String toString() {
        return "ComplianceEvent{" +
                "type=" + type +
                ", tenantId=" + tenantId +
                ", userId=" + userId +
                ", resource='" + resource + '\'' +
                ", action='" + action + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}