package com.crm.platform.workflow.service;

import com.crm.platform.workflow.entity.WorkflowExecution;
import com.crm.platform.workflow.entity.WorkflowStepExecution;
import com.crm.platform.workflow.exception.WorkflowExecutionException;
import com.crm.platform.workflow.repository.WorkflowStepExecutionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for workflow engine operations using Flowable
 */
@Service
@Transactional
public class WorkflowEngineService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngineService.class);

    private final ProcessEngine processEngine;
    private final RuntimeService runtimeService;
    private final WorkflowStepExecutionRepository stepExecutionRepository;
    private final WorkflowExecutionService workflowExecutionService;
    private final ObjectMapper objectMapper;

    @Autowired
    public WorkflowEngineService(ProcessEngine processEngine,
                               WorkflowStepExecutionRepository stepExecutionRepository,
                               WorkflowExecutionService workflowExecutionService,
                               ObjectMapper objectMapper) {
        this.processEngine = processEngine;
        this.runtimeService = processEngine.getRuntimeService();
        this.stepExecutionRepository = stepExecutionRepository;
        this.workflowExecutionService = workflowExecutionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute workflow using Flowable engine
     */
    public void executeWorkflow(WorkflowExecution execution) {
        logger.info("Executing workflow: {} definition: {}", 
                   execution.getId(), execution.getWorkflowDefinition().getId());

        try {
            // Update execution status to running
            execution.setStatus(WorkflowExecution.ExecutionStatus.RUNNING);
            execution.setStartedAt(LocalDateTime.now());

            // Prepare process variables
            Map<String, Object> processVariables = prepareProcessVariables(execution);

            // Deploy and start process
            String processDefinitionKey = deployWorkflowDefinition(execution);
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                processDefinitionKey, 
                execution.getId().toString(),
                processVariables
            );

            logger.info("Started process instance: {} for workflow execution: {}", 
                       processInstance.getId(), execution.getId());

            // Monitor process execution
            monitorProcessExecution(execution, processInstance);

        } catch (Exception e) {
            logger.error("Error executing workflow: {}", execution.getId(), e);
            workflowExecutionService.markExecutionAsFailed(execution, e.getMessage());
            throw new WorkflowExecutionException("Failed to execute workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel workflow execution
     */
    public void cancelWorkflow(WorkflowExecution execution) {
        logger.info("Cancelling workflow execution: {}", execution.getId());

        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(execution.getId().toString())
                    .singleResult();

            if (processInstance != null && !processInstance.isEnded()) {
                runtimeService.deleteProcessInstance(processInstance.getId(), "Cancelled by user");
                logger.info("Cancelled process instance: {} for workflow execution: {}", 
                           processInstance.getId(), execution.getId());
            }
        } catch (Exception e) {
            logger.error("Error cancelling workflow: {}", execution.getId(), e);
            throw new WorkflowExecutionException("Failed to cancel workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Suspend workflow execution
     */
    public void suspendWorkflow(WorkflowExecution execution) {
        logger.info("Suspending workflow execution: {}", execution.getId());

        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(execution.getId().toString())
                    .singleResult();

            if (processInstance != null && !processInstance.isSuspended()) {
                runtimeService.suspendProcessInstanceById(processInstance.getId());
                logger.info("Suspended process instance: {} for workflow execution: {}", 
                           processInstance.getId(), execution.getId());
            }
        } catch (Exception e) {
            logger.error("Error suspending workflow: {}", execution.getId(), e);
            throw new WorkflowExecutionException("Failed to suspend workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Resume workflow execution
     */
    public void resumeWorkflow(WorkflowExecution execution) {
        logger.info("Resuming workflow execution: {}", execution.getId());

        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(execution.getId().toString())
                    .singleResult();

            if (processInstance != null && processInstance.isSuspended()) {
                runtimeService.activateProcessInstanceById(processInstance.getId());
                logger.info("Resumed process instance: {} for workflow execution: {}", 
                           processInstance.getId(), execution.getId());
            }
        } catch (Exception e) {
            logger.error("Error resuming workflow: {}", execution.getId(), e);
            throw new WorkflowExecutionException("Failed to resume workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Deploy workflow definition to Flowable engine
     */
    private String deployWorkflowDefinition(WorkflowExecution execution) {
        try {
            // Convert workflow JSON to BPMN XML
            String bpmnXml = convertJsonToBpmn(execution.getWorkflowDefinition().getWorkflowJson());
            
            // Generate unique process definition key
            String processDefinitionKey = "workflow_" + execution.getWorkflowDefinition().getId().toString().replace("-", "_");
            
            // Deploy process definition
            processEngine.getRepositoryService()
                    .createDeployment()
                    .name("Workflow_" + execution.getWorkflowDefinition().getName())
                    .addString(processDefinitionKey + ".bpmn20.xml", bpmnXml)
                    .tenantId(execution.getTenantId().toString())
                    .deploy();

            logger.info("Deployed workflow definition: {} as process: {}", 
                       execution.getWorkflowDefinition().getId(), processDefinitionKey);

            return processDefinitionKey;
        } catch (Exception e) {
            logger.error("Error deploying workflow definition: {}", 
                        execution.getWorkflowDefinition().getId(), e);
            throw new WorkflowExecutionException("Failed to deploy workflow definition: " + e.getMessage(), e);
        }
    }

    /**
     * Convert workflow JSON to BPMN XML
     */
    private String convertJsonToBpmn(JsonNode workflowJson) {
        // This is a simplified conversion - in a real implementation,
        // you would have a comprehensive JSON to BPMN converter
        
        StringBuilder bpmnXml = new StringBuilder();
        bpmnXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        bpmnXml.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        bpmnXml.append("xmlns:flowable=\"http://flowable.org/bpmn\" ");
        bpmnXml.append("targetNamespace=\"http://www.crm.platform/workflow\">\n");
        
        String processId = "process_" + UUID.randomUUID().toString().replace("-", "_");
        bpmnXml.append("<process id=\"").append(processId).append("\" isExecutable=\"true\">\n");
        
        // Add start event
        bpmnXml.append("<startEvent id=\"start\" />\n");
        
        // Parse workflow steps from JSON and convert to BPMN tasks
        if (workflowJson.has("steps")) {
            JsonNode steps = workflowJson.get("steps");
            String previousElement = "start";
            
            for (int i = 0; i < steps.size(); i++) {
                JsonNode step = steps.get(i);
                String stepId = step.get("id").asText();
                String stepName = step.get("name").asText();
                String stepType = step.get("type").asText();
                
                // Create service task for each step
                bpmnXml.append("<serviceTask id=\"").append(stepId).append("\" ");
                bpmnXml.append("name=\"").append(stepName).append("\" ");
                bpmnXml.append("flowable:class=\"com.crm.platform.workflow.engine.WorkflowStepDelegate\">\n");
                bpmnXml.append("<extensionElements>\n");
                bpmnXml.append("<flowable:field name=\"stepType\" stringValue=\"").append(stepType).append("\" />\n");
                bpmnXml.append("<flowable:field name=\"stepConfig\" expression=\"${stepConfig_").append(stepId).append("}\" />\n");
                bpmnXml.append("</extensionElements>\n");
                bpmnXml.append("</serviceTask>\n");
                
                // Add sequence flow
                bpmnXml.append("<sequenceFlow id=\"flow_").append(i).append("\" ");
                bpmnXml.append("sourceRef=\"").append(previousElement).append("\" ");
                bpmnXml.append("targetRef=\"").append(stepId).append("\" />\n");
                
                previousElement = stepId;
            }
            
            // Add end event
            bpmnXml.append("<endEvent id=\"end\" />\n");
            bpmnXml.append("<sequenceFlow id=\"flow_end\" ");
            bpmnXml.append("sourceRef=\"").append(previousElement).append("\" ");
            bpmnXml.append("targetRef=\"end\" />\n");
        }
        
        bpmnXml.append("</process>\n");
        bpmnXml.append("</definitions>");
        
        return bpmnXml.toString();
    }

    /**
     * Prepare process variables for execution
     */
    private Map<String, Object> prepareProcessVariables(WorkflowExecution execution) {
        Map<String, Object> variables = new HashMap<>();
        
        // Add execution context
        variables.put("executionId", execution.getId().toString());
        variables.put("tenantId", execution.getTenantId().toString());
        variables.put("workflowDefinitionId", execution.getWorkflowDefinition().getId().toString());
        variables.put("triggerType", execution.getTriggerType());
        
        // Add trigger data
        if (execution.getTriggerData() != null) {
            variables.put("triggerData", execution.getTriggerData().toString());
        }
        
        // Add workflow variables
        if (execution.getVariables() != null) {
            try {
                Map<String, Object> workflowVariables = objectMapper.convertValue(
                    execution.getVariables(), Map.class);
                variables.putAll(workflowVariables);
            } catch (Exception e) {
                logger.warn("Error parsing workflow variables for execution: {}", execution.getId(), e);
            }
        }
        
        // Add step configurations
        JsonNode workflowJson = execution.getWorkflowDefinition().getWorkflowJson();
        if (workflowJson.has("steps")) {
            JsonNode steps = workflowJson.get("steps");
            for (JsonNode step : steps) {
                String stepId = step.get("id").asText();
                variables.put("stepConfig_" + stepId, step.toString());
            }
        }
        
        return variables;
    }

    /**
     * Monitor process execution
     */
    private void monitorProcessExecution(WorkflowExecution execution, ProcessInstance processInstance) {
        // This would typically be handled by Flowable listeners and delegates
        // For now, we'll create a simple monitoring mechanism
        
        try {
            // Check if process is still running
            ProcessInstance currentInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();
            
            if (currentInstance == null) {
                // Process completed
                workflowExecutionService.markExecutionAsCompleted(execution, null);
            }
        } catch (Exception e) {
            logger.error("Error monitoring process execution: {}", execution.getId(), e);
        }
    }

    /**
     * Create step execution record
     */
    public WorkflowStepExecution createStepExecution(UUID executionId, String stepId, 
                                                    String stepName, String stepType) {
        WorkflowExecution execution = workflowExecutionService.getExecutionEntity(executionId);
        
        WorkflowStepExecution stepExecution = new WorkflowStepExecution(
            execution.getTenantId(), execution, stepId, stepName, stepType);
        
        return stepExecutionRepository.save(stepExecution);
    }

    /**
     * Update step execution status
     */
    public void updateStepExecution(UUID stepExecutionId, WorkflowStepExecution.StepStatus status,
                                  JsonNode inputData, JsonNode outputData, String errorMessage) {
        WorkflowStepExecution stepExecution = stepExecutionRepository.findById(stepExecutionId)
                .orElseThrow(() -> new WorkflowExecutionException("Step execution not found: " + stepExecutionId));
        
        stepExecution.setStatus(status);
        stepExecution.setInputData(inputData);
        stepExecution.setOutputData(outputData);
        
        if (status == WorkflowStepExecution.StepStatus.RUNNING) {
            stepExecution.markAsStarted();
        } else if (status == WorkflowStepExecution.StepStatus.COMPLETED) {
            stepExecution.markAsCompleted(outputData);
        } else if (status == WorkflowStepExecution.StepStatus.FAILED) {
            stepExecution.markAsFailed(errorMessage, null);
        } else if (status == WorkflowStepExecution.StepStatus.SKIPPED) {
            stepExecution.markAsSkipped();
        }
        
        stepExecutionRepository.save(stepExecution);
        
        // Update overall execution progress
        updateExecutionProgress(stepExecution.getWorkflowExecution());
    }

    /**
     * Update execution progress based on step completions
     */
    private void updateExecutionProgress(WorkflowExecution execution) {
        // Calculate progress based on completed steps
        long totalSteps = stepExecutionRepository.countByWorkflowExecutionId(execution.getId());
        long completedSteps = stepExecutionRepository.countByWorkflowExecutionIdAndStatusIn(
            execution.getId(), 
            List.of(WorkflowStepExecution.StepStatus.COMPLETED, 
                   WorkflowStepExecution.StepStatus.SKIPPED)
        );
        
        if (totalSteps > 0) {
            int progressPercentage = (int) ((completedSteps * 100) / totalSteps);
            workflowExecutionService.updateExecutionProgress(
                execution.getId(), 
                execution.getCurrentStep(), 
                progressPercentage
            );
        }
    }
}