package com.ashokmart.dao;

import com.ashokmart.model.CheckoutItem;
import com.ashokmart.model.AdminOrderModels;
import com.ashokmart.model.Order;
import com.ashokmart.model.OrderItem;
import com.ashokmart.model.OrderItemView;
import com.ashokmart.model.OrderSummary;
import com.ashokmart.model.SellerOrderSummary;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface OrderDao {
    List<CheckoutItem> findCheckoutItems(Connection connection, long buyerId) throws SQLException;
    long createOrder(Connection connection, long buyerId, BigDecimal totalAmount) throws SQLException;
    void createOrderItem(Connection connection, OrderItem item) throws SQLException;
    int decrementStock(Connection connection, long productId, int quantity) throws SQLException;
    int clearCart(Connection connection, long buyerId) throws SQLException;
    Optional<Order> findOrderByBuyerId(Connection connection, long buyerId, long orderId) throws SQLException;
    List<OrderItemView> findOrderItems(Connection connection, long buyerId, long orderId) throws SQLException;
    List<OrderSummary> findOrdersByBuyerId(long buyerId) throws SQLException;
    Optional<Order> findOrderByIdAndBuyerId(long orderId, long buyerId) throws SQLException;
    List<OrderItemView> findOrderItems(long orderId, long buyerId) throws SQLException;
    List<SellerOrderSummary> findOrdersForSeller(long sellerId) throws SQLException;
    Optional<Order> findOrderForSeller(long orderId, long sellerId) throws SQLException;
    List<OrderItemView> findSellerOrderItems(long orderId, long sellerId) throws SQLException;
    boolean updateSellerOrderStatus(long orderId, long sellerId, String status) throws SQLException;
    List<AdminOrderModels.Summary> findAdminOrders(AdminOrderModels.Query query) throws SQLException;
    long countAdminOrders(AdminOrderModels.Query query) throws SQLException;
    Optional<AdminOrderModels.Details> findAdminOrderDetails(long orderId) throws SQLException;
    long countOrders() throws SQLException;
    long countOrdersByStatus(String status) throws SQLException;
    BigDecimal totalRevenue() throws SQLException;
    BigDecimal averageOrderValue() throws SQLException;
}
