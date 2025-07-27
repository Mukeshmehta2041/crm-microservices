package com.crm.platform.deals.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "deals", indexes = {
    @Index(name = "idx_deals_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_deals_pipeline_stage", columnList = "pipeline_id, stage_id"),
    @Index(name = "idx_deals_owner", columnList = "owner_id"),
    @Index(name = "idx_deals_account", columnList = "account_id"),
    @Index(name = "idx_deals_contact", columnList = "contact_id"),
    @Index(name = "idx_deals_amount", columnList = "tenant_id, amount"),
    @Index(name = "idx_deals_close_date", columnList = "tenant_id, expected_close_date"),
    @Index(name = "idx_deals_created_at", columnList = "tenant_id, created_at"),
    @Index(name = "idx_deals_status", columnList = "tenant_id, is_closed, is_won")
})
public class Deal {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "pipeline_id", nullable = false)
    private UUID pipelineId;

    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 13, fraction = 2)
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Size(max = 3)
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "probability", precision = 5, scale = 2)
    private BigDecimal probability;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "actual_close_date")
    private LocalDate actualCloseDate;

    @Size(max = 50)
    @Column(name = "deal_type", length = 50)
    private String dealType;

    @Size(max = 100)
    @Column(name = "lead_source", length = 100)
    private String leadSource;

    @Column(name = "next_step", columnDefinition = "TEXT")
    private String nextStep;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @Column(name = "is_won", nullable = false)
    private Boolean isWon = false;

    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private Map<String, Object> customFields;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
    public Deal() {}

    public Deal(String name, UUID pipelineId, UUID stageId, UUID ownerId, UUID tenantId) {
        this.name = name;
        this.pipelineId = pipelineId;
        this.stageId = stageId;
        this.ownerId = ownerId;
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }

    public UUID getPipelineId() { return pipelineId; }
    public void setPipelineId(UUID pipelineId) { this.pipelineId = pipelineId; }

    public UUID getStageId() { return stageId; }
    public void setStageId(UUID stageId) { this.stageId = stageId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getProbability() { return probability; }
    public void setProbability(BigDecimal probability) { this.probability = probability; }

    public LocalDate getExpectedCloseDate() { return expectedCloseDate; }
    public void setExpectedCloseDate(LocalDate expectedCloseDate) { this.expectedCloseDate = expectedCloseDate; }

    public LocalDate getActualCloseDate() { return actualCloseDate; }
    public void setActualCloseDate(LocalDate actualCloseDate) { this.actualCloseDate = actualCloseDate; }

    public String getDealType() { return dealType; }
    public void setDealType(String dealType) { this.dealType = dealType; }

    public String getLeadSource() { return leadSource; }
    public void setLeadSource(String leadSource) { this.leadSource = leadSource; }

    public String getNextStep() { return nextStep; }
    public void setNextStep(String nextStep) { this.nextStep = nextStep; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }

    public Boolean getIsWon() { return isWon; }
    public void setIsWon(Boolean isWon) { this.isWon = isWon; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public List<DealStageHistory> getStageHistory() { return stageHistory; }
    public void setStageHistory(List<DealStageHistory> stageHistory) { this.stageHistory = stageHistory; }

    // Business methods
    public boolean isOpen() {
        return !isClosed;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isWon() {
        return isWon && isClosed;
    }

    public boolean isLost() {
        return !isWon && isClosed;
    }

    public BigDecimal getWeightedAmount() {
        if (amount == null || probability == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(probability.divide(BigDecimal.valueOf(100)));
    }
}