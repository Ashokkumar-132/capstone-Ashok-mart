package com.ashokmart;

import com.ashokmart.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;

final class TestDatabaseFixtures {
    private TestDatabaseFixtures() {
    }

    static void insertSellerCatalog(DatabaseConnectionPool pool) throws Exception {
        try (Connection connection = pool.getConnection()) {
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (3, 'Seller One', 'seller1@fixture.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (4, 'Seller Two', 'seller2@fixture.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name, description) VALUES (1, 'General', 'General products')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, description, price, stock_quantity, enabled) VALUES (2, 4, 1, 'Other seller product', 'Other description', 10.00, 4, TRUE)");
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
