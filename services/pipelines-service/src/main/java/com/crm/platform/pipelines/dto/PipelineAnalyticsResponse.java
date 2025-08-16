package com.crm.platform.pipelines.dto;

import com.crm.platform.pipelines.entity.MetricType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class PipelineAnalyticsResponse {

    private UUID id;
    private UUID tenantId;
    private UUID pipelineId;
    private UUID stageId;
    private MetricType metricType;
    private String metricName;
    private BigDecimal metricValue;
    private String metricUnit;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime calculatedAt;
    private Map<String, Object> metadata;

    // Constructors
    public PipelineAnalyticsResponse() {}

    public PipelineAnalyticsResponse(UUID id, MetricType metricType, String metricName, 
                                   BigDecimal metricValue, String metricUnit) {
        this.id = id;
        this.metricType = metricType;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.metricUnit = metricUnit;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
    }

    public UUID getStageId() {
        return stageId;
    }

    public void setStageId(UUID stageId) {
        this.stageId = stageId;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public String getMetricUnit() {
        return metricUnit;
    }

    public void setMetricUnit(String metricUnit) {
        this.metricUnit = metricUnit;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "PipelineAnalyticsResponse{" +
                "id=" + id +
                ", metricType=" + metricType +
                ", metricName='" + metricName + '\'' +
                ", metricValue=" + metricValue +
                ", metricUnit='" + metricUnit + '\'' +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                '}';
    }
}