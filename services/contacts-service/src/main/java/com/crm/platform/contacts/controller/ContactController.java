package com.crm.platform.contacts.controller;

import com.crm.platform.contacts.dto.*;
import com.crm.platform.contacts.entity.Contact;
import com.crm.platform.contacts.service.ContactService;
import com.crm.platform.common.annotation.ApiVersion;
import com.crm.platform.common.controller.BaseController;
import com.crm.platform.common.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/contacts")
@ApiVersion("1")
@Tag(name = "Contacts", description = "Contact management operations with advanced features")
@Validated
public class ContactController extends BaseController<ContactResponse, UUID> {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    // Implement BaseController abstract methods
    @Override
    protected ContactResponse findById(UUID id) {
        return contactService.getContact(id);
    }

    @Override
    protected Page<ContactResponse> findAll(PageRequest pageRequest) {
        return contactService.findAllWithAdvancedFiltering(pageRequest);
    }

    @Override
    protected ContactResponse create(ContactResponse entity) {
        // This would need to be adapted based on your service layer
        throw new UnsupportedOperationException("Use createContact with proper request object");
    }

    @Override
    protected ContactResponse update(UUID id, ContactResponse entity) {
        // This would need to be adapted based on your service layer
        throw new UnsupportedOperationException("Use updateContact with proper request object");
    }

    @Override
    protected void delete(UUID id) {
        contactService.deleteContact(id, getCurrentUserId());
    }

    @Override
    protected BulkOperationResponse<ContactResponse> bulkOperation(BulkOperationRequest<ContactResponse> request) {
        return contactService.performBulkOperation(request, getCurrentUserId());
    }

    @Override
    protected Page<ContactResponse> search(String query, List<String> fields, PageRequest pageRequest) {
        return contactService.searchContacts(query, fields, pageRequest);
    }

    private UUID getCurrentUserId() {
        // This would typically be extracted from security context
        // For now, return a placeholder
        return UUID.randomUUID();
    }

    @PostMapping
    @Operation(summary = "Create new contact", 
               description = "Create a new contact with comprehensive validation and duplicate detection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contact created successfully",
                    content = @Content(schema = @Schema(implementation = com.crm.platform.common.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid contact data"),
            @ApiResponse(responseCode = "409", description = "Duplicate contact detected")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<ContactResponse>> createContact(
            @Parameter(description = "Contact data", required = true)
            @Valid @RequestBody ContactRequest request,
            
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId,
            
            @Parameter(description = "Skip duplicate detection")
            @RequestParam(defaultValue = "false") Boolean skipDuplicateCheck) {
        
        ContactResponse contact = contactService.createContact(request, userId, skipDuplicateCheck);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(com.crm.platform.common.dto.ApiResponse.success(contact));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID", 
               description = "Retrieve a contact with optional related data inclusion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact found"),
            @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<ContactResponse>> getContact(
            @Parameter(description = "Contact ID", required = true)
            @PathVariable UUID id,
            
            @Parameter(description = "Include related entities (account, activities, deals)")
            @RequestParam(required = false) List<String> include,
            
            @Parameter(description = "Specific fields to return")
            @RequestParam(required = false) List<String> fields) {
        
        ContactResponse contact = contactService.getContact(id, include, fields);
        return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(contact));
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
    @Operation(summary = "Get all contacts", 
               description = "Retrieve contacts with advanced filtering, sorting, and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contacts retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<List<ContactResponse>>> getContacts(
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(1000) Integer limit,
            
            @Parameter(description = "Sort criteria", example = "lastName:asc,createdAt:desc")
            @RequestParam(required = false) List<String> sort,
            
            @Parameter(description = "Filter by company")
            @RequestParam(required = false) String company,
            
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Filter by tags")
            @RequestParam(required = false) List<String> tags,
            
            @Parameter(description = "Filter by lead score range")
            @RequestParam(required = false) Integer minLeadScore,
            @RequestParam(required = false) Integer maxLeadScore,
            
            @Parameter(description = "Filter by creation date range")
            @RequestParam(required = false) String createdAfter,
            @RequestParam(required = false) String createdBefore,
            
            @Parameter(description = "Search term")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Fields to search in")
            @RequestParam(required = false) List<String> searchFields,
            
            @Parameter(description = "Include related entities")
            @RequestParam(required = false) List<String> include,
            
            @Parameter(description = "Specific fields to return")
            @RequestParam(required = false) List<String> fields) {
        
        // Build advanced page request
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setLimit(limit);
        pageRequest.setSearch(search);
        pageRequest.setSearchFields(searchFields);
        pageRequest.setFields(fields);
        pageRequest.setInclude(include);
        
        if (sort != null && !sort.isEmpty()) {
            pageRequest.setSort(SortCriteria.of(sort.toArray(new String[0])));
        }
        
        // Build filters
        List<FilterCriteria> filters = buildFilters(company, status, tags, minLeadScore, maxLeadScore, createdAfter, createdBefore);
        pageRequest.setFilters(filters);
        
        Page<ContactResponse> result = contactService.findAllWithAdvancedFiltering(pageRequest);
        
        com.crm.platform.common.dto.ApiResponse<List<ContactResponse>> response = 
                com.crm.platform.common.dto.ApiResponse.success(result.getContent());
        
        // Set pagination metadata
        com.crm.platform.common.dto.ApiResponse.PaginationMeta pagination = 
                new com.crm.platform.common.dto.ApiResponse.PaginationMeta();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotal(result.getTotalElements());
        pagination.setTotalPages(result.getTotalPages());
        pagination.setHasNext(result.hasNext());
        pagination.setHasPrev(result.hasPrevious());
        
        response.getMeta().setPagination(pagination);
        
        return ResponseEntity.ok(response);
    }
    
    private List<FilterCriteria> buildFilters(String company, String status, List<String> tags, 
                                            Integer minLeadScore, Integer maxLeadScore, 
                                            String createdAfter, String createdBefore) {
        List<FilterCriteria> filters = new ArrayList<>();
        
        if (company != null) {
            filters.add(FilterCriteria.like("company", company));
        }
        if (status != null) {
            filters.add(FilterCriteria.equals("status", status));
        }
        if (tags != null && !tags.isEmpty()) {
            filters.add(FilterCriteria.in("tags", tags.stream().map(Object.class::cast).toList()));
        }
        if (minLeadScore != null) {
            filters.add(new FilterCriteria("leadScore", FilterCriteria.FilterOperator.GREATER_THAN_OR_EQUAL, minLeadScore));
        }
        if (maxLeadScore != null) {
            filters.add(new FilterCriteria("leadScore", FilterCriteria.FilterOperator.LESS_THAN_OR_EQUAL, maxLeadScore));
        }
        if (createdAfter != null) {
            filters.add(new FilterCriteria("createdAt", FilterCriteria.FilterOperator.GREATER_THAN_OR_EQUAL, createdAfter));
        }
        if (createdBefore != null) {
            filters.add(new FilterCriteria("createdAt", FilterCriteria.FilterOperator.LESS_THAN_OR_EQUAL, createdBefore));
        }
        
        return filters;
    }

    @PostMapping("/search")
    @Operation(summary = "Advanced contact search", 
               description = "Search contacts with complex filtering criteria and full-text search")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<List<ContactResponse>>> searchContacts(
            @Parameter(description = "Advanced search request", required = true)
            @Valid @RequestBody PageRequest searchRequest) {
        
        Page<ContactResponse> result = contactService.searchContactsAdvanced(searchRequest);
        
        com.crm.platform.common.dto.ApiResponse<List<ContactResponse>> response = 
                com.crm.platform.common.dto.ApiResponse.success(result.getContent());
        
        // Set pagination metadata
        com.crm.platform.common.dto.ApiResponse.PaginationMeta pagination = 
                new com.crm.platform.common.dto.ApiResponse.PaginationMeta();
        pagination.setPage(searchRequest.getPage());
        pagination.setLimit(searchRequest.getLimit());
        pagination.setTotal(result.getTotalElements());
        pagination.setTotalPages(result.getTotalPages());
        pagination.setHasNext(result.hasNext());
        pagination.setHasPrev(result.hasPrevious());
        
        response.getMeta().setPagination(pagination);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk contact operations", 
               description = "Perform bulk create, update, or delete operations with progress tracking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk operation completed"),
            @ApiResponse(responseCode = "202", description = "Bulk operation accepted (async)"),
            @ApiResponse(responseCode = "400", description = "Invalid bulk request")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<BulkOperationResponse<ContactResponse>>> bulkContactOperation(
            @Parameter(description = "Bulk operation request", required = true)
            @Valid @RequestBody BulkOperationRequest<ContactRequest> request,
            
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId) {
        
        BulkOperationResponse<ContactResponse> response = contactService.performBulkContactOperation(request, userId);
        
        HttpStatus status = request.getAsync() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        
        return ResponseEntity.status(status)
                .body(com.crm.platform.common.dto.ApiResponse.success(response));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkDeleteContacts(
            @RequestBody List<UUID> contactIds,
            @RequestHeader("X-User-ID") UUID userId) {
        
        contactService.bulkDeleteContacts(contactIds, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/duplicates")
    @Operation(summary = "Find duplicate contacts", 
               description = "Find potential duplicate contacts based on matching criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Duplicates found"),
            @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<List<ContactResponse>>> findDuplicateContacts(
            @Parameter(description = "Contact ID", required = true)
            @PathVariable UUID id,
            
            @Parameter(description = "Matching threshold (0.0-1.0)")
            @RequestParam(defaultValue = "0.8") Double threshold,
            
            @Parameter(description = "Fields to match on")
            @RequestParam(required = false) List<String> matchFields) {
        
        List<ContactResponse> duplicates = contactService.findDuplicateContacts(id, threshold, matchFields);
        return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(duplicates));
    }

    @PostMapping("/{id}/merge")
    @Operation(summary = "Merge contacts", 
               description = "Merge duplicate contacts into a single contact record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contacts merged successfully"),
            @ApiResponse(responseCode = "404", description = "Contact not found"),
            @ApiResponse(responseCode = "400", description = "Invalid merge request")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<ContactResponse>> mergeContacts(
            @Parameter(description = "Primary contact ID", required = true)
            @PathVariable UUID id,
            
            @Parameter(description = "Merge request", required = true)
            @Valid @RequestBody ContactMergeRequest mergeRequest,
            
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId) {
        
        ContactResponse mergedContact = contactService.mergeContacts(id, mergeRequest, userId);
        return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(mergedContact));
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
    @Operation(summary = "Get contact relationships", 
               description = "Retrieve all relationships for a contact")
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<List<ContactRelationshipResponse>>> getContactRelationships(
            @Parameter(description = "Contact ID", required = true)
            @PathVariable UUID id) {
        
        List<ContactRelationshipResponse> relationships = contactService.getContactRelationships(id);
        return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(relationships));
    }
    
    // Additional advanced endpoints
    
    @GetMapping("/export")
    @Operation(summary = "Export contacts", 
               description = "Export contacts to CSV or Excel format")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid export parameters")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<ContactExportResponse>> exportContacts(
            @Parameter(description = "Export format (csv, excel)")
            @RequestParam(defaultValue = "csv") String format,
            
            @Parameter(description = "Fields to export")
            @RequestParam(required = false) List<String> fields,
            
            @Parameter(description = "Filter criteria")
            @RequestParam(required = false) String filters,
            
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId) {
        
        ContactExportResponse exportResponse = contactService.initiateExport(format, fields, filters, userId);
        return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(exportResponse));
    }
    
    @PostMapping("/import")
    @Operation(summary = "Import contacts", 
               description = "Import contacts from CSV or Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Import initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid import file")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<ContactImportResponse>> importContacts(
            @Parameter(description = "Import request", required = true)
            @Valid @RequestBody ContactImportRequest importRequest,
            
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId) {
        
        ContactImportResponse importResponse = contactService.initiateImport(importRequest, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(com.crm.platform.common.dto.ApiResponse.success(importResponse));
    }
    
    @GetMapping("/analytics")
    @Operation(summary = "Contact analytics", 
               description = "Get contact analytics and insights")
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<ContactAnalyticsResponse>> getContactAnalytics(
            @Parameter(description = "Date range start")
            @RequestParam(required = false) String startDate,
            
            @Parameter(description = "Date range end")
            @RequestParam(required = false) String endDate,
            
            @Parameter(description = "Group by field")
            @RequestParam(required = false) String groupBy) {
        
        ContactAnalyticsResponse analytics = contactService.getContactAnalytics(startDate, endDate, groupBy);
        return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(analytics));
    }
    
    @PostMapping("/{id}/enrich")
    @Operation(summary = "Enrich contact", 
               description = "Enrich contact data from external sources")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact enriched successfully"),
            @ApiResponse(responseCode = "404", description = "Contact not found"),
            @ApiResponse(responseCode = "503", description = "Enrichment service unavailable")
    })
    public ResponseEntity<com.crm.platform.common.dto.ApiResponse<ContactResponse>> enrichContact(
            @Parameter(description = "Contact ID", required = true)
            @PathVariable UUID id,
            
            @Parameter(description = "Enrichment sources")
            @RequestParam(required = false) List<String> sources,
            
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") UUID userId) {
        
        ContactResponse enrichedContact = contactService.enrichContact(id, sources, userId);
        return ResponseEntity.ok(com.crm.platform.common.dto.ApiResponse.success(enrichedContact));
    }
    
    // Async endpoint example
    @GetMapping("/{id}/activities")
    @Operation(summary = "Get contact activities", 
               description = "Retrieve activities associated with a contact")
    public CompletableFuture<ResponseEntity<com.crm.platform.common.dto.ApiResponse<List<ContactActivityResponse>>>> getContactActivities(
            @Parameter(description = "Contact ID", required = true)
            @PathVariable UUID id,
            
            @Parameter(description = "Activity types to include")
            @RequestParam(required = false) List<String> types,
            
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") Integer limit) {
        
        return contactService.getContactActivitiesAsync(id, types, page, limit)
                .thenApply(activities -> ResponseEntity.ok(
                        com.crm.platform.common.dto.ApiResponse.success(activities)));
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