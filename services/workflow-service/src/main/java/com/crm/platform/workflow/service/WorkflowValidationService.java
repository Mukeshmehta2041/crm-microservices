package com.crm.platform.workflow.service;

import com.crm.platform.workflow.exception.WorkflowValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for validating workflow definitions
 */
@Service
public class WorkflowValidationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowValidationService.class);

    private static final Set<String> SUPPORTED_STEP_TYPES = Set.of(
        "service_task", "user_task", "script_task", "send_task", "receive_task",
        "business_rule_task", "manual_task", "gateway", "event", "subprocess"
    );

    private static final Set<String> REQUIRED_STEP_FIELDS = Set.of("id", "name", "type");

    /**
     * Validate workflow definition JSON
     */
    public void validateWorkflowDefinition(JsonNode workflowJson) {
        logger.debug("Validating workflow definition");

        if (workflowJson == null || workflowJson.isNull()) {
            throw new WorkflowValidationException("Workflow JSON cannot be null");
        }

        // Validate basic structure
        validateBasicStructure(workflowJson);

        // Validate steps
        if (workflowJson.has("steps")) {
            validateSteps(workflowJson.get("steps"));
        }

        // Validate connections/flows
        if (workflowJson.has("connections")) {
            validateConnections(workflowJson.get("connections"), workflowJson.get("steps"));
        }

        // Validate variables schema
        if (workflowJson.has("variables")) {
            validateVariables(workflowJson.get("variables"));
        }

        logger.debug("Workflow definition validation completed successfully");
    }

    /**
     * Validate basic workflow structure
     */
    private void validateBasicStructure(JsonNode workflowJson) {
        // Check for required top-level fields
        if (!workflowJson.has("name") || workflowJson.get("name").asText().trim().isEmpty()) {
            throw new WorkflowValidationException("Workflow name is required");
        }

        if (!workflowJson.has("version")) {
            throw new WorkflowValidationException("Workflow version is required");
        }

        // Validate version format
        String version = workflowJson.get("version").asText();
        if (!version.matches("\\d+\\.\\d+(\\.\\d+)?")) {
            throw new WorkflowValidationException("Invalid version format. Expected: x.y or x.y.z");
        }

        // Check for steps array
        if (!workflowJson.has("steps") || !workflowJson.get("steps").isArray()) {
            throw new WorkflowValidationException("Workflow must have a steps array");
        }

        if (workflowJson.get("steps").size() == 0) {
            throw new WorkflowValidationException("Workflow must have at least one step");
        }
    }

    /**
     * Validate workflow steps
     */
    private void validateSteps(JsonNode stepsNode) {
        Set<String> stepIds = new HashSet<>();
        boolean hasStartStep = false;
        boolean hasEndStep = false;

        for (JsonNode step : stepsNode) {
            // Validate required fields
            for (String requiredField : REQUIRED_STEP_FIELDS) {
                if (!step.has(requiredField) || step.get(requiredField).asText().trim().isEmpty()) {
                    throw new WorkflowValidationException(
                        String.format("Step is missing required field: %s", requiredField));
                }
            }

            String stepId = step.get("id").asText();
            String stepType = step.get("type").asText();

            // Check for duplicate step IDs
            if (stepIds.contains(stepId)) {
                throw new WorkflowValidationException(
                    String.format("Duplicate step ID found: %s", stepId));
            }
            stepIds.add(stepId);

            // Validate step type
            if (!SUPPORTED_STEP_TYPES.contains(stepType)) {
                throw new WorkflowValidationException(
                    String.format("Unsupported step type: %s", stepType));
            }

            // Check for start and end steps
            if ("start_event".equals(stepType) || step.has("isStart") && step.get("isStart").asBoolean()) {
                hasStartStep = true;
            }
            if ("end_event".equals(stepType) || step.has("isEnd") && step.get("isEnd").asBoolean()) {
                hasEndStep = true;
            }

            // Validate step-specific configuration
            validateStepConfiguration(step, stepType);
        }

        // Ensure workflow has start and end steps
        if (!hasStartStep) {
            throw new WorkflowValidationException("Workflow must have a start step");
        }
        if (!hasEndStep) {
            throw new WorkflowValidationException("Workflow must have an end step");
        }
    }

    /**
     * Validate step-specific configuration
     */
    private void validateStepConfiguration(JsonNode step, String stepType) {
        switch (stepType) {
            case "service_task":
                validateServiceTask(step);
                break;
            case "user_task":
                validateUserTask(step);
                break;
            case "script_task":
                validateScriptTask(step);
                break;
            case "business_rule_task":
                validateBusinessRuleTask(step);
                break;
            case "gateway":
                validateGateway(step);
                break;
            default:
                // Basic validation for other step types
                break;
        }
    }

    /**
     * Validate service task configuration
     */
    private void validateServiceTask(JsonNode step) {
        if (!step.has("serviceClass") && !step.has("expression") && !step.has("delegateExpression")) {
            throw new WorkflowValidationException(
                String.format("Service task '%s' must have serviceClass, expression, or delegateExpression", 
                             step.get("id").asText()));
        }
    }

    /**
     * Validate user task configuration
     */
    private void validateUserTask(JsonNode step) {
        if (!step.has("assignee") && !step.has("candidateUsers") && !step.has("candidateGroups")) {
            throw new WorkflowValidationException(
                String.format("User task '%s' must have assignee, candidateUsers, or candidateGroups", 
                             step.get("id").asText()));
        }
    }

    /**
     * Validate script task configuration
     */
    private void validateScriptTask(JsonNode step) {
        if (!step.has("script") || step.get("script").asText().trim().isEmpty()) {
            throw new WorkflowValidationException(
                String.format("Script task '%s' must have a script", step.get("id").asText()));
        }

        if (!step.has("scriptFormat")) {
            throw new WorkflowValidationException(
                String.format("Script task '%s' must specify scriptFormat", step.get("id").asText()));
        }
    }

    /**
     * Validate business rule task configuration
     */
    private void validateBusinessRuleTask(JsonNode step) {
        if (!step.has("ruleId") && !step.has("ruleExpression")) {
            throw new WorkflowValidationException(
                String.format("Business rule task '%s' must have ruleId or ruleExpression", 
                             step.get("id").asText()));
        }
    }

    /**
     * Validate gateway configuration
     */
    private void validateGateway(JsonNode step) {
        if (!step.has("gatewayType")) {
            throw new WorkflowValidationException(
                String.format("Gateway '%s' must specify gatewayType", step.get("id").asText()));
        }

        String gatewayType = step.get("gatewayType").asText();
        if (!Set.of("exclusive", "inclusive", "parallel", "event").contains(gatewayType)) {
            throw new WorkflowValidationException(
                String.format("Gateway '%s' has invalid gatewayType: %s", 
                             step.get("id").asText(), gatewayType));
        }
    }

    /**
     * Validate workflow connections/flows
     */
    private void validateConnections(JsonNode connectionsNode, JsonNode stepsNode) {
        if (!connectionsNode.isArray()) {
            throw new WorkflowValidationException("Connections must be an array");
        }

        Set<String> stepIds = new HashSet<>();
        for (JsonNode step : stepsNode) {
            stepIds.add(step.get("id").asText());
        }

        for (JsonNode connection : connectionsNode) {
            if (!connection.has("from") || !connection.has("to")) {
                throw new WorkflowValidationException("Connection must have 'from' and 'to' fields");
            }

            String fromStepId = connection.get("from").asText();
            String toStepId = connection.get("to").asText();

            if (!stepIds.contains(fromStepId)) {
                throw new WorkflowValidationException(
                    String.format("Connection references unknown step ID: %s", fromStepId));
            }

            if (!stepIds.contains(toStepId)) {
                throw new WorkflowValidationException(
                    String.format("Connection references unknown step ID: %s", toStepId));
            }

            // Validate condition expression if present
            if (connection.has("condition")) {
                validateConditionExpression(connection.get("condition").asText());
            }
        }

        // Validate workflow connectivity (ensure all steps are reachable)
        validateWorkflowConnectivity(stepsNode, connectionsNode);
    }

    /**
     * Validate condition expression
     */
    private void validateConditionExpression(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            throw new WorkflowValidationException("Condition expression cannot be empty");
        }

        // Basic validation - in a real implementation, you might use a proper expression parser
        if (!condition.contains("${") || !condition.contains("}")) {
            throw new WorkflowValidationException(
                "Condition expression must be in format: ${expression}");
        }
    }

    /**
     * Validate workflow connectivity
     */
    private void validateWorkflowConnectivity(JsonNode stepsNode, JsonNode connectionsNode) {
        // This is a simplified connectivity check
        // In a real implementation, you would perform a proper graph traversal
        
        Set<String> reachableSteps = new HashSet<>();
        
        // Find start steps
        for (JsonNode step : stepsNode) {
            String stepType = step.get("type").asText();
            if ("start_event".equals(stepType) || 
                (step.has("isStart") && step.get("isStart").asBoolean())) {
                reachableSteps.add(step.get("id").asText());
            }
        }

        // Follow connections to find all reachable steps
        boolean changed = true;
        while (changed) {
            changed = false;
            for (JsonNode connection : connectionsNode) {
                String fromStepId = connection.get("from").asText();
                String toStepId = connection.get("to").asText();
                
                if (reachableSteps.contains(fromStepId) && !reachableSteps.contains(toStepId)) {
                    reachableSteps.add(toStepId);
                    changed = true;
                }
            }
        }

        // Check if all steps are reachable
        for (JsonNode step : stepsNode) {
            String stepId = step.get("id").asText();
            if (!reachableSteps.contains(stepId)) {
                throw new WorkflowValidationException(
                    String.format("Step '%s' is not reachable from start steps", stepId));
            }
        }
    }

    /**
     * Validate workflow variables
     */
    private void validateVariables(JsonNode variablesNode) {
        if (!variablesNode.isObject()) {
            throw new WorkflowValidationException("Variables must be an object");
        }

        variablesNode.fields().forEachRemaining(entry -> {
            String variableName = entry.getKey();
            JsonNode variableConfig = entry.getValue();

            // Validate variable name
            if (!variableName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                throw new WorkflowValidationException(
                    String.format("Invalid variable name: %s. Must start with letter and contain only letters, numbers, and underscores", 
                                 variableName));
            }

            // Validate variable configuration
            if (!variableConfig.has("type")) {
                throw new WorkflowValidationException(
                    String.format("Variable '%s' must have a type", variableName));
            }

            String variableType = variableConfig.get("type").asText();
            if (!Set.of("string", "number", "boolean", "date", "object", "array").contains(variableType)) {
                throw new WorkflowValidationException(
                    String.format("Variable '%s' has invalid type: %s", variableName, variableType));
            }
        });
    }

    /**
     * Validate business rule definition
     */
    public void validateBusinessRule(JsonNode conditions, JsonNode actions) {
        logger.debug("Validating business rule");

        if (conditions == null || conditions.isNull()) {
            throw new WorkflowValidationException("Business rule conditions cannot be null");
        }

        if (actions == null || actions.isNull()) {
            throw new WorkflowValidationException("Business rule actions cannot be null");
        }

        // Validate conditions structure
        validateRuleConditions(conditions);

        // Validate actions structure
        validateRuleActions(actions);

        logger.debug("Business rule validation completed successfully");
    }

    /**
     * Validate rule conditions
     */
    private void validateRuleConditions(JsonNode conditions) {
        if (!conditions.isObject() && !conditions.isArray()) {
            throw new WorkflowValidationException("Rule conditions must be an object or array");
        }

        if (conditions.isArray()) {
            for (JsonNode condition : conditions) {
                validateSingleCondition(condition);
            }
        } else {
            validateSingleCondition(conditions);
        }
    }

    /**
     * Validate single condition
     */
    private void validateSingleCondition(JsonNode condition) {
        if (!condition.has("field") || !condition.has("operator") || !condition.has("value")) {
            throw new WorkflowValidationException(
                "Rule condition must have 'field', 'operator', and 'value'");
        }

        String operator = condition.get("operator").asText();
        if (!Set.of("equals", "not_equals", "greater_than", "less_than", "contains", 
                   "starts_with", "ends_with", "in", "not_in", "is_null", "is_not_null").contains(operator)) {
            throw new WorkflowValidationException(
                String.format("Invalid condition operator: %s", operator));
        }
    }

    /**
     * Validate rule actions
     */
    private void validateRuleActions(JsonNode actions) {
        if (!actions.isArray()) {
            throw new WorkflowValidationException("Rule actions must be an array");
        }

        if (actions.size() == 0) {
            throw new WorkflowValidationException("Rule must have at least one action");
        }

        for (JsonNode action : actions) {
            validateSingleAction(action);
        }
    }

    /**
     * Validate single action
     */
    private void validateSingleAction(JsonNode action) {
        if (!action.has("type")) {
            throw new WorkflowValidationException("Rule action must have a 'type'");
        }

        String actionType = action.get("type").asText();
        if (!Set.of("set_field", "send_email", "create_task", "trigger_workflow", 
                   "send_notification", "call_webhook", "update_record").contains(actionType)) {
            throw new WorkflowValidationException(
                String.format("Invalid action type: %s", actionType));
        }

        // Validate action-specific configuration
        switch (actionType) {
            case "set_field":
                if (!action.has("field") || !action.has("value")) {
                    throw new WorkflowValidationException(
                        "set_field action must have 'field' and 'value'");
                }
                break;
            case "send_email":
                if (!action.has("to") || !action.has("subject")) {
                    throw new WorkflowValidationException(
                        "send_email action must have 'to' and 'subject'");
                }
                break;
            case "trigger_workflow":
                if (!action.has("workflowId")) {
                    throw new WorkflowValidationException(
                        "trigger_workflow action must have 'workflowId'");
                }
                break;
        }
    }
}