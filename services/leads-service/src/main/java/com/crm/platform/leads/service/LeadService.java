package com.crm.platform.leads.service;

import com.crm.platform.leads.exception.LeadBusinessException;
import com.crm.platform.common.util.TenantContext;
import com.crm.platform.leads.dto.LeadConversionRequest;
import com.crm.platform.leads.dto.LeadRequest;
import com.crm.platform.leads.dto.LeadResponse;
import com.crm.platform.leads.dto.LeadSearchRequest;
import com.crm.platform.leads.entity.Lead;
import com.crm.platform.leads.entity.LeadStatus;
import com.crm.platform.leads.entity.QualificationStatus;
import com.crm.platform.leads.repository.LeadRepository;
import com.crm.platform.leads.specification.LeadSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeadService {

    private static final Logger logger = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final LeadScoringService leadScoringService;
    private final LeadValidationService leadValidationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public LeadService(LeadRepository leadRepository,
                      LeadScoringService leadScoringService,
                      LeadValidationService leadValidationService,
                      KafkaTemplate<String, Object> kafkaTemplate) {
        this.leadRepository = leadRepository;
        this.leadScoringService = leadScoringService;
        this.leadValidationService = leadValidationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Create a new lead
     */
    public LeadResponse createLead(LeadRequest request, UUID currentUserId) {
        logger.info("Creating new lead for tenant: {}", TenantContext.getTenantId());

        // Validate the request
        leadValidationService.validateLeadRequest(request);

        // Check for duplicates
        leadValidationService.checkForDuplicates(request.getEmail(), request.getPhone());

        // Create lead entity
        Lead lead = new Lead(
            TenantContext.getTenantId(),
            request.getFirstName(),
            request.getLastName(),
            request.getOwnerId() != null ? request.getOwnerId() : currentUserId,
            currentUserId
        );

        // Map request fields to entity
        mapRequestToEntity(request, lead);

        // Calculate initial lead score
        int initialScore = leadScoringService.calculateLeadScore(lead);
        lead.setLeadScore(initialScore);

        // Save the lead
        Lead savedLead = leadRepository.save(lead);

        // Record initial score in history
        leadScoringService.updateLeadScore(savedLead, "Initial lead creation", "SYSTEM", currentUserId);

        // Publish lead created event
        publishLeadEvent("LEAD_CREATED", savedLead);

        logger.info("Created lead with ID: {} and initial score: {}", savedLead.getId(), initialScore);

        return new LeadResponse(savedLead);
    }

    /**
     * Get lead by ID
     */
    @Transactional(readOnly = true)
    public LeadResponse getLeadById(UUID leadId) {
        Lead lead = findLeadByIdAndTenant(leadId);
        return new LeadResponse(lead);
    }

    /**
     * Update an existing lead
     */
    public LeadResponse updateLead(UUID leadId, LeadRequest request, UUID currentUserId) {
        logger.info("Updating lead: {} for tenant: {}", leadId, TenantContext.getTenantId());

        Lead existingLead = findLeadByIdAndTenant(leadId);

        // Validate the request
        leadValidationService.validateLeadRequest(request);

        // Check for duplicates (excluding current lead)
        leadValidationService.checkForDuplicatesExcluding(request.getEmail(), request.getPhone(), leadId);

        // Store previous score for comparison
        int previousScore = existingLead.getLeadScore();

        // Map request fields to entity
        mapRequestToEntity(request, existingLead);
        existingLead.setUpdatedBy(currentUserId);

        // Recalculate lead score
        leadScoringService.updateLeadScore(existingLead, "Lead information updated", "USER_UPDATE", currentUserId);

        // Save the updated lead
        Lead savedLead = leadRepository.save(existingLead);

        // Publish lead updated event
        publishLeadEvent("LEAD_UPDATED", savedLead);

        logger.info("Updated lead: {} - Score changed from {} to {}", 
                   leadId, previousScore, savedLead.getLeadScore());

        return new LeadResponse(savedLead);
    }

    /**
     * Delete a lead
     */
    public void deleteLead(UUID leadId, UUID currentUserId) {
        logger.info("Deleting lead: {} for tenant: {}", leadId, TenantContext.getTenantId());

        Lead lead = findLeadByIdAndTenant(leadId);

        // Check if lead can be deleted (not converted)
        if (lead.isConverted()) {
            throw new LeadBusinessException("LEAD_CONVERTED", "Cannot delete converted lead");
        }

        // Publish lead deleted event before deletion
        publishLeadEvent("LEAD_DELETED", lead);

        leadRepository.delete(lead);

        logger.info("Deleted lead: {}", leadId);
    }

    /**
     * Search leads with advanced filtering
     */
    @Transactional(readOnly = true)
    public Page<LeadResponse> searchLeads(LeadSearchRequest searchRequest) {
        logger.debug("Searching leads for tenant: {} with criteria: {}", 
                    TenantContext.getTenantId(), searchRequest.getSearchTerm());

        // Build specification from search request
        Specification<Lead> spec = LeadSpecification.buildSpecification(searchRequest, TenantContext.getTenantId());

        // Create pageable with sorting
        Sort sort = createSort(searchRequest.getSortBy(), searchRequest.getSortDirection());
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

        // Execute search
        Page<Lead> leadPage = leadRepository.findAll(spec, pageable);

        // Convert to response DTOs
        return leadPage.map(LeadResponse::new);
    }

    /**
     * Get leads by owner
     */
    @Transactional(readOnly = true)
    public Page<LeadResponse> getLeadsByOwner(UUID ownerId, int page, int size, String sortBy, String sortDirection) {
        Sort sort = createSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Lead> leadPage = leadRepository.findByTenantIdAndOwnerId(
            TenantContext.getTenantId(), ownerId, pageable);

        return leadPage.map(LeadResponse::new);
    }

    /**
     * Get hot leads (score >= 80)
     */
    @Transactional(readOnly = true)
    public List<LeadResponse> getHotLeads() {
        List<Lead> hotLeads = leadRepository.findHotLeadsByTenantId(TenantContext.getTenantId());
        return hotLeads.stream().map(LeadResponse::new).collect(Collectors.toList());
    }

    /**
     * Get overdue leads (past follow-up date)
     */
    @Transactional(readOnly = true)
    public List<LeadResponse> getOverdueLeads() {
        List<Lead> overdueLeads = leadRepository.findOverdueLeads(
            TenantContext.getTenantId(), LocalDateTime.now());
        return overdueLeads.stream().map(LeadResponse::new).collect(Collectors.toList());
    }

    /**
     * Update lead status
     */
    public LeadResponse updateLeadStatus(UUID leadId, LeadStatus newStatus, UUID currentUserId) {
        logger.info("Updating lead status: {} to {} for tenant: {}", 
                   leadId, newStatus, TenantContext.getTenantId());

        Lead lead = findLeadByIdAndTenant(leadId);
        LeadStatus previousStatus = lead.getStatus();

        lead.setStatus(newStatus);
        lead.setUpdatedBy(currentUserId);

        // Update score based on status change
        leadScoringService.updateLeadScore(lead, "Status changed to " + newStatus, "STATUS_CHANGE", currentUserId);

        Lead savedLead = leadRepository.save(lead);

        // Publish status change event
        publishLeadStatusChangeEvent(savedLead, previousStatus, newStatus);

        logger.info("Updated lead status: {} from {} to {}", leadId, previousStatus, newStatus);

        return new LeadResponse(savedLead);
    }

    /**
     * Update lead qualification status
     */
    public LeadResponse updateQualificationStatus(UUID leadId, QualificationStatus newStatus, UUID currentUserId) {
        logger.info("Updating lead qualification status: {} to {} for tenant: {}", 
                   leadId, newStatus, TenantContext.getCurrentTenantId());

        Lead lead = findLeadByIdAndTenant(leadId);
        QualificationStatus previousStatus = lead.getQualificationStatus();

        lead.setQualificationStatus(newStatus);
        lead.setUpdatedBy(currentUserId);

        // Update score based on qualification change
        leadScoringService.updateLeadScore(lead, "Qualification changed to " + newStatus, "QUALIFICATION_CHANGE", currentUserId);

        Lead savedLead = leadRepository.save(lead);

        // Publish qualification change event
        publishLeadQualificationChangeEvent(savedLead, previousStatus, newStatus);

        logger.info("Updated lead qualification: {} from {} to {}", leadId, previousStatus, newStatus);

        return new LeadResponse(savedLead);
    }

    /**
     * Assign lead to new owner
     */
    public LeadResponse assignLead(UUID leadId, UUID newOwnerId, UUID currentUserId) {
        logger.info("Assigning lead: {} to owner: {} for tenant: {}", 
                   leadId, newOwnerId, TenantContext.getTenantId());

        Lead lead = findLeadByIdAndTenant(leadId);
        UUID previousOwnerId = lead.getOwnerId();

        lead.setOwnerId(newOwnerId);
        lead.setAssignedAt(LocalDateTime.now());
        lead.setUpdatedBy(currentUserId);

        Lead savedLead = leadRepository.save(lead);

        // Publish lead assignment event
        publishLeadAssignmentEvent(savedLead, previousOwnerId, newOwnerId);

        logger.info("Assigned lead: {} from owner: {} to owner: {}", leadId, previousOwnerId, newOwnerId);

        return new LeadResponse(savedLead);
    }

    /**
     * Convert lead to contact/account/deal
     */
    public LeadResponse convertLead(LeadConversionRequest conversionRequest, UUID currentUserId) {
        logger.info("Converting lead: {} for tenant: {}", 
                   conversionRequest.getLeadId(), TenantContext.getTenantId());

        Lead lead = findLeadByIdAndTenant(conversionRequest.getLeadId());

        // Validate lead can be converted
        if (lead.isConverted()) {
            throw new LeadBusinessException("LEAD_ALREADY_CONVERTED", "Lead is already converted");
        }

        if (!lead.getQualificationStatus().isQualified()) {
            throw new LeadBusinessException("LEAD_NOT_QUALIFIED", "Lead must be qualified before conversion");
        }

        // TODO: Implement actual conversion logic to create contact/account/deal
        // This would involve calling other services (contacts-service, accounts-service, deals-service)

        // For now, just mark as converted
        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedAt(LocalDateTime.now());
        lead.setUpdatedBy(currentUserId);

        Lead savedLead = leadRepository.save(lead);

        // Publish lead conversion event
        publishLeadConversionEvent(savedLead, conversionRequest);

        logger.info("Converted lead: {}", conversionRequest.getLeadId());

        return new LeadResponse(savedLead);
    }

    /**
     * Bulk update lead owners
     */
    public void bulkUpdateOwners(List<UUID> leadIds, UUID newOwnerId, UUID currentUserId) {
        logger.info("Bulk updating owners for {} leads to owner: {}", leadIds.size(), newOwnerId);

        int updatedCount = leadRepository.bulkUpdateOwner(
            TenantContext.getTenantId(), 
            leadIds, 
            newOwnerId, 
            LocalDateTime.now(), 
            currentUserId
        );

        logger.info("Bulk updated {} lead owners", updatedCount);

        // Publish bulk assignment event
        publishBulkAssignmentEvent(leadIds, newOwnerId);
    }

    /**
     * Bulk update lead statuses
     */
    public void bulkUpdateStatuses(List<UUID> leadIds, LeadStatus newStatus, UUID currentUserId) {
        logger.info("Bulk updating status for {} leads to status: {}", leadIds.size(), newStatus);

        int updatedCount = leadRepository.bulkUpdateStatus(
            TenantContext.getTenantId(), 
            leadIds, 
            newStatus, 
            currentUserId
        );

        logger.info("Bulk updated {} lead statuses", updatedCount);

        // Publish bulk status change event
        publishBulkStatusChangeEvent(leadIds, newStatus);
    }

    // Helper methods

    private Lead findLeadByIdAndTenant(UUID leadId) {
        return leadRepository.findByIdAndTenantId(leadId, TenantContext.getTenantId())
            .orElseThrow(() -> new LeadBusinessException("LEAD_NOT_FOUND", "Lead not found"));
    }

    private void mapRequestToEntity(LeadRequest request, Lead lead) {
        if (request.getEmail() != null) lead.setEmail(request.getEmail());
        if (request.getPhone() != null) lead.setPhone(request.getPhone());
        if (request.getMobile() != null) lead.setMobile(request.getMobile());
        if (request.getCompany() != null) lead.setCompany(request.getCompany());
        if (request.getTitle() != null) lead.setTitle(request.getTitle());
        if (request.getIndustry() != null) lead.setIndustry(request.getIndustry());
        if (request.getWebsite() != null) lead.setWebsite(request.getWebsite());
        if (request.getLeadSource() != null) lead.setLeadSource(request.getLeadSource());
        if (request.getLeadSourceDetail() != null) lead.setLeadSourceDetail(request.getLeadSourceDetail());
        if (request.getStatus() != null) lead.setStatus(request.getStatus());
        if (request.getQualificationStatus() != null) lead.setQualificationStatus(request.getQualificationStatus());
        if (request.getAnnualRevenue() != null) lead.setAnnualRevenue(request.getAnnualRevenue());
        if (request.getNumberOfEmployees() != null) lead.setNumberOfEmployees(request.getNumberOfEmployees());
        if (request.getBudget() != null) lead.setBudget(request.getBudget());
        if (request.getPurchaseTimeframe() != null) lead.setPurchaseTimeframe(request.getPurchaseTimeframe());
        if (request.getDecisionMaker() != null) lead.setDecisionMaker(request.getDecisionMaker());
        if (request.getPainPoints() != null) lead.setPainPoints(request.getPainPoints());
        if (request.getInterests() != null) lead.setInterests(request.getInterests());
        if (request.getNotes() != null) lead.setNotes(request.getNotes());
        if (request.getDoNotCall() != null) lead.setDoNotCall(request.getDoNotCall());
        if (request.getDoNotEmail() != null) lead.setDoNotEmail(request.getDoNotEmail());
        if (request.getEmailOptOut() != null) lead.setEmailOptOut(request.getEmailOptOut());
        if (request.getPreferredContactMethod() != null) lead.setPreferredContactMethod(request.getPreferredContactMethod());
        if (request.getTimezone() != null) lead.setTimezone(request.getTimezone());
        if (request.getLanguage() != null) lead.setLanguage(request.getLanguage());
        if (request.getNextFollowUpAt() != null) lead.setNextFollowUpAt(request.getNextFollowUpAt());
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    // Event publishing methods

    private void publishLeadEvent(String eventType, Lead lead) {
        try {
            kafkaTemplate.send("lead-events", eventType, new LeadResponse(lead));
        } catch (Exception e) {
            logger.error("Failed to publish lead event: {} for lead: {}", eventType, lead.getId(), e);
        }
    }

    private void publishLeadStatusChangeEvent(Lead lead, LeadStatus previousStatus, LeadStatus newStatus) {
        try {
            Map<String, Object> event = Map.of(
                "leadId", lead.getId(),
                "tenantId", lead.getTenantId(),
                "previousStatus", previousStatus,
                "newStatus", newStatus,
                "timestamp", LocalDateTime.now()
            );
            kafkaTemplate.send("lead-status-changes", lead.getId().toString(), event);
        } catch (Exception e) {
            logger.error("Failed to publish lead status change event for lead: {}", lead.getId(), e);
        }
    }

    private void publishLeadQualificationChangeEvent(Lead lead, QualificationStatus previousStatus, QualificationStatus newStatus) {
        try {
            Map<String, Object> event = Map.of(
                "leadId", lead.getId(),
                "tenantId", lead.getTenantId(),
                "previousQualificationStatus", previousStatus,
                "newQualificationStatus", newStatus,
                "timestamp", LocalDateTime.now()
            );
            kafkaTemplate.send("lead-qualification-changes", lead.getId().toString(), event);
        } catch (Exception e) {
            logger.error("Failed to publish lead qualification change event for lead: {}", lead.getId(), e);
        }
    }

    private void publishLeadAssignmentEvent(Lead lead, UUID previousOwnerId, UUID newOwnerId) {
        try {
            Map<String, Object> event = Map.of(
                "leadId", lead.getId(),
                "tenantId", lead.getTenantId(),
                "previousOwnerId", previousOwnerId,
                "newOwnerId", newOwnerId,
                "assignedAt", lead.getAssignedAt(),
                "timestamp", LocalDateTime.now()
            );
            kafkaTemplate.send("lead-assignments", lead.getId().toString(), event);
        } catch (Exception e) {
            logger.error("Failed to publish lead assignment event for lead: {}", lead.getId(), e);
        }
    }

    private void publishLeadConversionEvent(Lead lead, LeadConversionRequest conversionRequest) {
        try {
            Map<String, Object> event = Map.of(
                "leadId", lead.getId(),
                "tenantId", lead.getTenantId(),
                "convertedAt", lead.getConvertedAt(),
                "conversionRequest", conversionRequest,
                "timestamp", LocalDateTime.now()
            );
            kafkaTemplate.send("lead-conversions", lead.getId().toString(), event);
        } catch (Exception e) {
            logger.error("Failed to publish lead conversion event for lead: {}", lead.getId(), e);
        }
    }

    private void publishBulkAssignmentEvent(List<UUID> leadIds, UUID newOwnerId) {
        try {
            Map<String, Object> event = Map.of(
                "leadIds", leadIds,
                "tenantId", TenantContext.getTenantId(),
                "newOwnerId", newOwnerId,
                "timestamp", LocalDateTime.now()
            );
            kafkaTemplate.send("lead-bulk-assignments", "bulk", event);
        } catch (Exception e) {
            logger.error("Failed to publish bulk assignment event", e);
        }
    }

    private void publishBulkStatusChangeEvent(List<UUID> leadIds, LeadStatus newStatus) {
        try {
            Map<String, Object> event = Map.of(
                "leadIds", leadIds,
                "tenantId", TenantContext.getTenantId(),
                "newStatus", newStatus,
                "timestamp", LocalDateTime.now()
            );
            kafkaTemplate.send("lead-bulk-status-changes", "bulk", event);
        } catch (Exception e) {
            logger.error("Failed to publish bulk status change event", e);
        }
    }
}