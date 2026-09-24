package com.ashokmart;

import com.ashokmart.dao.OrderDao;
import com.ashokmart.dao.impl.OrderDaoImpl;
import com.ashokmart.model.SellerOrderDetails;
import com.ashokmart.model.SellerOrderSummary;
import com.ashokmart.service.SellerOrderException;
import com.ashokmart.service.SellerOrderService;
import com.ashokmart.service.impl.SellerOrderServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerOrderBackendTest {
    private DatabaseConnectionPool pool;
    private OrderDao orderDao;
    private SellerOrderService service;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "seller_orders_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        insertFixtures();
        orderDao = new OrderDaoImpl(pool);
        service = new SellerOrderServiceImpl(orderDao);
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void sellerSeesOnlyRelevantOrdersAndOwnItemsInMixedOrder() throws Exception {
        List<SellerOrderSummary> sellerAOrders = orderDao.findOrdersForSeller(3);
        assertEquals(1, sellerAOrders.size());
        assertEquals(1, sellerAOrders.get(0).getId());
        assertEquals(0, sellerAOrders.get(0).getSellerSubtotal().compareTo(new BigDecimal("20.00")));
        assertEquals(1, sellerAOrders.get(0).getItemCount());

        SellerOrderDetails details = service.getOrderDetailsForSeller(1, 3).orElseThrow();
        assertEquals(1, details.getItems().size());
        assertEquals("Seller A phone", details.getItems().get(0).getProductName());
        assertEquals(0, details.getSellerSubtotal().compareTo(new BigDecimal("20.00")));
        assertTrue(service.getOrderDetailsForSeller(2, 3).isEmpty());
    }

    @Test
    void sellerCanUpdateRelevantSharedOrderButNotUnrelatedOrder() throws Exception {
        service.updateOrderStatusForSeller(1, 3, "CONFIRMED");
        assertEquals("CONFIRMED", orderDao.findOrderByIdAndBuyerId(1, 1).orElseThrow().getStatus());
        assertThrows(SellerOrderException.class, () -> service.updateOrderStatusForSeller(2, 3, "PROCESSING"));
        service.updateOrderStatusForSeller(1, 4, "PROCESSING");
        assertEquals("PROCESSING", orderDao.findOrderByIdAndBuyerId(1, 1).orElseThrow().getStatus());
    }

    @Test
    void serviceRejectsUnsupportedAndInvalidTransitions() throws Exception {
        assertThrows(SellerOrderException.class, () -> service.updateOrderStatusForSeller(1, 3, "DELIVERED"));
        assertThrows(SellerOrderException.class, () -> service.updateOrderStatusForSeller(1, 3, "BOGUS"));
        service.updateOrderStatusForSeller(1, 3, "CONFIRMED");
        service.updateOrderStatusForSeller(1, 3, "PROCESSING");
        service.updateOrderStatusForSeller(1, 3, "SHIPPED");
        service.updateOrderStatusForSeller(1, 3, "DELIVERED");
        assertThrows(SellerOrderException.class, () -> service.updateOrderStatusForSeller(1, 3, "CANCELLED"));
    }

    @Test
    void invalidSellerAndInvalidOrderAreRejected() {
        assertThrows(SellerOrderException.class, () -> service.getOrdersForSeller(0));
        assertThrows(SellerOrderException.class, () -> service.getOrderDetailsForSeller(0, 3));
    }

    private void insertFixtures() throws Exception {
        try (Connection connection = pool.getConnection()) {
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Buyer', 'buyer@seller-orders.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (3, 'Seller A', 'seller-a@seller-orders.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (4, 'Seller B', 'seller-b@seller-orders.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'General')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (1, 3, 1, 'Seller A phone', 10.00, 4, TRUE)");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (2, 4, 1, 'Seller B cable', 10.00, 4, TRUE)");
            execute(connection, "INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (1, 1, 30.00, 'PENDING')");
            execute(connection, "INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (2, 1, 10.00, 'CONFIRMED')");
            execute(connection, "INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (1, 1, 1, 3, 2, 10.00, 20.00)");
            execute(connection, "INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (2, 1, 2, 4, 1, 10.00, 10.00)");
            execute(connection, "INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (3, 2, 2, 4, 1, 10.00, 10.00)");
        }
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
