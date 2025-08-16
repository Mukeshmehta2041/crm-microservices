package com.crm.platform.pipelines.service;

import com.crm.platform.pipelines.entity.Pipeline;
import com.crm.platform.pipelines.entity.PipelineStage;
import com.crm.platform.pipelines.entity.AutomationRule;
import com.crm.platform.pipelines.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PipelineEventService {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEventService.class);

    private static final String PIPELINE_TOPIC = "pipeline-events";
    private static final String STAGE_TOPIC = "pipeline-stage-events";
    private static final String AUTOMATION_TOPIC = "automation-rule-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public PipelineEventService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish pipeline created event
     */
    public void publishPipelineCreatedEvent(Pipeline pipeline) {
        try {
            PipelineCreatedEvent event = new PipelineCreatedEvent(
                    pipeline.getId(),
                    pipeline.getTenantId(),
                    pipeline.getName(),
                    pipeline.getIsDefault(),
                    pipeline.getCreatedBy()
            );

            kafkaTemplate.send(PIPELINE_TOPIC, pipeline.getId().toString(), event);
            logger.info("Published pipeline created event for pipeline: {}", pipeline.getId());
        } catch (Exception e) {
            logger.error("Failed to publish pipeline created event for pipeline: {}", pipeline.getId(), e);
        }
    }

    /**
     * Publish pipeline updated event
     */
    public void publishPipelineUpdatedEvent(Pipeline pipeline) {
        try {
            PipelineUpdatedEvent event = new PipelineUpdatedEvent(
                    pipeline.getId(),
                    pipeline.getTenantId(),
                    pipeline.getName(),
                    pipeline.getIsActive(),
                    pipeline.getIsDefault(),
                    pipeline.getUpdatedBy()
            );

            kafkaTemplate.send(PIPELINE_TOPIC, pipeline.getId().toString(), event);
            logger.info("Published pipeline updated event for pipeline: {}", pipeline.getId());
        } catch (Exception e) {
            logger.error("Failed to publish pipeline updated event for pipeline: {}", pipeline.getId(), e);
        }
    }

    /**
     * Publish pipeline deleted event
     */
    public void publishPipelineDeletedEvent(Pipeline pipeline) {
        try {
            PipelineDeletedEvent event = new PipelineDeletedEvent(
                    pipeline.getId(),
                    pipeline.getTenantId(),
                    pipeline.getName()
            );

            kafkaTemplate.send(PIPELINE_TOPIC, pipeline.getId().toString(), event);
            logger.info("Published pipeline deleted event for pipeline: {}", pipeline.getId());
        } catch (Exception e) {
            logger.error("Failed to publish pipeline deleted event for pipeline: {}", pipeline.getId(), e);
        }
    }

    /**
     * Publish pipeline cloned event
     */
    public void publishPipelineClonedEvent(Pipeline clonedPipeline, Pipeline templatePipeline) {
        try {
            PipelineClonedEvent event = new PipelineClonedEvent(
                    clonedPipeline.getId(),
                    clonedPipeline.getTenantId(),
                    clonedPipeline.getName(),
                    templatePipeline.getId(),
                    templatePipeline.getName(),
                    clonedPipeline.getCreatedBy()
            );

            kafkaTemplate.send(PIPELINE_TOPIC, clonedPipeline.getId().toString(), event);
            logger.info("Published pipeline cloned event for pipeline: {} from template: {}", 
                       clonedPipeline.getId(), templatePipeline.getId());
        } catch (Exception e) {
            logger.error("Failed to publish pipeline cloned event for pipeline: {}", clonedPipeline.getId(), e);
        }
    }

    /**
     * Publish pipelines reordered event
     */
    public void publishPipelinesReorderedEvent(UUID tenantId, List<UUID> pipelineIds) {
        try {
            PipelinesReorderedEvent event = new PipelinesReorderedEvent(
                    tenantId,
                    pipelineIds
            );

            kafkaTemplate.send(PIPELINE_TOPIC, tenantId.toString(), event);
            logger.info("Published pipelines reordered event for tenant: {}", tenantId);
        } catch (Exception e) {
            logger.error("Failed to publish pipelines reordered event for tenant: {}", tenantId, e);
        }
    }

    /**
     * Publish stage created event
     */
    public void publishStageCreatedEvent(PipelineStage stage) {
        try {
            StageCreatedEvent event = new StageCreatedEvent(
                    stage.getId(),
                    stage.getPipeline().getId(),
                    stage.getTenantId(),
                    stage.getName(),
                    stage.getDisplayOrder(),
                    stage.getCreatedBy()
            );

            kafkaTemplate.send(STAGE_TOPIC, stage.getId().toString(), event);
            logger.info("Published stage created event for stage: {}", stage.getId());
        } catch (Exception e) {
            logger.error("Failed to publish stage created event for stage: {}", stage.getId(), e);
        }
    }

    /**
     * Publish stage updated event
     */
    public void publishStageUpdatedEvent(PipelineStage stage) {
        try {
            StageUpdatedEvent event = new StageUpdatedEvent(
                    stage.getId(),
                    stage.getPipeline().getId(),
                    stage.getTenantId(),
                    stage.getName(),
                    stage.getIsActive(),
                    stage.getIsClosed(),
                    stage.getIsWon(),
                    stage.getUpdatedBy()
            );

            kafkaTemplate.send(STAGE_TOPIC, stage.getId().toString(), event);
            logger.info("Published stage updated event for stage: {}", stage.getId());
        } catch (Exception e) {
            logger.error("Failed to publish stage updated event for stage: {}", stage.getId(), e);
        }
    }

    /**
     * Publish stage deleted event
     */
    public void publishStageDeletedEvent(PipelineStage stage) {
        try {
            StageDeletedEvent event = new StageDeletedEvent(
                    stage.getId(),
                    stage.getPipeline().getId(),
                    stage.getTenantId(),
                    stage.getName()
            );

            kafkaTemplate.send(STAGE_TOPIC, stage.getId().toString(), event);
            logger.info("Published stage deleted event for stage: {}", stage.getId());
        } catch (Exception e) {
            logger.error("Failed to publish stage deleted event for stage: {}", stage.getId(), e);
        }
    }

    /**
     * Publish automation rule created event
     */
    public void publishAutomationRuleCreatedEvent(AutomationRule rule) {
        try {
            AutomationRuleCreatedEvent event = new AutomationRuleCreatedEvent(
                    rule.getId(),
                    rule.getTenantId(),
                    rule.getPipeline() != null ? rule.getPipeline().getId() : null,
                    rule.getStage() != null ? rule.getStage().getId() : null,
                    rule.getName(),
                    rule.getTriggerType(),
                    rule.getCreatedBy()
            );

            kafkaTemplate.send(AUTOMATION_TOPIC, rule.getId().toString(), event);
            logger.info("Published automation rule created event for rule: {}", rule.getId());
        } catch (Exception e) {
            logger.error("Failed to publish automation rule created event for rule: {}", rule.getId(), e);
        }
    }

    /**
     * Publish automation rule executed event
     */
    public void publishAutomationRuleExecutedEvent(AutomationRule rule, boolean success, String errorMessage) {
        try {
            AutomationRuleExecutedEvent event = new AutomationRuleExecutedEvent(
                    rule.getId(),
                    rule.getTenantId(),
                    rule.getName(),
                    rule.getTriggerType(),
                    success,
                    errorMessage,
                    rule.getExecutionCount(),
                    rule.getErrorCount()
            );

            kafkaTemplate.send(AUTOMATION_TOPIC, rule.getId().toString(), event);
            logger.info("Published automation rule executed event for rule: {} success: {}", rule.getId(), success);
        } catch (Exception e) {
            logger.error("Failed to publish automation rule executed event for rule: {}", rule.getId(), e);
        }
    }
}