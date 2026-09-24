package com.ashokmart.model;

import java.util.List;

public record OrderDetails(Order order, List<OrderItemView> items) {
    public Order getOrder() { return order; }
    public List<OrderItemView> getItems() { return items; }
}
