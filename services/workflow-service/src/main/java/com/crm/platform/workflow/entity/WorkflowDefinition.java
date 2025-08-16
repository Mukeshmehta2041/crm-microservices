package com.crm.platform.workflow.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a workflow definition
 */
@Entity
@Table(name = "workflow_definitions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name", "version"}))
public class WorkflowDefinition {

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

    @Column(name = "category")
    private String category;

    @NotNull
    @Positive
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @NotNull
    @Type(type = "jsonb")
    @Column(name = "workflow_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode workflowJson;

    @Type(type = "jsonb")
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private JsonNode triggerConfig;

    @Type(type = "jsonb")
    @Column(name = "variables_schema", columnDefinition = "jsonb")
    private JsonNode variablesSchema;

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

    @OneToMany(mappedBy = "workflowDefinition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowExecution> executions = new ArrayList<>();

    @OneToMany(mappedBy = "workflowDefinition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowTrigger> triggers = new ArrayList<>();

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
    public WorkflowDefinition() {}

    public WorkflowDefinition(UUID tenantId, String name, JsonNode workflowJson, UUID createdBy) {
        this.tenantId = tenantId;
        this.name = name;
        this.workflowJson = workflowJson;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public JsonNode getWorkflowJson() {
        return workflowJson;
    }

    public void setWorkflowJson(JsonNode workflowJson) {
        this.workflowJson = workflowJson;
    }

    public JsonNode getTriggerConfig() {
        return triggerConfig;
    }

    public void setTriggerConfig(JsonNode triggerConfig) {
        this.triggerConfig = triggerConfig;
    }

    public JsonNode getVariablesSchema() {
        return variablesSchema;
    }

    public void setVariablesSchema(JsonNode variablesSchema) {
        this.variablesSchema = variablesSchema;
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

    public List<WorkflowExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<WorkflowExecution> executions) {
        this.executions = executions;
    }

    public List<WorkflowTrigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<WorkflowTrigger> triggers) {
        this.triggers = triggers;
    }
}