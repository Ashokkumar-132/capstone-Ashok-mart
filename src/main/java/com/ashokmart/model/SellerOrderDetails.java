package com.ashokmart.model;

import java.math.BigDecimal;
import java.util.List;

public record SellerOrderDetails(Order order, List<OrderItemView> items, BigDecimal sellerSubtotal) {
    public Order getOrder() { return order; }
    public List<OrderItemView> getItems() { return items; }
    public BigDecimal getSellerSubtotal() { return sellerSubtotal; }
}
