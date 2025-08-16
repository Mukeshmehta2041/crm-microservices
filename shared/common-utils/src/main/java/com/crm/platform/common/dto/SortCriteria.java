package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Sorting criteria for API endpoints
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SortCriteria {
    
    @JsonProperty("field")
    private String field;
    
    @JsonProperty("direction")
    private SortDirection direction = SortDirection.ASC;
    
    @JsonProperty("nullsFirst")
    private Boolean nullsFirst;
    
    public enum SortDirection {
        ASC("asc"),
        DESC("desc");
        
        private final String value;
        
        SortDirection(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static SortDirection fromString(String value) {
            for (SortDirection direction : SortDirection.values()) {
                if (direction.value.equalsIgnoreCase(value)) {
                    return direction;
                }
            }
            throw new IllegalArgumentException("Unknown sort direction: " + value);
        }
    }
    
    // Constructors
    public SortCriteria() {}
    
    public SortCriteria(String field, SortDirection direction) {
        this.field = field;
        this.direction = direction;
    }
    
    public SortCriteria(String field, SortDirection direction, Boolean nullsFirst) {
        this.field = field;
        this.direction = direction;
        this.nullsFirst = nullsFirst;
    }
    
    // Static factory methods
    public static SortCriteria asc(String field) {
        return new SortCriteria(field, SortDirection.ASC);
    }
    
    public static SortCriteria desc(String field) {
        return new SortCriteria(field, SortDirection.DESC);
    }
    
    public static List<SortCriteria> of(String... fieldDirections) {
        return List.of(fieldDirections).stream()
                .map(fd -> {
                    String[] parts = fd.split(":");
                    String field = parts[0];
                    SortDirection direction = parts.length > 1 
                            ? SortDirection.fromString(parts[1]) 
                            : SortDirection.ASC;
                    return new SortCriteria(field, direction);
                })
                .toList();
    }
    
    // Getters and setters
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public SortDirection getDirection() {
        return direction;
    }
    
    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }
    
    public Boolean getNullsFirst() {
        return nullsFirst;
    }
    
    public void setNullsFirst(Boolean nullsFirst) {
        this.nullsFirst = nullsFirst;
    }
}