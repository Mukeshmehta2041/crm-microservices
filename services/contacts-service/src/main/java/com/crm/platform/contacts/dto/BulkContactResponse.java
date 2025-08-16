package com.crm.platform.contacts.dto;

import java.util.List;
import java.util.UUID;

public class BulkContactResponse {

    private UUID batchId;
    private Integer totalRecords;
    private Integer successCount;
    private Integer errorCount;
    private Integer duplicateCount;
    private List<BulkOperationError> errors;
    private String status; // PROCESSING, COMPLETED, FAILED
    private Integer progressPercentage;

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public List<BulkOperationError> getErrors() {
        return errors;
    }

    public void setErrors(List<BulkOperationError> errors) {
        this.errors = errors;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public static class BulkOperationError {
        private Integer recordIndex;
        private String field;
        private String errorCode;
        private String errorMessage;
        private Object rejectedValue;

        public Integer getRecordIndex() {
            return recordIndex;
        }

        public void setRecordIndex(Integer recordIndex) {
            this.recordIndex = recordIndex;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
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

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
    }
}