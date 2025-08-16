package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaginationMeta {
    
    private int page;
    private int limit;
    private long total;
    
    @JsonProperty("total_pages")
    private int totalPages;
    
    @JsonProperty("has_next")
    private boolean hasNext;
    
    @JsonProperty("has_prev")
    private boolean hasPrev;
    
    @JsonProperty("next_page")
    private Integer nextPage;
    
    @JsonProperty("prev_page")
    private Integer prevPage;
    
    @JsonProperty("first_page")
    private int firstPage = 1;
    
    @JsonProperty("last_page")
    private int lastPage;

    public PaginationMeta() {}

    public PaginationMeta(int page, int limit, long total) {
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / limit);
        this.hasNext = page < totalPages;
        this.hasPrev = page > 1;
        this.nextPage = hasNext ? page + 1 : null;
        this.prevPage = hasPrev ? page - 1 : null;
        this.lastPage = totalPages;
    }

    // Getters and Setters
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

    public boolean isHasPrev() { return hasPrev; }
    public void setHasPrev(boolean hasPrev) { this.hasPrev = hasPrev; }

    public Integer getNextPage() { return nextPage; }
    public void setNextPage(Integer nextPage) { this.nextPage = nextPage; }

    public Integer getPrevPage() { return prevPage; }
    public void setPrevPage(Integer prevPage) { this.prevPage = prevPage; }

    public int getFirstPage() { return firstPage; }
    public void setFirstPage(int firstPage) { this.firstPage = firstPage; }

    public int getLastPage() { return lastPage; }
    public void setLastPage(int lastPage) { this.lastPage = lastPage; }
}