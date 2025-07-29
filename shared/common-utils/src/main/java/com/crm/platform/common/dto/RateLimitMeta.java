package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class RateLimitMeta {
    
    private int limit;
    private int remaining;
    
    @JsonProperty("reset_at")
    private Instant resetAt;
    
    @JsonProperty("retry_after")
    private Integer retryAfter;

    public RateLimitMeta() {}

    public RateLimitMeta(int limit, int remaining, Instant resetAt) {
        this.limit = limit;
        this.remaining = remaining;
        this.resetAt = resetAt;
    }

    public RateLimitMeta(int limit, int remaining, Instant resetAt, Integer retryAfter) {
        this.limit = limit;
        this.remaining = remaining;
        this.resetAt = resetAt;
        this.retryAfter = retryAfter;
    }

    // Getters and Setters
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public int getRemaining() { return remaining; }
    public void setRemaining(int remaining) { this.remaining = remaining; }

    public Instant getResetAt() { return resetAt; }
    public void setResetAt(Instant resetAt) { this.resetAt = resetAt; }

    public Integer getRetryAfter() { return retryAfter; }
    public void setRetryAfter(Integer retryAfter) { this.retryAfter = retryAfter; }
}