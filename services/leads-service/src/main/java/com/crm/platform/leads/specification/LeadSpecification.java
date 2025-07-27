package com.crm.platform.leads.specification;

import com.crm.platform.leads.dto.LeadSearchRequest;
import com.crm.platform.leads.entity.Lead;
import com.crm.platform.leads.entity.LeadStatus;
import com.crm.platform.leads.entity.QualificationStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LeadSpecification {

    /**
     * Build comprehensive specification from search request
     */
    public static Specification<Lead> buildSpecification(LeadSearchRequest searchRequest, UUID tenantId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant isolation - always required
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            // General search term
            if (StringUtils.hasText(searchRequest.getSearchTerm())) {
                String searchTerm = "%" + searchRequest.getSearchTerm().toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("company")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("mobile")), searchTerm)
                );
                predicates.add(searchPredicate);
            }

            // Specific field searches
            if (StringUtils.hasText(searchRequest.getFirstName())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("firstName")), 
                    "%" + searchRequest.getFirstName().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(searchRequest.getLastName())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("lastName")), 
                    "%" + searchRequest.getLastName().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(searchRequest.getEmail())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")), 
                    "%" + searchRequest.getEmail().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(searchRequest.getPhone())) {
                predicates.add(criteriaBuilder.like(
                    root.get("phone"), 
                    "%" + searchRequest.getPhone() + "%"
                ));
            }

            if (StringUtils.hasText(searchRequest.getCompany())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("company")), 
                    "%" + searchRequest.getCompany().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(searchRequest.getTitle())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), 
                    "%" + searchRequest.getTitle().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(searchRequest.getIndustry())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("industry")), 
                    "%" + searchRequest.getIndustry().toLowerCase() + "%"
                ));
            }

            // Lead sources filter
            if (searchRequest.getLeadSources() != null && !searchRequest.getLeadSources().isEmpty()) {
                predicates.add(root.get("leadSource").in(searchRequest.getLeadSources()));
            }

            // Status filters
            if (searchRequest.getStatuses() != null && !searchRequest.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(searchRequest.getStatuses()));
            }

            if (searchRequest.getQualificationStatuses() != null && !searchRequest.getQualificationStatuses().isEmpty()) {
                predicates.add(root.get("qualificationStatus").in(searchRequest.getQualificationStatuses()));
            }

            // Lead score range
            if (searchRequest.getMinLeadScore() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("leadScore"), searchRequest.getMinLeadScore()
                ));
            }

            if (searchRequest.getMaxLeadScore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("leadScore"), searchRequest.getMaxLeadScore()
                ));
            }

            // Annual revenue range
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

            // Employee count range
            if (searchRequest.getMinEmployees() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("numberOfEmployees"), searchRequest.getMinEmployees()
                ));
            }

            if (searchRequest.getMaxEmployees() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("numberOfEmployees"), searchRequest.getMaxEmployees()
                ));
            }

            // Budget range
            if (searchRequest.getMinBudget() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("budget"), searchRequest.getMinBudget()
                ));
            }

            if (searchRequest.getMaxBudget() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("budget"), searchRequest.getMaxBudget()
                ));
            }

            // Purchase timeframes
            if (searchRequest.getPurchaseTimeframes() != null && !searchRequest.getPurchaseTimeframes().isEmpty()) {
                predicates.add(root.get("purchaseTimeframe").in(searchRequest.getPurchaseTimeframes()));
            }

            // Decision maker filter
            if (searchRequest.getDecisionMaker() != null) {
                predicates.add(criteriaBuilder.equal(root.get("decisionMaker"), searchRequest.getDecisionMaker()));
            }

            // Conversion status
            if (searchRequest.getConverted() != null) {
                if (searchRequest.getConverted()) {
                    predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNotNull(root.get("convertedContactId")),
                        criteriaBuilder.isNotNull(root.get("convertedAccountId")),
                        criteriaBuilder.isNotNull(root.get("convertedDealId"))
                    ));
                } else {
                    predicates.add(criteriaBuilder.and(
                        criteriaBuilder.isNull(root.get("convertedContactId")),
                        criteriaBuilder.isNull(root.get("convertedAccountId")),
                        criteriaBuilder.isNull(root.get("convertedDealId"))
                    ));
                }
            }

            // Owner filters
            if (searchRequest.getOwnerIds() != null && !searchRequest.getOwnerIds().isEmpty()) {
                predicates.add(root.get("ownerId").in(searchRequest.getOwnerIds()));
            }

            // Date range filters
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

            // Last activity filters
            if (searchRequest.getLastActivityAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("lastActivityAt"), searchRequest.getLastActivityAfter()
                ));
            }

            if (searchRequest.getLastActivityBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("lastActivityAt"), searchRequest.getLastActivityBefore()
                ));
            }

            // Next follow-up filters
            if (searchRequest.getNextFollowUpAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("nextFollowUpAt"), searchRequest.getNextFollowUpAfter()
                ));
            }

            if (searchRequest.getNextFollowUpBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("nextFollowUpAt"), searchRequest.getNextFollowUpBefore()
                ));
            }

            if (searchRequest.getHasNextFollowUp() != null) {
                if (searchRequest.getHasNextFollowUp()) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("nextFollowUpAt")));
                } else {
                    predicates.add(criteriaBuilder.isNull(root.get("nextFollowUpAt")));
                }
            }

            // Overdue filter
            if (searchRequest.getOverdue() != null && searchRequest.getOverdue()) {
                predicates.add(criteriaBuilder.and(
                    criteriaBuilder.isNotNull(root.get("nextFollowUpAt")),
                    criteriaBuilder.lessThan(root.get("nextFollowUpAt"), LocalDateTime.now())
                ));
            }

            // Contact preferences
            if (searchRequest.getTimezone() != null) {
                predicates.add(criteriaBuilder.equal(root.get("timezone"), searchRequest.getTimezone()));
            }

            if (searchRequest.getLanguage() != null) {
                predicates.add(criteriaBuilder.equal(root.get("language"), searchRequest.getLanguage()));
            }

            if (searchRequest.getDoNotCall() != null) {
                predicates.add(criteriaBuilder.equal(root.get("doNotCall"), searchRequest.getDoNotCall()));
            }

            if (searchRequest.getDoNotEmail() != null) {
                predicates.add(criteriaBuilder.equal(root.get("doNotEmail"), searchRequest.getDoNotEmail()));
            }

            if (searchRequest.getEmailOptOut() != null) {
                predicates.add(criteriaBuilder.equal(root.get("emailOptOut"), searchRequest.getEmailOptOut()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification for hot leads (score >= 80)
     */
    public static Specification<Lead> hotLeads(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.greaterThanOrEqualTo(root.get("leadScore"), 80)
        );
    }

    /**
     * Specification for warm leads (score 50-79)
     */
    public static Specification<Lead> warmLeads(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.between(root.get("leadScore"), 50, 79)
        );
    }

    /**
     * Specification for cold leads (score < 50)
     */
    public static Specification<Lead> coldLeads(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.lessThan(root.get("leadScore"), 50)
        );
    }

    /**
     * Specification for qualified leads
     */
    public static Specification<Lead> qualifiedLeads(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            root.get("qualificationStatus").in(
                QualificationStatus.MARKETING_QUALIFIED,
                QualificationStatus.SALES_QUALIFIED,
                QualificationStatus.QUALIFIED
            )
        );
    }

    /**
     * Specification for overdue leads
     */
    public static Specification<Lead> overdueLeads(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.isNotNull(root.get("nextFollowUpAt")),
            criteriaBuilder.lessThan(root.get("nextFollowUpAt"), LocalDateTime.now())
        );
    }

    /**
     * Specification for converted leads
     */
    public static Specification<Lead> convertedLeads(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.or(
                criteriaBuilder.isNotNull(root.get("convertedContactId")),
                criteriaBuilder.isNotNull(root.get("convertedAccountId")),
                criteriaBuilder.isNotNull(root.get("convertedDealId"))
            )
        );
    }

    /**
     * Specification for inactive leads (no activity in specified days)
     */
    public static Specification<Lead> inactiveLeads(UUID tenantId, int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.or(
                criteriaBuilder.isNull(root.get("lastActivityAt")),
                criteriaBuilder.lessThan(root.get("lastActivityAt"), cutoffDate)
            )
        );
    }

    /**
     * Specification for leads by owner
     */
    public static Specification<Lead> byOwner(UUID tenantId, UUID ownerId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.equal(root.get("ownerId"), ownerId)
        );
    }

    /**
     * Specification for leads by status
     */
    public static Specification<Lead> byStatus(UUID tenantId, LeadStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.equal(root.get("status"), status)
        );
    }

    /**
     * Specification for leads created in date range
     */
    public static Specification<Lead> createdBetween(UUID tenantId, LocalDateTime start, LocalDateTime end) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.equal(root.get("tenantId"), tenantId),
            criteriaBuilder.between(root.get("createdAt"), start, end)
        );
    }
}