package com.crm.platform.security.incident;

/**
 * Security incident status
 */
public enum IncidentStatus {
    OPEN("Incident reported and awaiting triage"),
    IN_PROGRESS("Incident being investigated or resolved"),
    RESOLVED("Incident resolved but awaiting closure"),
    CLOSED("Incident closed and documented"),
    CANCELLED("Incident cancelled (false positive)");
    
    private final String description;
    
    IncidentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}