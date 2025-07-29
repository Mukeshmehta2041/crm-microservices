package com.crm.platform.pipelines.specification;

import com.crm.platform.pipelines.dto.PipelineSearchRequest;
import com.crm.platform.pipelines.entity.Pipeline;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PipelineSpecification {

    public static Specification<Pipeline> buildSpecification(UUID tenantId, PipelineSearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by tenant
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            // Filter by name (case-insensitive partial match)
            if (searchRequest.getName() != null && !searchRequest.getName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + searchRequest.getName().trim().toLowerCase() + "%"
                ));
            }

            // Filter by description (case-insensitive partial match)
            if (searchRequest.getDescription() != null && !searchRequest.getDescription().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")),
                        "%" + searchRequest.getDescription().trim().toLowerCase() + "%"
                ));
            }

            // Filter by active status
            if (searchRequest.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), searchRequest.getIsActive()));
            }

            // Filter by default status
            if (searchRequest.getIsDefault() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDefault"), searchRequest.getIsDefault()));
            }

            // Filter by template ID
            if (searchRequest.getTemplateId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("templateId"), searchRequest.getTemplateId()));
            }

            // Filter by created by
            if (searchRequest.getCreatedBy() != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdBy"), searchRequest.getCreatedBy()));
            }

            // Filter by created date range
            if (searchRequest.getCreatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), searchRequest.getCreatedAfter()));
            }
            if (searchRequest.getCreatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), searchRequest.getCreatedBefore()));
            }

            // Filter by updated date range
            if (searchRequest.getUpdatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), searchRequest.getUpdatedAfter()));
            }
            if (searchRequest.getUpdatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), searchRequest.getUpdatedBefore()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}