package com.ashokmart.service;

import com.ashokmart.model.Order;

import java.util.List;
import com.ashokmart.model.OrderItemView;

public interface CheckoutService {
    Order checkout(long authenticatedBuyerId);
    Order findBuyerOrder(long authenticatedBuyerId, long orderId);
    List<OrderItemView> findBuyerOrderItems(long authenticatedBuyerId, long orderId);
}
