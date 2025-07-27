package com.crm.platform.deals.controller;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.deals.dto.*;
import com.crm.platform.deals.service.DealForecastService;
import com.crm.platform.deals.service.DealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deals")
@Tag(name = "Deals", description = "Deal management operations")
public class DealController {

    private static final Logger logger = LoggerFactory.getLogger(DealController.class);

    private final DealService dealService;
    private final DealForecastService dealForecastService;

    @Autowired
    public DealController(DealService dealService, DealForecastService dealForecastService) {
        this.dealService = dealService;
        this.dealForecastService = dealForecastService;
    }

    @PostMapping
    @Operation(summary = "Create a new deal")
    public ResponseEntity<ApiResponse<DealResponse>> createDeal(
            @Valid @RequestBody DealRequest request,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        DealResponse deal = dealService.createDeal(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(deal, "Deal created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deal by ID")
    public ResponseEntity<ApiResponse<DealResponse>> getDeal(
            @Parameter(description = "Deal ID") @PathVariable UUID id) {
        
        DealResponse deal = dealService.getDeal(id);
        return ResponseEntity.ok(ApiResponse.success(deal));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update deal")
    public ResponseEntity<ApiResponse<DealResponse>> updateDeal(
            @Parameter(description = "Deal ID") @PathVariable UUID id,
            @Valid @RequestBody DealRequest request,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        DealResponse deal = dealService.updateDeal(id, request, userId);
        
        return ResponseEntity.ok(ApiResponse.success(deal, "Deal updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete deal")
    public ResponseEntity<ApiResponse<Void>> deleteDeal(
            @Parameter(description = "Deal ID") @PathVariable UUID id,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        dealService.deleteDeal(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Deal deleted successfully"));
    }

    @PostMapping("/{id}/move-stage")
    @Operation(summary = "Move deal to different stage")
    public ResponseEntity<ApiResponse<DealResponse>> moveDealToStage(
            @Parameter(description = "Deal ID") @PathVariable UUID id,
            @Valid @RequestBody DealStageChangeRequest request,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        DealResponse deal = dealService.moveDealToStage(id, request, userId);
        
        return ResponseEntity.ok(ApiResponse.success(deal, "Deal moved to new stage successfully"));
    }

    @PostMapping("/search")
    @Operation(summary = "Search deals with advanced filters")
    public ResponseEntity<ApiResponse<Page<DealResponse>>> searchDeals(
            @Valid @RequestBody DealSearchRequest searchRequest) {
        
        Page<DealResponse> deals = dealService.searchDeals(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(deals));
    }

    @GetMapping("/pipeline/{pipelineId}")
    @Operation(summary = "Get deals by pipeline")
    public ResponseEntity<ApiResponse<List<DealResponse>>> getDealsByPipeline(
            @Parameter(description = "Pipeline ID") @PathVariable UUID pipelineId) {
        
        List<DealResponse> deals = dealService.getDealsByPipeline(pipelineId);
        return ResponseEntity.ok(ApiResponse.success(deals));
    }

    @GetMapping("/stage/{stageId}")
    @Operation(summary = "Get deals by stage")
    public ResponseEntity<ApiResponse<List<DealResponse>>> getDealsByStage(
            @Parameter(description = "Stage ID") @PathVariable UUID stageId) {
        
        List<DealResponse> deals = dealService.getDealsByStage(stageId);
        return ResponseEntity.ok(ApiResponse.success(deals));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get deals by owner")
    public ResponseEntity<ApiResponse<List<DealResponse>>> getDealsByOwner(
            @Parameter(description = "Owner ID") @PathVariable UUID ownerId) {
        
        List<DealResponse> deals = dealService.getDealsByOwner(ownerId);
        return ResponseEntity.ok(ApiResponse.success(deals));
    }

    // Bulk operations
    @PostMapping("/bulk")
    @Operation(summary = "Bulk create deals")
    public ResponseEntity<ApiResponse<List<DealResponse>>> bulkCreateDeals(
            @Valid @RequestBody List<DealRequest> requests,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        List<DealResponse> deals = dealService.bulkCreateDeals(requests, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(deals, "Deals created successfully"));
    }

    @PutMapping("/bulk/stage")
    @Operation(summary = "Bulk update deal stages")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateStage(
            @RequestParam List<UUID> dealIds,
            @RequestParam UUID stageId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        dealService.bulkUpdateStage(dealIds, stageId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Deal stages updated successfully"));
    }

    @PutMapping("/bulk/owner")
    @Operation(summary = "Bulk update deal owners")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateOwner(
            @RequestParam List<UUID> dealIds,
            @RequestParam UUID ownerId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        dealService.bulkUpdateOwner(dealIds, ownerId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Deal owners updated successfully"));
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete deals")
    public ResponseEntity<ApiResponse<Void>> bulkDeleteDeals(
            @RequestParam List<UUID> dealIds,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        dealService.bulkDeleteDeals(dealIds, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Deals deleted successfully"));
    }

    // Forecasting endpoints
    @GetMapping("/forecast")
    @Operation(summary = "Generate deal forecast for date range")
    public ResponseEntity<ApiResponse<DealForecastResponse>> generateForecast(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID pipelineId,
            @RequestParam(required = false) String currency) {
        
        DealForecastResponse forecast = dealForecastService.generateForecast(startDate, endDate, pipelineId, currency);
        return ResponseEntity.ok(ApiResponse.success(forecast));
    }

    @GetMapping("/forecast/quarterly")
    @Operation(summary = "Generate quarterly deal forecast")
    public ResponseEntity<ApiResponse<DealForecastResponse>> generateQuarterlyForecast(
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam(required = false) UUID pipelineId,
            @RequestParam(required = false) String currency) {
        
        DealForecastResponse forecast = dealForecastService.generateQuarterlyForecast(year, quarter, pipelineId, currency);
        return ResponseEntity.ok(ApiResponse.success(forecast));
    }

    @GetMapping("/forecast/monthly")
    @Operation(summary = "Generate monthly deal forecast")
    public ResponseEntity<ApiResponse<DealForecastResponse>> generateMonthlyForecast(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID pipelineId,
            @RequestParam(required = false) String currency) {
        
        DealForecastResponse forecast = dealForecastService.generateMonthlyForecast(year, month, pipelineId, currency);
        return ResponseEntity.ok(ApiResponse.success(forecast));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        logger.error("Error in DealController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An internal error occurred"));
    }
}