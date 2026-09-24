package com.ashokmart.dao.impl;

import com.ashokmart.dao.CartDao;
import com.ashokmart.model.Cart;
import com.ashokmart.model.CartItem;
import com.ashokmart.model.CartItemView;
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

public final class CartDaoImpl implements CartDao {
    private final DatabaseConnectionPool pool;

    public CartDaoImpl(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public Optional<Cart> findByUserId(long userId) throws SQLException {
        String sql = "SELECT id, user_id, created_at, updated_at FROM cart WHERE user_id = ?";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapCart(result)) : Optional.empty();
            }
        }
    }

    @Override
    public Cart findOrCreateByUserId(long userId) throws SQLException {
        try (Connection connection = pool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long cartId = findOrCreateCartId(connection, userId);
                Cart cart = loadCart(connection, cartId);
                connection.commit();
                return cart;
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            }
        }
    }

    @Override
    public List<CartItemView> findItemViewsByUserId(long userId) throws SQLException {
        String sql = "SELECT ci.product_id, p.name, p.image_url, p.price, ci.quantity, "
                + "p.stock_quantity, p.enabled FROM cart c "
                + "JOIN cart_items ci ON ci.cart_id = c.id "
                + "JOIN products p ON p.id = ci.product_id "
                + "WHERE c.user_id = ? ORDER BY ci.id ASC";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<CartItemView> items = new ArrayList<>();
                while (result.next()) {
                    BigDecimal price = result.getBigDecimal("price");
                    int quantity = result.getInt("quantity");
                    items.add(new CartItemView(result.getLong("product_id"), result.getString("name"),
                            result.getString("image_url"), price, quantity,
                            price.multiply(BigDecimal.valueOf(quantity)), result.getInt("stock_quantity"),
                            result.getBoolean("enabled")));
                }
                return items;
            }
        }
    }

    @Override
    public Optional<CartItem> findItemByUserIdAndProductId(long userId, long productId) throws SQLException {
        String sql = "SELECT c.id AS cart_id, ci.product_id, ci.quantity FROM cart c "
                + "JOIN cart_items ci ON ci.cart_id = c.id WHERE c.user_id = ? AND ci.product_id = ?";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, productId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(new CartItem(result.getLong("cart_id"),
                        result.getLong("product_id"), result.getInt("quantity"))) : Optional.empty();
            }
        }
    }

    @Override
    public void addItem(long userId, long productId, int quantity) throws SQLException {
        try (Connection connection = pool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long cartId = findOrCreateCartId(connection, userId);
                int existing = findQuantity(connection, cartId, productId);
                if (existing < 0) {
                    insertItem(connection, cartId, productId, quantity);
                } else {
                    updateQuantity(connection, cartId, productId, existing + quantity);
                }
                touchCart(connection, cartId);
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            }
        }
    }

    @Override
    public void updateItemQuantity(long userId, long productId, int quantity) throws SQLException {
        String sql = "UPDATE cart_items ci SET quantity = ? WHERE ci.cart_id = "
                + "(SELECT c.id FROM cart c WHERE c.user_id = ?) AND ci.product_id = ?";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, userId);
            statement.setLong(3, productId);
            int updated = statement.executeUpdate();
            if (updated > 0) {
                touchCartForUser(connection, userId);
            }
        }
    }

    @Override
    public void removeItem(long userId, long productId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = "
                + "(SELECT c.id FROM cart c WHERE c.user_id = ?) AND product_id = ?";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, productId);
            int deleted = statement.executeUpdate();
            if (deleted > 0) {
                touchCartForUser(connection, userId);
            }
        }
    }

    @Override
    public void clearCart(long userId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = "
                + "(SELECT c.id FROM cart c WHERE c.user_id = ?)";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            int deleted = statement.executeUpdate();
            if (deleted > 0) {
                touchCartForUser(connection, userId);
            }
        }
    }

    private long findOrCreateCartId(Connection connection, long userId) throws SQLException {
        String findSql = "SELECT id FROM cart WHERE user_id = ? FOR UPDATE";
        try (PreparedStatement find = connection.prepareStatement(findSql)) {
            find.setLong(1, userId);
            try (ResultSet result = find.executeQuery()) {
                if (result.next()) return result.getLong(1);
            }
        }
        String insertSql = "INSERT INTO cart (user_id) VALUES (?)";
        try (PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insert.setLong(1, userId);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Cart creation did not return an ID");
                return keys.getLong(1);
            }
        }
    }

    private Cart loadCart(Connection connection, long cartId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, user_id, created_at, updated_at FROM cart WHERE id = ?")) {
            statement.setLong(1, cartId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Cart could not be loaded");
                return mapCart(result);
            }
        }
    }

    private int findQuantity(Connection connection, long cartId, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT quantity FROM cart_items WHERE cart_id = ? AND product_id = ? FOR UPDATE")) {
            statement.setLong(1, cartId);
            statement.setLong(2, productId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : -1;
            }
        }
    }

    private void insertItem(Connection connection, long cartId, long productId, int quantity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)")) {
            statement.setLong(1, cartId);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.executeUpdate();
        }
    }

    private void updateQuantity(Connection connection, long cartId, long productId, int quantity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE cart_items SET quantity = ? WHERE cart_id = ? AND product_id = ?")) {
            statement.setInt(1, quantity);
            statement.setLong(2, cartId);
            statement.setLong(3, productId);
            statement.executeUpdate();
        }
    }

    private void touchCart(Connection connection, long cartId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE cart SET updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setLong(1, cartId);
            statement.executeUpdate();
        }
    }

    private void touchCartForUser(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE cart SET updated_at = CURRENT_TIMESTAMP WHERE user_id = ?")) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private Cart mapCart(ResultSet result) throws SQLException {
        return new Cart(result.getLong("id"), result.getLong("user_id"),
                result.getTimestamp("created_at").toLocalDateTime(), result.getTimestamp("updated_at").toLocalDateTime());
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original database exception.
        }
    }
}
