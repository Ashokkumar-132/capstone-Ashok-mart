package com.ashokmart.dao;

import com.ashokmart.model.Cart;
import com.ashokmart.model.CartItem;
import com.ashokmart.model.CartItemView;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CartDao {
    Optional<Cart> findByUserId(long userId) throws SQLException;
    Cart findOrCreateByUserId(long userId) throws SQLException;
    List<CartItemView> findItemViewsByUserId(long userId) throws SQLException;
    Optional<CartItem> findItemByUserIdAndProductId(long userId, long productId) throws SQLException;
    void addItem(long userId, long productId, int quantity) throws SQLException;
    void updateItemQuantity(long userId, long productId, int quantity) throws SQLException;
    void removeItem(long userId, long productId) throws SQLException;
    void clearCart(long userId) throws SQLException;
}
