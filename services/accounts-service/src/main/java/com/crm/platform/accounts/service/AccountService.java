package com.crm.platform.accounts.service;

import com.crm.platform.accounts.dto.AccountRequest;
import com.crm.platform.accounts.dto.AccountResponse;
import com.crm.platform.accounts.dto.AccountSearchRequest;
import com.crm.platform.accounts.entity.Account;
import com.crm.platform.accounts.entity.AccountRelationship;
import com.crm.platform.accounts.entity.RelationshipType;
import com.crm.platform.accounts.repository.AccountRepository;
import com.crm.platform.accounts.repository.AccountRelationshipRepository;
import com.crm.platform.accounts.specification.AccountSpecification;
import com.crm.platform.accounts.exception.AccountBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountRelationshipRepository relationshipRepository;
    private final AccountValidationService validationService;
    private final AccountHierarchyService hierarchyService;
    private final AccountDeduplicationService deduplicationService;

    @Autowired
    public AccountService(AccountRepository accountRepository,
                         AccountRelationshipRepository relationshipRepository,
                         AccountValidationService validationService,
                         AccountHierarchyService hierarchyService,
                         AccountDeduplicationService deduplicationService) {
        this.accountRepository = accountRepository;
        this.relationshipRepository = relationshipRepository;
        this.validationService = validationService;
        this.hierarchyService = hierarchyService;
        this.deduplicationService = deduplicationService;
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId, UUID tenantId) {
        logger.debug("Retrieving account with ID: {} for tenant: {}", accountId, tenantId);
        
        Account account = accountRepository.findByIdAndTenantId(accountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        return convertToResponse(account);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> searchAccounts(AccountSearchRequest searchRequest, UUID tenantId) {
        logger.debug("Searching accounts for tenant: {} with criteria: {}", tenantId, searchRequest);
        
        Specification<Account> spec = AccountSpecification.buildSpecification(searchRequest, tenantId);
        
        Pageable pageable = createPageable(searchRequest);
        
        Page<Account> accounts = accountRepository.findAll(spec, pageable);
        
        return accounts.map(this::convertToResponse);
    }

    public AccountResponse createAccount(AccountRequest request, UUID tenantId, UUID userId) {
        logger.debug("Creating new account for tenant: {}", tenantId);
        
        // Validate the request
        validationService.validateAccountRequest(request, tenantId);
        
        // Check for duplicates
        List<Account> duplicates = deduplicationService.findPotentialDuplicates(request, tenantId, null);
        if (!duplicates.isEmpty()) {
            logger.warn("Potential duplicate accounts found for: {}", request.getName());
            // Could throw exception or return warning - for now we'll proceed
        }
        
        // Create the account
        Account account = new Account(request.getName(), tenantId, 
                                    request.getOwnerId() != null ? request.getOwnerId() : userId, 
                                    userId);
        
        mapRequestToEntity(request, account);
        account.setUpdatedBy(userId);
        
        // Handle hierarchy if parent is specified
        if (request.getParentAccountId() != null) {
            hierarchyService.setParentAccount(account, request.getParentAccountId(), tenantId);
        }
        
        Account savedAccount = accountRepository.save(account);
        
        logger.info("Created account with ID: {} for tenant: {}", savedAccount.getId(), tenantId);
        
        return convertToResponse(savedAccount);
    }

    public AccountResponse updateAccount(UUID accountId, AccountRequest request, UUID tenantId, UUID userId) {
        logger.debug("Updating account with ID: {} for tenant: {}", accountId, tenantId);
        
        Account account = accountRepository.findByIdAndTenantId(accountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        // Validate the request
        validationService.validateAccountRequest(request, tenantId);
        
        // Check for duplicates (excluding current account)
        List<Account> duplicates = deduplicationService.findPotentialDuplicates(request, tenantId, accountId);
        if (!duplicates.isEmpty()) {
            logger.warn("Potential duplicate accounts found for: {}", request.getName());
        }
        
        // Handle hierarchy changes
        if (request.getParentAccountId() != null && 
            !request.getParentAccountId().equals(account.getParentAccount() != null ? account.getParentAccount().getId() : null)) {
            hierarchyService.changeParentAccount(account, request.getParentAccountId(), tenantId);
        } else if (request.getParentAccountId() == null && account.getParentAccount() != null) {
            hierarchyService.removeFromHierarchy(account);
        }
        
        mapRequestToEntity(request, account);
        account.setUpdatedBy(userId);
        
        Account savedAccount = accountRepository.save(account);
        
        logger.info("Updated account with ID: {} for tenant: {}", accountId, tenantId);
        
        return convertToResponse(savedAccount);
    }

    public void deleteAccount(UUID accountId, UUID tenantId, UUID userId) {
        logger.debug("Deleting account with ID: {} for tenant: {}", accountId, tenantId);
        
        Account account = accountRepository.findByIdAndTenantId(accountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        // Check if account has children
        if (account.hasChildren()) {
            throw new AccountBusinessException("ACCOUNT_HAS_CHILDREN", "Cannot delete account with child accounts");
        }
        
        // Remove from hierarchy
        if (account.getParentAccount() != null) {
            hierarchyService.removeFromHierarchy(account);
        }
        
        // Delete all relationships
        relationshipRepository.deleteAllRelationshipsByAccountId(tenantId, accountId);
        
        accountRepository.delete(account);
        
        logger.info("Deleted account with ID: {} for tenant: {}", accountId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountHierarchy(UUID rootAccountId, UUID tenantId) {
        logger.debug("Retrieving account hierarchy for root account: {} and tenant: {}", rootAccountId, tenantId);
        
        Account rootAccount = accountRepository.findByIdAndTenantId(rootAccountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        return hierarchyService.getAccountHierarchy(rootAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getRootAccounts(UUID tenantId) {
        logger.debug("Retrieving root accounts for tenant: {}", tenantId);
        
        List<Account> rootAccounts = accountRepository.findByTenantIdAndParentAccountIsNull(tenantId);
        
        return rootAccounts.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public AccountResponse mergeAccounts(UUID primaryAccountId, UUID secondaryAccountId, UUID tenantId, UUID userId) {
        logger.debug("Merging accounts: {} <- {} for tenant: {}", primaryAccountId, secondaryAccountId, tenantId);
        
        Account primaryAccount = accountRepository.findByIdAndTenantId(primaryAccountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "Primary account not found"));
        
        Account secondaryAccount = accountRepository.findByIdAndTenantId(secondaryAccountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "Secondary account not found"));
        
        return deduplicationService.mergeAccounts(primaryAccount, secondaryAccount, userId);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findPotentialDuplicates(UUID accountId, UUID tenantId) {
        logger.debug("Finding potential duplicates for account: {} and tenant: {}", accountId, tenantId);
        
        Account account = accountRepository.findByIdAndTenantId(accountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        List<Account> duplicates = deduplicationService.findPotentialDuplicates(account, tenantId);
        
        return duplicates.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    // Relationship management methods
    public void createRelationship(UUID fromAccountId, UUID toAccountId, RelationshipType relationshipType, 
                                 String description, UUID tenantId, UUID userId) {
        logger.debug("Creating relationship: {} -> {} ({}) for tenant: {}", 
                    fromAccountId, toAccountId, relationshipType, tenantId);
        
        Account fromAccount = accountRepository.findByIdAndTenantId(fromAccountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "From account not found"));
        
        Account toAccount = accountRepository.findByIdAndTenantId(toAccountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "To account not found"));
        
        // Check if relationship already exists
        if (relationshipRepository.existsByTenantIdAndFromAccountIdAndToAccountIdAndRelationshipType(
                tenantId, fromAccountId, toAccountId, relationshipType)) {
            throw new AccountBusinessException("RELATIONSHIP_EXISTS", "Relationship already exists");
        }
        
        AccountRelationship relationship = new AccountRelationship(fromAccount, toAccount, relationshipType, tenantId, userId);
        relationship.setDescription(description);
        
        relationshipRepository.save(relationship);
        
        logger.info("Created relationship: {} -> {} ({}) for tenant: {}", 
                   fromAccountId, toAccountId, relationshipType, tenantId);
    }

    @Transactional(readOnly = true)
    public List<AccountRelationship> getAccountRelationships(UUID accountId, UUID tenantId) {
        logger.debug("Retrieving relationships for account: {} and tenant: {}", accountId, tenantId);
        
        return relationshipRepository.findActiveRelationshipsByAccountId(tenantId, accountId);
    }

    // Bulk operations
    public List<AccountResponse> createAccountsBulk(List<AccountRequest> requests, UUID tenantId, UUID userId) {
        logger.debug("Creating {} accounts in bulk for tenant: {}", requests.size(), tenantId);
        
        List<AccountResponse> responses = new ArrayList<>();
        
        for (AccountRequest request : requests) {
            try {
                AccountResponse response = createAccount(request, tenantId, userId);
                responses.add(response);
            } catch (Exception e) {
                logger.error("Failed to create account: {}", request.getName(), e);
                // Continue with other accounts
            }
        }
        
        logger.info("Created {} out of {} accounts in bulk for tenant: {}", 
                   responses.size(), requests.size(), tenantId);
        
        return responses;
    }

    // Helper methods
    private Pageable createPageable(AccountSearchRequest searchRequest) {
        int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
        int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;
        
        Sort sort = Sort.unsorted();
        if (searchRequest.getSortBy() != null) {
            Sort.Direction direction = "desc".equalsIgnoreCase(searchRequest.getSortDirection()) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, searchRequest.getSortBy());
        }
        
        return PageRequest.of(page, size, sort);
    }

    private void mapRequestToEntity(AccountRequest request, Account account) {
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountType(request.getAccountType());
        account.setIndustry(request.getIndustry());
        account.setAnnualRevenue(request.getAnnualRevenue());
        account.setEmployeeCount(request.getEmployeeCount());
        account.setWebsite(request.getWebsite());
        account.setPhone(request.getPhone());
        account.setFax(request.getFax());
        account.setBillingAddress(request.getBillingAddress());
        account.setShippingAddress(request.getShippingAddress());
        account.setDescription(request.getDescription());
        account.setStatus(request.getStatus());
        account.setTags(request.getTags());
        account.setCustomFields(request.getCustomFields());
        account.setTerritoryId(request.getTerritoryId());
        
        if (request.getOwnerId() != null) {
            account.setOwnerId(request.getOwnerId());
        }
    }

    private AccountResponse convertToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        
        response.setId(account.getId());
        response.setTenantId(account.getTenantId());
        response.setName(account.getName());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountType(account.getAccountType());
        response.setIndustry(account.getIndustry());
        response.setAnnualRevenue(account.getAnnualRevenue());
        response.setEmployeeCount(account.getEmployeeCount());
        response.setWebsite(account.getWebsite());
        response.setPhone(account.getPhone());
        response.setFax(account.getFax());
        response.setBillingAddress(account.getBillingAddress());
        response.setShippingAddress(account.getShippingAddress());
        response.setDescription(account.getDescription());
        response.setStatus(account.getStatus());
        response.setTags(account.getTags());
        response.setCustomFields(account.getCustomFields());
        
        // Hierarchy information
        if (account.getParentAccount() != null) {
            response.setParentAccountId(account.getParentAccount().getId());
            response.setParentAccountName(account.getParentAccount().getName());
        }
        response.setHierarchyLevel(account.getHierarchyLevel());
        response.setHierarchyPath(account.getHierarchyPath());
        
        // Territory information
        response.setTerritoryId(account.getTerritoryId());
        
        // Ownership information
        response.setOwnerId(account.getOwnerId());
        
        // Audit information
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        response.setCreatedBy(account.getCreatedBy());
        response.setUpdatedBy(account.getUpdatedBy());
        
        return response;
    }
}