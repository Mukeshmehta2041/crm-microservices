package com.crm.platform.deals.service;

import com.crm.platform.deals.entity.Deal;
import com.crm.platform.deals.event.DealEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DealEventService {

    private static final Logger logger = LoggerFactory.getLogger(DealEventService.class);
    
    private static final String DEAL_EVENTS_TOPIC = "deal-events";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public DealEventService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDealCreated(Deal deal) {
        DealEvent event = createDealEvent(deal, "DEAL_CREATED");
        publishEvent(event);
        logger.info("Published DEAL_CREATED event for deal: {}", deal.getId());
    }

    public void publishDealUpdated(Deal deal) {
        DealEvent event = createDealEvent(deal, "DEAL_UPDATED");
        publishEvent(event);
        logger.info("Published DEAL_UPDATED event for deal: {}", deal.getId());
    }

    public void publishDealDeleted(Deal deal) {
        DealEvent event = createDealEvent(deal, "DEAL_DELETED");
        publishEvent(event);
        logger.info("Published DEAL_DELETED event for deal: {}", deal.getId());
    }

    public void publishDealStageChanged(Deal deal, UUID fromStageId, UUID toStageId) {
        DealEvent event = createDealEvent(deal, "DEAL_STAGE_CHANGED");
        
        Map<String, Object> stageChangeData = new HashMap<>();
        stageChangeData.put("fromStageId", fromStageId);
        stageChangeData.put("toStageId", toStageId);
        event.getEventData().put("stageChange", stageChangeData);
        
        publishEvent(event);
        logger.info("Published DEAL_STAGE_CHANGED event for deal: {} (from {} to {})", 
                   deal.getId(), fromStageId, toStageId);
    }

    public void publishDealClosed(Deal deal, boolean isWon) {
        DealEvent event = createDealEvent(deal, "DEAL_CLOSED");
        event.getEventData().put("isWon", isWon);
        publishEvent(event);
        logger.info("Published DEAL_CLOSED event for deal: {} (won: {})", deal.getId(), isWon);
    }

    public void publishDealReopened(Deal deal) {
        DealEvent event = createDealEvent(deal, "DEAL_REOPENED");
        publishEvent(event);
        logger.info("Published DEAL_REOPENED event for deal: {}", deal.getId());
    }

    private DealEvent createDealEvent(Deal deal, String eventType) {
        DealEvent event = new DealEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType(eventType);
        event.setDealId(deal.getId());
        event.setTenantId(deal.getTenantId());
        event.setPipelineId(deal.getPipelineId());
        event.setStageId(deal.getStageId());
        event.setOwnerId(deal.getOwnerId());
        event.setTimestamp(LocalDateTime.now());
        
        // Add deal data
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("dealName", deal.getName());
        eventData.put("amount", deal.getAmount());
        eventData.put("currency", deal.getCurrency());
        eventData.put("probability", deal.getProbability());
        eventData.put("isClosed", deal.getIsClosed());
        eventData.put("isWon", deal.getIsWon());
        eventData.put("expectedCloseDate", deal.getExpectedCloseDate());
        eventData.put("actualCloseDate", deal.getActualCloseDate());
        eventData.put("dealType", deal.getDealType());
        eventData.put("leadSource", deal.getLeadSource());
        eventData.put("accountId", deal.getAccountId());
        eventData.put("contactId", deal.getContactId());
        eventData.put("tags", deal.getTags());
        eventData.put("customFields", deal.getCustomFields());
        
        event.setEventData(eventData);
        
        return event;
    }

    private void publishEvent(DealEvent event) {
        try {
            kafkaTemplate.send(DEAL_EVENTS_TOPIC, event.getDealId().toString(), event)
                .addCallback(
                    result -> logger.debug("Event published successfully: {}", event.getEventType()),
                    failure -> logger.error("Failed to publish event: {}", event.getEventType(), failure)
                );
        } catch (Exception e) {
            logger.error("Error publishing deal event: {}", event.getEventType(), e);
        }
    }
}