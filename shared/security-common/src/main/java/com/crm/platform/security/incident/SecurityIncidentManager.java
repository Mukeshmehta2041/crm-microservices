package com.crm.platform.security.incident;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing security incidents and response procedures
 */
@Service
public class SecurityIncidentManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityIncidentManager.class);
    
    private static final String INCIDENT_KEY = "security:incidents:";
    private static final String INCIDENT_COUNTER_KEY = "security:incident_counter";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Create a new security incident
     */
    public SecurityIncident createIncident(IncidentType type, IncidentSeverity severity,
                                         String title, String description, UUID reportedBy,
                                         Map<String, Object> metadata) {
        
        String incidentId = generateIncidentId();
        
        SecurityIncident incident = new SecurityIncident(
            incidentId, type, severity, title, description, reportedBy, metadata
        );
        
        // Store incident
        String key = INCIDENT_KEY + incidentId;
        redisTemplate.opsForValue().set(key, incident, 365, TimeUnit.DAYS);
        
        // Add to active incidents list
        redisTemplate.opsForSet().add("security:active_incidents", incidentId);
        
        // Log incident creation
        logger.error("Security incident created - ID: {}, Type: {}, Severity: {}, Title: {}", 
                    incidentId, type, severity, title);
        
        // Trigger automated response based on severity
        triggerAutomatedResponse(incident);
        
        return incident;
    }
    
    /**
     * Update incident status
     */
    public void updateIncidentStatus(String incidentId, IncidentStatus newStatus, 
                                   UUID updatedBy, String notes) {
        SecurityIncident incident = getIncident(incidentId);
        if (incident == null) {
            throw new IllegalArgumentException("Incident not found: " + incidentId);
        }
        
        IncidentStatus oldStatus = incident.getStatus();
        incident.updateStatus(newStatus, updatedBy, notes);
        
        // Update stored incident
        String key = INCIDENT_KEY + incidentId;
        redisTemplate.opsForValue().set(key, incident, 365, TimeUnit.DAYS);
        
        // Remove from active incidents if resolved or closed
        if (newStatus == IncidentStatus.RESOLVED || newStatus == IncidentStatus.CLOSED) {
            redisTemplate.opsForSet().remove("security:active_incidents", incidentId);
        }
        
        logger.info("Incident {} status updated from {} to {} by user {}", 
                   incidentId, oldStatus, newStatus, updatedBy);
    }
    
    /**
     * Add timeline entry to incident
     */
    public void addTimelineEntry(String incidentId, String action, String description, 
                               UUID performedBy, Map<String, Object> metadata) {
        SecurityIncident incident = getIncident(incidentId);
        if (incident == null) {
            throw new IllegalArgumentException("Incident not found: " + incidentId);
        }
        
        IncidentTimelineEntry entry = new IncidentTimelineEntry(
            action, description, performedBy, metadata
        );
        
        incident.addTimelineEntry(entry);
        
        // Update stored incident
        String key = INCIDENT_KEY + incidentId;
        redisTemplate.opsForValue().set(key, incident, 365, TimeUnit.DAYS);
        
        logger.debug("Timeline entry added to incident {}: {}", incidentId, action);
    }
    
    /**
     * Get incident by ID
     */
    public SecurityIncident getIncident(String incidentId) {
        String key = INCIDENT_KEY + incidentId;
        return (SecurityIncident) redisTemplate.opsForValue().get(key);
    }
    
    /**
     * Get all active incidents
     */
    public List<SecurityIncident> getActiveIncidents() {
        Set<Object> incidentIds = redisTemplate.opsForSet().members("security:active_incidents");
        List<SecurityIncident> incidents = new ArrayList<>();
        
        if (incidentIds != null) {
            for (Object incidentId : incidentIds) {
                SecurityIncident incident = getIncident((String) incidentId);
                if (incident != null) {
                    incidents.add(incident);
                }
            }
        }
        
        // Sort by severity and creation time
        incidents.sort((a, b) -> {
            int severityCompare = b.getSeverity().getLevel() - a.getSeverity().getLevel();
            if (severityCompare != 0) {
                return severityCompare;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        
        return incidents;
    }
    
    /**
     * Get incidents by type and date range
     */
    public List<SecurityIncident> getIncidentsByType(IncidentType type, 
                                                   LocalDateTime startDate, 
                                                   LocalDateTime endDate) {
        // This is a simplified implementation
        // In production, you might want to use a more efficient storage/query mechanism
        List<SecurityIncident> allIncidents = getAllIncidents();
        
        return allIncidents.stream()
                .filter(incident -> incident.getType() == type)
                .filter(incident -> !incident.getCreatedAt().isBefore(startDate))
                .filter(incident -> !incident.getCreatedAt().isAfter(endDate))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }
    
    /**
     * Get incident statistics
     */
    public Map<String, Object> getIncidentStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<SecurityIncident> incidents = getAllIncidents().stream()
                .filter(incident -> !incident.getCreatedAt().isBefore(startDate))
                .filter(incident -> !incident.getCreatedAt().isAfter(endDate))
                .toList();
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total incidents
        stats.put("total_incidents", incidents.size());
        
        // By severity
        Map<String, Long> bySeverity = new HashMap<>();
        for (IncidentSeverity severity : IncidentSeverity.values()) {
            long count = incidents.stream()
                    .filter(incident -> incident.getSeverity() == severity)
                    .count();
            bySeverity.put(severity.name(), count);
        }
        stats.put("by_severity", bySeverity);
        
        // By type
        Map<String, Long> byType = new HashMap<>();
        for (IncidentType type : IncidentType.values()) {
            long count = incidents.stream()
                    .filter(incident -> incident.getType() == type)
                    .count();
            byType.put(type.name(), count);
        }
        stats.put("by_type", byType);
        
        // By status
        Map<String, Long> byStatus = new HashMap<>();
        for (IncidentStatus status : IncidentStatus.values()) {
            long count = incidents.stream()
                    .filter(incident -> incident.getStatus() == status)
                    .count();
            byStatus.put(status.name(), count);
        }
        stats.put("by_status", byStatus);
        
        // Average resolution time
        OptionalDouble avgResolutionTime = incidents.stream()
                .filter(incident -> incident.getStatus() == IncidentStatus.RESOLVED)
                .filter(incident -> incident.getResolvedAt() != null)
                .mapToLong(incident -> 
                    java.time.Duration.between(incident.getCreatedAt(), incident.getResolvedAt()).toMinutes())
                .average();
        
        stats.put("average_resolution_time_minutes", 
                 avgResolutionTime.isPresent() ? avgResolutionTime.getAsDouble() : 0);
        
        return stats;
    }
    
    /**
     * Trigger automated response based on incident severity
     */
    private void triggerAutomatedResponse(SecurityIncident incident) {
        switch (incident.getSeverity()) {
            case CRITICAL:
                // Immediate notification to security team
                logger.error("CRITICAL SECURITY INCIDENT - Immediate attention required: {}", 
                           incident.getTitle());
                // In production: send SMS, email, Slack alerts
                break;
                
            case HIGH:
                // Notification to security team
                logger.warn("HIGH SEVERITY SECURITY INCIDENT: {}", incident.getTitle());
                // In production: send email, Slack alerts
                break;
                
            case MEDIUM:
                // Log for review
                logger.warn("MEDIUM SEVERITY SECURITY INCIDENT: {}", incident.getTitle());
                break;
                
            case LOW:
                // Standard logging
                logger.info("LOW SEVERITY SECURITY INCIDENT: {}", incident.getTitle());
                break;
        }
        
        // Add automated response to timeline
        addTimelineEntry(incident.getId(), "AUTOMATED_RESPONSE", 
                        "Automated response triggered based on severity: " + incident.getSeverity(),
                        null, Map.of("severity", incident.getSeverity().name()));
    }
    
    /**
     * Generate unique incident ID
     */
    private String generateIncidentId() {
        Long counter = redisTemplate.opsForValue().increment(INCIDENT_COUNTER_KEY);
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("INC-%s-%04d", date, counter);
    }
    
    /**
     * Get all incidents (for internal use)
     */
    private List<SecurityIncident> getAllIncidents() {
        // This is a simplified implementation
        // In production, you would implement proper pagination and indexing
        Set<String> keys = redisTemplate.keys(INCIDENT_KEY + "*");
        List<SecurityIncident> incidents = new ArrayList<>();
        
        if (keys != null) {
            for (String key : keys) {
                SecurityIncident incident = (SecurityIncident) redisTemplate.opsForValue().get(key);
                if (incident != null) {
                    incidents.add(incident);
                }
            }
        }
        
        return incidents;
    }
}