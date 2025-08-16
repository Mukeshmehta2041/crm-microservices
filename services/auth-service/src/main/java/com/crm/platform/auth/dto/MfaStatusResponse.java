package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MfaStatusResponse {

    @JsonProperty("mfa_enabled")
    private boolean mfaEnabled;

    private String method;

    @JsonProperty("backup_codes_count")
    private Integer backupCodesCount;

    @JsonProperty("trusted_devices_count")
    private Integer trustedDevicesCount;

    @JsonProperty("setup_completed_at")
    private LocalDateTime setupCompletedAt;

    @JsonProperty("last_used_at")
    private LocalDateTime lastUsedAt;

    @JsonProperty("available_methods")
    private List<String> availableMethods;

    @JsonProperty("recovery_options")
    private List<String> recoveryOptions;

    // Constructors
    public MfaStatusResponse() {}

    public MfaStatusResponse(boolean mfaEnabled, String method) {
        this.mfaEnabled = mfaEnabled;
        this.method = method;
    }

    // Getters and Setters
    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Integer getBackupCodesCount() { return backupCodesCount; }
    public void setBackupCodesCount(Integer backupCodesCount) { this.backupCodesCount = backupCodesCount; }

    public Integer getTrustedDevicesCount() { return trustedDevicesCount; }
    public void setTrustedDevicesCount(Integer trustedDevicesCount) { this.trustedDevicesCount = trustedDevicesCount; }

    public LocalDateTime getSetupCompletedAt() { return setupCompletedAt; }
    public void setSetupCompletedAt(LocalDateTime setupCompletedAt) { this.setupCompletedAt = setupCompletedAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public List<String> getAvailableMethods() { return availableMethods; }
    public void setAvailableMethods(List<String> availableMethods) { this.availableMethods = availableMethods; }

    public List<String> getRecoveryOptions() { return recoveryOptions; }
    public void setRecoveryOptions(List<String> recoveryOptions) { this.recoveryOptions = recoveryOptions; }
}