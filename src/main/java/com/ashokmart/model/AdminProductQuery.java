package com.ashokmart.model;

public record AdminProductQuery(String search, Long categoryId, Boolean active, Boolean inStock, int page, int pageSize) {
    public AdminProductQuery {
        search = search == null || search.isBlank() ? null : search.trim();
        if (categoryId != null && categoryId <= 0) categoryId = null;
        page = Math.max(1, page);
        pageSize = Math.min(50, Math.max(1, pageSize));
    }
    public int offset() { return (page - 1) * pageSize; }
    public String getSearch() { return search; }
    public Long getCategoryId() { return categoryId; }
    public Boolean getActive() { return active; }
    public Boolean getInStock() { return inStock; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}

