package com.crm.platform.accounts.service;

import com.crm.platform.accounts.dto.AccountResponse;
import com.crm.platform.accounts.entity.Account;
import com.crm.platform.accounts.repository.AccountRepository;
import com.crm.platform.accounts.exception.AccountBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(AccountHierarchyService.class);
    private static final int MAX_HIERARCHY_DEPTH = 10;

    private final AccountRepository accountRepository;
    private final AccountValidationService validationService;

    @Autowired
    public AccountHierarchyService(AccountRepository accountRepository, 
                                  AccountValidationService validationService) {
        this.accountRepository = accountRepository;
        this.validationService = validationService;
    }

    public void setParentAccount(Account account, UUID parentAccountId, UUID tenantId) {
        logger.debug("Setting parent account {} for account {} in tenant {}", 
                    parentAccountId, account.getId(), tenantId);
        
        // Validate the hierarchy
        validationService.validateAccountHierarchy(account.getId(), parentAccountId, tenantId);
        
        Account parentAccount = accountRepository.findByIdAndTenantId(parentAccountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("PARENT_ACCOUNT_NOT_FOUND", "Parent account not found"));
        
        // Remove from current hierarchy if exists
        if (account.getParentAccount() != null) {
            removeFromHierarchy(account);
        }
        
        // Set new parent
        account.setParentAccount(parentAccount);
        account.setHierarchyLevel(parentAccount.getHierarchyLevel() + 1);
        
        // Validate hierarchy depth
        if (account.getHierarchyLevel() > MAX_HIERARCHY_DEPTH) {
            throw new AccountBusinessException("HIERARCHY_TOO_DEEP", "Maximum hierarchy depth exceeded");
        }
        
        updateHierarchyPath(account);
        
        // Update all child accounts' hierarchy information
        updateChildrenHierarchy(account);
        
        logger.info("Set parent account {} for account {} in tenant {}", 
                   parentAccountId, account.getId(), tenantId);
    }

    public void changeParentAccount(Account account, UUID newParentAccountId, UUID tenantId) {
        logger.debug("Changing parent account from {} to {} for account {} in tenant {}", 
                    account.getParentAccount() != null ? account.getParentAccount().getId() : null,
                    newParentAccountId, account.getId(), tenantId);
        
        setParentAccount(account, newParentAccountId, tenantId);
    }

    public void removeFromHierarchy(Account account) {
        logger.debug("Removing account {} from hierarchy", account.getId());
        
        if (account.getParentAccount() != null) {
            account.getParentAccount().removeChildAccount(account);
        }
        
        account.setParentAccount(null);
        account.setHierarchyLevel(0);
        updateHierarchyPath(account);
        
        // Update all child accounts' hierarchy information
        updateChildrenHierarchy(account);
        
        logger.info("Removed account {} from hierarchy", account.getId());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountHierarchy(Account rootAccount) {
        logger.debug("Getting account hierarchy for root account {}", rootAccount.getId());
        
        List<AccountResponse> hierarchy = new ArrayList<>();
        buildHierarchyResponse(rootAccount, hierarchy);
        
        return hierarchy;
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountAncestors(Account account) {
        logger.debug("Getting ancestors for account {}", account.getId());
        
        List<Account> ancestors = new ArrayList<>();
        Account current = account.getParentAccount();
        
        while (current != null) {
            ancestors.add(0, current); // Add to beginning to maintain order
            current = current.getParentAccount();
        }
        
        return ancestors;
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountDescendants(Account account) {
        logger.debug("Getting descendants for account {}", account.getId());
        
        List<Account> descendants = new ArrayList<>();
        collectDescendants(account, descendants);
        
        return descendants;
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountSiblings(Account account) {
        logger.debug("Getting siblings for account {}", account.getId());
        
        if (account.getParentAccount() == null) {
            // Root account - get other root accounts
            return accountRepository.findByTenantIdAndParentAccountIsNull(account.getTenantId())
                .stream()
                .filter(a -> !a.getId().equals(account.getId()))
                .collect(Collectors.toList());
        } else {
            // Get other children of the same parent
            return accountRepository.findByTenantIdAndParentAccount(account.getTenantId(), account.getParentAccount())
                .stream()
                .filter(a -> !a.getId().equals(account.getId()))
                .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public int getHierarchyDepth(Account rootAccount) {
        logger.debug("Calculating hierarchy depth for account {}", rootAccount.getId());
        
        return calculateMaxDepth(rootAccount, 0);
    }

    @Transactional(readOnly = true)
    public int getAccountCount(Account rootAccount) {
        logger.debug("Counting accounts in hierarchy for root account {}", rootAccount.getId());
        
        return countAccountsInHierarchy(rootAccount);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsAtLevel(UUID tenantId, int level) {
        logger.debug("Getting accounts at hierarchy level {} for tenant {}", level, tenantId);
        
        return accountRepository.findByTenantIdAndHierarchyLevelLessThanEqual(tenantId, level);
    }

    public void moveAccountToNewParent(UUID accountId, UUID newParentId, UUID tenantId) {
        logger.debug("Moving account {} to new parent {} in tenant {}", accountId, newParentId, tenantId);
        
        Account account = accountRepository.findByIdAndTenantId(accountId, tenantId)
            .orElseThrow(() -> new AccountBusinessException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        if (newParentId != null) {
            setParentAccount(account, newParentId, tenantId);
        } else {
            removeFromHierarchy(account);
        }
        
        accountRepository.save(account);
        
        logger.info("Moved account {} to new parent {} in tenant {}", accountId, newParentId, tenantId);
    }

    public void validateHierarchyIntegrity(UUID tenantId) {
        logger.debug("Validating hierarchy integrity for tenant {}", tenantId);
        
        List<Account> allAccounts = accountRepository.findByTenantId(tenantId);
        List<String> errors = new ArrayList<>();
        
        for (Account account : allAccounts) {
            // Check hierarchy path consistency
            String expectedPath = calculateHierarchyPath(account);
            if (!expectedPath.equals(account.getHierarchyPath())) {
                errors.add("Account " + account.getId() + " has incorrect hierarchy path");
            }
            
            // Check hierarchy level consistency
            int expectedLevel = calculateHierarchyLevel(account);
            if (expectedLevel != account.getHierarchyLevel()) {
                errors.add("Account " + account.getId() + " has incorrect hierarchy level");
            }
            
            // Check for circular references
            if (hasCircularReference(account)) {
                errors.add("Account " + account.getId() + " has circular reference in hierarchy");
            }
        }
        
        if (!errors.isEmpty()) {
            String errorMessage = "Hierarchy integrity issues found: " + String.join(", ", errors);
            logger.error("Hierarchy integrity validation failed for tenant {}: {}", tenantId, errorMessage);
            throw new AccountBusinessException("HIERARCHY_INTEGRITY_ERROR", errorMessage);
        }
        
        logger.info("Hierarchy integrity validation passed for tenant {}", tenantId);
    }

    // Private helper methods
    private void updateHierarchyPath(Account account) {
        if (account.getParentAccount() == null) {
            account.setHierarchyPath(account.getId().toString());
        } else {
            account.setHierarchyPath(account.getParentAccount().getHierarchyPath() + "/" + account.getId().toString());
        }
    }

    private void updateChildrenHierarchy(Account account) {
        for (Account child : account.getChildAccounts()) {
            child.setHierarchyLevel(account.getHierarchyLevel() + 1);
            updateHierarchyPath(child);
            updateChildrenHierarchy(child); // Recursive update
            accountRepository.save(child);
        }
    }

    private void buildHierarchyResponse(Account account, List<AccountResponse> hierarchy) {
        AccountResponse response = convertToHierarchyResponse(account);
        
        // Add child accounts
        List<AccountResponse.AccountHierarchyNode> children = new ArrayList<>();
        for (Account child : account.getChildAccounts()) {
            AccountResponse.AccountHierarchyNode childNode = new AccountResponse.AccountHierarchyNode(
                child.getId(), child.getName(), child.getAccountType(), child.getHierarchyLevel());
            
            // Recursively build child hierarchy
            List<AccountResponse.AccountHierarchyNode> grandChildren = new ArrayList<>();
            buildChildHierarchy(child, grandChildren);
            childNode.setChildren(grandChildren);
            
            children.add(childNode);
        }
        response.setChildAccounts(children);
        
        hierarchy.add(response);
    }

    private void buildChildHierarchy(Account account, List<AccountResponse.AccountHierarchyNode> children) {
        for (Account child : account.getChildAccounts()) {
            AccountResponse.AccountHierarchyNode childNode = new AccountResponse.AccountHierarchyNode(
                child.getId(), child.getName(), child.getAccountType(), child.getHierarchyLevel());
            
            List<AccountResponse.AccountHierarchyNode> grandChildren = new ArrayList<>();
            buildChildHierarchy(child, grandChildren);
            childNode.setChildren(grandChildren);
            
            children.add(childNode);
        }
    }

    private void collectDescendants(Account account, List<Account> descendants) {
        for (Account child : account.getChildAccounts()) {
            descendants.add(child);
            collectDescendants(child, descendants); // Recursive collection
        }
    }

    private int calculateMaxDepth(Account account, int currentDepth) {
        int maxDepth = currentDepth;
        
        for (Account child : account.getChildAccounts()) {
            int childDepth = calculateMaxDepth(child, currentDepth + 1);
            maxDepth = Math.max(maxDepth, childDepth);
        }
        
        return maxDepth;
    }

    private int countAccountsInHierarchy(Account account) {
        int count = 1; // Count the account itself
        
        for (Account child : account.getChildAccounts()) {
            count += countAccountsInHierarchy(child); // Recursive count
        }
        
        return count;
    }

    private String calculateHierarchyPath(Account account) {
        if (account.getParentAccount() == null) {
            return account.getId().toString();
        } else {
            return calculateHierarchyPath(account.getParentAccount()) + "/" + account.getId().toString();
        }
    }

    private int calculateHierarchyLevel(Account account) {
        if (account.getParentAccount() == null) {
            return 0;
        } else {
            return calculateHierarchyLevel(account.getParentAccount()) + 1;
        }
    }

    private boolean hasCircularReference(Account account) {
        Account current = account.getParentAccount();
        int depth = 0;
        
        while (current != null && depth < MAX_HIERARCHY_DEPTH) {
            if (current.getId().equals(account.getId())) {
                return true;
            }
            current = current.getParentAccount();
            depth++;
        }
        
        return depth >= MAX_HIERARCHY_DEPTH; // Assume circular if too deep
    }

    private AccountResponse convertToHierarchyResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setTenantId(account.getTenantId());
        response.setName(account.getName());
        response.setAccountType(account.getAccountType());
        response.setHierarchyLevel(account.getHierarchyLevel());
        response.setHierarchyPath(account.getHierarchyPath());
        
        if (account.getParentAccount() != null) {
            response.setParentAccountId(account.getParentAccount().getId());
            response.setParentAccountName(account.getParentAccount().getName());
        }
        
        return response;
    }
}