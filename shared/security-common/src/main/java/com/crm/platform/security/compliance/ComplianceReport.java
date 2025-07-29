package com.crm.platform.security.compliance;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Compliance report data structure
 */
public class ComplianceReport {
    
    private final ComplianceStandard standard;
    private final UUID tenantId;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final LocalDateTime generatedAt;
    private final Map<String, Object> sections;
    
    public ComplianceReport(ComplianceStandard standard, UUID tenantId, 
                           LocalDateTime startDate, LocalDateTime endDate) {
        this.standard = standard;
        this.tenantId = tenantId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedAt = LocalDateTime.now();
        this.sections = new HashMap<>();
    }
    
    public void addSection(String sectionName, Object sectionData) {
        sections.put(sectionName, sectionData);
    }
    
    public ComplianceStandard getStandard() {
        return standard;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public Map<String, Object> getSections() {
        return sections;
    }
    
    public Object getSection(String sectionName) {
        return sections.get(sectionName);
    }
    
    @Override
    public String toString() {
        return "ComplianceReport{" +
                "standard=" + standard +
                ", tenantId=" + tenantId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", generatedAt=" + generatedAt +
                ", sections=" + sections.keySet() +
                '}';
    }
}