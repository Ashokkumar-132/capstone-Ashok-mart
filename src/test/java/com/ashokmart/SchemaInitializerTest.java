package com.ashokmart;

import com.ashokmart.util.SchemaInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaInitializerTest {
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "test_" + UUID.randomUUID().toString().replace('-', '_');
        connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseName, "sa", "");
        SchemaInitializer.initialize(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void createsAllRequiredTablesAndKeyRelationships() throws SQLException {
        Set<String> tables;
        try (ResultSet result = connection.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
            tables = resultToSet(result, "TABLE_NAME");
        }

        assertTrue(tables.containsAll(Set.of("USERS", "CATEGORIES", "PRODUCTS", "CART", "CART_ITEMS",
                "ORDERS", "ORDER_ITEMS", "REVIEWS")));

        for (String table : Set.of("USERS", "CATEGORIES", "PRODUCTS", "CART", "CART_ITEMS",
                "ORDERS", "ORDER_ITEMS", "REVIEWS")) {
            try (ResultSet keys = connection.getMetaData().getPrimaryKeys(null, "PUBLIC", table)) {
                assertTrue(keys.next(), "Expected a primary key on " + table);
            }
        }

        try (ResultSet keys = connection.getMetaData().getImportedKeys(null, "PUBLIC", "PRODUCTS")) {
            Set<String> referencedTables = resultToSet(keys, "PKTABLE_NAME");
            assertTrue(referencedTables.containsAll(Set.of("USERS", "CATEGORIES")));
        }
        try (ResultSet keys = connection.getMetaData().getImportedKeys(null, "PUBLIC", "ORDER_ITEMS")) {
            Set<String> referencedTables = resultToSet(keys, "PKTABLE_NAME");
            assertTrue(referencedTables.containsAll(Set.of("ORDERS", "PRODUCTS", "USERS")));
        }
    }

    @Test
    void enforcesUserEmailUniqueness() throws SQLException {
        insertUser(1, "Buyer", "buyer@example.test", "BUYER");
        assertThrows(SQLException.class, () -> insertUser(2, "Other", "buyer@example.test", "BUYER"));
    }

    @Test
    void enforcesProductPriceAndStockConstraints() throws SQLException {
        insertUser(1, "Seller", "seller@example.test", "SELLER");
        insertCategory(1);
        assertThrows(SQLException.class, () -> execute("INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity) VALUES (1, 1, 1, 'Bad price', -1, 1)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity) VALUES (2, 1, 1, 'Bad stock', 1, -1)"));
    }

    @Test
    void enforcesCartAndCartItemUniquenessAndQuantity() throws SQLException {
        insertUser(1, "Buyer", "cart@example.test", "BUYER");
        insertUser(2, "Seller", "cart-seller@example.test", "SELLER");
        insertCategory(1);
        insertProduct(1);
        execute("INSERT INTO cart (id, user_id) VALUES (1, 1)");
        assertThrows(SQLException.class, () -> execute("INSERT INTO cart (id, user_id) VALUES (2, 1)"));
        execute("INSERT INTO cart_items (id, cart_id, product_id, quantity) VALUES (1, 1, 1, 1)");
        assertThrows(SQLException.class, () -> execute("INSERT INTO cart_items (id, cart_id, product_id, quantity) VALUES (2, 1, 1, 2)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO cart_items (id, cart_id, product_id, quantity) VALUES (3, 1, 1, 0)"));
    }

    @Test
    void rejectsInvalidOrderStatusAndNonPositiveOrderQuantity() throws SQLException {
        insertUser(1, "Buyer", "order-buyer@example.test", "BUYER");
        insertUser(2, "Seller", "order-seller@example.test", "SELLER");
        insertCategory(1);
        insertProduct(1);
        execute("INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (1, 1, 10, 'PENDING')");
        assertThrows(SQLException.class, () -> execute("INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (2, 1, 10, 'INVALID')"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (1, 1, 1, 2, 0, 10, 0)"));
    }

    @Test
    void acceptsRatingsOneThroughFiveRejectsOutsideRangeAndDuplicateReview() throws SQLException {
        insertUser(1, "Buyer", "review-buyer@example.test", "BUYER");
        insertUser(2, "Seller", "review-seller@example.test", "SELLER");
        insertCategory(1);
        for (long productId = 1; productId <= 5; productId++) {
            insertProduct(productId);
            execute("INSERT INTO reviews (id, product_id, buyer_id, rating, comment) VALUES (" + productId + ", " + productId + ", 1, " + productId + ", 'Review')");
        }
        assertThrows(SQLException.class, () -> execute("INSERT INTO reviews (id, product_id, buyer_id, rating) VALUES (6, 1, 1, 5)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO reviews (id, product_id, buyer_id, rating) VALUES (7, 1, 1, 0)"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO reviews (id, product_id, buyer_id, rating) VALUES (8, 1, 1, 6)"));
    }

    private void insertUser(long id, String name, String email, String role) throws SQLException {
        execute("INSERT INTO users (id, name, email, password_hash, role) VALUES (" + id + ", '" + name + "', '" + email + "', 'hash', '" + role + "')");
    }

    private void insertCategory(long id) throws SQLException {
        execute("INSERT INTO categories (id, name) VALUES (" + id + ", 'Category " + id + "')");
    }

    private void insertProduct(long id) throws SQLException {
        execute("INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity) VALUES (" + id + ", 2, 1, 'Product " + id + "', 10, 5)");
    }

    private void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static Set<String> resultToSet(ResultSet result, String column) throws SQLException {
        Set<String> values = new java.util.HashSet<>();
        while (result.next()) {
            values.add(result.getString(column));
        }
        return values;
    }
}
