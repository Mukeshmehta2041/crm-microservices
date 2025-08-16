package com.crm.platform.notification.consumer;

import com.crm.platform.notification.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumer.class);

    @KafkaListener(topics = "user-activity", groupId = "notification-service")
    public void handleUserActivity(@Payload UserActivityEvent event, 
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 Acknowledgment acknowledgment) {
        try {
            logger.info("Received user activity event: {} from topic: {}", event.getEventId(), topic);
            logger.debug("Event details - Type: {}, User: {}, Activity: {}", 
                        event.getEventType(), event.getUserId(), event.getActivityType());
            
            // TODO: Process notification logic here
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing user activity event: {}", event.getEventId(), e);
            // Don't acknowledge on error - message will be retried
        }
    }

    @KafkaListener(topics = "contact-events", groupId = "notification-service")
    public void handleContactEvent(@Payload ContactEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 Acknowledgment acknowledgment) {
        try {
            logger.info("Received contact event: {} from topic: {}", event.getEventId(), topic);
            logger.debug("Event details - Action: {}, Contact: {}, Name: {}", 
                        event.getAction(), event.getContactId(), event.getContactName());
            
            // TODO: Process notification logic here
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing contact event: {}", event.getEventId(), e);
        }
    }

    @KafkaListener(topics = "deal-events", groupId = "notification-service")
    public void handleDealEvent(@Payload DealEvent event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                              Acknowledgment acknowledgment) {
        try {
            logger.info("Received deal event: {} from topic: {}", event.getEventId(), topic);
            logger.debug("Event details - Action: {}, Deal: {}, Name: {}", 
                        event.getAction(), event.getDealId(), event.getDealName());
            
            // TODO: Process notification logic here
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing deal event: {}", event.getEventId(), e);
        }
    }

    @KafkaListener(topics = "auth-events", groupId = "notification-service")
    public void handleAuthEvent(@Payload AuthEvent event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                              Acknowledgment acknowledgment) {
        try {
            logger.info("Received auth event: {} from topic: {}", event.getEventId(), topic);
            logger.debug("Event details - Action: {}, User: {}, IP: {}", 
                        event.getAction(), event.getUserId(), event.getIpAddress());
            
            // TODO: Process notification logic here
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing auth event: {}", event.getEventId(), e);
        }
    }

    @KafkaListener(topics = "system-alerts", groupId = "notification-service")
    public void handleSystemAlert(@Payload SystemAlertEvent event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                Acknowledgment acknowledgment) {
        try {
            logger.info("Received system alert: {} from topic: {}", event.getEventId(), topic);
            logger.debug("Alert details - Type: {}, Severity: {}, Component: {}", 
                        event.getAlertType(), event.getSeverity(), event.getComponent());
            
            // TODO: Process notification logic here
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing system alert: {}", event.getEventId(), e);
        }
    }
}