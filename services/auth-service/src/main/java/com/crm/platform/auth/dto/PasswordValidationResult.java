package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * DTO for password validation results
 */
public class PasswordValidationResult {
    
    @JsonProperty("is_valid")
    private boolean isValid;
    
    @JsonProperty("strength_score")
    private int strengthScore; // 0-100
    
    @JsonProperty("strength_level")
    private StrengthLevel strengthLevel;
    
    @JsonProperty("validation_errors")
    private List<String> validationErrors;
    
    @JsonProperty("suggestions")
    private List<String> suggestions;
    
    @JsonProperty("is_compromised")
    private boolean isCompromised;

    public PasswordValidationResult() {
        this.validationErrors = new ArrayList<>();
        this.suggestions = new ArrayList<>();
    }

    public PasswordValidationResult(boolean isValid, int strengthScore, StrengthLevel strengthLevel) {
        this();
        this.isValid = isValid;
        this.strengthScore = strengthScore;
        this.strengthLevel = strengthLevel;
    }

    // Getters and Setters
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }

    public int getStrengthScore() { return strengthScore; }
    public void setStrengthScore(int strengthScore) { this.strengthScore = strengthScore; }

    public StrengthLevel getStrengthLevel() { return strengthLevel; }
    public void setStrengthLevel(StrengthLevel strengthLevel) { this.strengthLevel = strengthLevel; }

    public List<String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public boolean isCompromised() { return isCompromised; }
    public void setCompromised(boolean compromised) { isCompromised = compromised; }

    // Helper methods
    public void addValidationError(String error) {
        this.validationErrors.add(error);
        this.isValid = false;
    }

    public void addSuggestion(String suggestion) {
        this.suggestions.add(suggestion);
    }

    public enum StrengthLevel {
        VERY_WEAK, WEAK, FAIR, GOOD, STRONG, VERY_STRONG
    }
}