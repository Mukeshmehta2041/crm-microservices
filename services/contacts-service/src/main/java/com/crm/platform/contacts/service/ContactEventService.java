package com.crm.platform.contacts.service;

import com.crm.platform.contacts.entity.Contact;
import com.crm.platform.contacts.event.ContactEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ContactEventService {

    private static final Logger logger = LoggerFactory.getLogger(ContactEventService.class);
    private static final String CONTACT_EVENTS_TOPIC = "contact-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ContactEventService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishContactCreated(Contact contact) {
        ContactEvent event = new ContactEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("CONTACT_CREATED");
        event.setContactId(contact.getId());
        event.setTenantId(contact.getTenantId());
        event.setUserId(contact.getCreatedBy());
        event.setTimestamp(LocalDateTime.now());
        event.setContactData(contact);

        publishEvent(event);
        logger.info("Published CONTACT_CREATED event for contact: {}", contact.getId());
    }

    public void publishContactUpdated(Contact oldContact, Contact newContact) {
        ContactEvent event = new ContactEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("CONTACT_UPDATED");
        event.setContactId(newContact.getId());
        event.setTenantId(newContact.getTenantId());
        event.setUserId(newContact.getUpdatedBy());
        event.setTimestamp(LocalDateTime.now());
        event.setContactData(newContact);
        event.setPreviousContactData(oldContact);

        publishEvent(event);
        logger.info("Published CONTACT_UPDATED event for contact: {}", newContact.getId());
    }

    public void publishContactDeleted(Contact contact) {
        ContactEvent event = new ContactEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("CONTACT_DELETED");
        event.setContactId(contact.getId());
        event.setTenantId(contact.getTenantId());
        event.setUserId(contact.getUpdatedBy());
        event.setTimestamp(LocalDateTime.now());
        event.setContactData(contact);

        publishEvent(event);
        logger.info("Published CONTACT_DELETED event for contact: {}", contact.getId());
    }

    public void publishContactMerged(Contact mergedContact, List<Contact> duplicateContacts) {
        ContactEvent event = new ContactEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("CONTACT_MERGED");
        event.setContactId(mergedContact.getId());
        event.setTenantId(mergedContact.getTenantId());
        event.setUserId(mergedContact.getUpdatedBy());
        event.setTimestamp(LocalDateTime.now());
        event.setContactData(mergedContact);
        event.setMergedContactIds(duplicateContacts.stream().map(Contact::getId).toList());

        publishEvent(event);
        logger.info("Published CONTACT_MERGED event for contact: {} with {} duplicates", 
                   mergedContact.getId(), duplicateContacts.size());
    }

    private void publishEvent(ContactEvent event) {
        try {
            kafkaTemplate.send(CONTACT_EVENTS_TOPIC, event.getContactId().toString(), event);
        } catch (Exception e) {
            logger.error("Failed to publish contact event: {}", event.getEventType(), e);
        }
    }
}