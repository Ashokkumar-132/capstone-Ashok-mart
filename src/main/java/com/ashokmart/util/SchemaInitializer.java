package com.ashokmart.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Compatibility facade for the Commit 02 initializer. New application code
 * should use DatabaseInitializer with DatabaseConnectionPool.
 */
@Deprecated(forRemoval = false)
public final class SchemaInitializer {
    private SchemaInitializer() {
    }

    public static void initialize() throws SQLException, IOException {
        try (DatabaseConnectionPool pool = DatabaseConnectionPool.createDefault()) {
            DatabaseInitializer.initialize(pool);
        }
    }

    public static void initialize(Connection connection) throws SQLException, IOException {
        DatabaseInitializer.initialize(connection);
    }
}
