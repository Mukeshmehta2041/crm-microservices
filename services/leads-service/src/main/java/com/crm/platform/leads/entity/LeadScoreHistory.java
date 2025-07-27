package com.crm.platform.leads.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_score_history", indexes = {
    @Index(name = "idx_lead_score_history_lead_id", columnList = "lead_id"),
    @Index(name = "idx_lead_score_history_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_lead_score_history_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class LeadScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    @NotNull
    private UUID tenantId;

    @Column(name = "lead_id", nullable = false)
    @NotNull
    private UUID leadId;

    @Column(name = "previous_score", nullable = false)
    @Min(0)
    @Max(100)
    private Integer previousScore;

    @Column(name = "new_score", nullable = false)
    @Min(0)
    @Max(100)
    private Integer newScore;

    @Column(name = "score_change", nullable = false)
    private Integer scoreChange;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "rule_name", length = 100)
    private String ruleName;

    @Column(name = "rule_category", length = 50)
    private String ruleCategory;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    @NotNull
    private UUID createdBy;

    // Constructors
    public LeadScoreHistory() {}

    public LeadScoreHistory(UUID tenantId, UUID leadId, Integer previousScore, Integer newScore, 
                           String reason, UUID createdBy) {
        this.tenantId = tenantId;
        this.leadId = leadId;
        this.previousScore = previousScore;
        this.newScore = newScore;
        this.scoreChange = newScore - previousScore;
        this.reason = reason;
        this.createdBy = createdBy;
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

    public UUID getLeadId() {
        return leadId;
    }

    public void setLeadId(UUID leadId) {
        this.leadId = leadId;
    }

    public Integer getPreviousScore() {
        return previousScore;
    }

    public void setPreviousScore(Integer previousScore) {
        this.previousScore = previousScore;
    }

    public Integer getNewScore() {
        return newScore;
    }

    public void setNewScore(Integer newScore) {
        this.newScore = newScore;
    }

    public Integer getScoreChange() {
        return scoreChange;
    }

    public void setScoreChange(Integer scoreChange) {
        this.scoreChange = scoreChange;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleCategory() {
        return ruleCategory;
    }

    public void setRuleCategory(String ruleCategory) {
        this.ruleCategory = ruleCategory;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    // Helper methods
    public boolean isPositiveChange() {
        return scoreChange > 0;
    }

    public boolean isNegativeChange() {
        return scoreChange < 0;
    }

    public boolean isSignificantChange() {
        return Math.abs(scoreChange) >= 10;
    }
}