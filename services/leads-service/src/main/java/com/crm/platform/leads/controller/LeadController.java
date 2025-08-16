package com.crm.platform.leads.controller;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.leads.dto.LeadConversionRequest;
import com.crm.platform.leads.dto.LeadRequest;
import com.crm.platform.leads.dto.LeadResponse;
import com.crm.platform.leads.dto.LeadSearchRequest;
import com.crm.platform.leads.entity.LeadStatus;
import com.crm.platform.leads.entity.QualificationStatus;
import com.crm.platform.leads.service.LeadService;
import com.crm.platform.leads.service.LeadScoringService;
import com.crm.platform.leads.service.LeadValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "Leads", description = "Lead management operations")
public class LeadController {

    private static final Logger logger = LoggerFactory.getLogger(LeadController.class);

    private final LeadService leadService;
    private final LeadScoringService leadScoringService;
    private final LeadValidationService leadValidationService;

    @Autowired
    public LeadController(LeadService leadService,
                         LeadScoringService leadScoringService,
                         LeadValidationService leadValidationService) {
        this.leadService = leadService;
        this.leadScoringService = leadScoringService;
        this.leadValidationService = leadValidationService;
    }

    @PostMapping
    @Operation(summary = "Create a new lead", description = "Creates a new lead with automatic scoring")
    public ResponseEntity<ApiResponse<LeadResponse>> createLead(
            @Valid @RequestBody LeadRequest request,
            Authentication authentication) {
        
        logger.info("Creating new lead for user: {}", authentication.getName());
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        LeadResponse response = leadService.createLead(request, currentUserId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    @GetMapping("/{leadId}")
    @Operation(summary = "Get lead by ID", description = "Retrieves a lead by its unique identifier")
    public ResponseEntity<ApiResponse<LeadResponse>> getLeadById(
            @Parameter(description = "Lead ID") @PathVariable UUID leadId) {
        
        logger.debug("Retrieving lead: {}", leadId);
        
        LeadResponse response = leadService.getLeadById(leadId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{leadId}")
    @Operation(summary = "Update lead", description = "Updates an existing lead and recalculates score")
    public ResponseEntity<ApiResponse<LeadResponse>> updateLead(
            @Parameter(description = "Lead ID") @PathVariable UUID leadId,
            @Valid @RequestBody LeadRequest request,
            Authentication authentication) {
        
        logger.info("Updating lead: {} by user: {}", leadId, authentication.getName());
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        LeadResponse response = leadService.updateLead(leadId, request, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{leadId}")
    @Operation(summary = "Delete lead", description = "Deletes a lead (only if not converted)")
    public ResponseEntity<ApiResponse<Void>> deleteLead(
            @Parameter(description = "Lead ID") @PathVariable UUID leadId,
            Authentication authentication) {
        
        logger.info("Deleting lead: {} by user: {}", leadId, authentication.getName());
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        leadService.deleteLead(leadId, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success("Lead deleted successfully"));
    }

    @PostMapping("/search")
    @Operation(summary = "Search leads", description = "Advanced lead search with filtering and sorting")
    public ResponseEntity<ApiResponse<Page<LeadResponse>>> searchLeads(
            @RequestBody LeadSearchRequest searchRequest) {
        
        logger.debug("Searching leads with criteria: {}", searchRequest.getSearchTerm());
        
        Page<LeadResponse> response = leadService.searchLeads(searchRequest);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get leads by owner", description = "Retrieves leads assigned to a specific owner")
    public ResponseEntity<ApiResponse<Page<LeadResponse>>> getLeadsByOwner(
            @Parameter(description = "Owner ID") @PathVariable UUID ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        logger.debug("Retrieving leads for owner: {}", ownerId);
        
        Page<LeadResponse> response = leadService.getLeadsByOwner(ownerId, page, size, sortBy, sortDirection);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/hot")
    @Operation(summary = "Get hot leads", description = "Retrieves leads with score >= 80")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getHotLeads() {
        
        logger.debug("Retrieving hot leads");
        
        List<LeadResponse> response = leadService.getHotLeads();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue leads", description = "Retrieves leads past their follow-up date")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getOverdueLeads() {
        
        logger.debug("Retrieving overdue leads");
        
        List<LeadResponse> response = leadService.getOverdueLeads();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{leadId}/status")
    @Operation(summary = "Update lead status", description = "Updates the status of a lead")
    public ResponseEntity<ApiResponse<LeadResponse>> updateLeadStatus(
            @Parameter(description = "Lead ID") @PathVariable UUID leadId,
            @RequestParam LeadStatus status,
            Authentication authentication) {
        
        logger.info("Updating lead status: {} to {} by user: {}", leadId, status, authentication.getName());
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        LeadResponse response = leadService.updateLeadStatus(leadId, status, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{leadId}/qualification")
    @Operation(summary = "Update qualification status", description = "Updates the qualification status of a lead")
    public ResponseEntity<ApiResponse<LeadResponse>> updateQualificationStatus(
            @Parameter(description = "Lead ID") @PathVariable UUID leadId,
            @RequestParam QualificationStatus qualificationStatus,
            Authentication authentication) {
        
        logger.info("Updating lead qualification: {} to {} by user: {}", 
                   leadId, qualificationStatus, authentication.getName());
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        LeadResponse response = leadService.updateQualificationStatus(leadId, qualificationStatus, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{leadId}/assign")
    @Operation(summary = "Assign lead", description = "Assigns a lead to a new owner")
    public ResponseEntity<ApiResponse<LeadResponse>> assignLead(
            @Parameter(description = "Lead ID") @PathVariable UUID leadId,
            @RequestParam UUID ownerId,
            Authentication authentication) {
        
        logger.info("Assigning lead: {} to owner: {} by user: {}", 
                   leadId, ownerId, authentication.getName());
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        LeadResponse response = leadService.assignLead(leadId, ownerId, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{leadId}/convert")
    @Operation(summary = "Convert lead", description = "Converts a lead to contact/account/deal")
    public ResponseEntity<ApiResponse<LeadResponse>> convertLead(
            @Parameter(description = "Lead ID") @PathVariable UUID leadId,
            @Valid @RequestBody LeadConversionRequest conversionRequest,
            Authentication authentication) {
        
        logger.info("Converting lead: {} by user: {}", leadId, authentication.getName());
        
        // Ensure leadId matches the one in the request
        conversionRequest.setLeadId(leadId);
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        LeadResponse response = leadService.convertLead(conversionRequest, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{leadId}/score/breakdown")
    @Operation(summary = "Get lead scoring breakdown", description = "Retrieves detailed scoring breakdown for a lead")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeadScoringBreakdown(
            @Parameter(description = "Lead ID") @PathVariable UUID leadId) {
        
        logger.debug("Retrieving scoring breakdown for lead: {}", leadId);
        
        // First get the lead to pass to scoring service
        LeadResponse leadResponse = leadService.getLeadById(leadId);
        
        // Convert response back to entity for scoring (this is a simplification)
        // In a real implementation, you might want to modify the scoring service to work with DTOs
        // or create a separate method that doesn't require the full entity
        
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Scoring breakdown feature requires lead entity access")));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate lead data", description = "Validates lead data and returns quality assessment")
    public ResponseEntity<ApiResponse<LeadValidationService.LeadDataQuality>> validateLeadData(
            @Valid @RequestBody LeadRequest request) {
        
        logger.debug("Validating lead data");
        
        LeadValidationService.LeadDataQuality quality = leadValidationService.assessDataQuality(request);
        
        return ResponseEntity.ok(ApiResponse.success(quality));
    }

    // Bulk operations

    @PutMapping("/bulk/assign")
    @Operation(summary = "Bulk assign leads", description = "Assigns multiple leads to a new owner")
    public ResponseEntity<ApiResponse<Void>> bulkAssignLeads(
            @RequestBody List<UUID> leadIds,
            @RequestParam UUID ownerId,
            Authentication authentication) {
        
        logger.info("Bulk assigning {} leads to owner: {} by user: {}", 
                   leadIds.size(), ownerId, authentication.getName());
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        leadService.bulkUpdateOwners(leadIds, ownerId, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success("Leads assigned successfully"));
    }

    @PutMapping("/bulk/status")
    @Operation(summary = "Bulk update lead statuses", description = "Updates status for multiple leads")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateLeadStatuses(
            @RequestBody List<UUID> leadIds,
            @RequestParam LeadStatus status,
            Authentication authentication) {
        
        logger.info("Bulk updating status for {} leads to {} by user: {}", 
                   leadIds.size(), status, authentication.getName());
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        leadService.bulkUpdateStatuses(leadIds, status, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success("Lead statuses updated successfully"));
    }

    // Utility endpoints

    @GetMapping("/sources")
    @Operation(summary = "Get lead sources", description = "Retrieves all distinct lead sources for the tenant")
    public ResponseEntity<ApiResponse<List<String>>> getLeadSources() {
        
        logger.debug("Retrieving lead sources");
        
        // This would typically be implemented in the service layer
        // For now, return a placeholder response
        List<String> sources = List.of("Website", "Email Campaign", "Social Media", "Referral", "Trade Show", "Cold Call");
        
        return ResponseEntity.ok(ApiResponse.success(sources));
    }

    @GetMapping("/companies")
    @Operation(summary = "Get companies", description = "Retrieves all distinct companies for the tenant")
    public ResponseEntity<ApiResponse<List<String>>> getCompanies() {
        
        logger.debug("Retrieving companies");
        
        // This would typically be implemented in the service layer
        // For now, return a placeholder response
        List<String> companies = List.of("Acme Corp", "TechStart Inc", "Global Solutions", "Innovation Labs");
        
        return ResponseEntity.ok(ApiResponse.success(companies));
    }
}