package com.crm.platform.pipelines.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pipeline_stages")
@EntityListeners(AuditingEntityListener.class)
public class PipelineStage {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    @JsonBackReference
    private Pipeline pipeline;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 500)
    @Column(name = "description")
    private String description;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Column(name = "default_probability", precision = 5, scale = 2)
    private BigDecimal defaultProbability;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @NotNull
    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @NotNull
    @Column(name = "is_won", nullable = false)
    private Boolean isWon = false;

    @NotNull
    @Min(0)
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color code")
    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "automation_rules", columnDefinition = "jsonb")
    private Map<String, Object> automationRules;

    @Column(name = "stage_configuration", columnDefinition = "jsonb")
    private Map<String, Object> stageConfiguration;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotNull
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @NotNull
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<AutomationRule> stageAutomationRules = new ArrayList<>();

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<PipelineAnalytics> stageAnalytics = new ArrayList<>();

    // Constructors
    public PipelineStage() {}

    public PipelineStage(Pipeline pipeline, String name, String description, Integer displayOrder, UUID createdBy) {
        this.pipeline = pipeline;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
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

    public Pipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
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

    public BigDecimal getDefaultProbability() {
        return defaultProbability;
    }

    public void setDefaultProbability(BigDecimal defaultProbability) {
        this.defaultProbability = defaultProbability;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Boolean isClosed) {
        this.isClosed = isClosed;
    }

    public Boolean getIsWon() {
        return isWon;
    }

    public void setIsWon(Boolean isWon) {
        this.isWon = isWon;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Map<String, Object> getAutomationRules() {
        return automationRules;
    }

    public void setAutomationRules(Map<String, Object> automationRules) {
        this.automationRules = automationRules;
    }

    public Map<String, Object> getStageConfiguration() {
        return stageConfiguration;
    }

    public void setStageConfiguration(Map<String, Object> stageConfiguration) {
        this.stageConfiguration = stageConfiguration;
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

    public List<AutomationRule> getStageAutomationRules() {
        return stageAutomationRules;
    }

    public void setStageAutomationRules(List<AutomationRule> stageAutomationRules) {
        this.stageAutomationRules = stageAutomationRules;
    }

    public List<PipelineAnalytics> getStageAnalytics() {
        return stageAnalytics;
    }

    public void setStageAnalytics(List<PipelineAnalytics> stageAnalytics) {
        this.stageAnalytics = stageAnalytics;
    }

    // Helper methods
    public void addAutomationRule(AutomationRule rule) {
        stageAutomationRules.add(rule);
        rule.setStage(this);
    }

    public void removeAutomationRule(AutomationRule rule) {
        stageAutomationRules.remove(rule);
        rule.setStage(null);
    }

    public UUID getTenantId() {
        return pipeline != null ? pipeline.getTenantId() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PipelineStage)) return false;
        PipelineStage that = (PipelineStage) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PipelineStage{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayOrder=" + displayOrder +
                ", isActive=" + isActive +
                ", isClosed=" + isClosed +
                ", isWon=" + isWon +
                '}';
    }
}