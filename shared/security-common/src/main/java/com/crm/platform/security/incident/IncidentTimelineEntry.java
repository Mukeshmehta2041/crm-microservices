package com.crm.platform.security.incident;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Timeline entry for security incident
 */
public class IncidentTimelineEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String action;
    private final String description;
    private final UUID performedBy;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;
    
    public IncidentTimelineEntry(String action, String description, UUID performedBy,
                               Map<String, Object> metadata) {
        this.action = action;
        this.description = description;
        this.performedBy = performedBy;
        this.timestamp = LocalDateTime.now();
        this.metadata = metadata;
    }
    
    public String getAction() {
        return action;
    }
    
    public String getDescription() {
        return description;
    }
    
    public UUID getPerformedBy() {
        return performedBy;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    @Override
    public String toString() {
        return "IncidentTimelineEntry{" +
                "action='" + action + '\'' +
                ", description='" + description + '\'' +
                ", performedBy=" + performedBy +
                ", timestamp=" + timestamp +
                '}';
    }
}