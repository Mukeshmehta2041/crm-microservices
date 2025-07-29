package com.crm.platform.security.incident;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Security incident data structure
 */
public class SecurityIncident implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final IncidentType type;
    private final IncidentSeverity severity;
    private final String title;
    private final String description;
    private final UUID reportedBy;
    private final LocalDateTime createdAt;
    private final Map<String, Object> metadata;
    
    private IncidentStatus status;
    private UUID assignedTo;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private String resolution;
    private final List<IncidentTimelineEntry> timeline;
    
    public SecurityIncident(String id, IncidentType type, IncidentSeverity severity,
                           String title, String description, UUID reportedBy,
                           Map<String, Object> metadata) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.reportedBy = reportedBy;
        this.createdAt = LocalDateTime.now();
        this.metadata = metadata;
        this.status = IncidentStatus.OPEN;
        this.timeline = new ArrayList<>();
        
        // Add initial timeline entry
        addTimelineEntry(new IncidentTimelineEntry(
            "INCIDENT_CREATED",
            "Security incident created",
            reportedBy,
            Map.of("severity", severity.name(), "type", type.name())
        ));
    }
    
    public String getId() {
        return id;
    }
    
    public IncidentType getType() {
        return type;
    }
    
    public IncidentSeverity getSeverity() {
        return severity;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public UUID getReportedBy() {
        return reportedBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public IncidentStatus getStatus() {
        return status;
    }
    
    public UUID getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    
    public LocalDateTime getClosedAt() {
        return closedAt;
    }
    
    public String getResolution() {
        return resolution;
    }
    
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
    
    public List<IncidentTimelineEntry> getTimeline() {
        return new ArrayList<>(timeline);
    }
    
    public void addTimelineEntry(IncidentTimelineEntry entry) {
        timeline.add(entry);
    }
    
    public void updateStatus(IncidentStatus newStatus, UUID updatedBy, String notes) {
        IncidentStatus oldStatus = this.status;
        this.status = newStatus;
        
        if (newStatus == IncidentStatus.RESOLVED) {
            this.resolvedAt = LocalDateTime.now();
        } else if (newStatus == IncidentStatus.CLOSED) {
            this.closedAt = LocalDateTime.now();
        }
        
        // Add timeline entry
        addTimelineEntry(new IncidentTimelineEntry(
            "STATUS_CHANGED",
            String.format("Status changed from %s to %s. Notes: %s", 
                         oldStatus, newStatus, notes != null ? notes : "None"),
            updatedBy,
            Map.of("old_status", oldStatus.name(), "new_status", newStatus.name())
        ));
    }
    
    public boolean isActive() {
        return status == IncidentStatus.OPEN || status == IncidentStatus.IN_PROGRESS;
    }
    
    public boolean isResolved() {
        return status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED;
    }
    
    @Override
    public String toString() {
        return "SecurityIncident{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", severity=" + severity +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
}