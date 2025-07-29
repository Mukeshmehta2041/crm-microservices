package com.crm.platform.pipelines.service;

import com.crm.platform.pipelines.dto.*;
import com.crm.platform.pipelines.entity.Pipeline;
import com.crm.platform.pipelines.entity.PipelineStage;
import com.crm.platform.pipelines.exception.PipelineBusinessException;
import com.crm.platform.pipelines.repository.PipelineRepository;
import com.crm.platform.pipelines.repository.PipelineStageRepository;
import com.crm.platform.pipelines.specification.PipelineSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PipelineService {

    private static final Logger logger = LoggerFactory.getLogger(PipelineService.class);

    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository stageRepository;
    private final PipelineValidationService validationService;
    private final PipelineEventService eventService;
    private final PipelineMapper mapper;

    @Autowired
    public PipelineService(PipelineRepository pipelineRepository,
                          PipelineStageRepository stageRepository,
                          PipelineValidationService validationService,
                          PipelineEventService eventService,
                          PipelineMapper mapper) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.validationService = validationService;
        this.eventService = eventService;
        this.mapper = mapper;
    }

    /**
     * Create a new pipeline
     */
    @CacheEvict(value = "pipelines", key = "#tenantId")
    public PipelineResponse createPipeline(UUID tenantId, PipelineRequest request, UUID userId) {
        logger.info("Creating pipeline for tenant: {} by user: {}", tenantId, userId);

        // Validate request
        validationService.validatePipelineRequest(tenantId, request);

        // Create pipeline entity
        Pipeline pipeline = mapper.toEntity(request, tenantId, userId);

        // Handle default pipeline logic
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            handleDefaultPipelineChange(tenantId, null);
        }

        // Save pipeline
        pipeline = pipelineRepository.save(pipeline);

        // Create stages if provided
        if (request.getStages() != null && !request.getStages().isEmpty()) {
            createStagesForPipeline(pipeline, request.getStages(), userId);
        }

        // Publish event
        eventService.publishPipelineCreatedEvent(pipeline);

        logger.info("Pipeline created successfully: {}", pipeline.getId());
        return mapper.toResponse(pipeline);
    }

    /**
     * Update an existing pipeline
     */
    @CacheEvict(value = "pipelines", key = "#tenantId")
    public PipelineResponse updatePipeline(UUID tenantId, UUID pipelineId, PipelineRequest request, UUID userId) {
        logger.info("Updating pipeline: {} for tenant: {} by user: {}", pipelineId, tenantId, userId);

        // Find existing pipeline
        Pipeline pipeline = findPipelineByIdAndTenant(pipelineId, tenantId);

        // Validate update request
        validationService.validatePipelineUpdateRequest(tenantId, pipelineId, request);

        // Handle default pipeline logic
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(pipeline.getIsDefault())) {
            handleDefaultPipelineChange(tenantId, pipelineId);
        }

        // Update pipeline
        mapper.updateEntity(pipeline, request, userId);
        pipeline = pipelineRepository.save(pipeline);

        // Publish event
        eventService.publishPipelineUpdatedEvent(pipeline);

        logger.info("Pipeline updated successfully: {}", pipelineId);
        return mapper.toResponse(pipeline);
    }

    /**
     * Get pipeline by ID
     */
    @Cacheable(value = "pipelines", key = "#tenantId + '_' + #pipelineId")
    @Transactional(readOnly = true)
    public PipelineResponse getPipeline(UUID tenantId, UUID pipelineId) {
        logger.debug("Getting pipeline: {} for tenant: {}", pipelineId, tenantId);

        Pipeline pipeline = findPipelineByIdAndTenant(pipelineId, tenantId);
        return mapper.toResponse(pipeline);
    }

    /**
     * Get all pipelines for tenant
     */
    @Cacheable(value = "pipelines", key = "#tenantId + '_all'")
    @Transactional(readOnly = true)
    public List<PipelineResponse> getAllPipelines(UUID tenantId) {
        logger.debug("Getting all pipelines for tenant: {}", tenantId);

        List<Pipeline> pipelines = pipelineRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId);
        return pipelines.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active pipelines for tenant
     */
    @Cacheable(value = "pipelines", key = "#tenantId + '_active'")
    @Transactional(readOnly = true)
    public List<PipelineResponse> getActivePipelines(UUID tenantId) {
        logger.debug("Getting active pipelines for tenant: {}", tenantId);

        List<Pipeline> pipelines = pipelineRepository.findByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(tenantId);
        return pipelines.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get default pipeline for tenant
     */
    @Cacheable(value = "pipelines", key = "#tenantId + '_default'")
    @Transactional(readOnly = true)
    public Optional<PipelineResponse> getDefaultPipeline(UUID tenantId) {
        logger.debug("Getting default pipeline for tenant: {}", tenantId);

        return pipelineRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .map(mapper::toResponse);
    }

    /**
     * Search pipelines
     */
    @Transactional(readOnly = true)
    public Page<PipelineResponse> searchPipelines(UUID tenantId, PipelineSearchRequest searchRequest) {
        logger.debug("Searching pipelines for tenant: {} with criteria: {}", tenantId, searchRequest);

        Pageable pageable = createPageable(searchRequest);
        
        if (searchRequest.getName() != null && !searchRequest.getName().trim().isEmpty()) {
            Page<Pipeline> pipelines = pipelineRepository.searchByName(tenantId, searchRequest.getName().trim(), pageable);
            return pipelines.map(mapper::toResponse);
        }

        // For more complex searches, use specification
        Page<Pipeline> pipelines = pipelineRepository.findAll(
                PipelineSpecification.buildSpecification(tenantId, searchRequest), 
                pageable
        );
        return pipelines.map(mapper::toResponse);
    }

    /**
     * Delete pipeline
     */
    @CacheEvict(value = "pipelines", key = "#tenantId")
    public void deletePipeline(UUID tenantId, UUID pipelineId, UUID userId) {
        logger.info("Deleting pipeline: {} for tenant: {} by user: {}", pipelineId, tenantId, userId);

        Pipeline pipeline = findPipelineByIdAndTenant(pipelineId, tenantId);

        // Validate deletion
        validationService.validatePipelineDeletion(pipeline);

        // Publish event before deletion
        eventService.publishPipelineDeletedEvent(pipeline);

        // Delete pipeline (cascade will handle stages and rules)
        pipelineRepository.delete(pipeline);

        logger.info("Pipeline deleted successfully: {}", pipelineId);
    }

    /**
     * Clone pipeline from template
     */
    @CacheEvict(value = "pipelines", key = "#tenantId")
    public PipelineResponse clonePipeline(UUID tenantId, UUID templateId, String newName, UUID userId) {
        logger.info("Cloning pipeline from template: {} for tenant: {} by user: {}", templateId, tenantId, userId);

        // Find template pipeline
        Pipeline template = findPipelineByIdAndTenant(templateId, tenantId);

        // Validate clone request
        validationService.validatePipelineClone(tenantId, template, newName);

        // Create new pipeline from template
        Pipeline clonedPipeline = mapper.cloneFromTemplate(template, newName, userId);
        clonedPipeline = pipelineRepository.save(clonedPipeline);

        // Clone stages
        List<PipelineStage> templateStages = stageRepository.findByPipelineIdOrderByDisplayOrderAsc(templateId);
        for (PipelineStage templateStage : templateStages) {
            PipelineStage clonedStage = mapper.cloneStage(templateStage, clonedPipeline, userId);
            stageRepository.save(clonedStage);
        }

        // Publish event
        eventService.publishPipelineClonedEvent(clonedPipeline, template);

        logger.info("Pipeline cloned successfully: {} from template: {}", clonedPipeline.getId(), templateId);
        return mapper.toResponse(clonedPipeline);
    }

    /**
     * Get pipeline templates
     */
    @Cacheable(value = "pipelines", key = "#tenantId + '_templates'")
    @Transactional(readOnly = true)
    public List<PipelineResponse> getPipelineTemplates(UUID tenantId) {
        logger.debug("Getting pipeline templates for tenant: {}", tenantId);

        List<Pipeline> templates = pipelineRepository.findTemplates(tenantId);
        return templates.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Reorder pipelines
     */
    @CacheEvict(value = "pipelines", key = "#tenantId")
    public void reorderPipelines(UUID tenantId, List<UUID> pipelineIds, UUID userId) {
        logger.info("Reordering pipelines for tenant: {} by user: {}", tenantId, userId);

        List<Pipeline> pipelines = pipelineRepository.findAllById(pipelineIds);
        
        // Validate all pipelines belong to tenant
        pipelines.forEach(pipeline -> {
            if (!pipeline.getTenantId().equals(tenantId)) {
                throw new PipelineBusinessException("Pipeline does not belong to tenant: " + pipeline.getId());
            }
        });

        // Update display orders
        for (int i = 0; i < pipelineIds.size(); i++) {
            UUID pipelineId = pipelineIds.get(i);
            Pipeline pipeline = pipelines.stream()
                    .filter(p -> p.getId().equals(pipelineId))
                    .findFirst()
                    .orElseThrow(() -> new PipelineBusinessException("Pipeline not found: " + pipelineId));
            
            pipeline.setDisplayOrder(i);
            pipeline.setUpdatedBy(userId);
        }

        pipelineRepository.saveAll(pipelines);

        // Publish event
        eventService.publishPipelinesReorderedEvent(tenantId, pipelineIds);

        logger.info("Pipelines reordered successfully for tenant: {}", tenantId);
    }

    // Private helper methods

    private Pipeline findPipelineByIdAndTenant(UUID pipelineId, UUID tenantId) {
        return pipelineRepository.findById(pipelineId)
                .filter(pipeline -> pipeline.getTenantId().equals(tenantId))
                .orElseThrow(() -> new PipelineBusinessException("Pipeline not found: " + pipelineId));
    }

    private void handleDefaultPipelineChange(UUID tenantId, UUID newDefaultPipelineId) {
        // Remove default flag from current default pipeline
        pipelineRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .ifPresent(currentDefault -> {
                    if (newDefaultPipelineId == null || !currentDefault.getId().equals(newDefaultPipelineId)) {
                        currentDefault.setIsDefault(false);
                        pipelineRepository.save(currentDefault);
                    }
                });
    }

    private void createStagesForPipeline(Pipeline pipeline, List<PipelineStageRequest> stageRequests, UUID userId) {
        for (PipelineStageRequest stageRequest : stageRequests) {
            PipelineStage stage = mapper.toStageEntity(stageRequest, pipeline, userId);
            stageRepository.save(stage);
        }
    }

    private Pageable createPageable(PipelineSearchRequest searchRequest) {
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(searchRequest.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                searchRequest.getSortBy()
        );
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }
}