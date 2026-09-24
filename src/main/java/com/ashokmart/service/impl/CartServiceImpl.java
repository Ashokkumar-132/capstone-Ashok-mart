package com.ashokmart.service.impl;

import com.ashokmart.dao.CartDao;
import com.ashokmart.model.CartItemView;
import com.ashokmart.model.CartView;
import com.ashokmart.model.ProductSummary;
import com.ashokmart.service.CartService;
import com.ashokmart.service.CartValidationException;
import com.ashokmart.service.ProductService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CartServiceImpl implements CartService {
    private final CartDao cartDao;
    private final ProductService productService;

    public CartServiceImpl(CartDao cartDao, ProductService productService) {
        this.cartDao = cartDao;
        this.productService = productService;
    }

    @Override
    public CartView getCart(long authenticatedUserId) {
        requireAuthenticated(authenticatedUserId);
        try {
            List<CartItemView> items = cartDao.findItemViewsByUserId(authenticatedUserId);
            BigDecimal subtotal = BigDecimal.ZERO;
            int itemCount = 0;
            List<CartItemView> recalculated = new ArrayList<>();
            for (CartItemView item : items) {
                BigDecimal lineTotal = item.getCurrentPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                recalculated.add(item.withLineTotal(lineTotal));
                subtotal = subtotal.add(lineTotal);
                itemCount += item.getQuantity();
            }
            return new CartView(recalculated, subtotal, itemCount);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cart is temporarily unavailable", exception);
        }
    }

    @Override
    public void addToCart(long authenticatedUserId, long productId, int quantity) {
        requireAuthenticated(authenticatedUserId);
        requirePositiveProductAndQuantity(productId, quantity);
        ProductSummary product = availableProduct(productId);
        try {
            int existingQuantity = cartDao.findItemByUserIdAndProductId(authenticatedUserId, productId)
                    .map(item -> item.quantity()).orElse(0);
            int requestedTotal = safeAdd(existingQuantity, quantity);
            ensureWithinStock(requestedTotal, product.stockQuantity());
            cartDao.addItem(authenticatedUserId, productId, quantity);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cart could not be updated", exception);
        }
    }

    @Override
    public void updateQuantity(long authenticatedUserId, long productId, int quantity) {
        requireAuthenticated(authenticatedUserId);
        requirePositiveProductAndQuantity(productId, quantity);
        ProductSummary product = availableProduct(productId);
        ensureWithinStock(quantity, product.stockQuantity());
        try {
            cartDao.updateItemQuantity(authenticatedUserId, productId, quantity);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cart could not be updated", exception);
        }
    }

    @Override
    public void removeItem(long authenticatedUserId, long productId) {
        requireAuthenticated(authenticatedUserId);
        if (productId <= 0) throw new CartValidationException("A valid product is required");
        try {
            cartDao.removeItem(authenticatedUserId, productId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cart item could not be removed", exception);
        }
    }

    @Override
    public void clearCart(long authenticatedUserId) {
        requireAuthenticated(authenticatedUserId);
        try {
            cartDao.clearCart(authenticatedUserId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cart could not be cleared", exception);
        }
    }

    private ProductSummary availableProduct(long productId) {
        return productService.findProduct(productId)
                .orElseThrow(() -> new CartValidationException("This product is no longer available"));
    }

    private void ensureWithinStock(int quantity, int stock) {
        if (stock <= 0) throw new CartValidationException("This product is out of stock");
        if (quantity > stock) throw new CartValidationException("Only " + stock + " unit(s) are available");
    }

    private int safeAdd(int existing, int addition) {
        if (existing > Integer.MAX_VALUE - addition) {
            throw new CartValidationException("Requested quantity is too large");
        }
        return existing + addition;
    }

    private void requirePositiveProductAndQuantity(long productId, int quantity) {
        if (productId <= 0) throw new CartValidationException("A valid product is required");
        if (quantity <= 0) throw new CartValidationException("Quantity must be at least 1");
    }

    private void requireAuthenticated(long userId) {
        if (userId <= 0) throw new CartValidationException("You must be logged in to use the cart");
    }
}
