package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response for bulk operations with detailed progress and error tracking
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkOperationResponse<T> {
    
    @JsonProperty("jobId")
    private UUID jobId;
    
    @JsonProperty("status")
    private BulkOperationStatus status;
    
    @JsonProperty("totalRecords")
    private Integer totalRecords;
    
    @JsonProperty("processedRecords")
    private Integer processedRecords;
    
    @JsonProperty("successfulRecords")
    private Integer successfulRecords;
    
    @JsonProperty("failedRecords")
    private Integer failedRecords;
    
    @JsonProperty("skippedRecords")
    private Integer skippedRecords;
    
    @JsonProperty("progress")
    private Double progress;
    
    @JsonProperty("startedAt")
    private Instant startedAt;
    
    @JsonProperty("completedAt")
    private Instant completedAt;
    
    @JsonProperty("estimatedTimeRemaining")
    private Long estimatedTimeRemaining;
    
    @JsonProperty("successfulResults")
    private List<T> successfulResults;
    
    @JsonProperty("errors")
    private List<BulkOperationError> errors;
    
    @JsonProperty("warnings")
    private List<BulkOperationWarning> warnings;
    
    @JsonProperty("downloadUrl")
    private String downloadUrl;
    
    public enum BulkOperationStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        PARTIALLY_COMPLETED
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkOperationError {
        @JsonProperty("recordIndex")
        private Integer recordIndex;
        
        @JsonProperty("recordId")
        private UUID recordId;
        
        @JsonProperty("errorCode")
        private String errorCode;
        
        @JsonProperty("errorMessage")
        private String errorMessage;
        
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("value")
        private Object value;
        
        @JsonProperty("constraint")
        private String constraint;
        
        // Constructors
        public BulkOperationError() {}
        
        public BulkOperationError(Integer recordIndex, String errorCode, String errorMessage) {
            this.recordIndex = recordIndex;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
        
        public BulkOperationError(Integer recordIndex, String errorCode, String errorMessage, String field, Object value) {
            this.recordIndex = recordIndex;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.field = field;
            this.value = value;
        }
        
        // Getters and setters
        public Integer getRecordIndex() {
            return recordIndex;
        }
        
        public void setRecordIndex(Integer recordIndex) {
            this.recordIndex = recordIndex;
        }
        
        public UUID getRecordId() {
            return recordId;
        }
        
        public void setRecordId(UUID recordId) {
            this.recordId = recordId;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public Object getValue() {
            return value;
        }
        
        public void setValue(Object value) {
            this.value = value;
        }
        
        public String getConstraint() {
            return constraint;
        }
        
        public void setConstraint(String constraint) {
            this.constraint = constraint;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkOperationWarning {
        @JsonProperty("recordIndex")
        private Integer recordIndex;
        
        @JsonProperty("warningCode")
        private String warningCode;
        
        @JsonProperty("warningMessage")
        private String warningMessage;
        
        @JsonProperty("field")
        private String field;
        
        // Constructors
        public BulkOperationWarning() {}
        
        public BulkOperationWarning(Integer recordIndex, String warningCode, String warningMessage) {
            this.recordIndex = recordIndex;
            this.warningCode = warningCode;
            this.warningMessage = warningMessage;
        }
        
        // Getters and setters
        public Integer getRecordIndex() {
            return recordIndex;
        }
        
        public void setRecordIndex(Integer recordIndex) {
            this.recordIndex = recordIndex;
        }
        
        public String getWarningCode() {
            return warningCode;
        }
        
        public void setWarningCode(String warningCode) {
            this.warningCode = warningCode;
        }
        
        public String getWarningMessage() {
            return warningMessage;
        }
        
        public void setWarningMessage(String warningMessage) {
            this.warningMessage = warningMessage;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
    }
    
    // Constructors
    public BulkOperationResponse() {
        this.jobId = UUID.randomUUID();
        this.status = BulkOperationStatus.PENDING;
        this.startedAt = Instant.now();
    }
    
    public BulkOperationResponse(UUID jobId) {
        this.jobId = jobId;
        this.status = BulkOperationStatus.PENDING;
        this.startedAt = Instant.now();
    }
    
    // Utility methods
    public void calculateProgress() {
        if (totalRecords != null && totalRecords > 0) {
            this.progress = (double) (processedRecords != null ? processedRecords : 0) / totalRecords * 100.0;
        }
    }
    
    public void markCompleted() {
        this.status = BulkOperationStatus.COMPLETED;
        this.completedAt = Instant.now();
        calculateProgress();
    }
    
    public void markFailed() {
        this.status = BulkOperationStatus.FAILED;
        this.completedAt = Instant.now();
        calculateProgress();
    }
    
    public void markPartiallyCompleted() {
        this.status = BulkOperationStatus.PARTIALLY_COMPLETED;
        this.completedAt = Instant.now();
        calculateProgress();
    }
    
    // Getters and setters
    public UUID getJobId() {
        return jobId;
    }
    
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
    
    public BulkOperationStatus getStatus() {
        return status;
    }
    
    public void setStatus(BulkOperationStatus status) {
        this.status = status;
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
    
    public Integer getSuccessfulRecords() {
        return successfulRecords;
    }
    
    public void setSuccessfulRecords(Integer successfulRecords) {
        this.successfulRecords = successfulRecords;
    }
    
    public Integer getFailedRecords() {
        return failedRecords;
    }
    
    public void setFailedRecords(Integer failedRecords) {
        this.failedRecords = failedRecords;
    }
    
    public Integer getSkippedRecords() {
        return skippedRecords;
    }
    
    public void setSkippedRecords(Integer skippedRecords) {
        this.skippedRecords = skippedRecords;
    }
    
    public Double getProgress() {
        return progress;
    }
    
    public void setProgress(Double progress) {
        this.progress = progress;
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
    
    public Long getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }
    
    public void setEstimatedTimeRemaining(Long estimatedTimeRemaining) {
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }
    
    public List<T> getSuccessfulResults() {
        return successfulResults;
    }
    
    public void setSuccessfulResults(List<T> successfulResults) {
        this.successfulResults = successfulResults;
    }
    
    public List<BulkOperationError> getErrors() {
        return errors;
    }
    
    public void setErrors(List<BulkOperationError> errors) {
        this.errors = errors;
    }
    
    public List<BulkOperationWarning> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<BulkOperationWarning> warnings) {
        this.warnings = warnings;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}