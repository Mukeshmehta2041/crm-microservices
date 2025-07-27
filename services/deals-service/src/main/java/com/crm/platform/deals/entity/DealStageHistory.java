package com.crm.platform.deals.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deal_stage_history", indexes = {
    @Index(name = "idx_deal_stage_history_deal", columnList = "deal_id"),
    @Index(name = "idx_deal_stage_history_timestamp", columnList = "deal_id, changed_at")
})
public class DealStageHistory {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    @JsonIgnore
    private Deal deal;

    @Column(name = "deal_id", nullable = false, insertable = false, updatable = false)
    private UUID dealId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stage_id")
    private PipelineStage fromStage;

    @Column(name = "from_stage_id", insertable = false, updatable = false)
    private UUID fromStageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_stage_id", nullable = false)
    private PipelineStage toStage;

    @Column(name = "to_stage_id", nullable = false, insertable = false, updatable = false)
    private UUID toStageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    @Column(name = "pipeline_id", nullable = false, insertable = false, updatable = false)
    private UUID pipelineId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "duration_in_previous_stage_hours")
    private Long durationInPreviousStageHours;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public DealStageHistory() {}

    public DealStageHistory(Deal deal, PipelineStage fromStage, PipelineStage toStage, 
                           Pipeline pipeline, UUID changedBy, String reason) {
        this.deal = deal;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.pipeline = pipeline;
        this.changedBy = changedBy;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Deal getDeal() { return deal; }
    public void setDeal(Deal deal) { this.deal = deal; }

    public UUID getDealId() { return dealId; }
    public void setDealId(UUID dealId) { this.dealId = dealId; }

    public PipelineStage getFromStage() { return fromStage; }
    public void setFromStage(PipelineStage fromStage) { this.fromStage = fromStage; }

    public UUID getFromStageId() { return fromStageId; }
    public void setFromStageId(UUID fromStageId) { this.fromStageId = fromStageId; }

    public PipelineStage getToStage() { return toStage; }
    public void setToStage(PipelineStage toStage) { this.toStage = toStage; }

    public UUID getToStageId() { return toStageId; }
    public void setToStageId(UUID toStageId) { this.toStageId = toStageId; }

    public Pipeline getPipeline() { return pipeline; }
    public void setPipeline(Pipeline pipeline) { this.pipeline = pipeline; }

    public UUID getPipelineId() { return pipelineId; }
    public void setPipelineId(UUID pipelineId) { this.pipelineId = pipelineId; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public UUID getChangedBy() { return changedBy; }
    public void setChangedBy(UUID changedBy) { this.changedBy = changedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getDurationInPreviousStageHours() { return durationInPreviousStageHours; }
    public void setDurationInPreviousStageHours(Long durationInPreviousStageHours) { 
        this.durationInPreviousStageHours = durationInPreviousStageHours; 
    }
}