package com.ashokmart.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerOrderSummary(long id, BigDecimal sellerSubtotal, String status,
                                 LocalDateTime createdAt, long itemCount) {
    public long getId() { return id; }
    public BigDecimal getSellerSubtotal() { return sellerSubtotal; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getItemCount() { return itemCount; }
}
