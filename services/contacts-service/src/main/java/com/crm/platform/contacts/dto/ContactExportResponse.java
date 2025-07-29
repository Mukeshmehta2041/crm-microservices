package com.crm.platform.contacts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for contact export operations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactExportResponse {
    
    @JsonProperty("exportId")
    private UUID exportId;
    
    @JsonProperty("status")
    private ExportStatus status;
    
    @JsonProperty("format")
    private String format;
    
    @JsonProperty("totalRecords")
    private Integer totalRecords;
    
    @JsonProperty("processedRecords")
    private Integer processedRecords;
    
    @JsonProperty("progress")
    private Double progress;
    
    @JsonProperty("downloadUrl")
    private String downloadUrl;
    
    @JsonProperty("expiresAt")
    private Instant expiresAt;
    
    @JsonProperty("startedAt")
    private Instant startedAt;
    
    @JsonProperty("completedAt")
    private Instant completedAt;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    public enum ExportStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        EXPIRED
    }
    
    // Constructors
    public ContactExportResponse() {
        this.exportId = UUID.randomUUID();
        this.status = ExportStatus.PENDING;
        this.startedAt = Instant.now();
    }
    
    public ContactExportResponse(String format) {
        this();
        this.format = format;
    }
    
    // Getters and setters
    public UUID getExportId() {
        return exportId;
    }
    
    public void setExportId(UUID exportId) {
        this.exportId = exportId;
    }
    
    public ExportStatus getStatus() {
        return status;
    }
    
    public void setStatus(ExportStatus status) {
        this.status = status;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public Integer getTotalRecords() {
        return totalRecords;
    }
    
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    public Integer getProcessedRecords() {
        return processedRecords;
    }
    
    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }
    
    public Double getProgress() {
        return progress;
    }
    
    public void setProgress(Double progress) {
        this.progress = progress;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}