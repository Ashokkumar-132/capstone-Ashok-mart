package com.ashokmart.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminOrderModels {
    private AdminOrderModels() {}
    public record Query(String search, String status, int page, int pageSize) {
        public Query { search = search == null || search.isBlank() ? null : search.trim(); status = status == null || status.isBlank() ? null : status.trim().toUpperCase(); page = Math.max(1, page); pageSize = Math.min(50, Math.max(1, pageSize)); }
        public int offset() { return (page - 1) * pageSize; }
        public String getSearch() { return search; }
        public String getStatus() { return status; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
    }
    public record Summary(long id, String buyerName, String buyerEmail, BigDecimal totalAmount, String status, LocalDateTime createdAt, long itemCount) {
        public long getId(){return id;} public String getBuyerName(){return buyerName;} public String getBuyerEmail(){return buyerEmail;} public BigDecimal getTotalAmount(){return totalAmount;} public String getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;} public long getItemCount(){return itemCount;}
    }
    public record Item(long productId, String productName, String sellerName, String sellerEmail, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        public long getProductId(){return productId;} public String getProductName(){return productName;} public String getSellerName(){return sellerName;} public String getSellerEmail(){return sellerEmail;} public int getQuantity(){return quantity;} public BigDecimal getUnitPrice(){return unitPrice;} public BigDecimal getSubtotal(){return subtotal;}
    }
    public record Details(Order order, String buyerName, String buyerEmail, List<Item> items) {
        public Details { items = List.copyOf(items); }
        public Order getOrder(){return order;} public String getBuyerName(){return buyerName;} public String getBuyerEmail(){return buyerEmail;} public List<Item> getItems(){return items;}
    }
}

