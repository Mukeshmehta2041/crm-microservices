package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for password policy information
 */
public class PasswordPolicyResponse {
    
    @JsonProperty("min_length")
    private int minLength;
    
    @JsonProperty("max_length")
    private int maxLength;
    
    @JsonProperty("require_uppercase")
    private boolean requireUppercase;
    
    @JsonProperty("require_lowercase")
    private boolean requireLowercase;
    
    @JsonProperty("require_numbers")
    private boolean requireNumbers;
    
    @JsonProperty("require_special_chars")
    private boolean requireSpecialChars;
    
    @JsonProperty("min_special_chars")
    private int minSpecialChars;
    
    @JsonProperty("prevent_common_passwords")
    private boolean preventCommonPasswords;
    
    @JsonProperty("prevent_password_reuse")
    private boolean preventPasswordReuse;
    
    @JsonProperty("password_history_count")
    private int passwordHistoryCount;
    
    @JsonProperty("password_expiry_days")
    private Integer passwordExpiryDays;
    
    @JsonProperty("allowed_special_chars")
    private String allowedSpecialChars;

    public PasswordPolicyResponse() {}

    public PasswordPolicyResponse(int minLength, int maxLength, boolean requireUppercase, 
                                boolean requireLowercase, boolean requireNumbers, 
                                boolean requireSpecialChars, int minSpecialChars,
                                boolean preventCommonPasswords, boolean preventPasswordReuse,
                                int passwordHistoryCount, Integer passwordExpiryDays,
                                String allowedSpecialChars) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireNumbers = requireNumbers;
        this.requireSpecialChars = requireSpecialChars;
        this.minSpecialChars = minSpecialChars;
        this.preventCommonPasswords = preventCommonPasswords;
        this.preventPasswordReuse = preventPasswordReuse;
        this.passwordHistoryCount = passwordHistoryCount;
        this.passwordExpiryDays = passwordExpiryDays;
        this.allowedSpecialChars = allowedSpecialChars;
    }

    // Getters and Setters
    public int getMinLength() { return minLength; }
    public void setMinLength(int minLength) { this.minLength = minLength; }

    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }

    public boolean isRequireUppercase() { return requireUppercase; }
    public void setRequireUppercase(boolean requireUppercase) { this.requireUppercase = requireUppercase; }

    public boolean isRequireLowercase() { return requireLowercase; }
    public void setRequireLowercase(boolean requireLowercase) { this.requireLowercase = requireLowercase; }

    public boolean isRequireNumbers() { return requireNumbers; }
    public void setRequireNumbers(boolean requireNumbers) { this.requireNumbers = requireNumbers; }

    public boolean isRequireSpecialChars() { return requireSpecialChars; }
    public void setRequireSpecialChars(boolean requireSpecialChars) { this.requireSpecialChars = requireSpecialChars; }

    public int getMinSpecialChars() { return minSpecialChars; }
    public void setMinSpecialChars(int minSpecialChars) { this.minSpecialChars = minSpecialChars; }

    public boolean isPreventCommonPasswords() { return preventCommonPasswords; }
    public void setPreventCommonPasswords(boolean preventCommonPasswords) { this.preventCommonPasswords = preventCommonPasswords; }

    public boolean isPreventPasswordReuse() { return preventPasswordReuse; }
    public void setPreventPasswordReuse(boolean preventPasswordReuse) { this.preventPasswordReuse = preventPasswordReuse; }

    public int getPasswordHistoryCount() { return passwordHistoryCount; }
    public void setPasswordHistoryCount(int passwordHistoryCount) { this.passwordHistoryCount = passwordHistoryCount; }

    public Integer getPasswordExpiryDays() { return passwordExpiryDays; }
    public void setPasswordExpiryDays(Integer passwordExpiryDays) { this.passwordExpiryDays = passwordExpiryDays; }

    public String getAllowedSpecialChars() { return allowedSpecialChars; }
    public void setAllowedSpecialChars(String allowedSpecialChars) { this.allowedSpecialChars = allowedSpecialChars; }
}