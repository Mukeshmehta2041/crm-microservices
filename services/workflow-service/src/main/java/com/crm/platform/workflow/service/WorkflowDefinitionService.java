package com.crm.platform.workflow.service;

import com.crm.platform.workflow.dto.WorkflowDefinitionDto;
import com.crm.platform.workflow.entity.WorkflowDefinition;
import com.crm.platform.workflow.exception.WorkflowNotFoundException;
import com.crm.platform.workflow.exception.WorkflowValidationException;
import com.crm.platform.workflow.mapper.WorkflowDefinitionMapper;
import com.crm.platform.workflow.repository.WorkflowDefinitionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing workflow definitions
 */
@Service
@Transactional
public class WorkflowDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionService.class);

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowDefinitionMapper workflowDefinitionMapper;
    private final WorkflowValidationService workflowValidationService;

    @Autowired
    public WorkflowDefinitionService(WorkflowDefinitionRepository workflowDefinitionRepository,
                                   WorkflowDefinitionMapper workflowDefinitionMapper,
                                   WorkflowValidationService workflowValidationService) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowDefinitionMapper = workflowDefinitionMapper;
        this.workflowValidationService = workflowValidationService;
    }

    /**
     * Create a new workflow definition
     */
    @CacheEvict(value = "workflows", key = "#dto.tenantId")
    public WorkflowDefinitionDto createWorkflow(WorkflowDefinitionDto dto) {
        logger.info("Creating workflow definition: {} for tenant: {}", dto.getName(), dto.getTenantId());

        // Validate workflow JSON
        workflowValidationService.validateWorkflowDefinition(dto.getWorkflowJson());

        // Check if workflow with same name already exists
        if (workflowDefinitionRepository.existsByTenantIdAndName(dto.getTenantId(), dto.getName())) {
            // Get next version number
            List<WorkflowDefinition> existingVersions = workflowDefinitionRepository
                    .findByTenantIdAndNameOrderByVersionDesc(dto.getTenantId(), dto.getName());
            if (!existingVersions.isEmpty()) {
                dto.setVersion(existingVersions.get(0).getVersion() + 1);
            }
        }

        WorkflowDefinition entity = workflowDefinitionMapper.toEntity(dto);
        WorkflowDefinition savedEntity = workflowDefinitionRepository.save(entity);

        logger.info("Created workflow definition with ID: {} version: {}", 
                   savedEntity.getId(), savedEntity.getVersion());

        return workflowDefinitionMapper.toDto(savedEntity);
    }

    /**
     * Update an existing workflow definition
     */
    @CacheEvict(value = "workflows", key = "#dto.tenantId")
    public WorkflowDefinitionDto updateWorkflow(UUID id, WorkflowDefinitionDto dto) {
        logger.info("Updating workflow definition: {}", id);

        WorkflowDefinition existingEntity = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + id));

        // Validate tenant access
        if (!existingEntity.getTenantId().equals(dto.getTenantId())) {
            throw new WorkflowValidationException("Cannot update workflow from different tenant");
        }

        // Validate workflow JSON if provided
        if (dto.getWorkflowJson() != null) {
            workflowValidationService.validateWorkflowDefinition(dto.getWorkflowJson());
        }

        // Update entity
        workflowDefinitionMapper.updateEntity(dto, existingEntity);
        WorkflowDefinition savedEntity = workflowDefinitionRepository.save(existingEntity);

        logger.info("Updated workflow definition: {}", savedEntity.getId());

        return workflowDefinitionMapper.toDto(savedEntity);
    }

    /**
     * Get workflow definition by ID
     */
    @Cacheable(value = "workflows", key = "#tenantId + '_' + #id")
    @Transactional(readOnly = true)
    public WorkflowDefinitionDto getWorkflow(UUID tenantId, UUID id) {
        logger.debug("Getting workflow definition: {} for tenant: {}", id, tenantId);

        WorkflowDefinition entity = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + id));

        // Validate tenant access
        if (!entity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow not found with ID: " + id);
        }

        return workflowDefinitionMapper.toDto(entity);
    }

    /**
     * Get all workflow definitions for a tenant
     */
    @Transactional(readOnly = true)
    public Page<WorkflowDefinitionDto> getWorkflows(UUID tenantId, Pageable pageable) {
        logger.debug("Getting workflow definitions for tenant: {}", tenantId);

        Page<WorkflowDefinition> entities = workflowDefinitionRepository.findByTenantId(tenantId, pageable);
        return entities.map(workflowDefinitionMapper::toDto);
    }

    /**
     * Get active workflow definitions for a tenant
     */
    @Transactional(readOnly = true)
    public Page<WorkflowDefinitionDto> getActiveWorkflows(UUID tenantId, Pageable pageable) {
        logger.debug("Getting active workflow definitions for tenant: {}", tenantId);

        Page<WorkflowDefinition> entities = workflowDefinitionRepository
                .findByTenantIdAndIsActiveTrue(tenantId, pageable);
        return entities.map(workflowDefinitionMapper::toDto);
    }

    /**
     * Get published workflow definitions for a tenant
     */
    @Transactional(readOnly = true)
    public Page<WorkflowDefinitionDto> getPublishedWorkflows(UUID tenantId, Pageable pageable) {
        logger.debug("Getting published workflow definitions for tenant: {}", tenantId);

        Page<WorkflowDefinition> entities = workflowDefinitionRepository
                .findByTenantIdAndIsPublishedTrue(tenantId, pageable);
        return entities.map(workflowDefinitionMapper::toDto);
    }

    /**
     * Get workflow definitions by category
     */
    @Transactional(readOnly = true)
    public Page<WorkflowDefinitionDto> getWorkflowsByCategory(UUID tenantId, String category, Pageable pageable) {
        logger.debug("Getting workflow definitions by category: {} for tenant: {}", category, tenantId);

        Page<WorkflowDefinition> entities = workflowDefinitionRepository
                .findByTenantIdAndCategory(tenantId, category, pageable);
        return entities.map(workflowDefinitionMapper::toDto);
    }

    /**
     * Search workflow definitions by name
     */
    @Transactional(readOnly = true)
    public Page<WorkflowDefinitionDto> searchWorkflows(UUID tenantId, String name, Pageable pageable) {
        logger.debug("Searching workflow definitions by name: {} for tenant: {}", name, tenantId);

        Page<WorkflowDefinition> entities = workflowDefinitionRepository
                .findByTenantIdAndNameContainingIgnoreCase(tenantId, name, pageable);
        return entities.map(workflowDefinitionMapper::toDto);
    }

    /**
     * Get latest version of workflow by name
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowDefinitionDto> getLatestWorkflowVersion(UUID tenantId, String name) {
        logger.debug("Getting latest version of workflow: {} for tenant: {}", name, tenantId);

        List<WorkflowDefinition> versions = workflowDefinitionRepository
                .findLatestVersionByTenantIdAndName(tenantId, name, Pageable.ofSize(1));

        return versions.isEmpty() ? Optional.empty() : 
               Optional.of(workflowDefinitionMapper.toDto(versions.get(0)));
    }

    /**
     * Get all versions of workflow by name
     */
    @Transactional(readOnly = true)
    public List<WorkflowDefinitionDto> getWorkflowVersions(UUID tenantId, String name) {
        logger.debug("Getting all versions of workflow: {} for tenant: {}", name, tenantId);

        List<WorkflowDefinition> entities = workflowDefinitionRepository
                .findByTenantIdAndNameOrderByVersionDesc(tenantId, name);
        return workflowDefinitionMapper.toDtoList(entities);
    }

    /**
     * Publish workflow definition
     */
    @CacheEvict(value = "workflows", key = "#tenantId")
    public WorkflowDefinitionDto publishWorkflow(UUID tenantId, UUID id) {
        logger.info("Publishing workflow definition: {} for tenant: {}", id, tenantId);

        WorkflowDefinition entity = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + id));

        // Validate tenant access
        if (!entity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow not found with ID: " + id);
        }

        // Validate workflow before publishing
        workflowValidationService.validateWorkflowDefinition(entity.getWorkflowJson());

        entity.setIsPublished(true);
        entity.setIsActive(true);
        WorkflowDefinition savedEntity = workflowDefinitionRepository.save(entity);

        logger.info("Published workflow definition: {}", savedEntity.getId());

        return workflowDefinitionMapper.toDto(savedEntity);
    }

    /**
     * Unpublish workflow definition
     */
    @CacheEvict(value = "workflows", key = "#tenantId")
    public WorkflowDefinitionDto unpublishWorkflow(UUID tenantId, UUID id) {
        logger.info("Unpublishing workflow definition: {} for tenant: {}", id, tenantId);

        WorkflowDefinition entity = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + id));

        // Validate tenant access
        if (!entity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow not found with ID: " + id);
        }

        entity.setIsPublished(false);
        WorkflowDefinition savedEntity = workflowDefinitionRepository.save(entity);

        logger.info("Unpublished workflow definition: {}", savedEntity.getId());

        return workflowDefinitionMapper.toDto(savedEntity);
    }

    /**
     * Activate/deactivate workflow definition
     */
    @CacheEvict(value = "workflows", key = "#tenantId")
    public WorkflowDefinitionDto toggleWorkflowStatus(UUID tenantId, UUID id, boolean isActive) {
        logger.info("Toggling workflow definition status: {} to {} for tenant: {}", id, isActive, tenantId);

        WorkflowDefinition entity = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + id));

        // Validate tenant access
        if (!entity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow not found with ID: " + id);
        }

        entity.setIsActive(isActive);
        WorkflowDefinition savedEntity = workflowDefinitionRepository.save(entity);

        logger.info("Toggled workflow definition status: {} to {}", savedEntity.getId(), isActive);

        return workflowDefinitionMapper.toDto(savedEntity);
    }

    /**
     * Delete workflow definition
     */
    @CacheEvict(value = "workflows", key = "#tenantId")
    public void deleteWorkflow(UUID tenantId, UUID id) {
        logger.info("Deleting workflow definition: {} for tenant: {}", id, tenantId);

        WorkflowDefinition entity = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + id));

        // Validate tenant access
        if (!entity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow not found with ID: " + id);
        }

        // Check if workflow has active executions
        // This would be implemented based on business requirements

        workflowDefinitionRepository.delete(entity);

        logger.info("Deleted workflow definition: {}", id);
    }

    /**
     * Clone workflow definition
     */
    @CacheEvict(value = "workflows", key = "#tenantId")
    public WorkflowDefinitionDto cloneWorkflow(UUID tenantId, UUID id, String newName) {
        logger.info("Cloning workflow definition: {} to {} for tenant: {}", id, newName, tenantId);

        WorkflowDefinition originalEntity = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + id));

        // Validate tenant access
        if (!originalEntity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Workflow not found with ID: " + id);
        }

        // Create new workflow definition
        WorkflowDefinition clonedEntity = new WorkflowDefinition();
        clonedEntity.setTenantId(originalEntity.getTenantId());
        clonedEntity.setName(newName);
        clonedEntity.setDescription("Copy of " + originalEntity.getDescription());
        clonedEntity.setCategory(originalEntity.getCategory());
        clonedEntity.setVersion(1);
        clonedEntity.setIsActive(false);
        clonedEntity.setIsPublished(false);
        clonedEntity.setWorkflowJson(originalEntity.getWorkflowJson());
        clonedEntity.setTriggerConfig(originalEntity.getTriggerConfig());
        clonedEntity.setVariablesSchema(originalEntity.getVariablesSchema());
        clonedEntity.setCreatedBy(originalEntity.getCreatedBy());
        clonedEntity.setUpdatedBy(originalEntity.getUpdatedBy());

        WorkflowDefinition savedEntity = workflowDefinitionRepository.save(clonedEntity);

        logger.info("Cloned workflow definition: {} to {}", id, savedEntity.getId());

        return workflowDefinitionMapper.toDto(savedEntity);
    }

    /**
     * Get workflow statistics
     */
    @Transactional(readOnly = true)
    public WorkflowStatistics getWorkflowStatistics(UUID tenantId) {
        logger.debug("Getting workflow statistics for tenant: {}", tenantId);

        long totalWorkflows = workflowDefinitionRepository.countByTenantId(tenantId);
        long activeWorkflows = workflowDefinitionRepository.countByTenantIdAndIsActiveTrue(tenantId);
        long publishedWorkflows = workflowDefinitionRepository.countByTenantIdAndIsPublishedTrue(tenantId);

        return new WorkflowStatistics(totalWorkflows, activeWorkflows, publishedWorkflows);
    }

    /**
     * Inner class for workflow statistics
     */
    public static class WorkflowStatistics {
        private final long totalWorkflows;
        private final long activeWorkflows;
        private final long publishedWorkflows;

        public WorkflowStatistics(long totalWorkflows, long activeWorkflows, long publishedWorkflows) {
            this.totalWorkflows = totalWorkflows;
            this.activeWorkflows = activeWorkflows;
            this.publishedWorkflows = publishedWorkflows;
        }

        public long getTotalWorkflows() { return totalWorkflows; }
        public long getActiveWorkflows() { return activeWorkflows; }
        public long getPublishedWorkflows() { return publishedWorkflows; }
    }
}