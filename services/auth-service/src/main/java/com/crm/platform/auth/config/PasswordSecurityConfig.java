package com.crm.platform.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for password security settings
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.password")
public class PasswordSecurityConfig {

    private int minLength = 8;
    private int maxLength = 128;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireNumbers = true;
    private boolean requireSpecialChars = true;
    private int minSpecialChars = 1;
    private boolean preventCommonPasswords = true;
    private boolean preventPasswordReuse = true;
    private int historyCount = 5;
    private Integer expiryDays = 90;
    private int resetTokenExpiryHours = 24;
    private int maxResetAttemptsPerHour = 3;
    private int historyRetentionDays = 365;
    private int resetTokenCleanupDays = 7;

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

    public int getHistoryCount() { return historyCount; }
    public void setHistoryCount(int historyCount) { this.historyCount = historyCount; }

    public Integer getExpiryDays() { return expiryDays; }
    public void setExpiryDays(Integer expiryDays) { this.expiryDays = expiryDays; }

    public int getResetTokenExpiryHours() { return resetTokenExpiryHours; }
    public void setResetTokenExpiryHours(int resetTokenExpiryHours) { this.resetTokenExpiryHours = resetTokenExpiryHours; }

    public int getMaxResetAttemptsPerHour() { return maxResetAttemptsPerHour; }
    public void setMaxResetAttemptsPerHour(int maxResetAttemptsPerHour) { this.maxResetAttemptsPerHour = maxResetAttemptsPerHour; }

    public int getHistoryRetentionDays() { return historyRetentionDays; }
    public void setHistoryRetentionDays(int historyRetentionDays) { this.historyRetentionDays = historyRetentionDays; }

    public int getResetTokenCleanupDays() { return resetTokenCleanupDays; }
    public void setResetTokenCleanupDays(int resetTokenCleanupDays) { this.resetTokenCleanupDays = resetTokenCleanupDays; }
}