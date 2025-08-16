package com.crm.platform.security.compliance;

/**
 * Supported compliance standards
 */
public enum ComplianceStandard {
    GDPR("General Data Protection Regulation", "EU data protection regulation"),
    CCPA("California Consumer Privacy Act", "California privacy law"),
    HIPAA("Health Insurance Portability and Accountability Act", "US healthcare privacy law"),
    SOC2("Service Organization Control 2", "Security and availability controls"),
    ISO27001("ISO/IEC 27001", "Information security management standard"),
    PCI_DSS("Payment Card Industry Data Security Standard", "Credit card data protection"),
    FERPA("Family Educational Rights and Privacy Act", "Educational records privacy");
    
    private final String fullName;
    private final String description;
    
    ComplianceStandard(String fullName, String description) {
        this.fullName = fullName;
        this.description = description;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getDescription() {
        return description;
    }
}