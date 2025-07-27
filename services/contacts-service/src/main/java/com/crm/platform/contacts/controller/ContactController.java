package com.crm.platform.contacts.controller;

import com.crm.platform.contacts.dto.*;
import com.crm.platform.contacts.entity.Contact;
import com.crm.platform.contacts.service.ContactService;
import com.crm.platform.common.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponse>> createContact(
            @Valid @RequestBody ContactRequest request,
            @RequestHeader("X-User-ID") UUID userId) {
        
        ContactResponse contact = contactService.createContact(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(contact));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> getContact(@PathVariable UUID id) {
        ContactResponse contact = contactService.getContact(id);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContact(
            @PathVariable UUID id,
            @Valid @RequestBody ContactRequest request,
            @RequestHeader("X-User-ID") UUID userId) {
        
        ContactResponse contact = contactService.updateContact(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @PathVariable UUID id,
            @RequestHeader("X-User-ID") UUID userId) {
        
        contactService.deleteContact(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContactResponse>>> getContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        Page<ContactResponse> contacts = contactService.getContacts(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<ContactResponse>>> searchContacts(
            @RequestBody ContactSearchRequest searchRequest) {
        
        Page<ContactResponse> contacts = contactService.searchContacts(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkContactResponse>> bulkCreateContacts(
            @Valid @RequestBody BulkContactRequest bulkRequest,
            @RequestHeader("X-User-ID") UUID userId) {
        
        BulkContactResponse response = contactService.bulkCreateContacts(bulkRequest, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkDeleteContacts(
            @RequestBody List<UUID> contactIds,
            @RequestHeader("X-User-ID") UUID userId) {
        
        contactService.bulkDeleteContacts(contactIds, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/duplicates")
    public ResponseEntity<ApiResponse<List<Contact>>> findDuplicateContacts(@PathVariable UUID id) {
        List<Contact> duplicates = contactService.findDuplicateContacts(id);
        return ResponseEntity.ok(ApiResponse.success(duplicates));
    }

    @PostMapping("/{id}/merge")
    public ResponseEntity<ApiResponse<ContactResponse>> mergeContacts(
            @PathVariable UUID id,
            @RequestBody List<UUID> duplicateContactIds,
            @RequestHeader("X-User-ID") UUID userId) {
        
        ContactResponse mergedContact = contactService.mergeContacts(id, duplicateContactIds, userId);
        return ResponseEntity.ok(ApiResponse.success(mergedContact));
    }

    @PostMapping("/{id}/relationships")
    public ResponseEntity<ApiResponse<Void>> createContactRelationship(
            @PathVariable UUID id,
            @RequestBody ContactRelationshipRequest request,
            @RequestHeader("X-User-ID") UUID userId) {
        
        // This would be implemented in a ContactRelationshipService
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/relationships")
    public ResponseEntity<ApiResponse<List<ContactRelationshipResponse>>> getContactRelationships(
            @PathVariable UUID id) {
        
        // This would be implemented in a ContactRelationshipService
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }
}

// Additional DTOs for relationships
class ContactRelationshipRequest {
    private UUID relatedContactId;
    private String relationshipType;
    private String description;
    private Boolean isPrimary;

    // Getters and setters
    public UUID getRelatedContactId() { return relatedContactId; }
    public void setRelatedContactId(UUID relatedContactId) { this.relatedContactId = relatedContactId; }
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
}

class ContactRelationshipResponse {
    private UUID id;
    private UUID contactId;
    private UUID relatedContactId;
    private String relationshipType;
    private String description;
    private Boolean isPrimary;
    private ContactResponse relatedContact;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }
    public UUID getRelatedContactId() { return relatedContactId; }
    public void setRelatedContactId(UUID relatedContactId) { this.relatedContactId = relatedContactId; }
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    public ContactResponse getRelatedContact() { return relatedContact; }
    public void setRelatedContact(ContactResponse relatedContact) { this.relatedContact = relatedContact; }
}