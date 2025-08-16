package com.crm.platform.auth.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of password validation
 */
public class PasswordValidationResult {

    private boolean valid;
    private List<String> errors;
    private List<String> suggestions;
    private StrengthLevel strengthLevel;
    private int score;

    public PasswordValidationResult() {
        this.errors = new ArrayList<>();
        this.suggestions = new ArrayList<>();
        this.valid = true;
        this.score = 0;
        this.strengthLevel = StrengthLevel.VERY_WEAK;
    }

    public PasswordValidationResult(boolean valid) {
        this();
        this.valid = valid;
    }

    // Getters and Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public StrengthLevel getStrengthLevel() { return strengthLevel; }
    public void setStrengthLevel(StrengthLevel strengthLevel) { this.strengthLevel = strengthLevel; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    // Helper methods
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public void addSuggestion(String suggestion) {
        this.suggestions.add(suggestion);
    }

    public void addValidationError(String error) {
        addError(error);
    }

    public List<String> getValidationErrors() {
        return getErrors();
    }

    public void setCompromised(boolean compromised) {
        if (compromised) {
            addError("Password has been compromised in a data breach");
        }
    }

    public void setStrengthScore(int score) {
        this.score = score;
        this.strengthLevel = calculateStrengthLevel(score);
    }

    private StrengthLevel calculateStrengthLevel(int score) {
        if (score < 20) return StrengthLevel.VERY_WEAK;
        if (score < 40) return StrengthLevel.WEAK;
        if (score < 60) return StrengthLevel.FAIR;
        if (score < 80) return StrengthLevel.GOOD;
        if (score < 95) return StrengthLevel.STRONG;
        return StrengthLevel.VERY_STRONG;
    }

    public enum StrengthLevel {
        VERY_WEAK, WEAK, FAIR, GOOD, STRONG, VERY_STRONG
    }
}