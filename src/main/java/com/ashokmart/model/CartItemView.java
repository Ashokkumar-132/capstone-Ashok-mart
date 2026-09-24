package com.ashokmart.model;

import java.math.BigDecimal;

/** Cart display data populated from current database product values. */
public record CartItemView(
        long productId,
        String productName,
        String imageUrl,
        BigDecimal currentPrice,
        int quantity,
        BigDecimal lineTotal,
        int availableStock,
        boolean active) {

    public CartItemView withLineTotal(BigDecimal recalculatedLineTotal) {
        return new CartItemView(productId, productName, imageUrl, currentPrice, quantity,
                recalculatedLineTotal, availableStock, active);
    }

    public boolean isAvailable() {
        return active && availableStock > 0;
    }

    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public int getAvailableStock() { return availableStock; }
    public boolean isActive() { return active; }
}
