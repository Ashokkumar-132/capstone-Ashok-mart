package com.ashokmart.model;

import java.math.BigDecimal;
import java.util.List;

public record CartView(List<CartItemView> items, BigDecimal subtotal, int itemCount) {
    public CartView {
        items = List.copyOf(items);
        subtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
    }

    public List<CartItemView> getItems() { return items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public int getItemCount() { return itemCount; }
    public boolean isEmpty() { return items.isEmpty(); }
}
