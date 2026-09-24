package com.ashokmart;

import com.ashokmart.dao.CartDao;
import com.ashokmart.dao.ProductDao;
import com.ashokmart.dao.impl.CartDaoImpl;
import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.model.Cart;
import com.ashokmart.model.CartView;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSort;
import com.ashokmart.service.CartService;
import com.ashokmart.service.CartValidationException;
import com.ashokmart.service.ProductService;
import com.ashokmart.service.impl.CartServiceImpl;
import com.ashokmart.service.impl.ProductServiceImpl;
import com.ashokmart.util.DatabaseConfig;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartBackendTest {
    private DatabaseConnectionPool pool;
    private CartDao cartDao;
    private CartService cartService;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "cart_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        insertFixtures();
        ProductDao productDao = new ProductDaoImpl(pool);
        cartDao = new CartDaoImpl(pool);
        ProductService productService = new ProductServiceImpl(productDao);
        cartService = new CartServiceImpl(cartDao, productService);
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void daoCreatesOneCartAndSupportsItemMutations() throws Exception {
        Cart first = cartDao.findOrCreateByUserId(1);
        Cart second = cartDao.findOrCreateByUserId(1);
        assertEquals(first.getId(), second.getId());

        cartDao.addItem(1, 1, 2);
        assertEquals(2, cartDao.findItemByUserIdAndProductId(1, 1).orElseThrow().quantity());
        cartDao.updateItemQuantity(1, 1, 3);
        assertEquals(3, cartDao.findItemByUserIdAndProductId(1, 1).orElseThrow().quantity());
        cartDao.removeItem(1, 1);
        assertTrue(cartDao.findItemByUserIdAndProductId(1, 1).isEmpty());

        cartDao.addItem(1, 1, 1);
        cartDao.addItem(1, 2, 1);
        assertEquals(2, cartDao.findItemViewsByUserId(1).size());
        cartDao.clearCart(1);
        assertTrue(cartDao.findItemViewsByUserId(1).isEmpty());
    }

    @Test
    void serviceAddsAndUpdatesUsingCurrentDatabasePrices() throws Exception {
        cartService.addToCart(1, 1, 2);
        CartView initial = cartService.getCart(1);
        assertEquals(2, initial.getItemCount());
        assertEquals(new BigDecimal("20.00"), initial.getSubtotal());

        updatePrice(1, "12.50");
        CartView refreshed = cartService.getCart(1);
        assertEquals(new BigDecimal("25.00"), refreshed.getSubtotal());
        assertEquals(new BigDecimal("12.50"), refreshed.getItems().get(0).getCurrentPrice());

        cartService.addToCart(1, 1, 2);
        assertEquals(4, cartService.getCart(1).getItems().get(0).getQuantity());
        cartService.updateQuantity(1, 1, 5);
        assertEquals(5, cartService.getCart(1).getItems().get(0).getQuantity());
    }

    @Test
    void serviceRejectsInvalidUnavailableAndOverstockRequests() {
        assertThrows(CartValidationException.class, () -> cartService.addToCart(1, 1, 0));
        assertThrows(CartValidationException.class, () -> cartService.addToCart(1, 1, 6));
        assertThrows(CartValidationException.class, () -> cartService.addToCart(1, 999, 1));
        assertThrows(CartValidationException.class, () -> cartService.addToCart(1, 2, 1));
        assertThrows(CartValidationException.class, () -> cartService.addToCart(1, 3, 1));
        assertThrows(CartValidationException.class, () -> cartService.getCart(0));
    }

    @Test
    void usersCanOnlySeeAndChangeTheirOwnCartByAuthenticatedUserId() {
        cartService.addToCart(1, 1, 2);
        assertEquals(2, cartService.getCart(1).getItemCount());
        assertTrue(cartService.getCart(2).isEmpty());

        cartService.addToCart(2, 1, 1);
        assertEquals(1, cartService.getCart(2).getItemCount());
        cartService.removeItem(2, 1);
        assertTrue(cartService.getCart(2).isEmpty());
        assertEquals(2, cartService.getCart(1).getItemCount());
    }

    @Test
    void clearAndRemoveLeaveAnEmptyCartView() {
        cartService.addToCart(1, 1, 1);
        cartService.addToCart(1, 4, 1);
        cartService.removeItem(1, 1);
        assertFalse(cartService.getCart(1).isEmpty());
        cartService.clearCart(1);
        assertTrue(cartService.getCart(1).isEmpty());
        assertEquals(BigDecimal.ZERO, cartService.getCart(1).getSubtotal());
    }

    private void insertFixtures() throws Exception {
        try (Connection connection = pool.getConnection()) {
            insertUser(connection, 1, "Buyer One", "buyer1@cart.test");
            insertUser(connection, 2, "Buyer Two", "buyer2@cart.test");
            insertUser(connection, 3, "Seller", "seller@cart.test");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'General')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (1, 3, 1, 'Available', 10.00, 5, TRUE)");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (2, 3, 1, 'Out', 10.00, 0, TRUE)");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (3, 3, 1, 'Inactive', 10.00, 5, FALSE)");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (4, 3, 1, 'Second', 8.00, 2, TRUE)");
        }
    }

    private void insertUser(Connection connection, long id, String name, String email) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (id, name, email, password_hash, role) VALUES (?, ?, ?, 'hash', 'BUYER')")) {
            statement.setLong(1, id);
            statement.setString(2, name);
            statement.setString(3, email);
            statement.executeUpdate();
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

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
