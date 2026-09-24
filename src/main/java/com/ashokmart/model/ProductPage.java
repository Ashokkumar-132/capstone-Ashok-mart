package com.ashokmart.model;

import java.util.List;

public record ProductPage(
        List<ProductSummary> products,
        int currentPage,
        int pageSize,
        long totalResults,
        int totalPages) {

    public ProductPage {
        products = List.copyOf(products);
    }
}
