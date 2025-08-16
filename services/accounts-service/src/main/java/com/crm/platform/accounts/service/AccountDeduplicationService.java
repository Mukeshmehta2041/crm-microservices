package com.crm.platform.accounts.service;

import com.crm.platform.accounts.dto.AccountRequest;
import com.crm.platform.accounts.dto.AccountResponse;
import com.crm.platform.accounts.entity.Account;
import com.crm.platform.accounts.repository.AccountRepository;
import com.crm.platform.accounts.repository.AccountRelationshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccountDeduplicationService {

    private static final Logger logger = LoggerFactory.getLogger(AccountDeduplicationService.class);
    
    private static final double NAME_SIMILARITY_THRESHOLD = 0.8;
    private static final double OVERALL_SIMILARITY_THRESHOLD = 0.7;

    private final AccountRepository accountRepository;
    private final AccountRelationshipRepository relationshipRepository;

    @Autowired
    public AccountDeduplicationService(AccountRepository accountRepository,
                                     AccountRelationshipRepository relationshipRepository) {
        this.accountRepository = accountRepository;
        this.relationshipRepository = relationshipRepository;
    }

    @Transactional(readOnly = true)
    public List<Account> findPotentialDuplicates(AccountRequest request, UUID tenantId, UUID excludeId) {
        logger.debug("Finding potential duplicates for account request in tenant: {}", tenantId);
        
        List<Account> potentialDuplicates = new ArrayList<>();
        
        // Find exact matches first
        List<Account> exactMatches = accountRepository.findPotentialDuplicates(
            tenantId, request.getName(), request.getWebsite(), request.getPhone(), 
            excludeId != null ? excludeId : UUID.randomUUID());
        
        potentialDuplicates.addAll(exactMatches);
        
        // Find similar name matches
        if (StringUtils.hasText(request.getName())) {
            List<Account> nameMatches = accountRepository.findByTenantIdAndNameContainingIgnoreCase(
                tenantId, request.getName());
            
            for (Account account : nameMatches) {
                if (excludeId != null && account.getId().equals(excludeId)) {
                    continue;
                }
                
                if (!potentialDuplicates.contains(account)) {
                    double similarity = calculateSimilarity(request, account);
                    if (similarity >= OVERALL_SIMILARITY_THRESHOLD) {
                        potentialDuplicates.add(account);
                    }
                }
            }
        }
        
        logger.debug("Found {} potential duplicates for account request", potentialDuplicates.size());
        return potentialDuplicates;
    }

    @Transactional(readOnly = true)
    public List<Account> findPotentialDuplicates(Account account, UUID tenantId) {
        logger.debug("Finding potential duplicates for account: {} in tenant: {}", account.getId(), tenantId);
        
        AccountRequest request = convertToRequest(account);
        return findPotentialDuplicates(request, tenantId, account.getId());
    }

    public AccountResponse mergeAccounts(Account primaryAccount, Account secondaryAccount, UUID userId) {
        logger.debug("Merging accounts: {} <- {}", primaryAccount.getId(), secondaryAccount.getId());
        
        // Validate that accounts belong to the same tenant
        if (!primaryAccount.getTenantId().equals(secondaryAccount.getTenantId())) {
            throw new IllegalArgumentException("Cannot merge accounts from different tenants");
        }
        
        UUID tenantId = primaryAccount.getTenantId();
        
        // Merge account data
        mergeAccountData(primaryAccount, secondaryAccount, userId);
        
        // Move child accounts from secondary to primary
        moveChildAccounts(secondaryAccount, primaryAccount);
        
        // Update relationships
        updateRelationships(secondaryAccount, primaryAccount, tenantId);
        
        // Save the merged primary account
        Account mergedAccount = accountRepository.save(primaryAccount);
        
        // Delete the secondary account
        accountRepository.delete(secondaryAccount);
        
        logger.info("Successfully merged accounts: {} <- {}", primaryAccount.getId(), secondaryAccount.getId());
        
        return convertToResponse(mergedAccount);
    }

    @Transactional(readOnly = true)
    public double calculateSimilarity(AccountRequest request, Account account) {
        double totalScore = 0.0;
        int factors = 0;
        
        // Name similarity (weighted heavily)
        if (StringUtils.hasText(request.getName()) && StringUtils.hasText(account.getName())) {
            double nameScore = calculateStringSimilarity(request.getName(), account.getName());
            totalScore += nameScore * 3; // Weight name similarity heavily
            factors += 3;
        }
        
        // Website similarity
        if (StringUtils.hasText(request.getWebsite()) && StringUtils.hasText(account.getWebsite())) {
            double websiteScore = calculateStringSimilarity(request.getWebsite(), account.getWebsite());
            totalScore += websiteScore * 2;
            factors += 2;
        }
        
        // Phone similarity
        if (StringUtils.hasText(request.getPhone()) && StringUtils.hasText(account.getPhone())) {
            double phoneScore = calculatePhoneSimilarity(request.getPhone(), account.getPhone());
            totalScore += phoneScore * 2;
            factors += 2;
        }
        
        // Industry similarity
        if (StringUtils.hasText(request.getIndustry()) && StringUtils.hasText(account.getIndustry())) {
            double industryScore = request.getIndustry().equalsIgnoreCase(account.getIndustry()) ? 1.0 : 0.0;
            totalScore += industryScore;
            factors += 1;
        }
        
        // Account type similarity
        if (request.getAccountType() != null && account.getAccountType() != null) {
            double typeScore = request.getAccountType().equals(account.getAccountType()) ? 1.0 : 0.0;
            totalScore += typeScore;
            factors += 1;
        }
        
        return factors > 0 ? totalScore / factors : 0.0;
    }

    @Transactional(readOnly = true)
    public List<Account> findAllPotentialDuplicatesInTenant(UUID tenantId) {
        logger.debug("Finding all potential duplicates in tenant: {}", tenantId);
        
        List<Account> allAccounts = accountRepository.findByTenantId(tenantId);
        List<Account> duplicates = new ArrayList<>();
        
        for (int i = 0; i < allAccounts.size(); i++) {
            Account account1 = allAccounts.get(i);
            
            for (int j = i + 1; j < allAccounts.size(); j++) {
                Account account2 = allAccounts.get(j);
                
                double similarity = calculateSimilarity(convertToRequest(account1), account2);
                if (similarity >= OVERALL_SIMILARITY_THRESHOLD) {
                    if (!duplicates.contains(account1)) {
                        duplicates.add(account1);
                    }
                    if (!duplicates.contains(account2)) {
                        duplicates.add(account2);
                    }
                }
            }
        }
        
        logger.debug("Found {} accounts with potential duplicates in tenant: {}", duplicates.size(), tenantId);
        return duplicates;
    }

    // Private helper methods
    private void mergeAccountData(Account primary, Account secondary, UUID userId) {
        // Merge fields where primary is empty but secondary has data
        if (!StringUtils.hasText(primary.getAccountNumber()) && StringUtils.hasText(secondary.getAccountNumber())) {
            primary.setAccountNumber(secondary.getAccountNumber());
        }
        
        if (primary.getAccountType() == null && secondary.getAccountType() != null) {
            primary.setAccountType(secondary.getAccountType());
        }
        
        if (!StringUtils.hasText(primary.getIndustry()) && StringUtils.hasText(secondary.getIndustry())) {
            primary.setIndustry(secondary.getIndustry());
        }
        
        if (primary.getAnnualRevenue() == null && secondary.getAnnualRevenue() != null) {
            primary.setAnnualRevenue(secondary.getAnnualRevenue());
        }
        
        if (primary.getEmployeeCount() == null && secondary.getEmployeeCount() != null) {
            primary.setEmployeeCount(secondary.getEmployeeCount());
        }
        
        if (!StringUtils.hasText(primary.getWebsite()) && StringUtils.hasText(secondary.getWebsite())) {
            primary.setWebsite(secondary.getWebsite());
        }
        
        if (!StringUtils.hasText(primary.getPhone()) && StringUtils.hasText(secondary.getPhone())) {
            primary.setPhone(secondary.getPhone());
        }
        
        if (!StringUtils.hasText(primary.getFax()) && StringUtils.hasText(secondary.getFax())) {
            primary.setFax(secondary.getFax());
        }
        
        if (!StringUtils.hasText(primary.getBillingAddress()) && StringUtils.hasText(secondary.getBillingAddress())) {
            primary.setBillingAddress(secondary.getBillingAddress());
        }
        
        if (!StringUtils.hasText(primary.getShippingAddress()) && StringUtils.hasText(secondary.getShippingAddress())) {
            primary.setShippingAddress(secondary.getShippingAddress());
        }
        
        if (!StringUtils.hasText(primary.getDescription()) && StringUtils.hasText(secondary.getDescription())) {
            primary.setDescription(secondary.getDescription());
        }
        
        // Merge tags
        if (primary.getTags() == null && secondary.getTags() != null) {
            primary.setTags(secondary.getTags());
        } else if (primary.getTags() != null && secondary.getTags() != null) {
            // Merge unique tags
            List<String> mergedTags = new ArrayList<>();
            for (String tag : primary.getTags()) {
                mergedTags.add(tag);
            }
            for (String tag : secondary.getTags()) {
                if (!mergedTags.contains(tag)) {
                    mergedTags.add(tag);
                }
            }
            primary.setTags(mergedTags.toArray(new String[0]));
        }
        
        // Merge custom fields (this would need more sophisticated JSON merging)
        if (!StringUtils.hasText(primary.getCustomFields()) && StringUtils.hasText(secondary.getCustomFields())) {
            primary.setCustomFields(secondary.getCustomFields());
        }
        
        primary.setUpdatedBy(userId);
    }

    private void moveChildAccounts(Account fromAccount, Account toAccount) {
        List<Account> childAccounts = new ArrayList<>(fromAccount.getChildAccounts());
        
        for (Account child : childAccounts) {
            fromAccount.removeChildAccount(child);
            toAccount.addChildAccount(child);
            accountRepository.save(child);
        }
    }

    private void updateRelationships(Account fromAccount, Account toAccount, UUID tenantId) {
        // Update relationships where fromAccount is the source
        var outgoingRelationships = relationshipRepository.findByTenantIdAndFromAccount(tenantId, fromAccount);
        for (var relationship : outgoingRelationships) {
            relationship.setFromAccount(toAccount);
            relationshipRepository.save(relationship);
        }
        
        // Update relationships where fromAccount is the target
        var incomingRelationships = relationshipRepository.findByTenantIdAndToAccount(tenantId, fromAccount);
        for (var relationship : incomingRelationships) {
            relationship.setToAccount(toAccount);
            relationshipRepository.save(relationship);
        }
    }

    private double calculateStringSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        String s1 = str1.toLowerCase().trim();
        String s2 = str2.toLowerCase().trim();
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        // Use Levenshtein distance for similarity calculation
        int distance = calculateLevenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        return maxLength > 0 ? 1.0 - (double) distance / maxLength : 0.0;
    }

    private double calculatePhoneSimilarity(String phone1, String phone2) {
        if (phone1 == null || phone2 == null) {
            return 0.0;
        }
        
        // Normalize phone numbers by removing non-digits
        String normalized1 = phone1.replaceAll("[^0-9]", "");
        String normalized2 = phone2.replaceAll("[^0-9]", "");
        
        return normalized1.equals(normalized2) ? 1.0 : 0.0;
    }

    private int calculateLevenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[str1.length()][str2.length()];
    }

    private AccountRequest convertToRequest(Account account) {
        AccountRequest request = new AccountRequest();
        request.setName(account.getName());
        request.setAccountNumber(account.getAccountNumber());
        request.setAccountType(account.getAccountType());
        request.setIndustry(account.getIndustry());
        request.setAnnualRevenue(account.getAnnualRevenue());
        request.setEmployeeCount(account.getEmployeeCount());
        request.setWebsite(account.getWebsite());
        request.setPhone(account.getPhone());
        request.setFax(account.getFax());
        request.setBillingAddress(account.getBillingAddress());
        request.setShippingAddress(account.getShippingAddress());
        request.setDescription(account.getDescription());
        request.setStatus(account.getStatus());
        request.setTags(account.getTags());
        request.setCustomFields(account.getCustomFields());
        request.setTerritoryId(account.getTerritoryId());
        request.setOwnerId(account.getOwnerId());
        
        return request;
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
        response.setHierarchyLevel(account.getHierarchyLevel());
        response.setHierarchyPath(account.getHierarchyPath());
        response.setTerritoryId(account.getTerritoryId());
        response.setOwnerId(account.getOwnerId());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        response.setCreatedBy(account.getCreatedBy());
        response.setUpdatedBy(account.getUpdatedBy());
        
        return response;
    }
}