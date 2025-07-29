package com.crm.platform.pipelines.service;

import com.crm.platform.pipelines.dto.PipelineRequest;
import com.crm.platform.pipelines.dto.PipelineStageRequest;
import com.crm.platform.pipelines.entity.Pipeline;
import com.crm.platform.pipelines.exception.PipelineBusinessException;
import com.crm.platform.pipelines.repository.PipelineRepository;
import com.crm.platform.pipelines.repository.PipelineStageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PipelineValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PipelineValidationService.class);

    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository stageRepository;

    @Value("${pipelines.max-pipelines-per-tenant:50}")
    private int maxPipelinesPerTenant;

    @Value("${pipelines.max-stages-per-pipeline:20}")
    private int maxStagesPerPipeline;

    @Autowired
    public PipelineValidationService(PipelineRepository pipelineRepository,
                                   PipelineStageRepository stageRepository) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
    }

    /**
     * Validate pipeline creation request
     */
    public void validatePipelineRequest(UUID tenantId, PipelineRequest request) {
        logger.debug("Validating pipeline request for tenant: {}", tenantId);

        // Check pipeline count limit
        validatePipelineCountLimit(tenantId);

        // Check name uniqueness
        validatePipelineNameUniqueness(tenantId, request.getName(), null);

        // Validate stages if provided
        if (request.getStages() != null) {
            validateStages(request.getStages());
        }

        // Validate default pipeline logic
        validateDefaultPipelineLogic(tenantId, request.getIsDefault());

        logger.debug("Pipeline request validation passed for tenant: {}", tenantId);
    }

    /**
     * Validate pipeline update request
     */
    public void validatePipelineUpdateRequest(UUID tenantId, UUID pipelineId, PipelineRequest request) {
        logger.debug("Validating pipeline update request for pipeline: {} tenant: {}", pipelineId, tenantId);

        // Check name uniqueness (excluding current pipeline)
        validatePipelineNameUniqueness(tenantId, request.getName(), pipelineId);

        // Validate stages if provided
        if (request.getStages() != null) {
            validateStages(request.getStages());
        }

        logger.debug("Pipeline update request validation passed for pipeline: {}", pipelineId);
    }

    /**
     * Validate pipeline deletion
     */
    public void validatePipelineDeletion(Pipeline pipeline) {
        logger.debug("Validating pipeline deletion for pipeline: {}", pipeline.getId());

        // Check if it's the default pipeline
        if (Boolean.TRUE.equals(pipeline.getIsDefault())) {
            long activePipelineCount = pipelineRepository.countByTenantIdAndIsActiveTrue(pipeline.getTenantId());
            if (activePipelineCount <= 1) {
                throw new PipelineBusinessException("Cannot delete the only active pipeline");
            }
        }

        // Additional business rules can be added here
        // For example, check if there are active deals in this pipeline

        logger.debug("Pipeline deletion validation passed for pipeline: {}", pipeline.getId());
    }

    /**
     * Validate pipeline clone request
     */
    public void validatePipelineClone(UUID tenantId, Pipeline template, String newName) {
        logger.debug("Validating pipeline clone request for tenant: {} template: {}", tenantId, template.getId());

        // Check pipeline count limit
        validatePipelineCountLimit(tenantId);

        // Check new name uniqueness
        validatePipelineNameUniqueness(tenantId, newName, null);

        // Validate template belongs to tenant
        if (!template.getTenantId().equals(tenantId)) {
            throw new PipelineBusinessException("Template pipeline does not belong to tenant");
        }

        logger.debug("Pipeline clone validation passed for tenant: {}", tenantId);
    }

    /**
     * Validate stage request
     */
    public void validateStageRequest(UUID pipelineId, PipelineStageRequest request) {
        logger.debug("Validating stage request for pipeline: {}", pipelineId);

        // Check stage count limit
        validateStageCountLimit(pipelineId);

        // Check stage name uniqueness within pipeline
        validateStageNameUniqueness(pipelineId, request.getName(), null);

        // Check display order uniqueness within pipeline
        validateStageDisplayOrderUniqueness(pipelineId, request.getDisplayOrder(), null);

        // Validate business rules
        validateStageBusinessRules(request);

        logger.debug("Stage request validation passed for pipeline: {}", pipelineId);
    }

    /**
     * Validate stage update request
     */
    public void validateStageUpdateRequest(UUID pipelineId, UUID stageId, PipelineStageRequest request) {
        logger.debug("Validating stage update request for stage: {} pipeline: {}", stageId, pipelineId);

        // Check stage name uniqueness within pipeline (excluding current stage)
        validateStageNameUniqueness(pipelineId, request.getName(), stageId);

        // Check display order uniqueness within pipeline (excluding current stage)
        validateStageDisplayOrderUniqueness(pipelineId, request.getDisplayOrder(), stageId);

        // Validate business rules
        validateStageBusinessRules(request);

        logger.debug("Stage update request validation passed for stage: {}", stageId);
    }

    // Private validation methods

    private void validatePipelineCountLimit(UUID tenantId) {
        long currentCount = pipelineRepository.countByTenantIdAndIsActiveTrue(tenantId);
        if (currentCount >= maxPipelinesPerTenant) {
            throw new PipelineBusinessException(
                    String.format("Maximum number of pipelines (%d) reached for tenant", maxPipelinesPerTenant));
        }
    }

    private void validatePipelineNameUniqueness(UUID tenantId, String name, UUID excludePipelineId) {
        if (name == null || name.trim().isEmpty()) {
            throw new PipelineBusinessException("Pipeline name cannot be empty");
        }

        boolean exists = excludePipelineId == null
                ? pipelineRepository.existsByTenantIdAndName(tenantId, name.trim())
                : pipelineRepository.existsByTenantIdAndNameAndIdNot(tenantId, name.trim(), excludePipelineId);

        if (exists) {
            throw new PipelineBusinessException("Pipeline name already exists: " + name);
        }
    }

    private void validateDefaultPipelineLogic(UUID tenantId, Boolean isDefault) {
        // If this is the first pipeline for the tenant, it should be default
        long pipelineCount = pipelineRepository.countByTenantIdAndIsActiveTrue(tenantId);
        if (pipelineCount == 0 && !Boolean.TRUE.equals(isDefault)) {
            throw new PipelineBusinessException("First pipeline must be set as default");
        }
    }

    private void validateStages(List<PipelineStageRequest> stages) {
        if (stages.size() > maxStagesPerPipeline) {
            throw new PipelineBusinessException(
                    String.format("Maximum number of stages (%d) exceeded", maxStagesPerPipeline));
        }

        // Check for duplicate stage names
        Set<String> stageNames = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();

        for (PipelineStageRequest stage : stages) {
            // Check duplicate names
            if (!stageNames.add(stage.getName().trim().toLowerCase())) {
                throw new PipelineBusinessException("Duplicate stage name: " + stage.getName());
            }

            // Check duplicate display orders
            if (!displayOrders.add(stage.getDisplayOrder())) {
                throw new PipelineBusinessException("Duplicate display order: " + stage.getDisplayOrder());
            }

            // Validate individual stage
            validateStageBusinessRules(stage);
        }
    }

    private void validateStageCountLimit(UUID pipelineId) {
        long currentCount = stageRepository.countByPipelineIdAndIsActiveTrue(pipelineId);
        if (currentCount >= maxStagesPerPipeline) {
            throw new PipelineBusinessException(
                    String.format("Maximum number of stages (%d) reached for pipeline", maxStagesPerPipeline));
        }
    }

    private void validateStageNameUniqueness(UUID pipelineId, String name, UUID excludeStageId) {
        if (name == null || name.trim().isEmpty()) {
            throw new PipelineBusinessException("Stage name cannot be empty");
        }

        boolean exists = excludeStageId == null
                ? stageRepository.existsByPipelineIdAndName(pipelineId, name.trim())
                : stageRepository.existsByPipelineIdAndNameAndIdNot(pipelineId, name.trim(), excludeStageId);

        if (exists) {
            throw new PipelineBusinessException("Stage name already exists in pipeline: " + name);
        }
    }

    private void validateStageDisplayOrderUniqueness(UUID pipelineId, Integer displayOrder, UUID excludeStageId) {
        if (displayOrder == null || displayOrder < 0) {
            throw new PipelineBusinessException("Display order must be a non-negative number");
        }

        boolean exists = excludeStageId == null
                ? stageRepository.existsByPipelineIdAndDisplayOrder(pipelineId, displayOrder)
                : stageRepository.existsByPipelineIdAndDisplayOrderAndIdNot(pipelineId, displayOrder, excludeStageId);

        if (exists) {
            throw new PipelineBusinessException("Display order already exists in pipeline: " + displayOrder);
        }
    }

    private void validateStageBusinessRules(PipelineStageRequest stage) {
        // Validate won stage must be closed
        if (Boolean.TRUE.equals(stage.getIsWon()) && !Boolean.TRUE.equals(stage.getIsClosed())) {
            throw new PipelineBusinessException("Won stage must be marked as closed: " + stage.getName());
        }

        // Validate probability range
        if (stage.getDefaultProbability() != null) {
            if (stage.getDefaultProbability().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                stage.getDefaultProbability().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new PipelineBusinessException("Default probability must be between 0 and 100: " + stage.getName());
            }
        }

        // Validate color format if provided
        if (stage.getColor() != null && !stage.getColor().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new PipelineBusinessException("Invalid color format for stage: " + stage.getName());
        }
    }
}