package com.ashokmart.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private long id;
    private long buyerId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;

    public Order() {
    }

    public Order(long id, long buyerId, BigDecimal totalAmount, String status, LocalDateTime createdAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getBuyerId() { return buyerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
