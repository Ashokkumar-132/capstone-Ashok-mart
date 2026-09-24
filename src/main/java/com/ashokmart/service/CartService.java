package com.ashokmart.service;

import com.ashokmart.model.CartView;

public interface CartService {
    CartView getCart(long authenticatedUserId);
    void addToCart(long authenticatedUserId, long productId, int quantity);
    void updateQuantity(long authenticatedUserId, long productId, int quantity);
    void removeItem(long authenticatedUserId, long productId);
    void clearCart(long authenticatedUserId);
}
