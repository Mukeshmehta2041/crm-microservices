package com.crm.platform.deals.dto;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class DealStageChangeRequest {

    @NotNull(message = "Stage ID is required")
    private UUID stageId;

    private String reason;

    // Constructors
    public DealStageChangeRequest() {}

    public DealStageChangeRequest(UUID stageId, String reason) {
        this.stageId = stageId;
        this.reason = reason;
    }

    // Getters and Setters
    public UUID getStageId() { return stageId; }
    public void setStageId(UUID stageId) { this.stageId = stageId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}