package com.ashokmart.model;

import java.math.BigDecimal;

/** Immutable, service-validated criteria passed from the service layer to JDBC. */
public record ProductSearchCriteria(
        String searchTerm,
        Long categoryId,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        boolean inStockOnly,
        ProductSort sort,
        int page,
        int pageSize) {

    public int offset() {
        return (page - 1) * pageSize;
    }
}
