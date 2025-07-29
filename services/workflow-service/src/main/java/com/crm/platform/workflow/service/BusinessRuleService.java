package com.crm.platform.workflow.service;

import com.crm.platform.workflow.dto.BusinessRuleDto;
import com.crm.platform.workflow.entity.BusinessRule;
import com.crm.platform.workflow.entity.RuleExecution;
import com.crm.platform.workflow.exception.WorkflowNotFoundException;
import com.crm.platform.workflow.exception.WorkflowValidationException;
import com.crm.platform.workflow.mapper.BusinessRuleMapper;
import com.crm.platform.workflow.repository.BusinessRuleRepository;
import com.crm.platform.workflow.repository.RuleExecutionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing business rules
 */
@Service
@Transactional
public class BusinessRuleService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessRuleService.class);

    private final BusinessRuleRepository businessRuleRepository;
    private final RuleExecutionRepository ruleExecutionRepository;
    private final BusinessRuleMapper businessRuleMapper;
    private final WorkflowValidationService workflowValidationService;
    private final BusinessRuleEngineService businessRuleEngineService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public BusinessRuleService(BusinessRuleRepository businessRuleRepository,
                             RuleExecutionRepository ruleExecutionRepository,
                             BusinessRuleMapper businessRuleMapper,
                             WorkflowValidationService workflowValidationService,
                             BusinessRuleEngineService businessRuleEngineService,
                             KafkaTemplate<String, Object> kafkaTemplate) {
        this.businessRuleRepository = businessRuleRepository;
        this.ruleExecutionRepository = ruleExecutionRepository;
        this.businessRuleMapper = businessRuleMapper;
        this.workflowValidationService = workflowValidationService;
        this.businessRuleEngineService = businessRuleEngineService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Create a new business rule
     */
    @CacheEvict(value = "business-rules", key = "#dto.tenantId")
    public BusinessRuleDto createBusinessRule(BusinessRuleDto dto) {
        logger.info("Creating business rule: {} for tenant: {}", dto.getName(), dto.getTenantId());

        // Validate rule conditions and actions
        workflowValidationService.validateBusinessRule(dto.getConditions(), dto.getActions());

        // Check if rule with same name already exists
        if (businessRuleRepository.existsByTenantIdAndName(dto.getTenantId(), dto.getName())) {
            throw new WorkflowValidationException("Business rule with name '" + dto.getName() + "' already exists");
        }

        BusinessRule entity = businessRuleMapper.toEntity(dto);
        BusinessRule savedEntity = businessRuleRepository.save(entity);

        logger.info("Created business rule with ID: {}", savedEntity.getId());

        return businessRuleMapper.toDto(savedEntity);
    }

    /**
     * Update an existing business rule
     */
    @CacheEvict(value = "business-rules", key = "#dto.tenantId")
    public BusinessRuleDto updateBusinessRule(UUID id, BusinessRuleDto dto) {
        logger.info("Updating business rule: {}", id);

        BusinessRule existingEntity = businessRuleRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Business rule not found with ID: " + id));

        // Validate tenant access
        if (!existingEntity.getTenantId().equals(dto.getTenantId())) {
            throw new WorkflowValidationException("Cannot update business rule from different tenant");
        }

        // Validate rule conditions and actions if provided
        if (dto.getConditions() != null && dto.getActions() != null) {
            workflowValidationService.validateBusinessRule(dto.getConditions(), dto.getActions());
        }

        // Update entity
        businessRuleMapper.updateEntity(dto, existingEntity);
        BusinessRule savedEntity = businessRuleRepository.save(existingEntity);

        logger.info("Updated business rule: {}", savedEntity.getId());

        return businessRuleMapper.toDto(savedEntity);
    }

    /**
     * Get business rule by ID
     */
    @Cacheable(value = "business-rules", key = "#tenantId + '_' + #id")
    @Transactional(readOnly = true)
    public BusinessRuleDto getBusinessRule(UUID tenantId, UUID id) {
        logger.debug("Getting business rule: {} for tenant: {}", id, tenantId);

        BusinessRule entity = businessRuleRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Business rule not found with ID: " + id));

        // Validate tenant access
        if (!entity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Business rule not found with ID: " + id);
        }

        return businessRuleMapper.toDto(entity);
    }

    /**
     * Get all business rules for a tenant
     */
    @Transactional(readOnly = true)
    public Page<BusinessRuleDto> getBusinessRules(UUID tenantId, Pageable pageable) {
        logger.debug("Getting business rules for tenant: {}", tenantId);

        Page<BusinessRule> entities = businessRuleRepository.findByTenantId(tenantId, pageable);
        return entities.map(businessRuleMapper::toDto);
    }

    /**
     * Get active business rules for a tenant
     */
    @Transactional(readOnly = true)
    public Page<BusinessRuleDto> getActiveBusinessRules(UUID tenantId, Pageable pageable) {
        logger.debug("Getting active business rules for tenant: {}", tenantId);

        Page<BusinessRule> entities = businessRuleRepository.findByTenantIdAndIsActiveTrue(tenantId, pageable);
        return entities.map(businessRuleMapper::toDto);
    }

    /**
     * Get business rules by rule type
     */
    @Transactional(readOnly = true)
    public Page<BusinessRuleDto> getBusinessRulesByType(UUID tenantId, BusinessRule.RuleType ruleType, Pageable pageable) {
        logger.debug("Getting business rules by type: {} for tenant: {}", ruleType, tenantId);

        Page<BusinessRule> entities = businessRuleRepository.findByTenantIdAndRuleType(tenantId, ruleType, pageable);
        return entities.map(businessRuleMapper::toDto);
    }

    /**
     * Get business rules by entity type
     */
    @Transactional(readOnly = true)
    public Page<BusinessRuleDto> getBusinessRulesByEntityType(UUID tenantId, String entityType, Pageable pageable) {
        logger.debug("Getting business rules by entity type: {} for tenant: {}", entityType, tenantId);

        Page<BusinessRule> entities = businessRuleRepository.findByTenantIdAndEntityType(tenantId, entityType, pageable);
        return entities.map(businessRuleMapper::toDto);
    }

    /**
     * Search business rules by name
     */
    @Transactional(readOnly = true)
    public Page<BusinessRuleDto> searchBusinessRules(UUID tenantId, String name, Pageable pageable) {
        logger.debug("Searching business rules by name: {} for tenant: {}", name, tenantId);

        Page<BusinessRule> entities = businessRuleRepository
                .findByTenantIdAndNameContainingIgnoreCase(tenantId, name, pageable);
        return entities.map(businessRuleMapper::toDto);
    }

    /**
     * Execute business rules for entity
     */
    public void executeBusinessRules(UUID tenantId, String entityType, UUID entityId, 
                                   String triggerEvent, JsonNode entityData) {
        logger.debug("Executing business rules for entity: {} type: {} tenant: {}", entityId, entityType, tenantId);

        // Get active business rules for entity type
        List<BusinessRule> rules = businessRuleRepository
                .findByTenantIdAndEntityTypeAndIsActiveTrueOrderByPriorityDesc(tenantId, entityType);

        for (BusinessRule rule : rules) {
            try {
                // Execute rule
                RuleExecution execution = new RuleExecution(tenantId, rule, entityId, entityType);
                execution.setTriggerEvent(triggerEvent);
                execution.setInputData(entityData);

                long startTime = System.currentTimeMillis();
                
                boolean ruleMatched = businessRuleEngineService.evaluateRule(rule, entityData);
                
                if (ruleMatched) {
                    JsonNode result = businessRuleEngineService.executeRuleActions(rule, entityData);
                    execution.markAsCompleted(result, System.currentTimeMillis() - startTime);
                    
                    logger.debug("Executed business rule: {} for entity: {}", rule.getId(), entityId);
                } else {
                    execution.markAsSkipped();
                    logger.debug("Skipped business rule: {} for entity: {} (conditions not met)", rule.getId(), entityId);
                }

                ruleExecutionRepository.save(execution);

                // Publish rule execution event
                publishRuleExecutionEvent(execution);

            } catch (Exception e) {
                logger.error("Error executing business rule: {} for entity: {}", rule.getId(), entityId, e);
                
                RuleExecution execution = new RuleExecution(tenantId, rule, entityId, entityType);
                execution.setTriggerEvent(triggerEvent);
                execution.setInputData(entityData);
                execution.markAsFailed(e.getMessage(), null, System.currentTimeMillis());
                
                ruleExecutionRepository.save(execution);
            }
        }
    }

    /**
     * Test business rule with sample data
     */
    @Transactional(readOnly = true)
    public BusinessRuleTestResult testBusinessRule(UUID tenantId, UUID ruleId, JsonNode testData) {
        logger.debug("Testing business rule: {} for tenant: {}", ruleId, tenantId);

        BusinessRule rule = businessRuleRepository.findById(ruleId)
                .orElseThrow(() -> new WorkflowNotFoundException("Business rule not found with ID: " + ruleId));

        // Validate tenant access
        if (!rule.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Business rule not found with ID: " + ruleId);
        }

        try {
            long startTime = System.currentTimeMillis();
            
            boolean conditionsMet = businessRuleEngineService.evaluateRule(rule, testData);
            JsonNode result = null;
            
            if (conditionsMet) {
                result = businessRuleEngineService.executeRuleActions(rule, testData);
            }
            
            long duration = System.currentTimeMillis() - startTime;

            return new BusinessRuleTestResult(true, conditionsMet, result, duration, null);

        } catch (Exception e) {
            logger.error("Error testing business rule: {}", ruleId, e);
            return new BusinessRuleTestResult(false, false, null, 0, e.getMessage());
        }
    }

    /**
     * Activate/deactivate business rule
     */
    @CacheEvict(value = "business-rules", key = "#tenantId")
    public BusinessRuleDto toggleBusinessRuleStatus(UUID tenantId, UUID id, boolean isActive) {
        logger.info("Toggling business rule status: {} to {} for tenant: {}", id, isActive, tenantId);

        BusinessRule entity = businessRuleRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Business rule not found with ID: " + id));

        // Validate tenant access
        if (!entity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Business rule not found with ID: " + id);
        }

        entity.setIsActive(isActive);
        BusinessRule savedEntity = businessRuleRepository.save(entity);

        logger.info("Toggled business rule status: {} to {}", savedEntity.getId(), isActive);

        return businessRuleMapper.toDto(savedEntity);
    }

    /**
     * Delete business rule
     */
    @CacheEvict(value = "business-rules", key = "#tenantId")
    public void deleteBusinessRule(UUID tenantId, UUID id) {
        logger.info("Deleting business rule: {} for tenant: {}", id, tenantId);

        BusinessRule entity = businessRuleRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Business rule not found with ID: " + id));

        // Validate tenant access
        if (!entity.getTenantId().equals(tenantId)) {
            throw new WorkflowNotFoundException("Business rule not found with ID: " + id);
        }

        businessRuleRepository.delete(entity);

        logger.info("Deleted business rule: {}", id);
    }

    /**
     * Get business rule statistics
     */
    @Transactional(readOnly = true)
    public BusinessRuleStatistics getBusinessRuleStatistics(UUID tenantId) {
        logger.debug("Getting business rule statistics for tenant: {}", tenantId);

        long totalRules = businessRuleRepository.countByTenantId(tenantId);
        long activeRules = businessRuleRepository.countByTenantIdAndIsActiveTrue(tenantId);

        List<Object[]> ruleTypeStats = businessRuleRepository.getRuleStatsByTenantId(tenantId);
        List<Object[]> entityTypeStats = businessRuleRepository.getRuleStatsByEntityType(tenantId);

        return new BusinessRuleStatistics(totalRules, activeRules, ruleTypeStats, entityTypeStats);
    }

    /**
     * Publish rule execution event to Kafka
     */
    private void publishRuleExecutionEvent(RuleExecution execution) {
        try {
            kafkaTemplate.send("rule-execution-events", "rule.executed", execution);
        } catch (Exception e) {
            logger.error("Error publishing rule execution event for execution: {}", execution.getId(), e);
        }
    }

    /**
     * Inner class for business rule test result
     */
    public static class BusinessRuleTestResult {
        private final boolean success;
        private final boolean conditionsMet;
        private final JsonNode result;
        private final long executionTimeMs;
        private final String errorMessage;

        public BusinessRuleTestResult(boolean success, boolean conditionsMet, JsonNode result, 
                                    long executionTimeMs, String errorMessage) {
            this.success = success;
            this.conditionsMet = conditionsMet;
            this.result = result;
            this.executionTimeMs = executionTimeMs;
            this.errorMessage = errorMessage;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public boolean isConditionsMet() { return conditionsMet; }
        public JsonNode getResult() { return result; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Inner class for business rule statistics
     */
    public static class BusinessRuleStatistics {
        private final long totalRules;
        private final long activeRules;
        private final List<Object[]> ruleTypeStats;
        private final List<Object[]> entityTypeStats;

        public BusinessRuleStatistics(long totalRules, long activeRules, 
                                    List<Object[]> ruleTypeStats, List<Object[]> entityTypeStats) {
            this.totalRules = totalRules;
            this.activeRules = activeRules;
            this.ruleTypeStats = ruleTypeStats;
            this.entityTypeStats = entityTypeStats;
        }

        // Getters
        public long getTotalRules() { return totalRules; }
        public long getActiveRules() { return activeRules; }
        public List<Object[]> getRuleTypeStats() { return ruleTypeStats; }
        public List<Object[]> getEntityTypeStats() { return entityTypeStats; }
    }
}