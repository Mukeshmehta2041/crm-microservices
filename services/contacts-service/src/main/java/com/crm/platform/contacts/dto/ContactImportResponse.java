package com.crm.platform.contacts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for contact import operations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactImportResponse {
    
    @JsonProperty("importId")
    private UUID importId;
    
    @JsonProperty("status")
    private ImportStatus status;
    
    @JsonProperty("fileName")
    private String fileName;
    
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
    
    @JsonProperty("duplicateRecords")
    private Integer duplicateRecords;
    
    @JsonProperty("progress")
    private Double progress;
    
    @JsonProperty("startedAt")
    private Instant startedAt;
    
    @JsonProperty("completedAt")
    private Instant completedAt;
    
    @JsonProperty("estimatedTimeRemaining")
    private Long estimatedTimeRemaining;
    
    @JsonProperty("errors")
    private List<ImportError> errors;
    
    @JsonProperty("warnings")
    private List<ImportWarning> warnings;
    
    @JsonProperty("resultSummary")
    private ImportResultSummary resultSummary;
    
    public enum ImportStatus {
        PENDING,
        VALIDATING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        PARTIALLY_COMPLETED
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImportError {
        @JsonProperty("row")
        private Integer row;
        
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("value")
        private String value;
        
        @JsonProperty("errorCode")
        private String errorCode;
        
        @JsonProperty("errorMessage")
        private String errorMessage;
        
        // Constructors
        public ImportError() {}
        
        public ImportError(Integer row, String field, String errorCode, String errorMessage) {
            this.row = row;
            this.field = field;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
        
        // Getters and setters
        public Integer getRow() {
            return row;
        }
        
        public void setRow(Integer row) {
            this.row = row;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
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
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImportWarning {
        @JsonProperty("row")
        private Integer row;
        
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("warningCode")
        private String warningCode;
        
        @JsonProperty("warningMessage")
        private String warningMessage;
        
        // Constructors
        public ImportWarning() {}
        
        public ImportWarning(Integer row, String field, String warningCode, String warningMessage) {
            this.row = row;
            this.field = field;
            this.warningCode = warningCode;
            this.warningMessage = warningMessage;
        }
        
        // Getters and setters
        public Integer getRow() {
            return row;
        }
        
        public void setRow(Integer row) {
            this.row = row;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
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
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImportResultSummary {
        @JsonProperty("createdContacts")
        private Integer createdContacts;
        
        @JsonProperty("updatedContacts")
        private Integer updatedContacts;
        
        @JsonProperty("mergedContacts")
        private Integer mergedContacts;
        
        @JsonProperty("invalidRecords")
        private Integer invalidRecords;
        
        // Getters and setters
        public Integer getCreatedContacts() {
            return createdContacts;
        }
        
        public void setCreatedContacts(Integer createdContacts) {
            this.createdContacts = createdContacts;
        }
        
        public Integer getUpdatedContacts() {
            return updatedContacts;
        }
        
        public void setUpdatedContacts(Integer updatedContacts) {
            this.updatedContacts = updatedContacts;
        }
        
        public Integer getMergedContacts() {
            return mergedContacts;
        }
        
        public void setMergedContacts(Integer mergedContacts) {
            this.mergedContacts = mergedContacts;
        }
        
        public Integer getInvalidRecords() {
            return invalidRecords;
        }
        
        public void setInvalidRecords(Integer invalidRecords) {
            this.invalidRecords = invalidRecords;
        }
    }
    
    // Constructors
    public ContactImportResponse() {
        this.importId = UUID.randomUUID();
        this.status = ImportStatus.PENDING;
        this.startedAt = Instant.now();
    }
    
    public ContactImportResponse(String fileName) {
        this();
        this.fileName = fileName;
    }
    
    // Getters and setters
    public UUID getImportId() {
        return importId;
    }
    
    public void setImportId(UUID importId) {
        this.importId = importId;
    }
    
    public ImportStatus getStatus() {
        return status;
    }
    
    public void setStatus(ImportStatus status) {
        this.status = status;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
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
    
    public Integer getDuplicateRecords() {
        return duplicateRecords;
    }
    
    public void setDuplicateRecords(Integer duplicateRecords) {
        this.duplicateRecords = duplicateRecords;
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
    
    public List<ImportError> getErrors() {
        return errors;
    }
    
    public void setErrors(List<ImportError> errors) {
        this.errors = errors;
    }
    
    public List<ImportWarning> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<ImportWarning> warnings) {
        this.warnings = warnings;
    }
    
    public ImportResultSummary getResultSummary() {
        return resultSummary;
    }
    
    public void setResultSummary(ImportResultSummary resultSummary) {
        this.resultSummary = resultSummary;
    }
}