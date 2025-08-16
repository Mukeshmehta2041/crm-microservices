package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Advanced filtering criteria for API endpoints
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterCriteria {
    
    @JsonProperty("field")
    private String field;
    
    @JsonProperty("operator")
    private FilterOperator operator;
    
    @JsonProperty("value")
    private Object value;
    
    @JsonProperty("values")
    private List<Object> values;
    
    @JsonProperty("logicalOperator")
    private LogicalOperator logicalOperator = LogicalOperator.AND;
    
    @JsonProperty("nested")
    private List<FilterCriteria> nested;
    
    public enum FilterOperator {
        EQUALS("eq"),
        NOT_EQUALS("ne"),
        GREATER_THAN("gt"),
        GREATER_THAN_OR_EQUAL("gte"),
        LESS_THAN("lt"),
        LESS_THAN_OR_EQUAL("lte"),
        LIKE("like"),
        ILIKE("ilike"),
        IN("in"),
        NOT_IN("nin"),
        IS_NULL("null"),
        IS_NOT_NULL("not_null"),
        BETWEEN("between"),
        CONTAINS("contains"),
        STARTS_WITH("starts_with"),
        ENDS_WITH("ends_with"),
        REGEX("regex"),
        DATE_RANGE("date_range"),
        CUSTOM_FIELD("custom_field");
        
        private final String value;
        
        FilterOperator(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static FilterOperator fromString(String value) {
            for (FilterOperator op : FilterOperator.values()) {
                if (op.value.equals(value)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown filter operator: " + value);
        }
    }
    
    public enum LogicalOperator {
        AND, OR
    }
    
    // Constructors
    public FilterCriteria() {}
    
    public FilterCriteria(String field, FilterOperator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }
    
    public FilterCriteria(String field, FilterOperator operator, List<Object> values) {
        this.field = field;
        this.operator = operator;
        this.values = values;
    }
    
    // Static factory methods
    public static FilterCriteria equals(String field, Object value) {
        return new FilterCriteria(field, FilterOperator.EQUALS, value);
    }
    
    public static FilterCriteria like(String field, String value) {
        return new FilterCriteria(field, FilterOperator.LIKE, value);
    }
    
    public static FilterCriteria in(String field, List<Object> values) {
        return new FilterCriteria(field, FilterOperator.IN, values);
    }
    
    public static FilterCriteria dateRange(String field, LocalDateTime start, LocalDateTime end) {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setField(field);
        criteria.setOperator(FilterOperator.DATE_RANGE);
        criteria.setValues(List.of(start, end));
        return criteria;
    }
    
    public static FilterCriteria customField(String fieldName, FilterOperator operator, Object value) {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setField("customFields." + fieldName);
        criteria.setOperator(operator);
        criteria.setValue(value);
        return criteria;
    }
    
    // Getters and setters
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public FilterOperator getOperator() {
        return operator;
    }
    
    public void setOperator(FilterOperator operator) {
        this.operator = operator;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public List<Object> getValues() {
        return values;
    }
    
    public void setValues(List<Object> values) {
        this.values = values;
    }
    
    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }
    
    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }
    
    public List<FilterCriteria> getNested() {
        return nested;
    }
    
    public void setNested(List<FilterCriteria> nested) {
        this.nested = nested;
    }
}