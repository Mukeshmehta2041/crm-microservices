package com.crm.platform.contacts.specification;

import com.crm.platform.contacts.dto.ContactSearchRequest;
import com.crm.platform.contacts.entity.Contact;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContactSpecification {

    public static Specification<Contact> buildSpecification(ContactSearchRequest searchRequest, UUID tenantId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by tenant
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            // General query search
            if (searchRequest.getQuery() != null && !searchRequest.getQuery().isEmpty()) {
                String queryPattern = "%" + searchRequest.getQuery().toLowerCase() + "%";
                Predicate queryPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), queryPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), queryPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), queryPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), queryPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("department")), queryPattern)
                );
                predicates.add(queryPredicate);
            }

            // Specific field filters
            if (searchRequest.getFirstName() != null && !searchRequest.getFirstName().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("firstName")), 
                    "%" + searchRequest.getFirstName().toLowerCase() + "%"
                ));
            }

            if (searchRequest.getLastName() != null && !searchRequest.getLastName().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("lastName")), 
                    "%" + searchRequest.getLastName().toLowerCase() + "%"
                ));
            }

            if (searchRequest.getEmail() != null && !searchRequest.getEmail().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")), 
                    "%" + searchRequest.getEmail().toLowerCase() + "%"
                ));
            }

            if (searchRequest.getPhone() != null && !searchRequest.getPhone().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("phone"), "%" + searchRequest.getPhone() + "%"));
            }

            if (searchRequest.getTitle() != null && !searchRequest.getTitle().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), 
                    "%" + searchRequest.getTitle().toLowerCase() + "%"
                ));
            }

            if (searchRequest.getDepartment() != null && !searchRequest.getDepartment().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("department")), 
                    "%" + searchRequest.getDepartment().toLowerCase() + "%"
                ));
            }

            if (searchRequest.getContactStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("contactStatus"), searchRequest.getContactStatus()));
            }

            if (searchRequest.getMinLeadScore() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("leadScore"), searchRequest.getMinLeadScore()));
            }

            if (searchRequest.getMaxLeadScore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("leadScore"), searchRequest.getMaxLeadScore()));
            }

            if (searchRequest.getLeadSource() != null && !searchRequest.getLeadSource().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("leadSource"), searchRequest.getLeadSource()));
            }

            if (searchRequest.getOwnerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("ownerId"), searchRequest.getOwnerId()));
            }

            if (searchRequest.getAccountId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("accountId"), searchRequest.getAccountId()));
            }

            // Date filters
            if (searchRequest.getCreatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), searchRequest.getCreatedAfter()));
            }

            if (searchRequest.getCreatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), searchRequest.getCreatedBefore()));
            }

            if (searchRequest.getUpdatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), searchRequest.getUpdatedAfter()));
            }

            if (searchRequest.getUpdatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), searchRequest.getUpdatedBefore()));
            }

            // Tags filter
            if (searchRequest.getTags() != null && !searchRequest.getTags().isEmpty()) {
                // This would need native query for proper array operations
                // For now, we'll use a simple contains check
                Predicate tagsPredicate = criteriaBuilder.conjunction();
                for (String tag : searchRequest.getTags()) {
                    // This is a simplified version - in production, you'd want proper array operations
                    tagsPredicate = criteriaBuilder.and(tagsPredicate,
                        criteriaBuilder.like(root.get("tags").as(String.class), "%" + tag + "%"));
                }
                predicates.add(tagsPredicate);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}