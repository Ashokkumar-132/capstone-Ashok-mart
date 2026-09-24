package com.ashokmart.dao.impl;

import com.ashokmart.dao.OrderDao;
import com.ashokmart.model.CheckoutItem;
import com.ashokmart.model.Order;
import com.ashokmart.model.OrderItem;
import com.ashokmart.model.OrderItemView;
import com.ashokmart.util.DatabaseConnectionPool;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OrderDaoImpl implements OrderDao {
    private final DatabaseConnectionPool pool;

    public OrderDaoImpl(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public List<CheckoutItem> findCheckoutItems(Connection connection, long buyerId) throws SQLException {
        String sql = "SELECT p.id, p.seller_id, p.name, ci.quantity, p.price, p.stock_quantity, p.enabled "
                + "FROM cart c JOIN cart_items ci ON ci.cart_id = c.id "
                + "JOIN products p ON p.id = ci.product_id "
                + "WHERE c.user_id = ? ORDER BY ci.id FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                List<CheckoutItem> items = new ArrayList<>();
                while (result.next()) {
                    items.add(new CheckoutItem(result.getLong("id"), result.getLong("seller_id"),
                            result.getString("name"), result.getInt("quantity"), result.getBigDecimal("price"),
                            result.getInt("stock_quantity"), result.getBoolean("enabled")));
                }
                return items;
            }
        }
    }

    @Override
    public long createOrder(Connection connection, long buyerId, BigDecimal totalAmount) throws SQLException {
        String sql = "INSERT INTO orders (buyer_id, total_amount, status) VALUES (?, ?, 'PENDING')";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, buyerId);
            statement.setBigDecimal(2, totalAmount);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Order creation did not return an ID");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public void createOrderItem(Connection connection, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, seller_id, quantity, unit_price, subtotal) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.orderId());
            statement.setLong(2, item.productId());
            statement.setLong(3, item.sellerId());
            statement.setInt(4, item.quantity());
            statement.setBigDecimal(5, item.unitPrice());
            statement.setBigDecimal(6, item.subtotal());
            statement.executeUpdate();
        }
    }

    @Override
    public int decrementStock(Connection connection, long productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND enabled = TRUE AND stock_quantity >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            return statement.executeUpdate();
        }
    }

    @Override
    public int clearCart(Connection connection, long buyerId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = (SELECT id FROM cart WHERE user_id = ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buyerId);
            return statement.executeUpdate();
        }
    }

    @Override
    public Optional<Order> findOrderByBuyerId(Connection connection, long buyerId, long orderId) throws SQLException {
        String sql = "SELECT o.id, o.buyer_id, o.total_amount, o.status, o.created_at FROM orders o "
                + "WHERE o.id = ? AND o.buyer_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapOrder(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<OrderItemView> findOrderItems(Connection connection, long buyerId, long orderId) throws SQLException {
        String sql = "SELECT oi.product_id, p.name, oi.quantity, oi.unit_price, oi.subtotal "
                + "FROM order_items oi JOIN products p ON p.id = oi.product_id "
                + "JOIN orders o ON o.id = oi.order_id "
                + "WHERE oi.order_id = ? AND o.buyer_id = ? ORDER BY oi.id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                List<OrderItemView> items = new ArrayList<>();
                while (result.next()) {
                    items.add(new OrderItemView(result.getLong("product_id"), result.getString("name"),
                            result.getInt("quantity"), result.getBigDecimal("unit_price"),
                            result.getBigDecimal("subtotal")));
                }
                return items;
            }
        }
    }

    private Order mapOrder(ResultSet result) throws SQLException {
        return new Order(result.getLong("id"), result.getLong("buyer_id"),
                result.getBigDecimal("total_amount"), result.getString("status"),
                result.getTimestamp("created_at").toLocalDateTime());
    }
}
