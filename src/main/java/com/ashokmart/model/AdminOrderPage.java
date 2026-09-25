package com.ashokmart.model;

import java.util.List;

public record AdminOrderPage(List<AdminOrderModels.Summary> orders, int currentPage, int pageSize, long totalResults, int totalPages) {
    public AdminOrderPage { orders = List.copyOf(orders); }
    public List<AdminOrderModels.Summary> getOrders(){return orders;} public int getCurrentPage(){return currentPage;} public int getPageSize(){return pageSize;} public long getTotalResults(){return totalResults;} public int getTotalPages(){return totalPages;}
    public boolean hasPrevious(){return currentPage > 1;} public boolean hasNext(){return currentPage < totalPages;}
}

