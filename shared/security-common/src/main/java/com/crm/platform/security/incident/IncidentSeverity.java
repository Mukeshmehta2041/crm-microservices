package com.crm.platform.security.incident;

/**
 * Security incident severity levels
 */
public enum IncidentSeverity {
    LOW(1, "Low impact - minimal business disruption"),
    MEDIUM(2, "Medium impact - some business disruption"),
    HIGH(3, "High impact - significant business disruption"),
    CRITICAL(4, "Critical impact - severe business disruption");
    
    private final int level;
    private final String description;
    
    IncidentSeverity(int level, String description) {
        this.level = level;
        this.description = description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDescription() {
        return description;
    }
}