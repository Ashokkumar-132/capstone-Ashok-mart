package com.ashokmart;

import com.ashokmart.dao.CartDao;
import com.ashokmart.dao.OrderDao;
import com.ashokmart.dao.impl.CartDaoImpl;
import com.ashokmart.dao.impl.OrderDaoImpl;
import com.ashokmart.model.Order;
import com.ashokmart.service.CheckoutService;
import com.ashokmart.service.CheckoutValidationException;
import com.ashokmart.service.impl.CheckoutServiceImpl;
import com.ashokmart.util.DatabaseConfig;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class CheckoutBackendTest {
    private DatabaseConnectionPool pool;
    private CartDao cartDao;
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "checkout_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        insertFixtures();
        cartDao = new CartDaoImpl(pool);
        checkoutService = new CheckoutServiceImpl(pool, new OrderDaoImpl(pool));
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void successfulCheckoutCreatesOrderItemsDeductsStockAndClearsCart() throws Exception {
        cartDao.addItem(1, 1, 2);
        cartDao.addItem(1, 2, 1);

        Order order = checkoutService.checkout(1);

        assertEquals(0, order.getTotalAmount().compareTo(new BigDecimal("24.00")));
        assertEquals(1, count("SELECT COUNT(*) FROM orders"));
        assertEquals(2, count("SELECT COUNT(*) FROM order_items"));
        assertEquals(3, scalarInt("SELECT stock_quantity FROM products WHERE id = 1"));
        assertEquals(2, scalarInt("SELECT stock_quantity FROM products WHERE id = 2"));
        assertEquals(0, count("SELECT COUNT(*) FROM cart_items WHERE cart_id IN (SELECT id FROM cart WHERE user_id = 1)"));
    }

    @Test
    void checkoutUsesCurrentDatabasePriceNotTheStaleCartDisplayPrice() throws Exception {
        cartDao.addItem(1, 1, 1);
        updatePrice(1, "12.50");

        Order order = checkoutService.checkout(1);

        assertEquals(0, order.getTotalAmount().compareTo(new BigDecimal("12.50")));
        assertEquals(0, scalarDecimal("SELECT unit_price FROM order_items WHERE order_id = " + order.getId()).compareTo(new BigDecimal("12.50")));
    }

    @Test
    void emptyCartDoesNotCreateOrder() throws Exception {
        assertThrows(CheckoutValidationException.class, () -> checkoutService.checkout(1));
        assertEquals(0, count("SELECT COUNT(*) FROM orders"));
    }

    @Test
    void insufficientStockAbortsBeforeAnyOrderOrStockChange() throws Exception {
        cartDao.addItem(1, 1, 2);
        cartDao.addItem(1, 2, 1);
        updateStock(2, 0);

        assertThrows(CheckoutValidationException.class, () -> checkoutService.checkout(1));

        assertEquals(0, count("SELECT COUNT(*) FROM orders"));
        assertEquals(5, scalarInt("SELECT stock_quantity FROM products WHERE id = 1"));
        assertEquals(2, count("SELECT COUNT(*) FROM cart_items WHERE cart_id IN (SELECT id FROM cart WHERE user_id = 1)"));
    }

    @Test
    void stockConflictDuringDeductionRollsBackOrderStockAndCart() throws Exception {
        cartDao.addItem(1, 1, 2);
        cartDao.addItem(1, 2, 1);
        OrderDao orderDao = spy(new OrderDaoImpl(pool));
        doReturn(0).when(orderDao).decrementStock(org.mockito.ArgumentMatchers.any(Connection.class), org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(1));
        CheckoutService failingCheckout = new CheckoutServiceImpl(pool, orderDao);

        assertThrows(CheckoutValidationException.class, () -> failingCheckout.checkout(1));

        assertEquals(0, count("SELECT COUNT(*) FROM orders"));
        assertEquals(5, scalarInt("SELECT stock_quantity FROM products WHERE id = 1"));
        assertEquals(3, scalarInt("SELECT stock_quantity FROM products WHERE id = 2"));
        assertEquals(2, count("SELECT COUNT(*) FROM cart_items WHERE cart_id IN (SELECT id FROM cart WHERE user_id = 1)"));
    }

    @Test
    void checkoutIdentityIsBoundToBuyerAndOrderLookupIsBuyerScoped() throws Exception {
        cartDao.addItem(1, 1, 1);
        Order order = checkoutService.checkout(1);

        assertThrows(CheckoutValidationException.class, () -> checkoutService.findBuyerOrder(2, order.getId()));
        assertThrows(CheckoutValidationException.class, () -> checkoutService.checkout(0));
        assertEquals(order.getId(), checkoutService.findBuyerOrder(1, order.getId()).getId());
    }

    private void insertFixtures() throws Exception {
        try (Connection connection = pool.getConnection()) {
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Buyer One', 'buyer1@checkout.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (2, 'Buyer Two', 'buyer2@checkout.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (3, 'Seller', 'seller@checkout.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'General')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (1, 3, 1, 'Phone', 10.00, 5, TRUE)");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (2, 3, 1, 'Cable', 4.00, 3, TRUE)");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (3, 3, 1, 'Inactive', 8.00, 5, FALSE)");
        }
    }

    private void updatePrice(long productId, String price) throws Exception {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE products SET price = ? WHERE id = ?")) {
            statement.setBigDecimal(1, new BigDecimal(price));
            statement.setLong(2, productId);
            statement.executeUpdate();
        }
    }

    private void updateStock(long productId, int stock) throws Exception {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE products SET stock_quantity = ? WHERE id = ?")) {
            statement.setInt(1, stock);
            statement.setLong(2, productId);
            statement.executeUpdate();
        }
    }

    private int count(String sql) throws Exception {
        return scalarInt(sql);
    }

    private int scalarInt(String sql) throws Exception {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private BigDecimal scalarDecimal(String sql) throws Exception {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getBigDecimal(1);
        }
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
