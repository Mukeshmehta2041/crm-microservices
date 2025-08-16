package com.crm.platform.security.incident;

/**
 * Types of security incidents
 */
public enum IncidentType {
    DATA_BREACH("Unauthorized access to sensitive data"),
    MALWARE_INFECTION("Malware or virus detected"),
    PHISHING_ATTACK("Phishing attempt detected"),
    BRUTE_FORCE_ATTACK("Brute force login attempts"),
    DENIAL_OF_SERVICE("Service availability compromised"),
    UNAUTHORIZED_ACCESS("Unauthorized system access"),
    DATA_LOSS("Data loss or corruption"),
    INSIDER_THREAT("Suspicious insider activity"),
    SYSTEM_COMPROMISE("System security compromise"),
    VULNERABILITY_EXPLOIT("Security vulnerability exploited"),
    SOCIAL_ENGINEERING("Social engineering attack"),
    PHYSICAL_SECURITY("Physical security breach"),
    CONFIGURATION_ERROR("Security misconfiguration"),
    COMPLIANCE_VIOLATION("Compliance requirement violation"),
    OTHER("Other security incident");
    
    private final String description;
    
    IncidentType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}