package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request for bulk operations (create, update, delete)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkOperationRequest<T> {
    
    @JsonProperty("operation")
    private BulkOperation operation;
    
    @JsonProperty("data")
    private List<T> data;
    
    @JsonProperty("ids")
    private List<UUID> ids;
    
    @JsonProperty("filters")
    private List<FilterCriteria> filters;
    
    @JsonProperty("updateFields")
    private Map<String, Object> updateFields;
    
    @JsonProperty("validationOnly")
    private Boolean validationOnly = false;
    
    @JsonProperty("continueOnError")
    private Boolean continueOnError = true;
    
    @JsonProperty("batchSize")
    private Integer batchSize = 100;
    
    @JsonProperty("async")
    private Boolean async = false;
    
    public enum BulkOperation {
        CREATE,
        UPDATE,
        DELETE,
        UPSERT
    }
    
    // Constructors
    public BulkOperationRequest() {}
    
    public BulkOperationRequest(BulkOperation operation, List<T> data) {
        this.operation = operation;
        this.data = data;
    }
    
    public BulkOperationRequest(BulkOperation operation, List<UUID> ids, Map<String, Object> updateFields) {
        this.operation = operation;
        this.ids = ids;
        this.updateFields = updateFields;
    }
    
    // Static factory methods
    public static <T> BulkOperationRequest<T> create(List<T> data) {
        return new BulkOperationRequest<>(BulkOperation.CREATE, data);
    }
    
    public static <T> BulkOperationRequest<T> update(List<T> data) {
        return new BulkOperationRequest<>(BulkOperation.UPDATE, data);
    }
    
    public static <T> BulkOperationRequest<T> delete(List<UUID> ids) {
        BulkOperationRequest<T> request = new BulkOperationRequest<>();
        request.setOperation(BulkOperation.DELETE);
        request.setIds(ids);
        return request;
    }
    
    public static <T> BulkOperationRequest<T> updateByIds(List<UUID> ids, Map<String, Object> updateFields) {
        return new BulkOperationRequest<>(BulkOperation.UPDATE, ids, updateFields);
    }
    
    // Getters and setters
    public BulkOperation getOperation() {
        return operation;
    }
    
    public void setOperation(BulkOperation operation) {
        this.operation = operation;
    }
    
    public List<T> getData() {
        return data;
    }
    
    public void setData(List<T> data) {
        this.data = data;
    }
    
    public List<UUID> getIds() {
        return ids;
    }
    
    public void setIds(List<UUID> ids) {
        this.ids = ids;
    }
    
    public List<FilterCriteria> getFilters() {
        return filters;
    }
    
    public void setFilters(List<FilterCriteria> filters) {
        this.filters = filters;
    }
    
    public Map<String, Object> getUpdateFields() {
        return updateFields;
    }
    
    public void setUpdateFields(Map<String, Object> updateFields) {
        this.updateFields = updateFields;
    }
    
    public Boolean getValidationOnly() {
        return validationOnly;
    }
    
    public void setValidationOnly(Boolean validationOnly) {
        this.validationOnly = validationOnly;
    }
    
    public Boolean getContinueOnError() {
        return continueOnError;
    }
    
    public void setContinueOnError(Boolean continueOnError) {
        this.continueOnError = continueOnError;
    }
    
    public Integer getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(Integer batchSize) {
        this.batchSize = Math.min(Math.max(1, batchSize != null ? batchSize : 100), 1000);
    }
    
    public Boolean getAsync() {
        return async;
    }
    
    public void setAsync(Boolean async) {
        this.async = async;
    }
}