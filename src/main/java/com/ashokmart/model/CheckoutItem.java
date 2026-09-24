package com.ashokmart.model;

import java.math.BigDecimal;

/** Current cart/product values read inside the checkout transaction. */
public record CheckoutItem(long productId, long sellerId, String productName, int quantity,
                           BigDecimal unitPrice, int availableStock, boolean active) {
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public long getProductId() { return productId; }
    public long getSellerId() { return sellerId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getAvailableStock() { return availableStock; }
    public boolean isActive() { return active; }
}
