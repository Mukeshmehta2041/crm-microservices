package com.crm.platform.workflow.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a business rule
 */
@Entity
@Table(name = "business_rules",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
public class BusinessRule {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Column(name = "entity_type")
    private String entityType;

    @NotNull
    @Type(type = "jsonb")
    @Column(name = "conditions", nullable = false, columnDefinition = "jsonb")
    private JsonNode conditions;

    @NotNull
    @Type(type = "jsonb")
    @Column(name = "actions", nullable = false, columnDefinition = "jsonb")
    private JsonNode actions;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "priority")
    private Integer priority = 0;

    @NotNull
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @NotNull
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "businessRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleExecution> executions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public BusinessRule() {}

    public BusinessRule(UUID tenantId, String name, RuleType ruleType, 
                       JsonNode conditions, JsonNode actions, UUID createdBy) {
        this.tenantId = tenantId;
        this.name = name;
        this.ruleType = ruleType;
        this.conditions = conditions;
        this.actions = actions;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    // Enums
    public enum RuleType {
        VALIDATION, ASSIGNMENT, NOTIFICATION, FIELD_UPDATE, WORKFLOW_TRIGGER
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public JsonNode getConditions() {
        return conditions;
    }

    public void setConditions(JsonNode conditions) {
        this.conditions = conditions;
    }

    public JsonNode getActions() {
        return actions;
    }

    public void setActions(JsonNode actions) {
        this.actions = actions;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<RuleExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<RuleExecution> executions) {
        this.executions = executions;
    }
}