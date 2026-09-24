package com.ashokmart.model;

import java.math.BigDecimal;

/** Catalog projection containing display-ready product, category, and seller data. */
public record ProductSummary(
        long id,
        long sellerId,
        String sellerName,
        long categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        String imageUrl,
        boolean active) {

    public boolean isInStock() {
        return stockQuantity > 0;
    }
}
