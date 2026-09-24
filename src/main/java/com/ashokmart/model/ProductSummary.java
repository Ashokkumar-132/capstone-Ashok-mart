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

    public long getId() { return id; }
    public long getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }
}
