package com.ashokmart.service;

import com.ashokmart.model.SellerOrderDetails;
import com.ashokmart.model.SellerOrderSummary;

import java.util.List;
import java.util.Optional;

public interface SellerOrderService {
    List<SellerOrderSummary> getOrdersForSeller(long authenticatedSellerId);
    Optional<SellerOrderDetails> getOrderDetailsForSeller(long orderId, long authenticatedSellerId);
    void updateOrderStatusForSeller(long orderId, long authenticatedSellerId, String requestedStatus);
}
