package com.crm.platform.accounts.controller;

import com.crm.platform.accounts.dto.AccountRequest;
import com.crm.platform.accounts.dto.AccountResponse;
import com.crm.platform.accounts.dto.AccountSearchRequest;
import com.crm.platform.accounts.entity.AccountRelationship;
import com.crm.platform.accounts.entity.RelationshipType;
import com.crm.platform.accounts.service.AccountService;
import com.crm.platform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("GET /api/v1/accounts/{} for tenant: {}", id, tenantId);
        
        AccountResponse account = accountService.getAccount(id, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> searchAccounts(
            @Valid @RequestBody AccountSearchRequest searchRequest,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("POST /api/v1/accounts/search for tenant: {}", tenantId);
        
        Page<AccountResponse> accounts = accountService.searchAccounts(searchRequest, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> getAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) UUID parentAccountId,
            @RequestParam(required = false) Boolean rootAccountsOnly,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("GET /api/v1/accounts for tenant: {}", tenantId);
        
        AccountSearchRequest searchRequest = new AccountSearchRequest();
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDirection(sortDirection);
        searchRequest.setName(name);
        searchRequest.setParentAccountId(parentAccountId);
        searchRequest.setRootAccountsOnly(rootAccountsOnly);
        
        if (accountType != null) {
            try {
                searchRequest.setAccountTypes(List.of(com.crm.platform.accounts.entity.AccountType.valueOf(accountType.toUpperCase())));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid account type: {}", accountType);
            }
        }
        
        if (status != null) {
            try {
                searchRequest.setStatuses(List.of(com.crm.platform.accounts.entity.AccountStatus.valueOf(status.toUpperCase())));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid account status: {}", status);
            }
        }
        
        if (industry != null) {
            searchRequest.setIndustries(List.of(industry));
        }
        
        if (ownerId != null) {
            searchRequest.setOwnerIds(List.of(ownerId));
        }
        
        Page<AccountResponse> accounts = accountService.searchAccounts(searchRequest, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody AccountRequest request,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.debug("POST /api/v1/accounts for tenant: {}", tenantId);
        
        AccountResponse account = accountService.createAccount(request, tenantId, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(account));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable UUID id,
            @Valid @RequestBody AccountRequest request,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.debug("PUT /api/v1/accounts/{} for tenant: {}", id, tenantId);
        
        AccountResponse account = accountService.updateAccount(id, request, tenantId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAccount(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.debug("DELETE /api/v1/accounts/{} for tenant: {}", id, tenantId);
        
        accountService.deleteAccount(id, tenantId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));
    }

    // Hierarchy endpoints
    @GetMapping("/{id}/hierarchy")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountHierarchy(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("GET /api/v1/accounts/{}/hierarchy for tenant: {}", id, tenantId);
        
        List<AccountResponse> hierarchy = accountService.getAccountHierarchy(id, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(hierarchy));
    }

    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getRootAccounts(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("GET /api/v1/accounts/root for tenant: {}", tenantId);
        
        List<AccountResponse> rootAccounts = accountService.getRootAccounts(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(rootAccounts));
    }

    // Relationship endpoints
    @PostMapping("/{fromId}/relationships/{toId}")
    public ResponseEntity<ApiResponse<Void>> createRelationship(
            @PathVariable UUID fromId,
            @PathVariable UUID toId,
            @RequestParam RelationshipType relationshipType,
            @RequestParam(required = false) String description,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.debug("POST /api/v1/accounts/{}/relationships/{} for tenant: {}", 
                    fromId, toId, tenantId);
        
        accountService.createRelationship(fromId, toId, relationshipType, description, tenantId, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Relationship created successfully"));
    }

    @GetMapping("/{id}/relationships")
    public ResponseEntity<ApiResponse<List<AccountRelationship>>> getAccountRelationships(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("GET /api/v1/accounts/{}/relationships for tenant: {}", id, tenantId);
        
        List<AccountRelationship> relationships = accountService.getAccountRelationships(id, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(relationships));
    }

    // Deduplication endpoints
    @PostMapping("/{primaryId}/merge/{secondaryId}")
    public ResponseEntity<ApiResponse<AccountResponse>> mergeAccounts(
            @PathVariable UUID primaryId,
            @PathVariable UUID secondaryId,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.debug("POST /api/v1/accounts/{}/merge/{} for tenant: {}", 
                    primaryId, secondaryId, tenantId);
        
        AccountResponse mergedAccount = accountService.mergeAccounts(primaryId, secondaryId, tenantId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(mergedAccount));
    }

    @GetMapping("/{id}/duplicates")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> findPotentialDuplicates(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        
        logger.debug("GET /api/v1/accounts/{}/duplicates for tenant: {}", id, tenantId);
        
        List<AccountResponse> duplicates = accountService.findPotentialDuplicates(id, tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(duplicates));
    }

    // Bulk operations
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> createAccountsBulk(
            @Valid @RequestBody List<AccountRequest> requests,
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader("X-User-ID") UUID userId) {
        
        logger.debug("POST /api/v1/accounts/bulk for tenant: {} with {} accounts", 
                    tenantId, requests.size());
        
        List<AccountResponse> accounts = accountService.createAccountsBulk(requests, tenantId, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(accounts));
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Accounts service is healthy"));
    }
}