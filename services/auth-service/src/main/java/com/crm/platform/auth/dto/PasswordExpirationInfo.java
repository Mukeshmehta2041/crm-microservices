package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * DTO for password expiration information
 */
public class PasswordExpirationInfo {
    
    @JsonProperty("is_expired")
    private boolean isExpired;
    
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @JsonProperty("days_until_expiry")
    private Long daysUntilExpiry;
    
    @JsonProperty("password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    @JsonProperty("requires_change")
    private boolean requiresChange;
    
    @JsonProperty("warning_threshold_days")
    private int warningThresholdDays;

    public PasswordExpirationInfo() {}

    public PasswordExpirationInfo(boolean isExpired, LocalDateTime expiresAt, Long daysUntilExpiry,
                                LocalDateTime passwordChangedAt, boolean requiresChange, int warningThresholdDays) {
        this.isExpired = isExpired;
        this.expiresAt = expiresAt;
        this.daysUntilExpiry = daysUntilExpiry;
        this.passwordChangedAt = passwordChangedAt;
        this.requiresChange = requiresChange;
        this.warningThresholdDays = warningThresholdDays;
    }

    // Getters and Setters
    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { isExpired = expired; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Long getDaysUntilExpiry() { return daysUntilExpiry; }
    public void setDaysUntilExpiry(Long daysUntilExpiry) { this.daysUntilExpiry = daysUntilExpiry; }

    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public boolean isRequiresChange() { return requiresChange; }
    public void setRequiresChange(boolean requiresChange) { this.requiresChange = requiresChange; }

    public int getWarningThresholdDays() { return warningThresholdDays; }
    public void setWarningThresholdDays(int warningThresholdDays) { this.warningThresholdDays = warningThresholdDays; }
}