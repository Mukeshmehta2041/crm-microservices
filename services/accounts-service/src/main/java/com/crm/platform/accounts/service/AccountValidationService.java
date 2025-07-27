package com.crm.platform.accounts.service;

import com.crm.platform.accounts.dto.AccountRequest;
import com.crm.platform.accounts.repository.AccountRepository;
import com.crm.platform.accounts.exception.AccountBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AccountValidationService {

    private static final Logger logger = LoggerFactory.getLogger(AccountValidationService.class);
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]?[0-9]{7,15}$"
    );
    
    private static final Pattern WEBSITE_PATTERN = Pattern.compile(
        "^(https?://)?(www\\.)?[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.[a-zA-Z]{2,}$"
    );

    private final AccountRepository accountRepository;

    @Autowired
    public AccountValidationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void validateAccountRequest(AccountRequest request, UUID tenantId) {
        logger.debug("Validating account request for tenant: {}", tenantId);
        
        List<String> errors = new ArrayList<>();
        
        // Validate required fields
        validateRequiredFields(request, errors);
        
        // Validate field formats
        validateFieldFormats(request, errors);
        
        // Validate business rules
        validateBusinessRules(request, tenantId, errors);
        
        // Validate field lengths
        validateFieldLengths(request, errors);
        
        // Validate numeric fields
        validateNumericFields(request, errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = "Validation failed: " + String.join(", ", errors);
            logger.warn("Account validation failed: {}", errorMessage);
            throw new AccountBusinessException("VALIDATION_FAILED", errorMessage);
        }
        
        logger.debug("Account request validation passed for tenant: {}", tenantId);
    }

    private void validateRequiredFields(AccountRequest request, List<String> errors) {
        if (!StringUtils.hasText(request.getName())) {
            errors.add("Account name is required");
        }
    }

    private void validateFieldFormats(AccountRequest request, List<String> errors) {
        // Validate website format
        if (StringUtils.hasText(request.getWebsite()) && 
            !WEBSITE_PATTERN.matcher(request.getWebsite()).matches()) {
            errors.add("Invalid website format");
        }
        
        // Validate phone format
        if (StringUtils.hasText(request.getPhone()) && 
            !PHONE_PATTERN.matcher(request.getPhone().replaceAll("[\\s()-]", "")).matches()) {
            errors.add("Invalid phone number format");
        }
        
        // Validate fax format
        if (StringUtils.hasText(request.getFax()) && 
            !PHONE_PATTERN.matcher(request.getFax().replaceAll("[\\s()-]", "")).matches()) {
            errors.add("Invalid fax number format");
        }
    }

    private void validateBusinessRules(AccountRequest request, UUID tenantId, List<String> errors) {
        // Validate unique account number within tenant
        if (StringUtils.hasText(request.getAccountNumber())) {
            if (accountRepository.existsByTenantIdAndAccountNumber(tenantId, request.getAccountNumber())) {
                errors.add("Account number already exists");
            }
        }
        
        // Validate parent account exists and is not creating circular reference
        if (request.getParentAccountId() != null) {
            if (!accountRepository.existsById(request.getParentAccountId())) {
                errors.add("Parent account does not exist");
            }
            // Additional circular reference check would be done in hierarchy service
        }
        
        // Validate owner exists (this would typically check against users service)
        if (request.getOwnerId() != null) {
            // TODO: Validate owner exists in users service
        }
        
        // Validate territory exists (this would typically check against territory service)
        if (request.getTerritoryId() != null) {
            // TODO: Validate territory exists
        }
    }

    private void validateFieldLengths(AccountRequest request, List<String> errors) {
        if (StringUtils.hasText(request.getName()) && request.getName().length() > 255) {
            errors.add("Account name must not exceed 255 characters");
        }
        
        if (StringUtils.hasText(request.getAccountNumber()) && request.getAccountNumber().length() > 100) {
            errors.add("Account number must not exceed 100 characters");
        }
        
        if (StringUtils.hasText(request.getIndustry()) && request.getIndustry().length() > 100) {
            errors.add("Industry must not exceed 100 characters");
        }
        
        if (StringUtils.hasText(request.getWebsite()) && request.getWebsite().length() > 255) {
            errors.add("Website must not exceed 255 characters");
        }
        
        if (StringUtils.hasText(request.getPhone()) && request.getPhone().length() > 50) {
            errors.add("Phone must not exceed 50 characters");
        }
        
        if (StringUtils.hasText(request.getFax()) && request.getFax().length() > 50) {
            errors.add("Fax must not exceed 50 characters");
        }
        
        if (StringUtils.hasText(request.getDescription()) && request.getDescription().length() > 5000) {
            errors.add("Description must not exceed 5000 characters");
        }
    }

    private void validateNumericFields(AccountRequest request, List<String> errors) {
        // Validate annual revenue
        if (request.getAnnualRevenue() != null && request.getAnnualRevenue().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Annual revenue cannot be negative");
        }
        
        // Validate employee count
        if (request.getEmployeeCount() != null && request.getEmployeeCount() < 0) {
            errors.add("Employee count cannot be negative");
        }
        
        // Validate employee count reasonable upper limit
        if (request.getEmployeeCount() != null && request.getEmployeeCount() > 10000000) {
            errors.add("Employee count seems unreasonably high");
        }
    }

    public void validateAccountForDeletion(UUID accountId, UUID tenantId) {
        logger.debug("Validating account for deletion: {} for tenant: {}", accountId, tenantId);
        
        // Check if account has child accounts
        List<com.crm.platform.accounts.entity.Account> childAccounts = 
            accountRepository.findByTenantIdAndParentAccountId(tenantId, accountId);
        
        if (!childAccounts.isEmpty()) {
            throw new AccountBusinessException(
                "ACCOUNT_HAS_CHILDREN",
                "Cannot delete account with " + childAccounts.size() + " child accounts"
            );
        }
        
        // Additional business rule validations could be added here
        // For example: check if account has active deals, contacts, etc.
        
        logger.debug("Account validation for deletion passed: {}", accountId);
    }

    public void validateAccountHierarchy(UUID accountId, UUID parentAccountId, UUID tenantId) {
        logger.debug("Validating account hierarchy: {} -> {} for tenant: {}", accountId, parentAccountId, tenantId);
        
        if (accountId.equals(parentAccountId)) {
            throw new AccountBusinessException("CIRCULAR_HIERARCHY", "Account cannot be its own parent");
        }
        
        // Check for circular reference by traversing up the hierarchy
        UUID currentParentId = parentAccountId;
        int depth = 0;
        int maxDepth = 10; // Prevent infinite loops
        
        while (currentParentId != null && depth < maxDepth) {
            if (currentParentId.equals(accountId)) {
                throw new AccountBusinessException("CIRCULAR_HIERARCHY", "Circular hierarchy detected");
            }
            
            var parentAccount = accountRepository.findByIdAndTenantId(currentParentId, tenantId);
            if (parentAccount.isPresent() && parentAccount.get().getParentAccount() != null) {
                currentParentId = parentAccount.get().getParentAccount().getId();
            } else {
                currentParentId = null;
            }
            depth++;
        }
        
        if (depth >= maxDepth) {
            throw new AccountBusinessException("HIERARCHY_TOO_DEEP", "Hierarchy too deep");
        }
        
        logger.debug("Account hierarchy validation passed: {} -> {}", accountId, parentAccountId);
    }

    public void validateBulkOperation(List<AccountRequest> requests, UUID tenantId) {
        logger.debug("Validating bulk operation with {} accounts for tenant: {}", requests.size(), tenantId);
        
        if (requests == null || requests.isEmpty()) {
            throw new AccountBusinessException("EMPTY_BULK_REQUEST", "No accounts provided for bulk operation");
        }
        
        if (requests.size() > 1000) {
            throw new AccountBusinessException("BULK_LIMIT_EXCEEDED", "Bulk operation limited to 1000 accounts");
        }
        
        // Validate each request
        List<String> allErrors = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            try {
                validateAccountRequest(requests.get(i), tenantId);
            } catch (AccountBusinessException e) {
                allErrors.add("Account " + (i + 1) + ": " + e.getMessage());
            }
        }
        
        if (!allErrors.isEmpty()) {
            String errorMessage = "Bulk validation failed: " + String.join("; ", allErrors);
            throw new AccountBusinessException("BULK_VALIDATION_FAILED", errorMessage);
        }
        
        logger.debug("Bulk operation validation passed for {} accounts", requests.size());
    }
}