package com.ashokmart.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInfrastructureTest {
    private DatabaseConnectionPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.close();
        }
    }

    @Test
    void loadsSafeLocalDefaults() {
        DatabaseConfig config = DatabaseConfig.load();
        assertEquals(DatabaseConfig.DEFAULT_JDBC_URL, config.getJdbcUrl());
        assertEquals(DatabaseConfig.DEFAULT_USER, config.getUsername());
        assertEquals(DatabaseConfig.DEFAULT_MAX_POOL_SIZE, config.getMaximumPoolSize());
        assertEquals(DatabaseConfig.DEFAULT_MIN_IDLE, config.getMinimumIdle());
    }

    @Test
    void systemPropertyOverridesJdbcUrl() {
        String original = System.getProperty(DatabaseConfig.JDBC_URL_PROPERTY);
        try {
            System.setProperty(DatabaseConfig.JDBC_URL_PROPERTY, "jdbc:h2:mem:override");
            assertEquals("jdbc:h2:mem:override", DatabaseConfig.load().getJdbcUrl());
        } finally {
            if (original == null) {
                System.clearProperty(DatabaseConfig.JDBC_URL_PROPERTY);
            } else {
                System.setProperty(DatabaseConfig.JDBC_URL_PROPERTY, original);
            }
        }
    }

    @Test
    void poolProvidesValidConnectionsAndClosesCleanly() throws SQLException {
        pool = newPool();
        assertFalse(pool.isClosed());
        try (Connection connection = pool.getConnection()) {
            assertNotNull(connection);
            assertTrue(connection.isValid(2));
        }
        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    void schemaInitializesThroughThePooledDatasource() throws Exception {
        pool = newPool();
        DatabaseInitializer.initialize(pool);
        try (Connection connection = pool.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
            Set<String> names = new java.util.HashSet<>();
            while (tables.next()) {
                names.add(tables.getString("TABLE_NAME"));
            }
            assertTrue(names.containsAll(Set.of("USERS", "CATEGORIES", "PRODUCTS", "CART", "CART_ITEMS",
                    "ORDERS", "ORDER_ITEMS", "REVIEWS")));
        }
    }

    private DatabaseConnectionPool newPool() {
        String databaseName = "pool_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        return pool;
    }
}
