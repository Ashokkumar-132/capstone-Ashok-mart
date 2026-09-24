package com.ashokmart.model;

import java.math.BigDecimal;

public record OrderItemView(long productId, String productName, int quantity,
                            BigDecimal unitPrice, BigDecimal subtotal) {
    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
}
