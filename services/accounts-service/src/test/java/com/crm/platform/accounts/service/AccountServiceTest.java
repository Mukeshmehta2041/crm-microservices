package com.crm.platform.accounts.service;

import com.crm.platform.accounts.dto.AccountRequest;
import com.crm.platform.accounts.dto.AccountResponse;
import com.crm.platform.accounts.dto.AccountSearchRequest;
import com.crm.platform.accounts.entity.Account;
import com.crm.platform.accounts.entity.AccountStatus;
import com.crm.platform.accounts.entity.AccountType;
import com.crm.platform.accounts.repository.AccountRepository;
import com.crm.platform.accounts.repository.AccountRelationshipRepository;
import com.crm.platform.common.exception.CrmBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountRelationshipRepository relationshipRepository;

    @Mock
    private AccountValidationService validationService;

    @Mock
    private AccountHierarchyService hierarchyService;

    @Mock
    private AccountDeduplicationService deduplicationService;

    @InjectMocks
    private AccountService accountService;

    private UUID tenantId;
    private UUID userId;
    private UUID accountId;
    private Account testAccount;
    private AccountRequest testRequest;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testAccount = new Account("Test Company", tenantId, userId, userId);
        testAccount.setId(accountId);
        testAccount.setAccountType(AccountType.CUSTOMER);
        testAccount.setIndustry("Technology");
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setUpdatedAt(LocalDateTime.now());

        testRequest = new AccountRequest();
        testRequest.setName("Test Company");
        testRequest.setAccountType(AccountType.CUSTOMER);
        testRequest.setIndustry("Technology");
        testRequest.setStatus(AccountStatus.ACTIVE);
        testRequest.setOwnerId(userId);
    }

    @Test
    void getAccount_Success() {
        // Given
        when(accountRepository.findByIdAndTenantId(accountId, tenantId))
            .thenReturn(Optional.of(testAccount));

        // When
        AccountResponse result = accountService.getAccount(accountId, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(accountId, result.getId());
        assertEquals("Test Company", result.getName());
        assertEquals(AccountType.CUSTOMER, result.getAccountType());
        assertEquals("Technology", result.getIndustry());
        assertEquals(AccountStatus.ACTIVE, result.getStatus());
        
        verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
    }

    @Test
    void getAccount_NotFound() {
        // Given
        when(accountRepository.findByIdAndTenantId(accountId, tenantId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(CrmBusinessException.class, 
            () -> accountService.getAccount(accountId, tenantId));
        
        verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
    }

    @Test
    void searchAccounts_Success() {
        // Given
        AccountSearchRequest searchRequest = new AccountSearchRequest();
        searchRequest.setName("Test");
        searchRequest.setPage(0);
        searchRequest.setSize(20);

        List<Account> accounts = Arrays.asList(testAccount);
        Page<Account> accountPage = new PageImpl<>(accounts);

        when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(accountPage);

        // When
        Page<AccountResponse> result = accountService.searchAccounts(searchRequest, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Company", result.getContent().get(0).getName());
        
        verify(accountRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void createAccount_Success() {
        // Given
        doNothing().when(validationService).validateAccountRequest(testRequest, tenantId);
        when(deduplicationService.findPotentialDuplicates(testRequest, tenantId, null))
            .thenReturn(Arrays.asList());
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        AccountResponse result = accountService.createAccount(testRequest, tenantId, userId);

        // Then
        assertNotNull(result);
        assertEquals("Test Company", result.getName());
        assertEquals(AccountType.CUSTOMER, result.getAccountType());
        
        verify(validationService).validateAccountRequest(testRequest, tenantId);
        verify(deduplicationService).findPotentialDuplicates(testRequest, tenantId, null);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_WithParent_Success() {
        // Given
        UUID parentAccountId = UUID.randomUUID();
        testRequest.setParentAccountId(parentAccountId);

        doNothing().when(validationService).validateAccountRequest(testRequest, tenantId);
        when(deduplicationService.findPotentialDuplicates(testRequest, tenantId, null))
            .thenReturn(Arrays.asList());
        doNothing().when(hierarchyService).setParentAccount(any(Account.class), eq(parentAccountId), eq(tenantId));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        AccountResponse result = accountService.createAccount(testRequest, tenantId, userId);

        // Then
        assertNotNull(result);
        verify(hierarchyService).setParentAccount(any(Account.class), eq(parentAccountId), eq(tenantId));
    }

    @Test
    void updateAccount_Success() {
        // Given
        when(accountRepository.findByIdAndTenantId(accountId, tenantId))
            .thenReturn(Optional.of(testAccount));
        doNothing().when(validationService).validateAccountRequest(testRequest, tenantId);
        when(deduplicationService.findPotentialDuplicates(testRequest, tenantId, accountId))
            .thenReturn(Arrays.asList());
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        AccountResponse result = accountService.updateAccount(accountId, testRequest, tenantId, userId);

        // Then
        assertNotNull(result);
        assertEquals("Test Company", result.getName());
        
        verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
        verify(validationService).validateAccountRequest(testRequest, tenantId);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void updateAccount_NotFound() {
        // Given
        when(accountRepository.findByIdAndTenantId(accountId, tenantId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(CrmBusinessException.class, 
            () -> accountService.updateAccount(accountId, testRequest, tenantId, userId));
        
        verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deleteAccount_Success() {
        // Given
        when(accountRepository.findByIdAndTenantId(accountId, tenantId))
            .thenReturn(Optional.of(testAccount));
        doNothing().when(relationshipRepository).deleteAllRelationshipsByAccountId(tenantId, accountId);

        // When
        accountService.deleteAccount(accountId, tenantId, userId);

        // Then
        verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
        verify(relationshipRepository).deleteAllRelationshipsByAccountId(tenantId, accountId);
        verify(accountRepository).delete(testAccount);
    }

    @Test
    void deleteAccount_WithChildren_ThrowsException() {
        // Given
        Account childAccount = new Account("Child Company", tenantId, userId, userId);
        testAccount.addChildAccount(childAccount);
        
        when(accountRepository.findByIdAndTenantId(accountId, tenantId))
            .thenReturn(Optional.of(testAccount));

        // When & Then
        assertThrows(CrmBusinessException.class, 
            () -> accountService.deleteAccount(accountId, tenantId, userId));
        
        verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void getRootAccounts_Success() {
        // Given
        List<Account> rootAccounts = Arrays.asList(testAccount);
        when(accountRepository.findByTenantIdAndParentAccountIsNull(tenantId))
            .thenReturn(rootAccounts);

        // When
        List<AccountResponse> result = accountService.getRootAccounts(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Company", result.get(0).getName());
        
        verify(accountRepository).findByTenantIdAndParentAccountIsNull(tenantId);
    }

    @Test
    void getAccountHierarchy_Success() {
        // Given
        when(accountRepository.findByIdAndTenantId(accountId, tenantId))
            .thenReturn(Optional.of(testAccount));
        when(hierarchyService.getAccountHierarchy(testAccount))
            .thenReturn(Arrays.asList(convertToResponse(testAccount)));

        // When
        List<AccountResponse> result = accountService.getAccountHierarchy(accountId, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
        verify(hierarchyService).getAccountHierarchy(testAccount);
    }

    @Test
    void mergeAccounts_Success() {
        // Given
        UUID primaryAccountId = UUID.randomUUID();
        UUID secondaryAccountId = UUID.randomUUID();
        
        Account primaryAccount = new Account("Primary Company", tenantId, userId, userId);
        primaryAccount.setId(primaryAccountId);
        
        Account secondaryAccount = new Account("Secondary Company", tenantId, userId, userId);
        secondaryAccount.setId(secondaryAccountId);

        when(accountRepository.findByIdAndTenantId(primaryAccountId, tenantId))
            .thenReturn(Optional.of(primaryAccount));
        when(accountRepository.findByIdAndTenantId(secondaryAccountId, tenantId))
            .thenReturn(Optional.of(secondaryAccount));
        when(deduplicationService.mergeAccounts(primaryAccount, secondaryAccount, userId))
            .thenReturn(convertToResponse(primaryAccount));

        // When
        AccountResponse result = accountService.mergeAccounts(primaryAccountId, secondaryAccountId, tenantId, userId);

        // Then
        assertNotNull(result);
        
        verify(accountRepository).findByIdAndTenantId(primaryAccountId, tenantId);
        verify(accountRepository).findByIdAndTenantId(secondaryAccountId, tenantId);
        verify(deduplicationService).mergeAccounts(primaryAccount, secondaryAccount, userId);
    }

    @Test
    void findPotentialDuplicates_Success() {
        // Given
        Account duplicateAccount = new Account("Similar Company", tenantId, userId, userId);
        
        when(accountRepository.findByIdAndTenantId(accountId, tenantId))
            .thenReturn(Optional.of(testAccount));
        when(deduplicationService.findPotentialDuplicates(testAccount, tenantId))
            .thenReturn(Arrays.asList(duplicateAccount));

        // When
        List<AccountResponse> result = accountService.findPotentialDuplicates(accountId, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
        verify(deduplicationService).findPotentialDuplicates(testAccount, tenantId);
    }

    @Test
    void createAccountsBulk_Success() {
        // Given
        AccountRequest request1 = new AccountRequest();
        request1.setName("Company 1");
        request1.setOwnerId(userId);
        
        AccountRequest request2 = new AccountRequest();
        request2.setName("Company 2");
        request2.setOwnerId(userId);
        
        List<AccountRequest> requests = Arrays.asList(request1, request2);

        Account account1 = new Account("Company 1", tenantId, userId, userId);
        Account account2 = new Account("Company 2", tenantId, userId, userId);

        doNothing().when(validationService).validateAccountRequest(any(AccountRequest.class), eq(tenantId));
        when(deduplicationService.findPotentialDuplicates(any(AccountRequest.class), eq(tenantId), isNull()))
            .thenReturn(Arrays.asList());
        when(accountRepository.save(any(Account.class)))
            .thenReturn(account1)
            .thenReturn(account2);

        // When
        List<AccountResponse> result = accountService.createAccountsBulk(requests, tenantId, userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        verify(validationService, times(2)).validateAccountRequest(any(AccountRequest.class), eq(tenantId));
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    private AccountResponse convertToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setTenantId(account.getTenantId());
        response.setName(account.getName());
        response.setAccountType(account.getAccountType());
        response.setIndustry(account.getIndustry());
        response.setStatus(account.getStatus());
        response.setOwnerId(account.getOwnerId());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        response.setCreatedBy(account.getCreatedBy());
        response.setUpdatedBy(account.getUpdatedBy());
        return response;
    }
}