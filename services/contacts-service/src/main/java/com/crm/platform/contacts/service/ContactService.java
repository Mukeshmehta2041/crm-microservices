package com.crm.platform.contacts.service;

import com.crm.platform.contacts.dto.*;
import com.crm.platform.contacts.entity.Contact;
import com.crm.platform.contacts.entity.ContactStatus;
import com.crm.platform.contacts.event.ContactEvent;
import com.crm.platform.contacts.exception.ContactBusinessException;
import com.crm.platform.contacts.repository.ContactRepository;
import com.crm.platform.contacts.specification.ContactSpecification;
import com.crm.platform.common.util.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    private final ContactRepository contactRepository;
    private final ContactDeduplicationService deduplicationService;
    private final ContactValidationService validationService;
    private final ContactEventService eventService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ContactService(ContactRepository contactRepository,
                         ContactDeduplicationService deduplicationService,
                         ContactValidationService validationService,
                         ContactEventService eventService,
                         KafkaTemplate<String, Object> kafkaTemplate) {
        this.contactRepository = contactRepository;
        this.deduplicationService = deduplicationService;
        this.validationService = validationService;
        this.eventService = eventService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public ContactResponse createContact(ContactRequest request, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        logger.info("Creating contact for tenant: {}, user: {}", tenantId, userId);

        // Validate request
        validationService.validateContactRequest(request, tenantId);

        // Check for duplicates
        if (deduplicationService.isDuplicateContact(request, tenantId)) {
            throw new ContactBusinessException("DUPLICATE_CONTACT", "Contact already exists");
        }

        // Create contact entity
        Contact contact = mapToEntity(request, tenantId, userId);
        contact = contactRepository.save(contact);

        // Publish event
        eventService.publishContactCreated(contact);

        logger.info("Contact created successfully: {}", contact.getId());
        return mapToResponse(contact);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "contacts", key = "#tenantId + '_' + #contactId")
    public ContactResponse getContact(UUID contactId) {
        UUID tenantId = TenantContext.getTenantId();
        Contact contact = contactRepository.findByTenantIdAndId(tenantId, contactId)
            .orElseThrow(() -> new ContactBusinessException("CONTACT_NOT_FOUND", "Contact not found"));
        
        return mapToResponse(contact);
    }

    @Transactional
    @CacheEvict(value = "contacts", key = "#tenantId + '_' + #contactId")
    public ContactResponse updateContact(UUID contactId, ContactRequest request, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        logger.info("Updating contact: {} for tenant: {}", contactId, tenantId);

        Contact existingContact = contactRepository.findByTenantIdAndId(tenantId, contactId)
            .orElseThrow(() -> new ContactBusinessException("CONTACT_NOT_FOUND", "Contact not found"));

        // Validate request
        validationService.validateContactRequest(request, tenantId);

        // Check for duplicates (excluding current contact)
        if (deduplicationService.isDuplicateContact(request, tenantId, contactId)) {
            throw new ContactBusinessException("DUPLICATE_CONTACT", "Contact already exists");
        }

        // Store old values for event
        Contact oldContact = new Contact();
        BeanUtils.copyProperties(existingContact, oldContact);

        // Update contact
        updateContactFromRequest(existingContact, request, userId);
        Contact updatedContact = contactRepository.save(existingContact);

        // Publish event
        eventService.publishContactUpdated(oldContact, updatedContact);

        logger.info("Contact updated successfully: {}", contactId);
        return mapToResponse(updatedContact);
    }

    @Transactional
    @CacheEvict(value = "contacts", key = "#tenantId + '_' + #contactId")
    public void deleteContact(UUID contactId, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        logger.info("Deleting contact: {} for tenant: {}", contactId, tenantId);

        Contact contact = contactRepository.findByTenantIdAndId(tenantId, contactId)
            .orElseThrow(() -> new ContactBusinessException("CONTACT_NOT_FOUND", "Contact not found"));

        contactRepository.delete(contact);

        // Publish event
        eventService.publishContactDeleted(contact);

        logger.info("Contact deleted successfully: {}", contactId);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> searchContacts(ContactSearchRequest searchRequest) {
        UUID tenantId = TenantContext.getTenantId();
        
        // Build specification
        Specification<Contact> spec = ContactSpecification.buildSpecification(searchRequest, tenantId);
        
        // Build pageable
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(searchRequest.getSortDirection()) ? 
                Sort.Direction.DESC : Sort.Direction.ASC,
            searchRequest.getSortBy()
        );
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
        
        // Execute search
        Page<Contact> contacts = contactRepository.findAll(spec, pageable);
        
        return contacts.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> getContacts(int page, int size, String sortBy, String sortDirection) {
        UUID tenantId = TenantContext.getTenantId();
        
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Contact> contacts = contactRepository.findByTenantId(tenantId, pageable);
        return contacts.map(this::mapToResponse);
    }

    @Transactional
    public BulkContactResponse bulkCreateContacts(BulkContactRequest bulkRequest, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        UUID batchId = bulkRequest.getBatchId() != null ? bulkRequest.getBatchId() : UUID.randomUUID();
        
        logger.info("Starting bulk contact creation for tenant: {}, batch: {}, records: {}", 
                   tenantId, batchId, bulkRequest.getContacts().size());

        BulkContactResponse response = new BulkContactResponse();
        response.setBatchId(batchId);
        response.setTotalRecords(bulkRequest.getContacts().size());
        response.setStatus("PROCESSING");
        
        List<BulkContactResponse.BulkOperationError> errors = new ArrayList<>();
        int successCount = 0;
        int duplicateCount = 0;

        for (int i = 0; i < bulkRequest.getContacts().size(); i++) {
            ContactRequest contactRequest = bulkRequest.getContacts().get(i);
            
            try {
                // Validate
                validationService.validateContactRequest(contactRequest, tenantId);
                
                // Check duplicates
                if (deduplicationService.isDuplicateContact(contactRequest, tenantId)) {
                    if (bulkRequest.getSkipDuplicates()) {
                        duplicateCount++;
                        continue;
                    } else if (bulkRequest.getUpdateExisting()) {
                        // Find and update existing contact
                        updateExistingContact(contactRequest, tenantId, userId);
                        successCount++;
                        continue;
                    } else {
                        throw new ContactBusinessException("DUPLICATE_CONTACT", "Contact already exists");
                    }
                }
                
                if (!bulkRequest.getValidateOnly()) {
                    // Create contact
                    Contact contact = mapToEntity(contactRequest, tenantId, userId);
                    contact = contactRepository.save(contact);
                    
                    // Publish event
                    eventService.publishContactCreated(contact);
                }
                
                successCount++;
                
            } catch (Exception e) {
                BulkContactResponse.BulkOperationError error = new BulkContactResponse.BulkOperationError();
                error.setRecordIndex(i);
                error.setErrorCode("VALIDATION_ERROR");
                error.setErrorMessage(e.getMessage());
                errors.add(error);
            }
        }

        response.setSuccessCount(successCount);
        response.setErrorCount(errors.size());
        response.setDuplicateCount(duplicateCount);
        response.setErrors(errors);
        response.setStatus("COMPLETED");
        response.setProgressPercentage(100);

        logger.info("Bulk contact creation completed for batch: {}, success: {}, errors: {}, duplicates: {}", 
                   batchId, successCount, errors.size(), duplicateCount);

        return response;
    }

    @Transactional
    public void bulkDeleteContacts(List<UUID> contactIds, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        logger.info("Bulk deleting contacts for tenant: {}, count: {}", tenantId, contactIds.size());

        List<Contact> contacts = contactRepository.findByTenantIdAndIdIn(tenantId, contactIds);
        
        if (contacts.size() != contactIds.size()) {
            throw new ContactBusinessException("CONTACTS_NOT_FOUND", "Some contacts not found");
        }

        contactRepository.deleteByTenantIdAndIdIn(tenantId, contactIds);

        // Publish events
        contacts.forEach(eventService::publishContactDeleted);

        logger.info("Bulk delete completed for {} contacts", contacts.size());
    }

    @Transactional(readOnly = true)
    public List<Contact> findDuplicateContacts(UUID contactId) {
        UUID tenantId = TenantContext.getTenantId();
        Contact contact = contactRepository.findByTenantIdAndId(tenantId, contactId)
            .orElseThrow(() -> new ContactBusinessException("CONTACT_NOT_FOUND", "Contact not found"));
        
        return deduplicationService.findDuplicates(contact, tenantId);
    }

    @Transactional
    public ContactResponse mergeContacts(UUID primaryContactId, List<UUID> duplicateContactIds, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        logger.info("Merging contacts - primary: {}, duplicates: {}", primaryContactId, duplicateContactIds);

        Contact primaryContact = contactRepository.findByTenantIdAndId(tenantId, primaryContactId)
            .orElseThrow(() -> new ContactBusinessException("CONTACT_NOT_FOUND", "Primary contact not found"));

        List<Contact> duplicateContacts = contactRepository.findByTenantIdAndIdIn(tenantId, duplicateContactIds);
        
        if (duplicateContacts.size() != duplicateContactIds.size()) {
            throw new ContactBusinessException("CONTACTS_NOT_FOUND", "Some duplicate contacts not found");
        }

        // Merge logic
        Contact mergedContact = deduplicationService.mergeContacts(primaryContact, duplicateContacts, userId);
        mergedContact = contactRepository.save(mergedContact);

        // Delete duplicates
        contactRepository.deleteAll(duplicateContacts);

        // Publish events
        eventService.publishContactMerged(mergedContact, duplicateContacts);

        logger.info("Contact merge completed - result: {}", mergedContact.getId());
        return mapToResponse(mergedContact);
    }

    private Contact mapToEntity(ContactRequest request, UUID tenantId, UUID userId) {
        Contact contact = new Contact();
        contact.setTenantId(tenantId);
        contact.setAccountId(request.getAccountId());
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setMobile(request.getMobile());
        contact.setTitle(request.getTitle());
        contact.setDepartment(request.getDepartment());
        contact.setMailingAddress(request.getMailingAddress());
        contact.setSocialProfiles(request.getSocialProfiles());
        contact.setLeadSource(request.getLeadSource());
        contact.setContactStatus(request.getContactStatus() != null ? request.getContactStatus() : ContactStatus.ACTIVE);
        contact.setLeadScore(request.getLeadScore() != null ? request.getLeadScore() : 0);
        contact.setDoNotCall(request.getDoNotCall() != null ? request.getDoNotCall() : false);
        contact.setDoNotEmail(request.getDoNotEmail() != null ? request.getDoNotEmail() : false);
        contact.setEmailOptOut(request.getEmailOptOut() != null ? request.getEmailOptOut() : false);
        contact.setPreferredContactMethod(request.getPreferredContactMethod());
        contact.setTimezone(request.getTimezone());
        contact.setLanguage(request.getLanguage() != null ? request.getLanguage() : "en-US");
        contact.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
        contact.setNotes(request.getNotes());
        contact.setCustomFields(request.getCustomFields());
        contact.setOwnerId(request.getOwnerId() != null ? request.getOwnerId() : userId);
        contact.setCreatedBy(userId);
        contact.setUpdatedBy(userId);
        
        return contact;
    }

    private void updateContactFromRequest(Contact contact, ContactRequest request, UUID userId) {
        if (request.getAccountId() != null) contact.setAccountId(request.getAccountId());
        if (request.getFirstName() != null) contact.setFirstName(request.getFirstName());
        if (request.getLastName() != null) contact.setLastName(request.getLastName());
        if (request.getEmail() != null) contact.setEmail(request.getEmail());
        if (request.getPhone() != null) contact.setPhone(request.getPhone());
        if (request.getMobile() != null) contact.setMobile(request.getMobile());
        if (request.getTitle() != null) contact.setTitle(request.getTitle());
        if (request.getDepartment() != null) contact.setDepartment(request.getDepartment());
        if (request.getMailingAddress() != null) contact.setMailingAddress(request.getMailingAddress());
        if (request.getSocialProfiles() != null) contact.setSocialProfiles(request.getSocialProfiles());
        if (request.getLeadSource() != null) contact.setLeadSource(request.getLeadSource());
        if (request.getContactStatus() != null) contact.setContactStatus(request.getContactStatus());
        if (request.getLeadScore() != null) contact.setLeadScore(request.getLeadScore());
        if (request.getDoNotCall() != null) contact.setDoNotCall(request.getDoNotCall());
        if (request.getDoNotEmail() != null) contact.setDoNotEmail(request.getDoNotEmail());
        if (request.getEmailOptOut() != null) contact.setEmailOptOut(request.getEmailOptOut());
        if (request.getPreferredContactMethod() != null) contact.setPreferredContactMethod(request.getPreferredContactMethod());
        if (request.getTimezone() != null) contact.setTimezone(request.getTimezone());
        if (request.getLanguage() != null) contact.setLanguage(request.getLanguage());
        if (request.getTags() != null) contact.setTags(request.getTags());
        if (request.getNotes() != null) contact.setNotes(request.getNotes());
        if (request.getCustomFields() != null) contact.setCustomFields(request.getCustomFields());
        if (request.getOwnerId() != null) contact.setOwnerId(request.getOwnerId());
        
        contact.setUpdatedBy(userId);
    }

    private void updateExistingContact(ContactRequest request, UUID tenantId, UUID userId) {
        // Find existing contact by email or other criteria
        Optional<Contact> existingContact = contactRepository.findByTenantIdAndEmail(tenantId, request.getEmail());
        
        if (existingContact.isPresent()) {
            updateContactFromRequest(existingContact.get(), request, userId);
            contactRepository.save(existingContact.get());
            eventService.publishContactUpdated(existingContact.get(), existingContact.get());
        }
    }

    private ContactResponse mapToResponse(Contact contact) {
        ContactResponse response = new ContactResponse();
        BeanUtils.copyProperties(contact, response);
        response.setFullName(contact.getFullName());
        response.setDisplayName(contact.getDisplayName());
        return response;
    }
}