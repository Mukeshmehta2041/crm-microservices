package com.crm.platform.leads.service;

import com.crm.platform.leads.exception.LeadBusinessException;
import com.crm.platform.common.util.TenantContext;
import com.crm.platform.leads.dto.LeadRequest;
import com.crm.platform.leads.repository.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class LeadValidationService {

    private static final Logger logger = LoggerFactory.getLogger(LeadValidationService.class);

    private final LeadRepository leadRepository;

    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[\\+]?[1-9]?[0-9]{7,15}$"
    );
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?(www\\.)?[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.[a-zA-Z]{2,}$"
    );

    @Autowired
    public LeadValidationService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    /**
     * Validate lead request data
     */
    public void validateLeadRequest(LeadRequest request) {
        List<String> errors = new ArrayList<>();

        // Required field validation
        validateRequiredFields(request, errors);

        // Format validation
        validateFormats(request, errors);

        // Business rule validation
        validateBusinessRules(request, errors);

        if (!errors.isEmpty()) {
            throw new LeadBusinessException("VALIDATION_FAILED", "Lead validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Check for duplicate leads by email or phone
     */
    public void checkForDuplicates(String email, String phone) {
        checkForDuplicatesExcluding(email, phone, null);
    }

    /**
     * Check for duplicate leads excluding a specific lead ID
     */
    public void checkForDuplicatesExcluding(String email, String phone, UUID excludeLeadId) {
        UUID tenantId = TenantContext.getTenantId();

        // Check email duplication
        if (StringUtils.hasText(email)) {
            var existingByEmail = leadRepository.findByTenantIdAndEmail(tenantId, email);
            if (existingByEmail.isPresent() && 
                (excludeLeadId == null || !existingByEmail.get().getId().equals(excludeLeadId))) {
                throw new LeadBusinessException("DUPLICATE_EMAIL", "Lead with this email already exists");
            }
        }

        // Check phone duplication
        if (StringUtils.hasText(phone)) {
            var existingByPhone = leadRepository.findByTenantIdAndPhone(tenantId, phone);
            if (existingByPhone.isPresent() && 
                (excludeLeadId == null || !existingByPhone.get().getId().equals(excludeLeadId))) {
                throw new LeadBusinessException("DUPLICATE_PHONE", "Lead with this phone already exists");
            }
        }
    }

    /**
     * Validate lead data quality and completeness
     */
    public LeadDataQuality assessDataQuality(LeadRequest request) {
        LeadDataQuality quality = new LeadDataQuality();
        int totalFields = 0;
        int completedFields = 0;
        List<String> missingFields = new ArrayList<>();
        List<String> qualityIssues = new ArrayList<>();

        // Check essential fields
        String[] essentialFields = {"firstName", "lastName", "email", "phone", "company"};
        for (String field : essentialFields) {
            totalFields++;
            if (hasValue(getFieldValue(request, field))) {
                completedFields++;
            } else {
                missingFields.add(field);
            }
        }

        // Check additional fields
        String[] additionalFields = {"title", "industry", "website", "leadSource", "annualRevenue", "numberOfEmployees"};
        for (String field : additionalFields) {
            totalFields++;
            if (hasValue(getFieldValue(request, field))) {
                completedFields++;
            }
        }

        // Calculate completeness percentage
        quality.setCompletenessPercentage((completedFields * 100) / totalFields);
        quality.setMissingFields(missingFields);

        // Check data quality issues
        if (StringUtils.hasText(request.getEmail()) && !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            qualityIssues.add("Invalid email format");
        }

        if (StringUtils.hasText(request.getPhone()) && !PHONE_PATTERN.matcher(request.getPhone().replaceAll("[\\s\\-\\(\\)]", "")).matches()) {
            qualityIssues.add("Invalid phone format");
        }

        if (StringUtils.hasText(request.getWebsite()) && !URL_PATTERN.matcher(request.getWebsite()).matches()) {
            qualityIssues.add("Invalid website format");
        }

        quality.setQualityIssues(qualityIssues);

        // Determine overall quality score
        int qualityScore = quality.getCompletenessPercentage();
        if (!qualityIssues.isEmpty()) {
            qualityScore -= (qualityIssues.size() * 10); // Deduct 10 points per quality issue
        }
        quality.setQualityScore(Math.max(0, qualityScore));

        return quality;
    }

    /**
     * Validate lead scoring prerequisites
     */
    public void validateScoringPrerequisites(LeadRequest request) {
        List<String> warnings = new ArrayList<>();

        // Check for minimum required information for accurate scoring
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhone())) {
            warnings.add("No contact information provided - scoring may be inaccurate");
        }

        if (!StringUtils.hasText(request.getCompany())) {
            warnings.add("Company information missing - firmographic scoring unavailable");
        }

        if (request.getAnnualRevenue() == null && request.getNumberOfEmployees() == null) {
            warnings.add("Company size information missing - firmographic scoring limited");
        }

        if (!StringUtils.hasText(request.getTitle())) {
            warnings.add("Job title missing - role-based scoring unavailable");
        }

        if (!warnings.isEmpty()) {
            logger.warn("Lead scoring prerequisites not fully met: {}", String.join(", ", warnings));
        }
    }

    private void validateRequiredFields(LeadRequest request, List<String> errors) {
        if (!StringUtils.hasText(request.getFirstName())) {
            errors.add("First name is required");
        }

        if (!StringUtils.hasText(request.getLastName())) {
            errors.add("Last name is required");
        }

        // At least one contact method should be provided
        if (!StringUtils.hasText(request.getEmail()) && 
            !StringUtils.hasText(request.getPhone()) && 
            !StringUtils.hasText(request.getMobile())) {
            errors.add("At least one contact method (email, phone, or mobile) is required");
        }
    }

    private void validateFormats(LeadRequest request, List<String> errors) {
        // Email format validation
        if (StringUtils.hasText(request.getEmail()) && 
            !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            errors.add("Invalid email format");
        }

        // Phone format validation
        if (StringUtils.hasText(request.getPhone())) {
            String cleanPhone = request.getPhone().replaceAll("[\\s\\-\\(\\)]", "");
            if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                errors.add("Invalid phone format");
            }
        }

        // Mobile format validation
        if (StringUtils.hasText(request.getMobile())) {
            String cleanMobile = request.getMobile().replaceAll("[\\s\\-\\(\\)]", "");
            if (!PHONE_PATTERN.matcher(cleanMobile).matches()) {
                errors.add("Invalid mobile format");
            }
        }

        // Website format validation
        if (StringUtils.hasText(request.getWebsite()) && 
            !URL_PATTERN.matcher(request.getWebsite()).matches()) {
            errors.add("Invalid website format");
        }
    }

    private void validateBusinessRules(LeadRequest request, List<String> errors) {
        // Lead score validation
        if (request.getLeadScore() != null && 
            (request.getLeadScore() < 0 || request.getLeadScore() > 100)) {
            errors.add("Lead score must be between 0 and 100");
        }

        // Annual revenue validation
        if (request.getAnnualRevenue() != null && 
            request.getAnnualRevenue().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Annual revenue must be positive");
        }

        // Number of employees validation
        if (request.getNumberOfEmployees() != null && request.getNumberOfEmployees() < 0) {
            errors.add("Number of employees must be positive");
        }

        // Budget validation
        if (request.getBudget() != null && 
            request.getBudget().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Budget must be positive");
        }

        // Contact preferences validation
        if (Boolean.TRUE.equals(request.getDoNotEmail()) && 
            Boolean.TRUE.equals(request.getEmailOptOut()) &&
            Boolean.TRUE.equals(request.getDoNotCall()) &&
            !StringUtils.hasText(request.getPreferredContactMethod())) {
            errors.add("If all contact methods are restricted, preferred contact method must be specified");
        }
    }

    private boolean hasValue(Object value) {
        if (value == null) return false;
        if (value instanceof String) return StringUtils.hasText((String) value);
        return true;
    }

    private Object getFieldValue(LeadRequest request, String fieldName) {
        switch (fieldName) {
            case "firstName": return request.getFirstName();
            case "lastName": return request.getLastName();
            case "email": return request.getEmail();
            case "phone": return request.getPhone();
            case "company": return request.getCompany();
            case "title": return request.getTitle();
            case "industry": return request.getIndustry();
            case "website": return request.getWebsite();
            case "leadSource": return request.getLeadSource();
            case "annualRevenue": return request.getAnnualRevenue();
            case "numberOfEmployees": return request.getNumberOfEmployees();
            default: return null;
        }
    }

    /**
     * Data quality assessment result
     */
    public static class LeadDataQuality {
        private int completenessPercentage;
        private int qualityScore;
        private List<String> missingFields;
        private List<String> qualityIssues;

        public LeadDataQuality() {
            this.missingFields = new ArrayList<>();
            this.qualityIssues = new ArrayList<>();
        }

        // Getters and setters
        public int getCompletenessPercentage() {
            return completenessPercentage;
        }

        public void setCompletenessPercentage(int completenessPercentage) {
            this.completenessPercentage = completenessPercentage;
        }

        public int getQualityScore() {
            return qualityScore;
        }

        public void setQualityScore(int qualityScore) {
            this.qualityScore = qualityScore;
        }

        public List<String> getMissingFields() {
            return missingFields;
        }

        public void setMissingFields(List<String> missingFields) {
            this.missingFields = missingFields;
        }

        public List<String> getQualityIssues() {
            return qualityIssues;
        }

        public void setQualityIssues(List<String> qualityIssues) {
            this.qualityIssues = qualityIssues;
        }
    }
}