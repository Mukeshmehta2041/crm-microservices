package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Pagination request with support for both offset-based and cursor-based pagination
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageRequest {
    
    // Offset-based pagination
    @JsonProperty("page")
    private Integer page = 1;
    
    @JsonProperty("limit")
    private Integer limit = 20;
    
    // Cursor-based pagination
    @JsonProperty("cursor")
    private String cursor;
    
    @JsonProperty("direction")
    private CursorDirection direction = CursorDirection.FORWARD;
    
    // Sorting
    @JsonProperty("sort")
    private List<SortCriteria> sort;
    
    // Filtering
    @JsonProperty("filters")
    private List<FilterCriteria> filters;
    
    // Search
    @JsonProperty("search")
    private String search;
    
    @JsonProperty("searchFields")
    private List<String> searchFields;
    
    // Include related data
    @JsonProperty("include")
    private List<String> include;
    
    // Field selection
    @JsonProperty("fields")
    private List<String> fields;
    
    public enum CursorDirection {
        FORWARD, BACKWARD
    }
    
    // Constructors
    public PageRequest() {}
    
    public PageRequest(Integer page, Integer limit) {
        this.page = page;
        this.limit = limit;
    }
    
    public PageRequest(String cursor, Integer limit, CursorDirection direction) {
        this.cursor = cursor;
        this.limit = limit;
        this.direction = direction;
    }
    
    // Static factory methods
    public static PageRequest of(int page, int limit) {
        return new PageRequest(page, limit);
    }
    
    public static PageRequest cursor(String cursor, int limit) {
        return new PageRequest(cursor, limit, CursorDirection.FORWARD);
    }
    
    // Validation methods
    public boolean isOffsetBased() {
        return cursor == null;
    }
    
    public boolean isCursorBased() {
        return cursor != null;
    }
    
    public int getOffset() {
        return (page - 1) * limit;
    }
    
    // Getters and setters
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = Math.max(1, page != null ? page : 1);
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = Math.min(Math.max(1, limit != null ? limit : 20), 1000);
    }
    
    public String getCursor() {
        return cursor;
    }
    
    public void setCursor(String cursor) {
        this.cursor = cursor;
    }
    
    public CursorDirection getDirection() {
        return direction;
    }
    
    public void setDirection(CursorDirection direction) {
        this.direction = direction;
    }
    
    public List<SortCriteria> getSort() {
        return sort;
    }
    
    public void setSort(List<SortCriteria> sort) {
        this.sort = sort;
    }
    
    public List<FilterCriteria> getFilters() {
        return filters;
    }
    
    public void setFilters(List<FilterCriteria> filters) {
        this.filters = filters;
    }
    
    public String getSearch() {
        return search;
    }
    
    public void setSearch(String search) {
        this.search = search;
    }
    
    public List<String> getSearchFields() {
        return searchFields;
    }
    
    public void setSearchFields(List<String> searchFields) {
        this.searchFields = searchFields;
    }
    
    public List<String> getInclude() {
        return include;
    }
    
    public void setInclude(List<String> include) {
        this.include = include;
    }
    
    public List<String> getFields() {
        return fields;
    }
    
    public void setFields(List<String> fields) {
        this.fields = fields;
    }
}