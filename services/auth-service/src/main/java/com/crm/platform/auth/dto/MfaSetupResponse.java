package com.crm.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MfaSetupResponse {
    
    private String method;
    private String secret;
    
    @JsonProperty("qr_code")
    private String qrCode;
    
    @JsonProperty("backup_codes")
    private List<String> backupCodes;
    
    @JsonProperty("setup_complete")
    private boolean setupComplete;

    public MfaSetupResponse() {}

    public MfaSetupResponse(String method, String secret, String qrCode, List<String> backupCodes, boolean setupComplete) {
        this.method = method;
        this.secret = secret;
        this.qrCode = qrCode;
        this.backupCodes = backupCodes;
        this.setupComplete = setupComplete;
    }

    // Getters and Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public List<String> getBackupCodes() { return backupCodes; }
    public void setBackupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; }

    public boolean isSetupComplete() { return setupComplete; }
    public void setSetupComplete(boolean setupComplete) { this.setupComplete = setupComplete; }
}