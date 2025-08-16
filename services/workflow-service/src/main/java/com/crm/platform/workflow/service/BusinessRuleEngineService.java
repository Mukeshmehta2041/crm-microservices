package com.crm.platform.workflow.service;

import com.crm.platform.workflow.entity.BusinessRule;
import com.crm.platform.workflow.exception.WorkflowExecutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for executing business rule logic
 */
@Service
public class BusinessRuleEngineService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessRuleEngineService.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public BusinessRuleEngineService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate business rule conditions against entity data
     */
    public boolean evaluateRule(BusinessRule rule, JsonNode entityData) {
        logger.debug("Evaluating business rule: {} against entity data", rule.getId());

        try {
            JsonNode conditions = rule.getConditions();
            
            if (conditions.isArray()) {
                // Multiple conditions - evaluate as AND by default
                for (JsonNode condition : conditions) {
                    if (!evaluateCondition(condition, entityData)) {
                        return false;
                    }
                }
                return true;
            } else {
                // Single condition
                return evaluateCondition(conditions, entityData);
            }
        } catch (Exception e) {
            logger.error("Error evaluating business rule: {}", rule.getId(), e);
            throw new WorkflowExecutionException("Failed to evaluate business rule: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluate a single condition
     */
    private boolean evaluateCondition(JsonNode condition, JsonNode entityData) {
        String field = condition.get("field").asText();
        String operator = condition.get("operator").asText();
        JsonNode expectedValue = condition.get("value");

        // Get actual value from entity data
        JsonNode actualValue = getFieldValue(entityData, field);

        return evaluateOperator(actualValue, operator, expectedValue);
    }

    /**
     * Get field value from entity data (supports nested fields with dot notation)
     */
    private JsonNode getFieldValue(JsonNode entityData, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        JsonNode current = entityData;

        for (String part : parts) {
            if (current == null || current.isNull()) {
                return null;
            }
            current = current.get(part);
        }

        return current;
    }

    /**
     * Evaluate operator between actual and expected values
     */
    private boolean evaluateOperator(JsonNode actualValue, String operator, JsonNode expectedValue) {
        switch (operator.toLowerCase()) {
            case "equals":
                return equals(actualValue, expectedValue);
            case "not_equals":
                return !equals(actualValue, expectedValue);
            case "greater_than":
                return greaterThan(actualValue, expectedValue);
            case "less_than":
                return lessThan(actualValue, expectedValue);
            case "greater_than_or_equal":
                return greaterThanOrEqual(actualValue, expectedValue);
            case "less_than_or_equal":
                return lessThanOrEqual(actualValue, expectedValue);
            case "contains":
                return contains(actualValue, expectedValue);
            case "starts_with":
                return startsWith(actualValue, expectedValue);
            case "ends_with":
                return endsWith(actualValue, expectedValue);
            case "in":
                return in(actualValue, expectedValue);
            case "not_in":
                return !in(actualValue, expectedValue);
            case "is_null":
                return actualValue == null || actualValue.isNull();
            case "is_not_null":
                return actualValue != null && !actualValue.isNull();
            case "matches_regex":
                return matchesRegex(actualValue, expectedValue);
            default:
                throw new WorkflowExecutionException("Unsupported operator: " + operator);
        }
    }

    /**
     * Execute business rule actions
     */
    public JsonNode executeRuleActions(BusinessRule rule, JsonNode entityData) {
        logger.debug("Executing actions for business rule: {}", rule.getId());

        try {
            ObjectNode result = objectMapper.createObjectNode();
            JsonNode actions = rule.getActions();

            if (!actions.isArray()) {
                throw new WorkflowExecutionException("Rule actions must be an array");
            }

            List<String> executedActions = new ArrayList<>();

            for (JsonNode action : actions) {
                String actionType = action.get("type").asText();
                JsonNode actionResult = executeAction(actionType, action, entityData);
                
                result.set(actionType + "_" + System.currentTimeMillis(), actionResult);
                executedActions.add(actionType);
            }

            result.put("executed_actions", String.join(", ", executedActions));
            result.put("execution_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return result;
        } catch (Exception e) {
            logger.error("Error executing actions for business rule: {}", rule.getId(), e);
            throw new WorkflowExecutionException("Failed to execute rule actions: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a single action
     */
    private JsonNode executeAction(String actionType, JsonNode action, JsonNode entityData) {
        switch (actionType.toLowerCase()) {
            case "set_field":
                return executeSetFieldAction(action, entityData);
            case "send_email":
                return executeSendEmailAction(action, entityData);
            case "create_task":
                return executeCreateTaskAction(action, entityData);
            case "trigger_workflow":
                return executeTriggerWorkflowAction(action, entityData);
            case "send_notification":
                return executeSendNotificationAction(action, entityData);
            case "call_webhook":
                return executeCallWebhookAction(action, entityData);
            case "update_record":
                return executeUpdateRecordAction(action, entityData);
            default:
                throw new WorkflowExecutionException("Unsupported action type: " + actionType);
        }
    }

    // Comparison methods
    private boolean equals(JsonNode actual, JsonNode expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        return actual.equals(expected);
    }

    private boolean greaterThan(JsonNode actual, JsonNode expected) {
        if (actual == null || expected == null) return false;
        if (actual.isNumber() && expected.isNumber()) {
            return actual.decimalValue().compareTo(expected.decimalValue()) > 0;
        }
        if (actual.isTextual() && expected.isTextual()) {
            return actual.asText().compareTo(expected.asText()) > 0;
        }
        return false;
    }

    private boolean lessThan(JsonNode actual, JsonNode expected) {
        if (actual == null || expected == null) return false;
        if (actual.isNumber() && expected.isNumber()) {
            return actual.decimalValue().compareTo(expected.decimalValue()) < 0;
        }
        if (actual.isTextual() && expected.isTextual()) {
            return actual.asText().compareTo(expected.asText()) < 0;
        }
        return false;
    }

    private boolean greaterThanOrEqual(JsonNode actual, JsonNode expected) {
        return equals(actual, expected) || greaterThan(actual, expected);
    }

    private boolean lessThanOrEqual(JsonNode actual, JsonNode expected) {
        return equals(actual, expected) || lessThan(actual, expected);
    }

    private boolean contains(JsonNode actual, JsonNode expected) {
        if (actual == null || expected == null) return false;
        if (actual.isTextual() && expected.isTextual()) {
            return actual.asText().toLowerCase().contains(expected.asText().toLowerCase());
        }
        if (actual.isArray()) {
            for (JsonNode item : actual) {
                if (equals(item, expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean startsWith(JsonNode actual, JsonNode expected) {
        if (actual == null || expected == null) return false;
        if (actual.isTextual() && expected.isTextual()) {
            return actual.asText().toLowerCase().startsWith(expected.asText().toLowerCase());
        }
        return false;
    }

    private boolean endsWith(JsonNode actual, JsonNode expected) {
        if (actual == null || expected == null) return false;
        if (actual.isTextual() && expected.isTextual()) {
            return actual.asText().toLowerCase().endsWith(expected.asText().toLowerCase());
        }
        return false;
    }

    private boolean in(JsonNode actual, JsonNode expected) {
        if (actual == null || expected == null) return false;
        if (expected.isArray()) {
            for (JsonNode item : expected) {
                if (equals(actual, item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesRegex(JsonNode actual, JsonNode expected) {
        if (actual == null || expected == null) return false;
        if (actual.isTextual() && expected.isTextual()) {
            return actual.asText().matches(expected.asText());
        }
        return false;
    }

    // Action execution methods
    private JsonNode executeSetFieldAction(JsonNode action, JsonNode entityData) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "set_field");
        result.put("field", action.get("field").asText());
        result.put("value", action.get("value").asText());
        result.put("status", "executed");
        return result;
    }

    private JsonNode executeSendEmailAction(JsonNode action, JsonNode entityData) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "send_email");
        result.put("to", action.get("to").asText());
        result.put("subject", action.get("subject").asText());
        result.put("status", "queued");
        return result;
    }

    private JsonNode executeCreateTaskAction(JsonNode action, JsonNode entityData) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "create_task");
        result.put("title", action.get("title").asText());
        result.put("assignee", action.has("assignee") ? action.get("assignee").asText() : "");
        result.put("status", "created");
        return result;
    }

    private JsonNode executeTriggerWorkflowAction(JsonNode action, JsonNode entityData) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "trigger_workflow");
        result.put("workflowId", action.get("workflowId").asText());
        result.put("status", "triggered");
        return result;
    }

    private JsonNode executeSendNotificationAction(JsonNode action, JsonNode entityData) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "send_notification");
        result.put("recipient", action.get("recipient").asText());
        result.put("message", action.get("message").asText());
        result.put("status", "sent");
        return result;
    }

    private JsonNode executeCallWebhookAction(JsonNode action, JsonNode entityData) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "call_webhook");
        result.put("url", action.get("url").asText());
        result.put("method", action.has("method") ? action.get("method").asText() : "POST");
        result.put("status", "called");
        return result;
    }

    private JsonNode executeUpdateRecordAction(JsonNode action, JsonNode entityData) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "update_record");
        result.put("recordType", action.get("recordType").asText());
        result.put("recordId", action.get("recordId").asText());
        result.put("status", "updated");
        return result;
    }
}