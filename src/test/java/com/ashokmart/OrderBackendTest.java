package com.ashokmart;

import com.ashokmart.dao.OrderDao;
import com.ashokmart.dao.impl.OrderDaoImpl;
import com.ashokmart.model.OrderDetails;
import com.ashokmart.model.OrderSummary;
import com.ashokmart.service.OrderService;
import com.ashokmart.service.OrderValidationException;
import com.ashokmart.service.impl.OrderServiceImpl;
import com.ashokmart.util.DatabaseConfig;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderBackendTest {
    private DatabaseConnectionPool pool;
    private OrderDao orderDao;
    private OrderService orderService;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "orders_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        insertFixtures();
        orderDao = new OrderDaoImpl(pool);
        orderService = new OrderServiceImpl(orderDao);
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void daoReturnsOnlyBuyerOrdersWithItemCounts() throws Exception {
        insertOrder(1, 1, "24.00", "CONFIRMED");
        insertOrder(2, 1, "9.99", "PENDING");
        insertOrder(3, 2, "15.00", "DELIVERED");
        insertOrderItem(1, 1, 2, "9.99", "19.98");
        insertOrderItem(1, 2, 1, "4.02", "4.02");
        insertOrderItem(2, 1, 1, "9.99", "9.99");

        List<OrderSummary> orders = orderDao.findOrdersByBuyerId(1);

        assertEquals(2, orders.size());
        assertEquals(2, orders.get(0).getId());
        assertEquals(1, orders.get(0).getItemCount());
        assertEquals(2, orders.get(1).getItemCount());
    }

    @Test
    void daoOwnershipQueryDoesNotReturnAnotherBuyersOrder() throws Exception {
        insertOrder(1, 1, "10.00", "CONFIRMED");
        insertOrder(2, 2, "15.00", "DELIVERED");

        assertTrue(orderDao.findOrderByIdAndBuyerId(2, 1).isEmpty());
        assertTrue(orderDao.findOrderItems(2, 1).isEmpty());
    }

    @Test
    void orderItemsUseStoredHistoricalPricesAndCurrentProductImageOnlyForPresentation() throws Exception {
        insertOrder(1, 1, "19.98", "CONFIRMED");
        insertOrderItem(1, 1, 2, "9.99", "19.98");
        updateProductPrice("25.00");

        var items = orderDao.findOrderItems(1, 1);

        assertEquals(1, items.size());
        assertEquals(0, items.get(0).getUnitPrice().compareTo(new BigDecimal("9.99")));
        assertEquals("/images/phone.jpg", items.get(0).getImageUrl());
    }

    @Test
    void serviceReturnsOwnDetailsAndHidesOtherBuyers() throws Exception {
        insertOrder(1, 1, "10.00", "CONFIRMED");
        insertOrder(2, 2, "15.00", "DELIVERED");
        insertOrderItem(1, 1, 1, "10.00", "10.00");

        OrderDetails own = orderService.getOrderDetailsForBuyer(1, 1).orElseThrow();

        assertEquals(1, own.getOrder().getId());
        assertEquals(1, own.getItems().size());
        assertTrue(orderService.getOrderDetailsForBuyer(2, 1).isEmpty());
    }

    @Test
    void emptyHistoryAndInvalidBuyerAreHandledSafely() throws Exception {
        assertTrue(orderService.getOrdersForBuyer(1).isEmpty());
        assertThrows(OrderValidationException.class, () -> orderService.getOrdersForBuyer(0));
        assertThrows(OrderValidationException.class, () -> orderService.getOrderDetailsForBuyer(0, 1));
    }

    private void insertFixtures() throws Exception {
        try (Connection connection = pool.getConnection()) {
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Buyer One', 'buyer1@orders.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (2, 'Buyer Two', 'buyer2@orders.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (3, 'Seller', 'seller@orders.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'General')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, image_url, enabled) VALUES (1, 3, 1, 'Phone', 10.00, 5, '/images/phone.jpg', TRUE)");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (2, 3, 1, 'Cable', 4.02, 5, TRUE)");
        }
    }

    private void insertOrder(long id, long buyerId, String total, String status) throws Exception {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, id);
            statement.setLong(2, buyerId);
            statement.setBigDecimal(3, new BigDecimal(total));
            statement.setString(4, status);
            statement.executeUpdate();
        }
    }

    private void insertOrderItem(long orderId, long productId, int quantity, String unitPrice, String subtotal) throws Exception {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO order_items (order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (?, ?, 3, ?, ?, ?)")) {
            statement.setLong(1, orderId);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.setBigDecimal(4, new BigDecimal(unitPrice));
            statement.setBigDecimal(5, new BigDecimal(subtotal));
            statement.executeUpdate();
        }
    }

    private void updateProductPrice(String price) throws Exception {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE products SET price = ? WHERE id = 1")) {
            statement.setBigDecimal(1, new BigDecimal(price));
            statement.executeUpdate();
        }
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
