package com.ashokmart.model;

import java.util.List;

public record AdminProductPage(List<ProductSummary> products, int currentPage, int pageSize, long totalResults, int totalPages) {
    public AdminProductPage { products = List.copyOf(products); }
    public List<ProductSummary> getProducts() { return products; }
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }
    public long getTotalResults() { return totalResults; }
    public int getTotalPages() { return totalPages; }
    public boolean hasPrevious() { return currentPage > 1; }
    public boolean hasNext() { return currentPage < totalPages; }
}

