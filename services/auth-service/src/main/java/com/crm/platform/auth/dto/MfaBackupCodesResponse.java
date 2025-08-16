package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class MfaBackupCodesResponse {
    
    @JsonProperty("backup_codes")
    private List<String> backupCodes;
    
    @JsonProperty("unused_codes")
    private int unusedCodes;
    
    @JsonProperty("total_codes")
    private int totalCodes;
    
    @JsonProperty("generated_at")
    private Instant generatedAt;
    
    @JsonProperty("remaining_codes")
    private int remainingCodes;

    public MfaBackupCodesResponse() {}

    public MfaBackupCodesResponse(List<String> backupCodes, int unusedCodes, int totalCodes, Instant generatedAt) {
        this.backupCodes = backupCodes;
        this.unusedCodes = unusedCodes;
        this.totalCodes = totalCodes;
        this.generatedAt = generatedAt;
    }

    // Getters and Setters
    public List<String> getBackupCodes() { return backupCodes; }
    public void setBackupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; }

    public int getUnusedCodes() { return unusedCodes; }
    public void setUnusedCodes(int unusedCodes) { this.unusedCodes = unusedCodes; }

    public int getTotalCodes() { return totalCodes; }
    public void setTotalCodes(int totalCodes) { this.totalCodes = totalCodes; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public int getRemainingCodes() { return remainingCodes; }
    public void setRemainingCodes(int remainingCodes) { this.remainingCodes = remainingCodes; }
}