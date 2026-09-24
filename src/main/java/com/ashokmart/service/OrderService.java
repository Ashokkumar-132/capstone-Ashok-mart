package com.ashokmart.service;

import com.ashokmart.model.OrderDetails;
import com.ashokmart.model.OrderSummary;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<OrderSummary> getOrdersForBuyer(long authenticatedBuyerId);
    Optional<OrderDetails> getOrderDetailsForBuyer(long orderId, long authenticatedBuyerId);
}
