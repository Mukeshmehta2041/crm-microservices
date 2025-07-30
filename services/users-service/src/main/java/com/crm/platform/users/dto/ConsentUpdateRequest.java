package com.crm.platform.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating user consent
 */
public class ConsentUpdateRequest {
    
    @NotNull(message = "Consent value is required")
    @JsonProperty("consent_given")
    private Boolean consentGiven;
    
    @JsonProperty("consent_type")
    private ConsentType consentType = ConsentType.GDPR;

    public ConsentUpdateRequest() {}

    public ConsentUpdateRequest(Boolean consentGiven, ConsentType consentType) {
        this.consentGiven = consentGiven;
        this.consentType = consentType;
    }

    // Getters and Setters
    public Boolean getConsentGiven() { return consentGiven; }
    public void setConsentGiven(Boolean consentGiven) { this.consentGiven = consentGiven; }

    public ConsentType getConsentType() { return consentType; }
    public void setConsentType(ConsentType consentType) { this.consentType = consentType; }

    public enum ConsentType {
        GDPR, MARKETING
    }
}