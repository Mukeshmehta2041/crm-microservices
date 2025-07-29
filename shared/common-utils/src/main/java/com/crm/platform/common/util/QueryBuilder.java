package com.crm.platform.common.util;

import com.crm.platform.common.dto.FilterCriteria;
import com.crm.platform.common.dto.SortCriteria;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for building JPA queries from filter and sort criteria
 */
public class QueryBuilder {
    
    /**
     * Build JPA Specification from filter criteria
     */
    public static <T> Specification<T> buildSpecification(List<FilterCriteria> filters) {
        return (root, query, criteriaBuilder) -> {
            if (filters == null || filters.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            
            List<Predicate> predicates = new ArrayList<>();
            
            for (FilterCriteria filter : filters) {
                Predicate predicate = buildPredicate(filter, root, criteriaBuilder);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Build individual predicate from filter criteria
     */
    private static <T> Predicate buildPredicate(FilterCriteria filter, Root<T> root, CriteriaBuilder cb) {
        if (filter.getField() == null || filter.getOperator() == null) {
            return null;
        }
        
        Path<Object> path = getPath(root, filter.getField());
        
        switch (filter.getOperator()) {
            case EQUALS:
                return cb.equal(path, filter.getValue());
                
            case NOT_EQUALS:
                return cb.notEqual(path, filter.getValue());
                
            case GREATER_THAN:
                return cb.greaterThan((Path<Comparable>) path, (Comparable) filter.getValue());
                
            case GREATER_THAN_OR_EQUAL:
                return cb.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) filter.getValue());
                
            case LESS_THAN:
                return cb.lessThan((Path<Comparable>) path, (Comparable) filter.getValue());
                
            case LESS_THAN_OR_EQUAL:
                return cb.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) filter.getValue());
                
            case LIKE:
                return cb.like((Path<String>) path, "%" + filter.getValue() + "%");
                
            case ILIKE:
                return cb.like(cb.lower((Path<String>) path), ("%" + filter.getValue() + "%").toLowerCase());
                
            case IN:
                return path.in(filter.getValues());
                
            case NOT_IN:
                return cb.not(path.in(filter.getValues()));
                
            case IS_NULL:
                return cb.isNull(path);
                
            case IS_NOT_NULL:
                return cb.isNotNull(path);
                
            case BETWEEN:
                if (filter.getValues() != null && filter.getValues().size() == 2) {
                    return cb.between((Path<Comparable>) path, 
                            (Comparable) filter.getValues().get(0), 
                            (Comparable) filter.getValues().get(1));
                }
                break;
                
            case CONTAINS:
                return cb.like((Path<String>) path, "%" + filter.getValue() + "%");
                
            case STARTS_WITH:
                return cb.like((Path<String>) path, filter.getValue() + "%");
                
            case ENDS_WITH:
                return cb.like((Path<String>) path, "%" + filter.getValue());
                
            case DATE_RANGE:
                if (filter.getValues() != null && filter.getValues().size() == 2) {
                    LocalDateTime start = (LocalDateTime) filter.getValues().get(0);
                    LocalDateTime end = (LocalDateTime) filter.getValues().get(1);
                    return cb.between((Path<LocalDateTime>) path, start, end);
                }
                break;
                
            case CUSTOM_FIELD:
                return buildCustomFieldPredicate(filter, root, cb);
                
            default:
                return null;
        }
        
        return null;
    }
    
    /**
     * Build predicate for custom fields stored in JSONB
     */
    private static <T> Predicate buildCustomFieldPredicate(FilterCriteria filter, Root<T> root, CriteriaBuilder cb) {
        // Extract custom field name from field path (e.g., "customFields.industry")
        String fieldPath = filter.getField();
        if (!fieldPath.startsWith("customFields.")) {
            return null;
        }
        
        String customFieldName = fieldPath.substring("customFields.".length());
        
        // Use JSON path expression for PostgreSQL JSONB
        Expression<String> jsonPath = cb.function("jsonb_extract_path_text", String.class,
                root.get("customFields"), cb.literal(customFieldName));
        
        switch (filter.getOperator()) {
            case EQUALS:
                return cb.equal(jsonPath, filter.getValue().toString());
            case LIKE:
                return cb.like(jsonPath, "%" + filter.getValue() + "%");
            case IS_NULL:
                return cb.isNull(jsonPath);
            case IS_NOT_NULL:
                return cb.isNotNull(jsonPath);
            default:
                return cb.equal(jsonPath, filter.getValue().toString());
        }
    }
    
    /**
     * Get nested path from root
     */
    private static <T> Path<Object> getPath(Root<T> root, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Path<Object> path = root.get(parts[0]);
        
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        
        return path;
    }
    
    /**
     * Build Spring Data Sort from sort criteria
     */
    public static Sort buildSort(List<SortCriteria> sortCriteria) {
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            return Sort.unsorted();
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        
        for (SortCriteria criteria : sortCriteria) {
            Sort.Direction direction = criteria.getDirection() == SortCriteria.SortDirection.DESC 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC;
            
            Sort.Order order = new Sort.Order(direction, criteria.getField());
            
            if (criteria.getNullsFirst() != null) {
                order = criteria.getNullsFirst() ? order.nullsFirst() : order.nullsLast();
            }
            
            orders.add(order);
        }
        
        return Sort.by(orders);
    }
    
    /**
     * Build Spring Data Pageable from page request
     */
    public static Pageable buildPageable(com.crm.platform.common.dto.PageRequest pageRequest) {
        if (pageRequest == null) {
            return PageRequest.of(0, 20);
        }
        
        Sort sort = buildSort(pageRequest.getSort());
        
        return PageRequest.of(
                pageRequest.getPage() - 1, // Spring Data uses 0-based indexing
                pageRequest.getLimit(),
                sort
        );
    }
    
    /**
     * Build full-text search specification
     */
    public static <T> Specification<T> buildSearchSpecification(String searchTerm, List<String> searchFields) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty() || searchFields == null || searchFields.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            List<Predicate> searchPredicates = new ArrayList<>();
            
            for (String field : searchFields) {
                Path<String> fieldPath = (Path<String>) getPath(root, field);
                searchPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(fieldPath), searchPattern));
            }
            
            return criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Combine multiple specifications with AND logic
     */
    public static <T> Specification<T> combineWithAnd(List<Specification<T>> specifications) {
        return specifications.stream()
                .reduce(Specification.where(null), Specification::and);
    }
    
    /**
     * Combine multiple specifications with OR logic
     */
    public static <T> Specification<T> combineWithOr(List<Specification<T>> specifications) {
        return specifications.stream()
                .reduce(Specification.where(null), Specification::or);
    }
}