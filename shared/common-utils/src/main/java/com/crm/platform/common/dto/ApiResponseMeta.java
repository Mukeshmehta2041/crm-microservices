package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseMeta {

  private String timestamp;
  private String version;

  @JsonProperty("request_id")
  private String requestId;

  private PaginationMeta pagination;

  @JsonProperty("rate_limit")
  private RateLimitMeta rateLimit;

  @JsonProperty("processing_time")
  private String processingTime;

  @JsonProperty("cache_hit")
  private Boolean cacheHit;

  private String message;

  public ApiResponseMeta() {
  }

  public ApiResponseMeta(String timestamp, String version, String requestId,
      PaginationMeta pagination, RateLimitMeta rateLimit, String processingTime) {
    this.timestamp = timestamp;
    this.version = version;
    this.requestId = requestId;
    this.pagination = pagination;
    this.rateLimit = rateLimit;
    this.processingTime = processingTime;
  }

  public ApiResponseMeta(String timestamp, String version, String requestId,
      PaginationMeta pagination, RateLimitMeta rateLimit, String processingTime, String message) {
    this.timestamp = timestamp;
    this.version = version;
    this.requestId = requestId;
    this.pagination = pagination;
    this.rateLimit = rateLimit;
    this.processingTime = processingTime;
    this.message = message;
  }

  // Getters and Setters
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

  public PaginationMeta getPagination() {
    return pagination;
  }

  public void setPagination(PaginationMeta pagination) {
    this.pagination = pagination;
  }

  public RateLimitMeta getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(RateLimitMeta rateLimit) {
    this.rateLimit = rateLimit;
  }

  public String getProcessingTime() {
    return processingTime;
  }

  public void setProcessingTime(String processingTime) {
    this.processingTime = processingTime;
  }

  public Boolean getCacheHit() {
    return cacheHit;
  }

  public void setCacheHit(Boolean cacheHit) {
    this.cacheHit = cacheHit;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}