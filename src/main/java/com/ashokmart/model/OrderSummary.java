package com.ashokmart.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummary(long id, BigDecimal totalAmount, String status,
                           LocalDateTime createdAt, long itemCount) {
    public long getId() { return id; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getItemCount() { return itemCount; }
}
