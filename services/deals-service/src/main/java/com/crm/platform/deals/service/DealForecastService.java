package com.crm.platform.deals.service;

import com.crm.platform.common.util.TenantContext;
import com.crm.platform.deals.dto.DealForecastResponse;
import com.crm.platform.deals.entity.Deal;
import com.crm.platform.deals.entity.Pipeline;
import com.crm.platform.deals.entity.PipelineStage;
import com.crm.platform.deals.repository.DealRepository;
import com.crm.platform.deals.repository.PipelineRepository;
import com.crm.platform.deals.repository.PipelineStageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DealForecastService {

    private static final Logger logger = LoggerFactory.getLogger(DealForecastService.class);

    private final DealRepository dealRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository pipelineStageRepository;

    @Autowired
    public DealForecastService(DealRepository dealRepository,
                              PipelineRepository pipelineRepository,
                              PipelineStageRepository pipelineStageRepository) {
        this.dealRepository = dealRepository;
        this.pipelineRepository = pipelineRepository;
        this.pipelineStageRepository = pipelineStageRepository;
    }

    public DealForecastResponse generateForecast(LocalDate startDate, LocalDate endDate, 
                                               UUID pipelineId, String currency) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Generating forecast for tenant {} from {} to {}", tenantId, startDate, endDate);

        DealForecastResponse forecast = new DealForecastResponse();
        forecast.setForecastPeriodStart(startDate);
        forecast.setForecastPeriodEnd(endDate);
        forecast.setCurrency(currency != null ? currency : "USD");

        // Get deals for forecast period
        List<Deal> forecastDeals = dealRepository.findDealsForForecastPeriod(tenantId, startDate, endDate, false);
        
        // Filter by pipeline if specified
        if (pipelineId != null) {
            forecastDeals = forecastDeals.stream()
                .filter(deal -> deal.getPipelineId().equals(pipelineId))
                .collect(Collectors.toList());
        }

        // Filter by currency if specified
        if (currency != null) {
            forecastDeals = forecastDeals.stream()
                .filter(deal -> currency.equals(deal.getCurrency()))
                .collect(Collectors.toList());
        }

        // Calculate overall metrics
        calculateOverallMetrics(forecast, forecastDeals);

        // Generate pipeline forecasts
        forecast.setPipelineForecasts(generatePipelineForecasts(forecastDeals));

        // Generate stage forecasts
        forecast.setStageForecasts(generateStageForecasts(forecastDeals));

        // Generate owner forecasts
        forecast.setOwnerForecasts(generateOwnerForecasts(forecastDeals));

        // Generate time period forecasts
        forecast.setTimePeriodForecasts(generateTimePeriodForecasts(forecastDeals, startDate, endDate));

        logger.info("Generated forecast with {} deals, total pipeline value: {}", 
                   forecastDeals.size(), forecast.getTotalPipelineValue());

        return forecast;
    }

    public DealForecastResponse generateQuarterlyForecast(int year, int quarter, UUID pipelineId, String currency) {
        LocalDate startDate = getQuarterStartDate(year, quarter);
        LocalDate endDate = getQuarterEndDate(year, quarter);
        return generateForecast(startDate, endDate, pipelineId, currency);
    }

    public DealForecastResponse generateMonthlyForecast(int year, int month, UUID pipelineId, String currency) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        return generateForecast(startDate, endDate, pipelineId, currency);
    }

    private void calculateOverallMetrics(DealForecastResponse forecast, List<Deal> deals) {
        BigDecimal totalPipelineValue = BigDecimal.ZERO;
        BigDecimal totalWeightedValue = BigDecimal.ZERO;
        BigDecimal totalCommittedValue = BigDecimal.ZERO;
        BigDecimal totalBestCaseValue = BigDecimal.ZERO;
        BigDecimal totalWorstCaseValue = BigDecimal.ZERO;

        int openDealsCount = 0;
        int closedWonDealsCount = 0;
        int closedLostDealsCount = 0;

        for (Deal deal : deals) {
            if (deal.getAmount() != null) {
                totalPipelineValue = totalPipelineValue.add(deal.getAmount());
                
                BigDecimal weightedAmount = deal.getWeightedAmount();
                totalWeightedValue = totalWeightedValue.add(weightedAmount);

                // Calculate committed, best case, and worst case based on probability
                if (deal.getProbability() != null) {
                    BigDecimal probability = deal.getProbability();
                    if (probability.compareTo(BigDecimal.valueOf(75)) >= 0) {
                        totalCommittedValue = totalCommittedValue.add(deal.getAmount());
                    }
                    if (probability.compareTo(BigDecimal.valueOf(25)) >= 0) {
                        totalBestCaseValue = totalBestCaseValue.add(deal.getAmount());
                    }
                    if (probability.compareTo(BigDecimal.valueOf(90)) >= 0) {
                        totalWorstCaseValue = totalWorstCaseValue.add(deal.getAmount());
                    }
                }
            }

            if (deal.getIsClosed()) {
                if (deal.getIsWon()) {
                    closedWonDealsCount++;
                } else {
                    closedLostDealsCount++;
                }
            } else {
                openDealsCount++;
            }
        }

        forecast.setTotalPipelineValue(totalPipelineValue);
        forecast.setTotalWeightedValue(totalWeightedValue);
        forecast.setTotalCommittedValue(totalCommittedValue);
        forecast.setTotalBestCaseValue(totalBestCaseValue);
        forecast.setTotalWorstCaseValue(totalWorstCaseValue);
        forecast.setTotalDealsCount(deals.size());
        forecast.setOpenDealsCount(openDealsCount);
        forecast.setClosedWonDealsCount(closedWonDealsCount);
        forecast.setClosedLostDealsCount(closedLostDealsCount);
    }

    private List<DealForecastResponse.PipelineForecast> generatePipelineForecasts(List<Deal> deals) {
        Map<UUID, List<Deal>> dealsByPipeline = deals.stream()
            .collect(Collectors.groupingBy(Deal::getPipelineId));

        List<DealForecastResponse.PipelineForecast> pipelineForecasts = new ArrayList<>();

        for (Map.Entry<UUID, List<Deal>> entry : dealsByPipeline.entrySet()) {
            UUID pipelineId = entry.getKey();
            List<Deal> pipelineDeals = entry.getValue();

            DealForecastResponse.PipelineForecast pipelineForecast = new DealForecastResponse.PipelineForecast();
            pipelineForecast.setPipelineId(pipelineId);

            // Get pipeline name
            try {
                Pipeline pipeline = pipelineRepository.findById(pipelineId).orElse(null);
                if (pipeline != null) {
                    pipelineForecast.setPipelineName(pipeline.getName());
                }
            } catch (Exception e) {
                logger.warn("Error loading pipeline {}: {}", pipelineId, e.getMessage());
            }

            // Calculate metrics
            BigDecimal totalValue = pipelineDeals.stream()
                .filter(deal -> deal.getAmount() != null)
                .map(Deal::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal weightedValue = pipelineDeals.stream()
                .map(Deal::getWeightedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            pipelineForecast.setTotalValue(totalValue);
            pipelineForecast.setWeightedValue(weightedValue);
            pipelineForecast.setDealsCount(pipelineDeals.size());

            if (pipelineDeals.size() > 0) {
                BigDecimal averageDealSize = totalValue.divide(
                    BigDecimal.valueOf(pipelineDeals.size()), 2, RoundingMode.HALF_UP);
                pipelineForecast.setAverageDealSize(averageDealSize);

                // Calculate win rate (simplified - would need historical data for accurate calculation)
                long wonDeals = pipelineDeals.stream()
                    .filter(deal -> deal.getIsClosed() && deal.getIsWon())
                    .count();
                long closedDeals = pipelineDeals.stream()
                    .filter(Deal::getIsClosed)
                    .count();

                if (closedDeals > 0) {
                    BigDecimal winRate = BigDecimal.valueOf(wonDeals)
                        .divide(BigDecimal.valueOf(closedDeals), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    pipelineForecast.setWinRate(winRate);
                }
            }

            pipelineForecasts.add(pipelineForecast);
        }

        return pipelineForecasts;
    }

    private List<DealForecastResponse.StageForecast> generateStageForecasts(List<Deal> deals) {
        Map<UUID, List<Deal>> dealsByStage = deals.stream()
            .collect(Collectors.groupingBy(Deal::getStageId));

        List<DealForecastResponse.StageForecast> stageForecasts = new ArrayList<>();

        for (Map.Entry<UUID, List<Deal>> entry : dealsByStage.entrySet()) {
            UUID stageId = entry.getKey();
            List<Deal> stageDeals = entry.getValue();

            DealForecastResponse.StageForecast stageForecast = new DealForecastResponse.StageForecast();
            stageForecast.setStageId(stageId);

            // Get stage name
            try {
                PipelineStage stage = pipelineStageRepository.findById(stageId).orElse(null);
                if (stage != null) {
                    stageForecast.setStageName(stage.getName());
                }
            } catch (Exception e) {
                logger.warn("Error loading stage {}: {}", stageId, e.getMessage());
            }

            // Calculate metrics
            BigDecimal totalValue = stageDeals.stream()
                .filter(deal -> deal.getAmount() != null)
                .map(Deal::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal weightedValue = stageDeals.stream()
                .map(Deal::getWeightedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal averageProbability = stageDeals.stream()
                .filter(deal -> deal.getProbability() != null)
                .map(Deal::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(stageDeals.size()), 2, RoundingMode.HALF_UP);

            stageForecast.setTotalValue(totalValue);
            stageForecast.setWeightedValue(weightedValue);
            stageForecast.setDealsCount(stageDeals.size());
            stageForecast.setAverageProbability(averageProbability);

            stageForecasts.add(stageForecast);
        }

        return stageForecasts;
    }

    private List<DealForecastResponse.OwnerForecast> generateOwnerForecasts(List<Deal> deals) {
        Map<UUID, List<Deal>> dealsByOwner = deals.stream()
            .collect(Collectors.groupingBy(Deal::getOwnerId));

        List<DealForecastResponse.OwnerForecast> ownerForecasts = new ArrayList<>();

        for (Map.Entry<UUID, List<Deal>> entry : dealsByOwner.entrySet()) {
            UUID ownerId = entry.getKey();
            List<Deal> ownerDeals = entry.getValue();

            DealForecastResponse.OwnerForecast ownerForecast = new DealForecastResponse.OwnerForecast();
            ownerForecast.setOwnerId(ownerId);

            // Calculate metrics
            BigDecimal totalValue = ownerDeals.stream()
                .filter(deal -> deal.getAmount() != null)
                .map(Deal::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal weightedValue = ownerDeals.stream()
                .map(Deal::getWeightedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            ownerForecast.setTotalValue(totalValue);
            ownerForecast.setWeightedValue(weightedValue);
            ownerForecast.setDealsCount(ownerDeals.size());

            // Note: Quota and quota attainment would typically come from a separate quota management system
            // For now, we'll leave these as null or set default values

            ownerForecasts.add(ownerForecast);
        }

        return ownerForecasts;
    }

    private List<DealForecastResponse.TimePeriodForecast> generateTimePeriodForecasts(
            List<Deal> deals, LocalDate startDate, LocalDate endDate) {
        
        List<DealForecastResponse.TimePeriodForecast> timePeriodForecasts = new ArrayList<>();

        // Group deals by month
        LocalDate currentMonth = startDate.withDayOfMonth(1);
        while (!currentMonth.isAfter(endDate)) {
            LocalDate monthStart = currentMonth;
            LocalDate monthEnd = currentMonth.with(TemporalAdjusters.lastDayOfMonth());
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }

            List<Deal> monthDeals = deals.stream()
                .filter(deal -> deal.getExpectedCloseDate() != null)
                .filter(deal -> !deal.getExpectedCloseDate().isBefore(monthStart) && 
                               !deal.getExpectedCloseDate().isAfter(monthEnd))
                .collect(Collectors.toList());

            if (!monthDeals.isEmpty()) {
                DealForecastResponse.TimePeriodForecast timePeriodForecast = 
                    new DealForecastResponse.TimePeriodForecast();
                
                timePeriodForecast.setPeriodStart(monthStart);
                timePeriodForecast.setPeriodEnd(monthEnd);

                BigDecimal totalValue = monthDeals.stream()
                    .filter(deal -> deal.getAmount() != null)
                    .map(Deal::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal weightedValue = monthDeals.stream()
                    .map(Deal::getWeightedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                timePeriodForecast.setTotalValue(totalValue);
                timePeriodForecast.setWeightedValue(weightedValue);
                timePeriodForecast.setDealsCount(monthDeals.size());

                // Estimate expected closures based on probability
                int expectedClosures = (int) monthDeals.stream()
                    .filter(deal -> deal.getProbability() != null)
                    .mapToDouble(deal -> deal.getProbability().doubleValue() / 100.0)
                    .sum();
                timePeriodForecast.setExpectedClosures(expectedClosures);

                timePeriodForecasts.add(timePeriodForecast);
            }

            currentMonth = currentMonth.plusMonths(1);
        }

        return timePeriodForecasts;
    }

    private LocalDate getQuarterStartDate(int year, int quarter) {
        int month = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, month, 1);
    }

    private LocalDate getQuarterEndDate(int year, int quarter) {
        int month = quarter * 3;
        return LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth());
    }
}