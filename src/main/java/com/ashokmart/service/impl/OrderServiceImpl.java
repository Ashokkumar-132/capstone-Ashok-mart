package com.ashokmart.service.impl;

import com.ashokmart.dao.OrderDao;
import com.ashokmart.model.Order;
import com.ashokmart.model.OrderDetails;
import com.ashokmart.model.OrderSummary;
import com.ashokmart.service.OrderService;
import com.ashokmart.service.OrderValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class OrderServiceImpl implements OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderDao orderDao;

    public OrderServiceImpl(OrderDao orderDao) {
        this.orderDao = orderDao;
    }

    @Override
    public List<OrderSummary> getOrdersForBuyer(long authenticatedBuyerId) {
        requireBuyer(authenticatedBuyerId);
        try {
            return orderDao.findOrdersByBuyerId(authenticatedBuyerId);
        } catch (SQLException exception) {
            LOGGER.error("Buyer order history lookup failed", exception);
            throw new IllegalStateException("Orders are temporarily unavailable. Please try again.", exception);
        }
    }

    @Override
    public Optional<OrderDetails> getOrderDetailsForBuyer(long orderId, long authenticatedBuyerId) {
        requireBuyer(authenticatedBuyerId);
        if (orderId <= 0) throw new OrderValidationException("A valid order is required.");
        try {
            Optional<Order> order = orderDao.findOrderByIdAndBuyerId(orderId, authenticatedBuyerId);
            if (order.isEmpty()) return Optional.empty();
            return Optional.of(new OrderDetails(order.get(), orderDao.findOrderItems(orderId, authenticatedBuyerId)));
        } catch (SQLException exception) {
            LOGGER.error("Buyer order detail lookup failed", exception);
            throw new IllegalStateException("Order details are temporarily unavailable. Please try again.", exception);
        }
    }

    private void requireBuyer(long buyerId) {
        if (buyerId <= 0) throw new OrderValidationException("You must be logged in as a buyer to view orders.");
    }
}
