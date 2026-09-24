package com.ashokmart.service.impl;

import com.ashokmart.dao.OrderDao;
import com.ashokmart.model.Order;
import com.ashokmart.model.OrderItemView;
import com.ashokmart.model.SellerOrderDetails;
import com.ashokmart.model.SellerOrderSummary;
import com.ashokmart.service.SellerOrderException;
import com.ashokmart.service.SellerOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SellerOrderServiceImpl implements SellerOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerOrderServiceImpl.class);
    private static final Map<String, Set<String>> TRANSITIONS = transitions();
    private final OrderDao orderDao;

    public SellerOrderServiceImpl(OrderDao orderDao) {
        this.orderDao = orderDao;
    }

    @Override
    public List<SellerOrderSummary> getOrdersForSeller(long authenticatedSellerId) {
        requireSeller(authenticatedSellerId);
        try {
            return orderDao.findOrdersForSeller(authenticatedSellerId);
        } catch (SQLException exception) {
            LOGGER.error("Seller order lookup failed", exception);
            throw new IllegalStateException("Seller orders are temporarily unavailable", exception);
        }
    }

    @Override
    public Optional<SellerOrderDetails> getOrderDetailsForSeller(long orderId, long authenticatedSellerId) {
        requireSeller(authenticatedSellerId);
        if (orderId <= 0) throw new SellerOrderException("A valid order is required");
        try {
            Optional<Order> order = orderDao.findOrderForSeller(orderId, authenticatedSellerId);
            if (order.isEmpty()) return Optional.empty();
            List<OrderItemView> items = orderDao.findSellerOrderItems(orderId, authenticatedSellerId);
            BigDecimal subtotal = items.stream().map(OrderItemView::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            return Optional.of(new SellerOrderDetails(order.get(), items, subtotal));
        } catch (SQLException exception) {
            LOGGER.error("Seller order detail lookup failed", exception);
            throw new IllegalStateException("Seller order details are temporarily unavailable", exception);
        }
    }

    @Override
    public void updateOrderStatusForSeller(long orderId, long authenticatedSellerId, String requestedStatus) {
        requireSeller(authenticatedSellerId);
        if (orderId <= 0) throw new SellerOrderException("A valid order is required");
        String next = normalizeStatus(requestedStatus);
        try {
            Optional<Order> order = orderDao.findOrderForSeller(orderId, authenticatedSellerId);
            if (order.isEmpty()) throw new SellerOrderException("Order not found");
            String current = order.get().getStatus().toUpperCase(Locale.ROOT);
            if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
                throw new SellerOrderException("That order status transition is not allowed");
            }
            if (!orderDao.updateSellerOrderStatus(orderId, authenticatedSellerId, next)) {
                throw new SellerOrderException("Order not found");
            }
        } catch (SQLException exception) {
            LOGGER.error("Seller order status update failed", exception);
            throw new IllegalStateException("Order status could not be updated", exception);
        }
    }

    private String normalizeStatus(String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) throw new SellerOrderException("Status is required");
        String status = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!TRANSITIONS.containsKey(status) && !TRANSITIONS.values().stream().anyMatch(values -> values.contains(status))) {
            throw new SellerOrderException("Unsupported order status");
        }
        return status;
    }

    private void requireSeller(long sellerId) {
        if (sellerId <= 0) throw new SellerOrderException("You must be logged in as a seller");
    }

    private static Map<String, Set<String>> transitions() {
        Map<String, Set<String>> transitions = new java.util.HashMap<>();
        transitions.put("PENDING", Set.of("CONFIRMED", "CANCELLED"));
        transitions.put("CONFIRMED", Set.of("PROCESSING", "CANCELLED"));
        transitions.put("PROCESSING", Set.of("SHIPPED", "CANCELLED"));
        transitions.put("SHIPPED", Set.of("DELIVERED"));
        transitions.put("DELIVERED", Set.of());
        transitions.put("CANCELLED", Set.of());
        return Map.copyOf(transitions);
    }
}
