package com.crm.platform.deals.service;

import com.crm.platform.common.exception.CrmBusinessException;
import com.crm.platform.common.util.TenantContext;
import com.crm.platform.deals.dto.*;
import com.crm.platform.deals.entity.Deal;
import com.crm.platform.deals.entity.DealStageHistory;
import com.crm.platform.deals.entity.Pipeline;
import com.crm.platform.deals.entity.PipelineStage;
import com.crm.platform.deals.repository.DealRepository;
import com.crm.platform.deals.repository.DealStageHistoryRepository;
import com.crm.platform.deals.repository.PipelineRepository;
import com.crm.platform.deals.repository.PipelineStageRepository;
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

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DealService {

    private static final Logger logger = LoggerFactory.getLogger(DealService.class);

    private final DealRepository dealRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository pipelineStageRepository;
    private final DealStageHistoryRepository dealStageHistoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DealEventService dealEventService;
    private final DealValidationService dealValidationService;

    @Autowired
    public DealService(DealRepository dealRepository,
                      PipelineRepository pipelineRepository,
                      PipelineStageRepository pipelineStageRepository,
                      DealStageHistoryRepository dealStageHistoryRepository,
                      KafkaTemplate<String, Object> kafkaTemplate,
                      DealEventService dealEventService,
                      DealValidationService dealValidationService) {
        this.dealRepository = dealRepository;
        this.pipelineRepository = pipelineRepository;
        this.pipelineStageRepository = pipelineStageRepository;
        this.dealStageHistoryRepository = dealStageHistoryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.dealEventService = dealEventService;
        this.dealValidationService = dealValidationService;
    }

    public DealResponse createDeal(DealRequest request, UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Creating deal '{}' for tenant {} by user {}", request.getName(), tenantId, userId);

        // Validate the request
        dealValidationService.validateDealRequest(request, tenantId);

        // Create deal entity
        Deal deal = new Deal();
        mapRequestToEntity(request, deal);
        deal.setTenantId(tenantId);
        deal.setCreatedBy(userId);
        deal.setUpdatedBy(userId);

        // Set default probability from stage if not provided
        if (deal.getProbability() == null) {
            PipelineStage stage = pipelineStageRepository.findById(request.getStageId())
                .orElseThrow(() -> new CrmBusinessException("STAGE_NOT_FOUND", "Pipeline stage not found"));
            deal.setProbability(stage.getDefaultProbability());
        }

        // Save deal
        deal = dealRepository.save(deal);

        // Create initial stage history
        createStageHistoryEntry(deal, null, deal.getStageId(), userId, "Deal created");

        // Publish event
        dealEventService.publishDealCreated(deal);

        logger.info("Deal created with ID: {}", deal.getId());
        return mapEntityToResponse(deal);
    }

    @Transactional(readOnly = true)
    public DealResponse getDeal(UUID dealId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Deal deal = dealRepository.findByIdAndTenantId(dealId, tenantId)
            .orElseThrow(() -> new CrmBusinessException("DEAL_NOT_FOUND", "Deal not found"));
        
        return mapEntityToResponse(deal);
    }

    public DealResponse updateDeal(UUID dealId, DealRequest request, UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Updating deal {} for tenant {} by user {}", dealId, tenantId, userId);

        Deal deal = dealRepository.findByIdAndTenantId(dealId, tenantId)
            .orElseThrow(() -> new CrmBusinessException("DEAL_NOT_FOUND", "Deal not found"));

        // Validate the request
        dealValidationService.validateDealRequest(request, tenantId);

        // Store original stage for history tracking
        UUID originalStageId = deal.getStageId();

        // Update deal
        mapRequestToEntity(request, deal);
        deal.setUpdatedBy(userId);

        // Handle stage change
        if (!originalStageId.equals(request.getStageId())) {
            handleStageChange(deal, originalStageId, request.getStageId(), userId, "Stage updated");
        }

        deal = dealRepository.save(deal);

        // Publish event
        dealEventService.publishDealUpdated(deal);

        logger.info("Deal {} updated successfully", dealId);
        return mapEntityToResponse(deal);
    }

    public void deleteDeal(UUID dealId, UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Deleting deal {} for tenant {} by user {}", dealId, tenantId, userId);

        Deal deal = dealRepository.findByIdAndTenantId(dealId, tenantId)
            .orElseThrow(() -> new CrmBusinessException("DEAL_NOT_FOUND", "Deal not found"));

        // Delete stage history
        dealStageHistoryRepository.deleteByDealId(dealId);

        // Delete deal
        dealRepository.delete(deal);

        // Publish event
        dealEventService.publishDealDeleted(deal);

        logger.info("Deal {} deleted successfully", dealId);
    }

    public DealResponse moveDealToStage(UUID dealId, DealStageChangeRequest request, UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Moving deal {} to stage {} for tenant {} by user {}", 
                   dealId, request.getStageId(), tenantId, userId);

        Deal deal = dealRepository.findByIdAndTenantId(dealId, tenantId)
            .orElseThrow(() -> new CrmBusinessException("DEAL_NOT_FOUND", "Deal not found"));

        // Validate stage exists and belongs to the same pipeline
        PipelineStage newStage = pipelineStageRepository.findByIdAndTenantId(request.getStageId(), tenantId)
            .orElseThrow(() -> new CrmBusinessException("STAGE_NOT_FOUND", "Pipeline stage not found"));

        if (!newStage.getPipelineId().equals(deal.getPipelineId())) {
            throw new CrmBusinessException("INVALID_STAGE", "Stage does not belong to deal's pipeline");
        }

        UUID originalStageId = deal.getStageId();
        
        // Handle stage change
        handleStageChange(deal, originalStageId, request.getStageId(), userId, request.getReason());

        deal = dealRepository.save(deal);

        // Publish event
        dealEventService.publishDealStageChanged(deal, originalStageId, request.getStageId());

        logger.info("Deal {} moved to stage {} successfully", dealId, request.getStageId());
        return mapEntityToResponse(deal);
    }

    @Transactional(readOnly = true)
    public Page<DealResponse> searchDeals(DealSearchRequest searchRequest) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        Specification<Deal> spec = createSearchSpecification(searchRequest, tenantId);
        
        Sort sort = createSort(searchRequest.getSortBy(), searchRequest.getSortDirection());
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
        
        Page<Deal> deals = dealRepository.findAll(spec, pageable);
        
        return deals.map(this::mapEntityToResponse);
    }

    @Transactional(readOnly = true)
    public List<DealResponse> getDealsByPipeline(UUID pipelineId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<Deal> deals = dealRepository.findByTenantIdAndPipelineId(tenantId, pipelineId);
        return deals.stream().map(this::mapEntityToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DealResponse> getDealsByStage(UUID stageId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<Deal> deals = dealRepository.findByTenantIdAndStageId(tenantId, stageId);
        return deals.stream().map(this::mapEntityToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DealResponse> getDealsByOwner(UUID ownerId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<Deal> deals = dealRepository.findByTenantIdAndOwnerId(tenantId, ownerId);
        return deals.stream().map(this::mapEntityToResponse).collect(Collectors.toList());
    }

    // Bulk operations
    public List<DealResponse> bulkCreateDeals(List<DealRequest> requests, UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Bulk creating {} deals for tenant {} by user {}", requests.size(), tenantId, userId);

        List<Deal> deals = new ArrayList<>();
        
        for (DealRequest request : requests) {
            dealValidationService.validateDealRequest(request, tenantId);
            
            Deal deal = new Deal();
            mapRequestToEntity(request, deal);
            deal.setTenantId(tenantId);
            deal.setCreatedBy(userId);
            deal.setUpdatedBy(userId);
            
            deals.add(deal);
        }

        deals = dealRepository.saveAll(deals);

        // Create stage history entries and publish events
        for (Deal deal : deals) {
            createStageHistoryEntry(deal, null, deal.getStageId(), userId, "Deal created");
            dealEventService.publishDealCreated(deal);
        }

        logger.info("Bulk created {} deals successfully", deals.size());
        return deals.stream().map(this::mapEntityToResponse).collect(Collectors.toList());
    }

    public void bulkUpdateStage(List<UUID> dealIds, UUID newStageId, UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Bulk updating stage for {} deals to stage {} by user {}", 
                   dealIds.size(), newStageId, userId);

        // Validate stage
        PipelineStage newStage = pipelineStageRepository.findByIdAndTenantId(newStageId, tenantId)
            .orElseThrow(() -> new CrmBusinessException("STAGE_NOT_FOUND", "Pipeline stage not found"));

        // Get deals and validate they belong to the same pipeline
        List<Deal> deals = dealRepository.findAllById(dealIds);
        for (Deal deal : deals) {
            if (!deal.getTenantId().equals(tenantId)) {
                throw new CrmBusinessException("DEAL_NOT_FOUND", "Deal not found");
            }
            if (!deal.getPipelineId().equals(newStage.getPipelineId())) {
                throw new CrmBusinessException("INVALID_STAGE", 
                    "Stage does not belong to deal's pipeline: " + deal.getId());
            }
        }

        // Update deals
        for (Deal deal : deals) {
            UUID originalStageId = deal.getStageId();
            handleStageChange(deal, originalStageId, newStageId, userId, "Bulk stage update");
        }

        dealRepository.saveAll(deals);

        // Publish events
        for (Deal deal : deals) {
            dealEventService.publishDealStageChanged(deal, deal.getStageId(), newStageId);
        }

        logger.info("Bulk updated stage for {} deals successfully", deals.size());
    }

    public void bulkUpdateOwner(List<UUID> dealIds, UUID newOwnerId, UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Bulk updating owner for {} deals to owner {} by user {}", 
                   dealIds.size(), newOwnerId, userId);

        int updatedCount = dealRepository.bulkUpdateOwner(dealIds, newOwnerId, userId, tenantId);
        
        // Get updated deals and publish events
        List<Deal> deals = dealRepository.findAllById(dealIds);
        for (Deal deal : deals) {
            dealEventService.publishDealUpdated(deal);
        }

        logger.info("Bulk updated owner for {} deals successfully", updatedCount);
    }

    public void bulkDeleteDeals(List<UUID> dealIds, UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        logger.info("Bulk deleting {} deals for tenant {} by user {}", dealIds.size(), tenantId, userId);

        // Get deals for event publishing
        List<Deal> deals = dealRepository.findAllById(dealIds);
        
        // Delete stage history
        dealStageHistoryRepository.deleteByDealIdIn(dealIds);
        
        // Delete deals
        dealRepository.deleteByTenantIdAndIdIn(tenantId, dealIds);

        // Publish events
        for (Deal deal : deals) {
            dealEventService.publishDealDeleted(deal);
        }

        logger.info("Bulk deleted {} deals successfully", deals.size());
    }

    // Private helper methods
    private void handleStageChange(Deal deal, UUID fromStageId, UUID toStageId, UUID userId, String reason) {
        // Update deal stage and related fields
        PipelineStage newStage = pipelineStageRepository.findById(toStageId)
            .orElseThrow(() -> new CrmBusinessException("STAGE_NOT_FOUND", "Pipeline stage not found"));

        deal.setStageId(toStageId);
        deal.setUpdatedBy(userId);

        // Update deal status if moving to closed stage
        if (newStage.getIsClosed()) {
            deal.setIsClosed(true);
            deal.setIsWon(newStage.getIsWon());
            if (deal.getActualCloseDate() == null) {
                deal.setActualCloseDate(java.time.LocalDate.now());
            }
        } else {
            deal.setIsClosed(false);
            deal.setIsWon(false);
            deal.setActualCloseDate(null);
        }

        // Update probability if stage has default probability
        if (newStage.getDefaultProbability() != null) {
            deal.setProbability(newStage.getDefaultProbability());
        }

        // Create stage history entry
        createStageHistoryEntry(deal, fromStageId, toStageId, userId, reason);
    }

    private void createStageHistoryEntry(Deal deal, UUID fromStageId, UUID toStageId, UUID userId, String reason) {
        DealStageHistory history = new DealStageHistory();
        history.setDeal(deal);
        history.setFromStageId(fromStageId);
        history.setToStageId(toStageId);
        history.setPipelineId(deal.getPipelineId());
        history.setChangedBy(userId);
        history.setReason(reason);

        // Calculate duration in previous stage
        if (fromStageId != null) {
            List<DealStageHistory> previousHistory = dealStageHistoryRepository
                .findByDealIdOrderByChangedAtDesc(deal.getId());
            if (!previousHistory.isEmpty()) {
                LocalDateTime previousChangeTime = previousHistory.get(0).getChangedAt();
                long hours = ChronoUnit.HOURS.between(previousChangeTime, LocalDateTime.now());
                history.setDurationInPreviousStageHours(hours);
            }
        }

        dealStageHistoryRepository.save(history);
    }

    private Specification<Deal> createSearchSpecification(DealSearchRequest searchRequest, UUID tenantId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant filter
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            // Name filter
            if (searchRequest.getName() != null && !searchRequest.getName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + searchRequest.getName().toLowerCase() + "%"
                ));
            }

            // Pipeline filter
            if (searchRequest.getPipelineIds() != null && !searchRequest.getPipelineIds().isEmpty()) {
                predicates.add(root.get("pipelineId").in(searchRequest.getPipelineIds()));
            }

            // Stage filter
            if (searchRequest.getStageIds() != null && !searchRequest.getStageIds().isEmpty()) {
                predicates.add(root.get("stageId").in(searchRequest.getStageIds()));
            }

            // Owner filter
            if (searchRequest.getOwnerIds() != null && !searchRequest.getOwnerIds().isEmpty()) {
                predicates.add(root.get("ownerId").in(searchRequest.getOwnerIds()));
            }

            // Amount range filter
            if (searchRequest.getMinAmount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), searchRequest.getMinAmount()));
            }
            if (searchRequest.getMaxAmount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), searchRequest.getMaxAmount()));
            }

            // Probability range filter
            if (searchRequest.getMinProbability() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("probability"), searchRequest.getMinProbability()));
            }
            if (searchRequest.getMaxProbability() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("probability"), searchRequest.getMaxProbability()));
            }

            // Date filters
            if (searchRequest.getExpectedCloseDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("expectedCloseDate"), searchRequest.getExpectedCloseDateFrom()));
            }
            if (searchRequest.getExpectedCloseDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("expectedCloseDate"), searchRequest.getExpectedCloseDateTo()));
            }

            // Status filters
            if (searchRequest.getIsClosed() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isClosed"), searchRequest.getIsClosed()));
            }
            if (searchRequest.getIsWon() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isWon"), searchRequest.getIsWon()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        return Sort.by(direction, sortBy);
    }

    private void mapRequestToEntity(DealRequest request, Deal deal) {
        deal.setName(request.getName());
        deal.setPipelineId(request.getPipelineId());
        deal.setStageId(request.getStageId());
        deal.setAccountId(request.getAccountId());
        deal.setContactId(request.getContactId());
        deal.setAmount(request.getAmount());
        deal.setCurrency(request.getCurrency());
        deal.setProbability(request.getProbability());
        deal.setExpectedCloseDate(request.getExpectedCloseDate());
        deal.setDealType(request.getDealType());
        deal.setLeadSource(request.getLeadSource());
        deal.setNextStep(request.getNextStep());
        deal.setDescription(request.getDescription());
        deal.setTags(request.getTags());
        deal.setCustomFields(request.getCustomFields());
        deal.setOwnerId(request.getOwnerId());
    }

    private DealResponse mapEntityToResponse(Deal deal) {
        DealResponse response = new DealResponse();
        response.setId(deal.getId());
        response.setTenantId(deal.getTenantId());
        response.setAccountId(deal.getAccountId());
        response.setContactId(deal.getContactId());
        response.setPipelineId(deal.getPipelineId());
        response.setStageId(deal.getStageId());
        response.setName(deal.getName());
        response.setAmount(deal.getAmount());
        response.setCurrency(deal.getCurrency());
        response.setProbability(deal.getProbability());
        response.setWeightedAmount(deal.getWeightedAmount());
        response.setExpectedCloseDate(deal.getExpectedCloseDate());
        response.setActualCloseDate(deal.getActualCloseDate());
        response.setDealType(deal.getDealType());
        response.setLeadSource(deal.getLeadSource());
        response.setNextStep(deal.getNextStep());
        response.setDescription(deal.getDescription());
        response.setIsClosed(deal.getIsClosed());
        response.setIsWon(deal.getIsWon());
        response.setTags(deal.getTags());
        response.setCustomFields(deal.getCustomFields());
        response.setOwnerId(deal.getOwnerId());
        response.setCreatedAt(deal.getCreatedAt());
        response.setUpdatedAt(deal.getUpdatedAt());
        response.setCreatedBy(deal.getCreatedBy());
        response.setUpdatedBy(deal.getUpdatedBy());

        // Add pipeline and stage information
        try {
            Pipeline pipeline = pipelineRepository.findById(deal.getPipelineId()).orElse(null);
            if (pipeline != null) {
                response.setPipelineName(pipeline.getName());
            }

            PipelineStage stage = pipelineStageRepository.findById(deal.getStageId()).orElse(null);
            if (stage != null) {
                response.setStageName(stage.getName());
                response.setStageColor(stage.getColor());
            }
        } catch (Exception e) {
            logger.warn("Error loading pipeline/stage information for deal {}: {}", deal.getId(), e.getMessage());
        }

        return response;
    }
}