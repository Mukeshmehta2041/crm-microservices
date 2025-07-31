package com.crm.platform.notification.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class DealEvent extends BaseEvent {
    private String action; // CREATED, UPDATED, STAGE_CHANGED, WON, LOST
    private UUID dealId;
    private String dealName;
    private BigDecimal dealValue;
    private String stage;
    private String previousStage;

    public DealEvent() {
        super();
    }

    public DealEvent(UUID tenantId, UUID userId, String action, UUID dealId, String dealName) {
        super("dealEvent", tenantId, userId, "deal-service");
        this.action = action;
        this.dealId = dealId;
        this.dealName = dealName;
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UUID getDealId() {
        return dealId;
    }

    public void setDealId(UUID dealId) {
        this.dealId = dealId;
    }

    public String getDealName() {
        return dealName;
    }

    public void setDealName(String dealName) {
        this.dealName = dealName;
    }

    public BigDecimal getDealValue() {
        return dealValue;
    }

    public void setDealValue(BigDecimal dealValue) {
        this.dealValue = dealValue;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getPreviousStage() {
        return previousStage;
    }

    public void setPreviousStage(String previousStage) {
        this.previousStage = previousStage;
    }
}