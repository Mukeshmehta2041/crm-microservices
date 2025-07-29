package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized API response wrapper for all services
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("data")
    private T data;
    
    @JsonProperty("meta")
    private ResponseMeta meta;
    
    @JsonProperty("errors")
    private List<ErrorDetail> errors;
    
    public ApiResponse() {
        this.meta = new ResponseMeta();
        this.meta.setTimestamp(Instant.now().toString());
    }
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
    
    public static ApiResponse<String> success(String message) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(message);
        return response;
    }
    
    public static <T> ApiResponse<T> error(List<ErrorDetail> errors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setErrors(errors);
        return response;
    }
    
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setErrors(List.of(new ErrorDetail("GENERAL_ERROR", message, null, null, null)));
        return response;
    }
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public ResponseMeta getMeta() {
        return meta;
    }
    
    public void setMeta(ResponseMeta meta) {
        this.meta = meta;
    }
    
    public List<ErrorDetail> getErrors() {
        return errors;
    }
    
    public void setErrors(List<ErrorDetail> errors) {
        this.errors = errors;
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseMeta {
        @JsonProperty("timestamp")
        private String timestamp;
        
        @JsonProperty("version")
        private String version = "v1";
        
        @JsonProperty("requestId")
        private String requestId;
        
        @JsonProperty("processingTime")
        private String processingTime;
        
        @JsonProperty("pagination")
        private PaginationMeta pagination;
        
        @JsonProperty("aggregations")
        private Map<String, Object> aggregations;
        
        @JsonProperty("filters")
        private Map<String, Object> filters;
        
        @JsonProperty("rateLimit")
        private RateLimitMeta rateLimit;
        
        // Getters and setters
        public String getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getRequestId() {
            return requestId;
        }
        
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
        
        public String getProcessingTime() {
            return processingTime;
        }
        
        public void setProcessingTime(String processingTime) {
            this.processingTime = processingTime;
        }
        
        public PaginationMeta getPagination() {
            return pagination;
        }
        
        public void setPagination(PaginationMeta pagination) {
            this.pagination = pagination;
        }
        
        public Map<String, Object> getAggregations() {
            return aggregations;
        }
        
        public void setAggregations(Map<String, Object> aggregations) {
            this.aggregations = aggregations;
        }
        
        public Map<String, Object> getFilters() {
            return filters;
        }
        
        public void setFilters(Map<String, Object> filters) {
            this.filters = filters;
        }
        
        public RateLimitMeta getRateLimit() {
            return rateLimit;
        }
        
        public void setRateLimit(RateLimitMeta rateLimit) {
            this.rateLimit = rateLimit;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationMeta {
        @JsonProperty("page")
        private Integer page;
        
        @JsonProperty("limit")
        private Integer limit;
        
        @JsonProperty("total")
        private Long total;
        
        @JsonProperty("totalPages")
        private Integer totalPages;
        
        @JsonProperty("hasNext")
        private Boolean hasNext;
        
        @JsonProperty("hasPrev")
        private Boolean hasPrev;
        
        // Getters and setters
        public Integer getPage() {
            return page;
        }
        
        public void setPage(Integer page) {
            this.page = page;
        }
        
        public Integer getLimit() {
            return limit;
        }
        
        public void setLimit(Integer limit) {
            this.limit = limit;
        }
        
        public Long getTotal() {
            return total;
        }
        
        public void setTotal(Long total) {
            this.total = total;
        }
        
        public Integer getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(Integer totalPages) {
            this.totalPages = totalPages;
        }
        
        public Boolean getHasNext() {
            return hasNext;
        }
        
        public void setHasNext(Boolean hasNext) {
            this.hasNext = hasNext;
        }
        
        public Boolean getHasPrev() {
            return hasPrev;
        }
        
        public void setHasPrev(Boolean hasPrev) {
            this.hasPrev = hasPrev;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RateLimitMeta {
        @JsonProperty("limit")
        private Integer limit;
        
        @JsonProperty("remaining")
        private Integer remaining;
        
        @JsonProperty("resetAt")
        private String resetAt;
        
        // Getters and setters
        public Integer getLimit() {
            return limit;
        }
        
        public void setLimit(Integer limit) {
            this.limit = limit;
        }
        
        public Integer getRemaining() {
            return remaining;
        }
        
        public void setRemaining(Integer remaining) {
            this.remaining = remaining;
        }
        
        public String getResetAt() {
            return resetAt;
        }
        
        public void setResetAt(String resetAt) {
            this.resetAt = resetAt;
        }
    }
}