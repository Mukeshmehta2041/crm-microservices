package com.crm.platform.deals.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pipeline_stages", indexes = {
    @Index(name = "idx_pipeline_stages_pipeline", columnList = "pipeline_id"),
    @Index(name = "idx_pipeline_stages_order", columnList = "pipeline_id, display_order"),
    @Index(name = "idx_pipeline_stages_active", columnList = "pipeline_id, is_active")
})
public class PipelineStage {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    @JsonIgnore
    private Pipeline pipeline;

    @Column(name = "pipeline_id", nullable = false, insertable = false, updatable = false)
    private UUID pipelineId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "default_probability", precision = 5, scale = 2)
    private BigDecimal defaultProbability;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @Column(name = "is_won", nullable = false)
    private Boolean isWon = false;

    @Min(0)
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Size(max = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "automation_rules", columnDefinition = "jsonb")
    private Map<String, Object> automationRules;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @OneToMany(mappedBy = "stageId", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Deal> deals;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<DealStageHistory> stageHistory;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public PipelineStage() {}

    public PipelineStage(String name, Pipeline pipeline, Integer displayOrder, UUID createdBy) {
        this.name = name;
        this.pipeline = pipeline;
        this.displayOrder = displayOrder;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Pipeline getPipeline() { return pipeline; }
    public void setPipeline(Pipeline pipeline) { this.pipeline = pipeline; }

    public UUID getPipelineId() { return pipelineId; }
    public void setPipelineId(UUID pipelineId) { this.pipelineId = pipelineId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDefaultProbability() { return defaultProbability; }
    public void setDefaultProbability(BigDecimal defaultProbability) { this.defaultProbability = defaultProbability; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }

    public Boolean getIsWon() { return isWon; }
    public void setIsWon(Boolean isWon) { this.isWon = isWon; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Map<String, Object> getAutomationRules() { return automationRules; }
    public void setAutomationRules(Map<String, Object> automationRules) { this.automationRules = automationRules; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public List<Deal> getDeals() { return deals; }
    public void setDeals(List<Deal> deals) { this.deals = deals; }

    public List<DealStageHistory> getStageHistory() { return stageHistory; }
    public void setStageHistory(List<DealStageHistory> stageHistory) { this.stageHistory = stageHistory; }

    // Business methods
    public boolean isClosedStage() {
        return isClosed;
    }

    public boolean isWonStage() {
        return isWon && isClosed;
    }

    public boolean isLostStage() {
        return !isWon && isClosed;
    }
}