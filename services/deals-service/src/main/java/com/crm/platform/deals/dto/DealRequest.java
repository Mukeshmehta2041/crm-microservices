package com.crm.platform.deals.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class DealRequest {

    @NotBlank(message = "Deal name is required")
    @Size(max = 255, message = "Deal name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Pipeline ID is required")
    private UUID pipelineId;

    @NotNull(message = "Stage ID is required")
    private UUID stageId;

    private UUID accountId;

    private UUID contactId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer digits and 2 decimal places")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency must be 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter code")
    private String currency = "USD";

    @DecimalMin(value = "0.0", message = "Probability must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Probability must be between 0 and 100")
    @Digits(integer = 3, fraction = 2, message = "Probability must have at most 3 integer digits and 2 decimal places")
    private BigDecimal probability;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedCloseDate;

    @Size(max = 50, message = "Deal type must not exceed 50 characters")
    private String dealType;

    @Size(max = 100, message = "Lead source must not exceed 100 characters")
    private String leadSource;

    private String nextStep;

    private String description;

    private String[] tags;

    private Map<String, Object> customFields;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    // Constructors
    public DealRequest() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getPipelineId() { return pipelineId; }
    public void setPipelineId(UUID pipelineId) { this.pipelineId = pipelineId; }

    public UUID getStageId() { return stageId; }
    public void setStageId(UUID stageId) { this.stageId = stageId; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getProbability() { return probability; }
    public void setProbability(BigDecimal probability) { this.probability = probability; }

    public LocalDate getExpectedCloseDate() { return expectedCloseDate; }
    public void setExpectedCloseDate(LocalDate expectedCloseDate) { this.expectedCloseDate = expectedCloseDate; }

    public String getDealType() { return dealType; }
    public void setDealType(String dealType) { this.dealType = dealType; }

    public String getLeadSource() { return leadSource; }
    public void setLeadSource(String leadSource) { this.leadSource = leadSource; }

    public String getNextStep() { return nextStep; }
    public void setNextStep(String nextStep) { this.nextStep = nextStep; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
}