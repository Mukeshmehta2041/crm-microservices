package com.crm.platform.workflow.graphql;

import com.crm.platform.workflow.dto.BusinessRuleDto;
import com.crm.platform.workflow.dto.WorkflowDefinitionDto;
import com.crm.platform.workflow.dto.WorkflowExecutionDto;
import com.crm.platform.workflow.entity.BusinessRule;
import com.crm.platform.workflow.entity.WorkflowExecution;
import com.crm.platform.workflow.service.BusinessRuleService;
import com.crm.platform.workflow.service.WorkflowDefinitionService;
import com.crm.platform.workflow.service.WorkflowExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.kickstart.tools.GraphQLSubscriptionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GraphQL resolver for workflow operations
 */
@Component
public class WorkflowGraphQLResolver implements GraphQLQueryResolver, GraphQLMutationResolver, GraphQLSubscriptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowGraphQLResolver.class);

    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowExecutionService workflowExecutionService;
    private final BusinessRuleService businessRuleService;

    @Autowired
    public WorkflowGraphQLResolver(WorkflowDefinitionService workflowDefinitionService,
                                 WorkflowExecutionService workflowExecutionService,
                                 BusinessRuleService businessRuleService) {
        this.workflowDefinitionService = workflowDefinitionService;
        this.workflowExecutionService = workflowExecutionService;
        this.businessRuleService = businessRuleService;
    }

    // Query Resolvers

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW', 'READ')")
    public WorkflowDefinitionDto workflowDefinition(UUID id) {
        UUID tenantId = getCurrentTenantId();
        return workflowDefinitionService.getWorkflow(tenantId, id);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW', 'READ')")
    public Page<WorkflowDefinitionDto> workflowDefinitions(Boolean activeOnly, Boolean publishedOnly, 
                                                          String category, String search, 
                                                          Integer page, Integer size) {
        UUID tenantId = getCurrentTenantId();
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        if (search != null && !search.trim().isEmpty()) {
            return workflowDefinitionService.searchWorkflows(tenantId, search, pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            return workflowDefinitionService.getWorkflowsByCategory(tenantId, category, pageable);
        } else if (Boolean.TRUE.equals(publishedOnly)) {
            return workflowDefinitionService.getPublishedWorkflows(tenantId, pageable);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            return workflowDefinitionService.getActiveWorkflows(tenantId, pageable);
        } else {
            return workflowDefinitionService.getWorkflows(tenantId, pageable);
        }
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW', 'READ')")
    public List<WorkflowDefinitionDto> workflowVersions(String name) {
        UUID tenantId = getCurrentTenantId();
        return workflowDefinitionService.getWorkflowVersions(tenantId, name);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW', 'READ')")
    public WorkflowDefinitionDto latestWorkflowVersion(String name) {
        UUID tenantId = getCurrentTenantId();
        Optional<WorkflowDefinitionDto> workflow = workflowDefinitionService.getLatestWorkflowVersion(tenantId, name);
        return workflow.orElse(null);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public WorkflowExecutionDto workflowExecution(UUID id) {
        UUID tenantId = getCurrentTenantId();
        return workflowExecutionService.getExecution(tenantId, id);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public Page<WorkflowExecutionDto> workflowExecutions(WorkflowExecution.ExecutionStatus status,
                                                        UUID workflowDefinitionId, String triggerType,
                                                        Integer page, Integer size) {
        UUID tenantId = getCurrentTenantId();
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        if (workflowDefinitionId != null) {
            return workflowExecutionService.getExecutionsByWorkflow(tenantId, workflowDefinitionId, pageable);
        } else if (status != null) {
            return workflowExecutionService.getExecutionsByStatus(tenantId, status, pageable);
        } else {
            return workflowExecutionService.getExecutions(tenantId, pageable);
        }
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public List<WorkflowExecutionDto> runningWorkflowExecutions() {
        UUID tenantId = getCurrentTenantId();
        return workflowExecutionService.getRunningExecutions(tenantId);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'BUSINESS_RULE', 'READ')")
    public BusinessRuleDto businessRule(UUID id) {
        UUID tenantId = getCurrentTenantId();
        return businessRuleService.getBusinessRule(tenantId, id);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'BUSINESS_RULE', 'READ')")
    public Page<BusinessRuleDto> businessRules(Boolean activeOnly, BusinessRule.RuleType ruleType,
                                             String entityType, String search,
                                             Integer page, Integer size) {
        UUID tenantId = getCurrentTenantId();
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        if (search != null && !search.trim().isEmpty()) {
            return businessRuleService.searchBusinessRules(tenantId, search, pageable);
        } else if (entityType != null && !entityType.trim().isEmpty()) {
            return businessRuleService.getBusinessRulesByEntityType(tenantId, entityType, pageable);
        } else if (ruleType != null) {
            return businessRuleService.getBusinessRulesByType(tenantId, ruleType, pageable);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            return businessRuleService.getActiveBusinessRules(tenantId, pageable);
        } else {
            return businessRuleService.getBusinessRules(tenantId, pageable);
        }
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW', 'READ')")
    public WorkflowDefinitionService.WorkflowStatistics workflowStatistics() {
        UUID tenantId = getCurrentTenantId();
        return workflowDefinitionService.getWorkflowStatistics(tenantId);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW_EXECUTION', 'READ')")
    public WorkflowExecutionService.ExecutionStatistics executionStatistics() {
        UUID tenantId = getCurrentTenantId();
        return workflowExecutionService.getExecutionStatistics(tenantId);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'BUSINESS_RULE', 'READ')")
    public BusinessRuleService.BusinessRuleStatistics businessRuleStatistics() {
        UUID tenantId = getCurrentTenantId();
        return businessRuleService.getBusinessRuleStatistics(tenantId);
    }

    // Mutation Resolvers

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW', 'CREATE')")
    public WorkflowDefinitionDto createWorkflowDefinition(CreateWorkflowDefinitionInput input) {
        UUID tenantId = getCurrentTenantId();
        UUID userId = getCurrentUserId();

        WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
        dto.setTenantId(tenantId);
        dto.setName(input.getName());
        dto.setDescription(input.getDescription());
        dto.setCategory(input.getCategory());
        dto.setWorkflowJson(input.getWorkflowJson());
        dto.setTriggerConfig(input.getTriggerConfig());
        dto.setVariablesSchema(input.getVariablesSchema());
        dto.setCreatedBy(userId);
        dto.setUpdatedBy(userId);

        return workflowDefinitionService.createWorkflow(dto);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW', 'UPDATE')")
    public WorkflowDefinitionDto updateWorkflowDefinition(UUID id, UpdateWorkflowDefinitionInput input) {
        UUID tenantId = getCurrentTenantId();
        UUID userId = getCurrentUserId();

        WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
        dto.setTenantId(tenantId);
        dto.setName(input.getName());
        dto.setDescription(input.getDescription());
        dto.setCategory(input.getCategory());
        dto.setWorkflowJson(input.getWorkflowJson());
        dto.setTriggerConfig(input.getTriggerConfig());
        dto.setVariablesSchema(input.getVariablesSchema());
        dto.setUpdatedBy(userId);

        return workflowDefinitionService.updateWorkflow(id, dto);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'WORKFLOW_EXECUTION', 'CREATE')")
    public WorkflowExecutionDto startWorkflowExecution(StartWorkflowExecutionInput input) {
        UUID tenantId = getCurrentTenantId();
        UUID userId = getCurrentUserId();

        return workflowExecutionService.startWorkflow(
            tenantId,
            input.getWorkflowDefinitionId(),
            input.getTriggerType(),
            input.getTriggerData(),
            input.getVariables(),
            userId
        );
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'BUSINESS_RULE', 'CREATE')")
    public BusinessRuleDto createBusinessRule(CreateBusinessRuleInput input) {
        UUID tenantId = getCurrentTenantId();
        UUID userId = getCurrentUserId();

        BusinessRuleDto dto = new BusinessRuleDto();
        dto.setTenantId(tenantId);
        dto.setName(input.getName());
        dto.setDescription(input.getDescription());
        dto.setRuleType(input.getRuleType());
        dto.setEntityType(input.getEntityType());
        dto.setConditions(input.getConditions());
        dto.setActions(input.getActions());
        dto.setPriority(input.getPriority());
        dto.setCreatedBy(userId);
        dto.setUpdatedBy(userId);

        return businessRuleService.createBusinessRule(dto);
    }

    @PreAuthorize("hasPermission(authentication.principal.tenantId, 'BUSINESS_RULE', 'READ')")
    public BusinessRuleService.BusinessRuleTestResult testBusinessRule(UUID id, JsonNode testData) {
        UUID tenantId = getCurrentTenantId();
        return businessRuleService.testBusinessRule(tenantId, id, testData);
    }

    // Subscription Resolvers

    public Flux<WorkflowExecutionDto> workflowExecutionUpdated(UUID executionId) {
        // Implementation would use reactive streams to publish execution updates
        return Flux.empty(); // Placeholder
    }

    public Flux<WorkflowExecutionDto> workflowExecutionStarted() {
        // Implementation would use reactive streams to publish started executions
        return Flux.empty(); // Placeholder
    }

    public Flux<WorkflowExecutionDto> workflowExecutionCompleted() {
        // Implementation would use reactive streams to publish completed executions
        return Flux.empty(); // Placeholder
    }

    public Flux<WorkflowExecutionDto> workflowExecutionFailed() {
        // Implementation would use reactive streams to publish failed executions
        return Flux.empty(); // Placeholder
    }

    // Helper methods

    private UUID getCurrentTenantId() {
        // Implementation would extract tenant ID from security context
        return UUID.randomUUID(); // Placeholder
    }

    private UUID getCurrentUserId() {
        // Implementation would extract user ID from security context
        return UUID.randomUUID(); // Placeholder
    }

    // Input classes

    public static class CreateWorkflowDefinitionInput {
        private String name;
        private String description;
        private String category;
        private JsonNode workflowJson;
        private JsonNode triggerConfig;
        private JsonNode variablesSchema;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public JsonNode getWorkflowJson() { return workflowJson; }
        public void setWorkflowJson(JsonNode workflowJson) { this.workflowJson = workflowJson; }
        public JsonNode getTriggerConfig() { return triggerConfig; }
        public void setTriggerConfig(JsonNode triggerConfig) { this.triggerConfig = triggerConfig; }
        public JsonNode getVariablesSchema() { return variablesSchema; }
        public void setVariablesSchema(JsonNode variablesSchema) { this.variablesSchema = variablesSchema; }
    }

    public static class UpdateWorkflowDefinitionInput {
        private String name;
        private String description;
        private String category;
        private JsonNode workflowJson;
        private JsonNode triggerConfig;
        private JsonNode variablesSchema;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public JsonNode getWorkflowJson() { return workflowJson; }
        public void setWorkflowJson(JsonNode workflowJson) { this.workflowJson = workflowJson; }
        public JsonNode getTriggerConfig() { return triggerConfig; }
        public void setTriggerConfig(JsonNode triggerConfig) { this.triggerConfig = triggerConfig; }
        public JsonNode getVariablesSchema() { return variablesSchema; }
        public void setVariablesSchema(JsonNode variablesSchema) { this.variablesSchema = variablesSchema; }
    }

    public static class StartWorkflowExecutionInput {
        private UUID workflowDefinitionId;
        private String triggerType;
        private JsonNode triggerData;
        private JsonNode variables;

        // Getters and setters
        public UUID getWorkflowDefinitionId() { return workflowDefinitionId; }
        public void setWorkflowDefinitionId(UUID workflowDefinitionId) { this.workflowDefinitionId = workflowDefinitionId; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public JsonNode getTriggerData() { return triggerData; }
        public void setTriggerData(JsonNode triggerData) { this.triggerData = triggerData; }
        public JsonNode getVariables() { return variables; }
        public void setVariables(JsonNode variables) { this.variables = variables; }
    }

    public static class CreateBusinessRuleInput {
        private String name;
        private String description;
        private BusinessRule.RuleType ruleType;
        private String entityType;
        private JsonNode conditions;
        private JsonNode actions;
        private Integer priority = 0;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BusinessRule.RuleType getRuleType() { return ruleType; }
        public void setRuleType(BusinessRule.RuleType ruleType) { this.ruleType = ruleType; }
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public JsonNode getConditions() { return conditions; }
        public void setConditions(JsonNode conditions) { this.conditions = conditions; }
        public JsonNode getActions() { return actions; }
        public void setActions(JsonNode actions) { this.actions = actions; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }
}