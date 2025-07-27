package com.crm.platform.deals.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DealForecastResponse {

    private LocalDate forecastPeriodStart;
    private LocalDate forecastPeriodEnd;
    private String currency;
    
    // Overall forecast metrics
    private BigDecimal totalPipelineValue;
    private BigDecimal totalWeightedValue;
    private BigDecimal totalCommittedValue;
    private BigDecimal totalBestCaseValue;
    private BigDecimal totalWorstCaseValue;
    
    private Integer totalDealsCount;
    private Integer openDealsCount;
    private Integer closedWonDealsCount;
    private Integer closedLostDealsCount;
    
    // Forecast by pipeline
    private List<PipelineForecast> pipelineForecasts;
    
    // Forecast by stage
    private List<StageForecast> stageForecasts;
    
    // Forecast by owner
    private List<OwnerForecast> ownerForecasts;
    
    // Time-based forecast
    private List<TimePeriodForecast> timePeriodForecasts;

    // Constructors
    public DealForecastResponse() {}

    // Inner classes for detailed forecasts
    public static class PipelineForecast {
        private UUID pipelineId;
        private String pipelineName;
        private BigDecimal totalValue;
        private BigDecimal weightedValue;
        private Integer dealsCount;
        private BigDecimal averageDealSize;
        private BigDecimal winRate;

        // Getters and Setters
        public UUID getPipelineId() { return pipelineId; }
        public void setPipelineId(UUID pipelineId) { this.pipelineId = pipelineId; }

        public String getPipelineName() { return pipelineName; }
        public void setPipelineName(String pipelineName) { this.pipelineName = pipelineName; }

        public BigDecimal getTotalValue() { return totalValue; }
        public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

        public BigDecimal getWeightedValue() { return weightedValue; }
        public void setWeightedValue(BigDecimal weightedValue) { this.weightedValue = weightedValue; }

        public Integer getDealsCount() { return dealsCount; }
        public void setDealsCount(Integer dealsCount) { this.dealsCount = dealsCount; }

        public BigDecimal getAverageDealSize() { return averageDealSize; }
        public void setAverageDealSize(BigDecimal averageDealSize) { this.averageDealSize = averageDealSize; }

        public BigDecimal getWinRate() { return winRate; }
        public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    }

    public static class StageForecast {
        private UUID stageId;
        private String stageName;
        private BigDecimal totalValue;
        private BigDecimal weightedValue;
        private Integer dealsCount;
        private BigDecimal averageProbability;

        // Getters and Setters
        public UUID getStageId() { return stageId; }
        public void setStageId(UUID stageId) { this.stageId = stageId; }

        public String getStageName() { return stageName; }
        public void setStageName(String stageName) { this.stageName = stageName; }

        public BigDecimal getTotalValue() { return totalValue; }
        public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

        public BigDecimal getWeightedValue() { return weightedValue; }
        public void setWeightedValue(BigDecimal weightedValue) { this.weightedValue = weightedValue; }

        public Integer getDealsCount() { return dealsCount; }
        public void setDealsCount(Integer dealsCount) { this.dealsCount = dealsCount; }

        public BigDecimal getAverageProbability() { return averageProbability; }
        public void setAverageProbability(BigDecimal averageProbability) { this.averageProbability = averageProbability; }
    }

    public static class OwnerForecast {
        private UUID ownerId;
        private String ownerName;
        private BigDecimal totalValue;
        private BigDecimal weightedValue;
        private Integer dealsCount;
        private BigDecimal quota;
        private BigDecimal quotaAttainment;

        // Getters and Setters
        public UUID getOwnerId() { return ownerId; }
        public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

        public BigDecimal getTotalValue() { return totalValue; }
        public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

        public BigDecimal getWeightedValue() { return weightedValue; }
        public void setWeightedValue(BigDecimal weightedValue) { this.weightedValue = weightedValue; }

        public Integer getDealsCount() { return dealsCount; }
        public void setDealsCount(Integer dealsCount) { this.dealsCount = dealsCount; }

        public BigDecimal getQuota() { return quota; }
        public void setQuota(BigDecimal quota) { this.quota = quota; }

        public BigDecimal getQuotaAttainment() { return quotaAttainment; }
        public void setQuotaAttainment(BigDecimal quotaAttainment) { this.quotaAttainment = quotaAttainment; }
    }

    public static class TimePeriodForecast {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private BigDecimal totalValue;
        private BigDecimal weightedValue;
        private Integer dealsCount;
        private Integer expectedClosures;

        // Getters and Setters
        public LocalDate getPeriodStart() { return periodStart; }
        public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

        public LocalDate getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

        public BigDecimal getTotalValue() { return totalValue; }
        public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

        public BigDecimal getWeightedValue() { return weightedValue; }
        public void setWeightedValue(BigDecimal weightedValue) { this.weightedValue = weightedValue; }

        public Integer getDealsCount() { return dealsCount; }
        public void setDealsCount(Integer dealsCount) { this.dealsCount = dealsCount; }

        public Integer getExpectedClosures() { return expectedClosures; }
        public void setExpectedClosures(Integer expectedClosures) { this.expectedClosures = expectedClosures; }
    }

    // Main class getters and setters
    public LocalDate getForecastPeriodStart() { return forecastPeriodStart; }
    public void setForecastPeriodStart(LocalDate forecastPeriodStart) { this.forecastPeriodStart = forecastPeriodStart; }

    public LocalDate getForecastPeriodEnd() { return forecastPeriodEnd; }
    public void setForecastPeriodEnd(LocalDate forecastPeriodEnd) { this.forecastPeriodEnd = forecastPeriodEnd; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getTotalPipelineValue() { return totalPipelineValue; }
    public void setTotalPipelineValue(BigDecimal totalPipelineValue) { this.totalPipelineValue = totalPipelineValue; }

    public BigDecimal getTotalWeightedValue() { return totalWeightedValue; }
    public void setTotalWeightedValue(BigDecimal totalWeightedValue) { this.totalWeightedValue = totalWeightedValue; }

    public BigDecimal getTotalCommittedValue() { return totalCommittedValue; }
    public void setTotalCommittedValue(BigDecimal totalCommittedValue) { this.totalCommittedValue = totalCommittedValue; }

    public BigDecimal getTotalBestCaseValue() { return totalBestCaseValue; }
    public void setTotalBestCaseValue(BigDecimal totalBestCaseValue) { this.totalBestCaseValue = totalBestCaseValue; }

    public BigDecimal getTotalWorstCaseValue() { return totalWorstCaseValue; }
    public void setTotalWorstCaseValue(BigDecimal totalWorstCaseValue) { this.totalWorstCaseValue = totalWorstCaseValue; }

    public Integer getTotalDealsCount() { return totalDealsCount; }
    public void setTotalDealsCount(Integer totalDealsCount) { this.totalDealsCount = totalDealsCount; }

    public Integer getOpenDealsCount() { return openDealsCount; }
    public void setOpenDealsCount(Integer openDealsCount) { this.openDealsCount = openDealsCount; }

    public Integer getClosedWonDealsCount() { return closedWonDealsCount; }
    public void setClosedWonDealsCount(Integer closedWonDealsCount) { this.closedWonDealsCount = closedWonDealsCount; }

    public Integer getClosedLostDealsCount() { return closedLostDealsCount; }
    public void setClosedLostDealsCount(Integer closedLostDealsCount) { this.closedLostDealsCount = closedLostDealsCount; }

    public List<PipelineForecast> getPipelineForecasts() { return pipelineForecasts; }
    public void setPipelineForecasts(List<PipelineForecast> pipelineForecasts) { this.pipelineForecasts = pipelineForecasts; }

    public List<StageForecast> getStageForecasts() { return stageForecasts; }
    public void setStageForecasts(List<StageForecast> stageForecasts) { this.stageForecasts = stageForecasts; }

    public List<OwnerForecast> getOwnerForecasts() { return ownerForecasts; }
    public void setOwnerForecasts(List<OwnerForecast> ownerForecasts) { this.ownerForecasts = ownerForecasts; }

    public List<TimePeriodForecast> getTimePeriodForecasts() { return timePeriodForecasts; }
    public void setTimePeriodForecasts(List<TimePeriodForecast> timePeriodForecasts) { this.timePeriodForecasts = timePeriodForecasts; }
}