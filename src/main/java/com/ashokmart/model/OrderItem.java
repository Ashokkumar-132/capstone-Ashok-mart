package com.ashokmart.model;

import java.math.BigDecimal;

public record OrderItem(long orderId, long productId, long sellerId, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal) {
    public long getOrderId() { return orderId; }
    public long getProductId() { return productId; }
    public long getSellerId() { return sellerId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
}
