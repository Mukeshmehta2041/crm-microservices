package com.crm.platform.accounts.specification;

import com.crm.platform.accounts.dto.AccountSearchRequest;
import com.crm.platform.accounts.entity.Account;
import com.crm.platform.accounts.entity.AccountStatus;
import com.crm.platform.accounts.entity.AccountType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountSpecification {

    public static Specification<Account> buildSpecification(AccountSearchRequest searchRequest, UUID tenantId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Always filter by tenant
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            
            // Name filter
            if (StringUtils.hasText(searchRequest.getName())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + searchRequest.getName().toLowerCase() + "%"
                ));
            }
            
            // Account number filter
            if (StringUtils.hasText(searchRequest.getAccountNumber())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("accountNumber")),
                    "%" + searchRequest.getAccountNumber().toLowerCase() + "%"
                ));
            }
            
            // Account types filter
            if (searchRequest.getAccountTypes() != null && !searchRequest.getAccountTypes().isEmpty()) {
                predicates.add(root.get("accountType").in(searchRequest.getAccountTypes()));
            }
            
            // Industries filter
            if (searchRequest.getIndustries() != null && !searchRequest.getIndustries().isEmpty()) {
                predicates.add(root.get("industry").in(searchRequest.getIndustries()));
            }
            
            // Annual revenue range filter
            if (searchRequest.getMinAnnualRevenue() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("annualRevenue"), searchRequest.getMinAnnualRevenue()
                ));
            }
            if (searchRequest.getMaxAnnualRevenue() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("annualRevenue"), searchRequest.getMaxAnnualRevenue()
                ));
            }
            
            // Employee count range filter
            if (searchRequest.getMinEmployeeCount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("employeeCount"), searchRequest.getMinEmployeeCount()
                ));
            }
            if (searchRequest.getMaxEmployeeCount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("employeeCount"), searchRequest.getMaxEmployeeCount()
                ));
            }
            
            // Website filter
            if (StringUtils.hasText(searchRequest.getWebsite())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("website")),
                    "%" + searchRequest.getWebsite().toLowerCase() + "%"
                ));
            }
            
            // Phone filter
            if (StringUtils.hasText(searchRequest.getPhone())) {
                predicates.add(criteriaBuilder.like(
                    root.get("phone"),
                    "%" + searchRequest.getPhone() + "%"
                ));
            }
            
            // Status filter
            if (searchRequest.getStatuses() != null && !searchRequest.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(searchRequest.getStatuses()));
            }
            
            // Tags filter
            if (searchRequest.getTags() != null && !searchRequest.getTags().isEmpty()) {
                for (String tag : searchRequest.getTags()) {
                    predicates.add(criteriaBuilder.isTrue(
                        criteriaBuilder.function("array_contains", Boolean.class,
                            root.get("tags"), criteriaBuilder.literal(tag))
                    ));
                }
            }
            
            // Hierarchy filters
            if (searchRequest.getParentAccountId() != null) {
                if (searchRequest.getIncludeChildAccounts() != null && searchRequest.getIncludeChildAccounts()) {
                    // Include the parent and all its descendants
                    Join<Account, Account> parentJoin = root.join("parentAccount", JoinType.LEFT);
                    predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("id"), searchRequest.getParentAccountId()),
                        criteriaBuilder.like(root.get("hierarchyPath"), 
                            "%" + searchRequest.getParentAccountId().toString() + "%")
                    ));
                } else {
                    // Only direct children
                    predicates.add(criteriaBuilder.equal(
                        root.get("parentAccount").get("id"), searchRequest.getParentAccountId()
                    ));
                }
            }
            
            // Max hierarchy level filter
            if (searchRequest.getMaxHierarchyLevel() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("hierarchyLevel"), searchRequest.getMaxHierarchyLevel()
                ));
            }
            
            // Root accounts only filter
            if (searchRequest.getRootAccountsOnly() != null && searchRequest.getRootAccountsOnly()) {
                predicates.add(criteriaBuilder.isNull(root.get("parentAccount")));
            }
            
            // Territory filter
            if (searchRequest.getTerritoryIds() != null && !searchRequest.getTerritoryIds().isEmpty()) {
                predicates.add(root.get("territoryId").in(searchRequest.getTerritoryIds()));
            }
            
            // Owner filter
            if (searchRequest.getOwnerIds() != null && !searchRequest.getOwnerIds().isEmpty()) {
                predicates.add(root.get("ownerId").in(searchRequest.getOwnerIds()));
            }
            
            // Date filters
            if (searchRequest.getCreatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), searchRequest.getCreatedAfter()
                ));
            }
            if (searchRequest.getCreatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), searchRequest.getCreatedBefore()
                ));
            }
            if (searchRequest.getUpdatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("updatedAt"), searchRequest.getUpdatedAfter()
                ));
            }
            if (searchRequest.getUpdatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("updatedAt"), searchRequest.getUpdatedBefore()
                ));
            }
            
            // Relationship filter
            if (searchRequest.getRelatedAccountId() != null) {
                Subquery<UUID> relationshipSubquery = query.subquery(UUID.class);
                Root<com.crm.platform.accounts.entity.AccountRelationship> relationshipRoot = 
                    relationshipSubquery.from(com.crm.platform.accounts.entity.AccountRelationship.class);
                
                relationshipSubquery.select(relationshipRoot.get("fromAccount").get("id"));
                
                List<Predicate> relationshipPredicates = new ArrayList<>();
                relationshipPredicates.add(criteriaBuilder.equal(
                    relationshipRoot.get("tenantId"), tenantId
                ));
                relationshipPredicates.add(criteriaBuilder.or(
                    criteriaBuilder.equal(relationshipRoot.get("toAccount").get("id"), 
                                        searchRequest.getRelatedAccountId()),
                    criteriaBuilder.equal(relationshipRoot.get("fromAccount").get("id"), 
                                        searchRequest.getRelatedAccountId())
                ));
                relationshipPredicates.add(criteriaBuilder.isTrue(relationshipRoot.get("isActive")));
                
                if (StringUtils.hasText(searchRequest.getRelationshipType())) {
                    relationshipPredicates.add(criteriaBuilder.equal(
                        relationshipRoot.get("relationshipType"), 
                        com.crm.platform.accounts.entity.RelationshipType.valueOf(searchRequest.getRelationshipType())
                    ));
                }
                
                relationshipSubquery.where(relationshipPredicates.toArray(new Predicate[0]));
                
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.in(root.get("id")).value(relationshipSubquery),
                    criteriaBuilder.equal(root.get("id"), searchRequest.getRelatedAccountId())
                ));
            }
            
            // General search term filter
            if (StringUtils.hasText(searchRequest.getSearchTerm())) {
                String searchTerm = "%" + searchRequest.getSearchTerm().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("accountNumber")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("website")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("industry")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchTerm)
                ));
            }
            
            // Custom fields filter (using JSONB contains)
            if (StringUtils.hasText(searchRequest.getCustomFieldsQuery())) {
                predicates.add(criteriaBuilder.isTrue(
                    criteriaBuilder.function("jsonb_contains", Boolean.class,
                        root.get("customFields"), 
                        criteriaBuilder.literal(searchRequest.getCustomFieldsQuery()))
                ));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Account> hasName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(name)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")),
                "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Account> hasAccountType(AccountType accountType) {
        return (root, query, criteriaBuilder) -> {
            if (accountType == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("accountType"), accountType);
        };
    }

    public static Specification<Account> hasStatus(AccountStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Account> hasIndustry(String industry) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(industry)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("industry")),
                industry.toLowerCase()
            );
        };
    }

    public static Specification<Account> belongsToTenant(UUID tenantId) {
        return (root, query, criteriaBuilder) -> {
            if (tenantId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("tenantId"), tenantId);
        };
    }

    public static Specification<Account> hasOwner(UUID ownerId) {
        return (root, query, criteriaBuilder) -> {
            if (ownerId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("ownerId"), ownerId);
        };
    }

    public static Specification<Account> isRootAccount() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isNull(root.get("parentAccount"));
    }

    public static Specification<Account> hasParent(UUID parentAccountId) {
        return (root, query, criteriaBuilder) -> {
            if (parentAccountId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("parentAccount").get("id"), parentAccountId);
        };
    }

    public static Specification<Account> hasHierarchyLevel(Integer level) {
        return (root, query, criteriaBuilder) -> {
            if (level == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("hierarchyLevel"), level);
        };
    }

    public static Specification<Account> hasTag(String tag) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(tag)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.isTrue(
                criteriaBuilder.function("array_contains", Boolean.class,
                    root.get("tags"), criteriaBuilder.literal(tag))
            );
        };
    }

    public static Specification<Account> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Account> updatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), endDate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}