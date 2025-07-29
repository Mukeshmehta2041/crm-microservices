package com.crm.platform.pipelines.service;

import com.crm.platform.pipelines.dto.*;
import com.crm.platform.pipelines.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PipelineMapper {

    /**
     * Convert PipelineRequest to Pipeline entity
     */
    public Pipeline toEntity(PipelineRequest request, UUID tenantId, UUID userId) {
        Pipeline pipeline = new Pipeline();
        pipeline.setTenantId(tenantId);
        pipeline.setName(request.getName());
        pipeline.setDescription(request.getDescription());
        pipeline.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        pipeline.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        pipeline.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        pipeline.setTemplateId(request.getTemplateId());
        pipeline.setConfiguration(request.getConfiguration());
        pipeline.setCreatedBy(userId);
        pipeline.setUpdatedBy(userId);
        return pipeline;
    }

    /**
     * Update Pipeline entity from PipelineRequest
     */
    public void updateEntity(Pipeline pipeline, PipelineRequest request, UUID userId) {
        pipeline.setName(request.getName());
        pipeline.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            pipeline.setIsActive(request.getIsActive());
        }
        if (request.getIsDefault() != null) {
            pipeline.setIsDefault(request.getIsDefault());
        }
        if (request.getDisplayOrder() != null) {
            pipeline.setDisplayOrder(request.getDisplayOrder());
        }
        pipeline.setTemplateId(request.getTemplateId());
        pipeline.setConfiguration(request.getConfiguration());
        pipeline.setUpdatedBy(userId);
    }

    /**
     * Convert Pipeline entity to PipelineResponse
     */
    public PipelineResponse toResponse(Pipeline pipeline) {
        PipelineResponse response = new PipelineResponse();
        response.setId(pipeline.getId());
        response.setTenantId(pipeline.getTenantId());
        response.setName(pipeline.getName());
        response.setDescription(pipeline.getDescription());
        response.setIsActive(pipeline.getIsActive());
        response.setIsDefault(pipeline.getIsDefault());
        response.setDisplayOrder(pipeline.getDisplayOrder());
        response.setTemplateId(pipeline.getTemplateId());
        response.setConfiguration(pipeline.getConfiguration());
        response.setCreatedAt(pipeline.getCreatedAt());
        response.setUpdatedAt(pipeline.getUpdatedAt());
        response.setCreatedBy(pipeline.getCreatedBy());
        response.setUpdatedBy(pipeline.getUpdatedBy());

        // Map stages if loaded
        if (pipeline.getStages() != null && !pipeline.getStages().isEmpty()) {
            response.setStages(pipeline.getStages().stream()
                    .map(this::toStageResponse)
                    .collect(Collectors.toList()));
        }

        // Map automation rules if loaded
        if (pipeline.getAutomationRules() != null && !pipeline.getAutomationRules().isEmpty()) {
            response.setAutomationRules(pipeline.getAutomationRules().stream()
                    .map(this::toAutomationRuleResponse)
                    .collect(Collectors.toList()));
        }

        // Set metrics
        response.setMetrics(createPipelineMetrics(pipeline));

        return response;
    }

    /**
     * Convert PipelineStageRequest to PipelineStage entity
     */
    public PipelineStage toStageEntity(PipelineStageRequest request, Pipeline pipeline, UUID userId) {
        PipelineStage stage = new PipelineStage();
        stage.setPipeline(pipeline);
        stage.setName(request.getName());
        stage.setDescription(request.getDescription());
        stage.setDefaultProbability(request.getDefaultProbability());
        stage.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        stage.setIsClosed(request.getIsClosed() != null ? request.getIsClosed() : false);
        stage.setIsWon(request.getIsWon() != null ? request.getIsWon() : false);
        stage.setDisplayOrder(request.getDisplayOrder());
        stage.setColor(request.getColor());
        stage.setAutomationRules(request.getAutomationRules());
        stage.setStageConfiguration(request.getStageConfiguration());
        stage.setCreatedBy(userId);
        stage.setUpdatedBy(userId);
        return stage;
    }

    /**
     * Update PipelineStage entity from PipelineStageRequest
     */
    public void updateStageEntity(PipelineStage stage, PipelineStageRequest request, UUID userId) {
        stage.setName(request.getName());
        stage.setDescription(request.getDescription());
        stage.setDefaultProbability(request.getDefaultProbability());
        if (request.getIsActive() != null) {
            stage.setIsActive(request.getIsActive());
        }
        if (request.getIsClosed() != null) {
            stage.setIsClosed(request.getIsClosed());
        }
        if (request.getIsWon() != null) {
            stage.setIsWon(request.getIsWon());
        }
        stage.setDisplayOrder(request.getDisplayOrder());
        stage.setColor(request.getColor());
        stage.setAutomationRules(request.getAutomationRules());
        stage.setStageConfiguration(request.getStageConfiguration());
        stage.setUpdatedBy(userId);
    }

    /**
     * Convert PipelineStage entity to PipelineStageResponse
     */
    public PipelineStageResponse toStageResponse(PipelineStage stage) {
        PipelineStageResponse response = new PipelineStageResponse();
        response.setId(stage.getId());
        response.setPipelineId(stage.getPipeline().getId());
        response.setName(stage.getName());
        response.setDescription(stage.getDescription());
        response.setDefaultProbability(stage.getDefaultProbability());
        response.setIsActive(stage.getIsActive());
        response.setIsClosed(stage.getIsClosed());
        response.setIsWon(stage.getIsWon());
        response.setDisplayOrder(stage.getDisplayOrder());
        response.setColor(stage.getColor());
        response.setAutomationRules(stage.getAutomationRules());
        response.setStageConfiguration(stage.getStageConfiguration());
        response.setCreatedAt(stage.getCreatedAt());
        response.setUpdatedAt(stage.getUpdatedAt());
        response.setCreatedBy(stage.getCreatedBy());
        response.setUpdatedBy(stage.getUpdatedBy());

        // Map stage automation rules if loaded
        if (stage.getStageAutomationRules() != null && !stage.getStageAutomationRules().isEmpty()) {
            response.setStageAutomationRules(stage.getStageAutomationRules().stream()
                    .map(this::toAutomationRuleResponse)
                    .collect(Collectors.toList()));
        }

        // Set metrics
        response.setMetrics(createStageMetrics(stage));

        return response;
    }

    /**
     * Convert AutomationRuleRequest to AutomationRule entity
     */
    public AutomationRule toAutomationRuleEntity(AutomationRuleRequest request, UUID tenantId, UUID userId) {
        AutomationRule rule = new AutomationRule();
        rule.setTenantId(tenantId);
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setTriggerType(request.getTriggerType());
        rule.setTriggerConditions(request.getTriggerConditions());
        rule.setActions(request.getActions());
        rule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        rule.setExecutionOrder(request.getExecutionOrder() != null ? request.getExecutionOrder() : 0);
        rule.setCreatedBy(userId);
        rule.setUpdatedBy(userId);
        return rule;
    }

    /**
     * Update AutomationRule entity from AutomationRuleRequest
     */
    public void updateAutomationRuleEntity(AutomationRule rule, AutomationRuleRequest request, UUID userId) {
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setTriggerType(request.getTriggerType());
        rule.setTriggerConditions(request.getTriggerConditions());
        rule.setActions(request.getActions());
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }
        if (request.getExecutionOrder() != null) {
            rule.setExecutionOrder(request.getExecutionOrder());
        }
        rule.setUpdatedBy(userId);
    }

    /**
     * Convert AutomationRule entity to AutomationRuleResponse
     */
    public AutomationRuleResponse toAutomationRuleResponse(AutomationRule rule) {
        AutomationRuleResponse response = new AutomationRuleResponse();
        response.setId(rule.getId());
        response.setTenantId(rule.getTenantId());
        response.setPipelineId(rule.getPipeline() != null ? rule.getPipeline().getId() : null);
        response.setStageId(rule.getStage() != null ? rule.getStage().getId() : null);
        response.setName(rule.getName());
        response.setDescription(rule.getDescription());
        response.setTriggerType(rule.getTriggerType());
        response.setTriggerConditions(rule.getTriggerConditions());
        response.setActions(rule.getActions());
        response.setIsActive(rule.getIsActive());
        response.setExecutionOrder(rule.getExecutionOrder());
        response.setLastExecutedAt(rule.getLastExecutedAt());
        response.setExecutionCount(rule.getExecutionCount());
        response.setErrorCount(rule.getErrorCount());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        response.setCreatedBy(rule.getCreatedBy());
        response.setUpdatedBy(rule.getUpdatedBy());

        // Set metrics
        response.setMetrics(createRuleMetrics(rule));

        return response;
    }

    /**
     * Convert PipelineAnalytics entity to PipelineAnalyticsResponse
     */
    public PipelineAnalyticsResponse toAnalyticsResponse(PipelineAnalytics analytics) {
        PipelineAnalyticsResponse response = new PipelineAnalyticsResponse();
        response.setId(analytics.getId());
        response.setTenantId(analytics.getTenantId());
        response.setPipelineId(analytics.getPipeline().getId());
        response.setStageId(analytics.getStage() != null ? analytics.getStage().getId() : null);
        response.setMetricType(analytics.getMetricType());
        response.setMetricName(analytics.getMetricName());
        response.setMetricValue(analytics.getMetricValue());
        response.setMetricUnit(analytics.getMetricUnit());
        response.setPeriodStart(analytics.getPeriodStart());
        response.setPeriodEnd(analytics.getPeriodEnd());
        response.setCalculatedAt(analytics.getCalculatedAt());
        response.setMetadata(analytics.getMetadata());
        return response;
    }

    /**
     * Clone pipeline from template
     */
    public Pipeline cloneFromTemplate(Pipeline template, String newName, UUID userId) {
        Pipeline cloned = new Pipeline();
        cloned.setTenantId(template.getTenantId());
        cloned.setName(newName);
        cloned.setDescription(template.getDescription());
        cloned.setIsActive(true);
        cloned.setIsDefault(false);
        cloned.setDisplayOrder(0); // Will be set appropriately
        cloned.setTemplateId(template.getId());
        cloned.setConfiguration(template.getConfiguration());
        cloned.setCreatedBy(userId);
        cloned.setUpdatedBy(userId);
        return cloned;
    }

    /**
     * Clone stage from template
     */
    public PipelineStage cloneStage(PipelineStage templateStage, Pipeline newPipeline, UUID userId) {
        PipelineStage cloned = new PipelineStage();
        cloned.setPipeline(newPipeline);
        cloned.setName(templateStage.getName());
        cloned.setDescription(templateStage.getDescription());
        cloned.setDefaultProbability(templateStage.getDefaultProbability());
        cloned.setIsActive(templateStage.getIsActive());
        cloned.setIsClosed(templateStage.getIsClosed());
        cloned.setIsWon(templateStage.getIsWon());
        cloned.setDisplayOrder(templateStage.getDisplayOrder());
        cloned.setColor(templateStage.getColor());
        cloned.setAutomationRules(templateStage.getAutomationRules());
        cloned.setStageConfiguration(templateStage.getStageConfiguration());
        cloned.setCreatedBy(userId);
        cloned.setUpdatedBy(userId);
        return cloned;
    }

    // Private helper methods

    private PipelineResponse.PipelineMetrics createPipelineMetrics(Pipeline pipeline) {
        PipelineResponse.PipelineMetrics metrics = new PipelineResponse.PipelineMetrics();
        
        if (pipeline.getStages() != null) {
            metrics.setStageCount(pipeline.getStages().size());
            metrics.setActiveStageCount((int) pipeline.getStages().stream()
                    .filter(stage -> Boolean.TRUE.equals(stage.getIsActive()))
                    .count());
            metrics.setClosedStageCount((int) pipeline.getStages().stream()
                    .filter(stage -> Boolean.TRUE.equals(stage.getIsClosed()))
                    .count());
        }

        if (pipeline.getAutomationRules() != null) {
            metrics.setAutomationRuleCount(pipeline.getAutomationRules().size());
        }

        return metrics;
    }

    private PipelineStageResponse.StageMetrics createStageMetrics(PipelineStage stage) {
        PipelineStageResponse.StageMetrics metrics = new PipelineStageResponse.StageMetrics();
        
        if (stage.getStageAutomationRules() != null) {
            metrics.setAutomationRuleCount(stage.getStageAutomationRules().size());
        }

        // Additional metrics would be calculated from analytics data
        // This is a placeholder for now
        
        return metrics;
    }

    private AutomationRuleResponse.RuleMetrics createRuleMetrics(AutomationRule rule) {
        AutomationRuleResponse.RuleMetrics metrics = new AutomationRuleResponse.RuleMetrics();
        
        if (rule.getExecutionCount() > 0) {
            double successRate = ((double) (rule.getExecutionCount() - rule.getErrorCount()) / rule.getExecutionCount()) * 100;
            double errorRate = ((double) rule.getErrorCount() / rule.getExecutionCount()) * 100;
            
            metrics.setSuccessRate(successRate);
            metrics.setErrorRate(errorRate);
        }

        metrics.setLastSuccessfulExecution(rule.getLastExecutedAt());
        
        return metrics;
    }
}