package com.crm.platform.security.compliance;

/**
 * Types of compliance events for audit trail
 */
public enum ComplianceEventType {
    DATA_ACCESS("Data access event"),
    DATA_MODIFICATION("Data modification event"),
    DATA_DELETION("Data deletion event"),
    DATA_EXPORT("Data export event"),
    DATA_IMPORT("Data import event"),
    USER_LOGIN("User login event"),
    USER_LOGOUT("User logout event"),
    PERMISSION_CHANGE("Permission change event"),
    CONFIGURATION_CHANGE("Configuration change event"),
    SECURITY_INCIDENT("Security incident"),
    PRIVACY_REQUEST("Privacy request"),
    CONSENT_CHANGE("Consent change event"),
    DATA_BREACH("Data breach event"),
    SYSTEM_ACCESS("System access event"),
    ADMIN_ACTION("Administrative action");
    
    private final String description;
    
    ComplianceEventType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}