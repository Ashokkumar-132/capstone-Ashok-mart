package com.ashokmart.service.impl;

import com.ashokmart.dao.OrderDao;
import com.ashokmart.model.CheckoutItem;
import com.ashokmart.model.Order;
import com.ashokmart.model.OrderItem;
import com.ashokmart.model.OrderItemView;
import com.ashokmart.service.CheckoutService;
import com.ashokmart.service.CheckoutValidationException;
import com.ashokmart.util.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class CheckoutServiceImpl implements CheckoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckoutServiceImpl.class);
    private final DatabaseConnectionPool pool;
    private final OrderDao orderDao;

    public CheckoutServiceImpl(DatabaseConnectionPool pool, OrderDao orderDao) {
        this.pool = pool;
        this.orderDao = orderDao;
    }

    @Override
    public Order checkout(long authenticatedBuyerId) {
        requireAuthenticatedBuyer(authenticatedBuyerId);
        try (Connection connection = pool.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                List<CheckoutItem> items = orderDao.findCheckoutItems(connection, authenticatedBuyerId);
                if (items.isEmpty()) {
                    throw new CheckoutValidationException("Your cart is empty.");
                }
                BigDecimal total = validateAndCalculate(items);
                long orderId = orderDao.createOrder(connection, authenticatedBuyerId, total);
                for (CheckoutItem item : items) {
                    orderDao.createOrderItem(connection, new OrderItem(orderId, item.productId(), item.sellerId(),
                            item.quantity(), item.unitPrice(), item.lineTotal()));
                    if (orderDao.decrementStock(connection, item.productId(), item.quantity()) != 1) {
                        throw new CheckoutValidationException("Stock changed while placing your order. Please review your cart.");
                    }
                }
                if (orderDao.clearCart(connection, authenticatedBuyerId) != items.size()) {
                    throw new IllegalStateException("Cart was changed during checkout");
                }
                Order created = orderDao.findOrderByBuyerId(connection, authenticatedBuyerId, orderId)
                        .orElseThrow(() -> new IllegalStateException("Created order could not be reloaded"));
                connection.commit();
                return created;
            } catch (CheckoutValidationException exception) {
                rollback(connection);
                throw exception;
            } catch (SQLException | IllegalStateException exception) {
                rollback(connection);
                if (exception instanceof IllegalStateException stateException) {
                    throw stateException;
                }
                LOGGER.error("Checkout transaction failed", exception);
                throw new IllegalStateException("Checkout could not be completed. Please try again.", exception);
            } finally {
                restoreAutoCommit(connection, previousAutoCommit);
            }
        } catch (SQLException exception) {
            LOGGER.error("Checkout connection failed", exception);
            throw new IllegalStateException("Checkout is temporarily unavailable. Please try again.", exception);
        }
    }

    @Override
    public Order findBuyerOrder(long authenticatedBuyerId, long orderId) {
        requireAuthenticatedBuyer(authenticatedBuyerId);
        if (orderId <= 0) throw new CheckoutValidationException("A valid order is required.");
        try (Connection connection = pool.getConnection()) {
            return orderDao.findOrderByBuyerId(connection, authenticatedBuyerId, orderId)
                    .orElseThrow(() -> new CheckoutValidationException("That order could not be found."));
        } catch (CheckoutValidationException exception) {
            throw exception;
        } catch (SQLException exception) {
            LOGGER.error("Order lookup failed", exception);
            throw new IllegalStateException("Order details are temporarily unavailable.", exception);
        }
    }

    @Override
    public List<OrderItemView> findBuyerOrderItems(long authenticatedBuyerId, long orderId) {
        requireAuthenticatedBuyer(authenticatedBuyerId);
        if (orderId <= 0) throw new CheckoutValidationException("A valid order is required.");
        try (Connection connection = pool.getConnection()) {
            return orderDao.findOrderItems(connection, authenticatedBuyerId, orderId);
        } catch (SQLException exception) {
            LOGGER.error("Order item lookup failed", exception);
            throw new IllegalStateException("Order details are temporarily unavailable.", exception);
        }
    }

    private BigDecimal validateAndCalculate(List<CheckoutItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CheckoutItem item : items) {
            if (!item.active()) {
                throw new CheckoutValidationException("One or more products are no longer available. Please review your cart.");
            }
            if (item.quantity() <= 0) {
                throw new CheckoutValidationException("Your cart contains an invalid quantity. Please review your cart.");
            }
            if (item.availableStock() < item.quantity()) {
                throw new CheckoutValidationException("One or more products no longer have enough stock. Please review your cart.");
            }
            total = total.add(item.lineTotal());
        }
        return total;
    }

    private void requireAuthenticatedBuyer(long buyerId) {
        if (buyerId <= 0) throw new CheckoutValidationException("You must be logged in as a buyer to checkout.");
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            LOGGER.error("Checkout rollback failed", rollbackException);
        }
    }

    private void restoreAutoCommit(Connection connection, boolean previousAutoCommit) {
        try {
            if (connection.getAutoCommit() != previousAutoCommit) {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException restoreException) {
            LOGGER.error("Could not restore checkout connection state", restoreException);
        }
    }
}
