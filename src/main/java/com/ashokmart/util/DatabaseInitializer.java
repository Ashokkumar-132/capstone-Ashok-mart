package com.ashokmart.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Applies the existing classpath schema.sql through a configured datasource. */
public final class DatabaseInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    private DatabaseInitializer() {
    }

    public static void initialize(DatabaseConnectionPool pool) throws SQLException, IOException {
        initialize(pool.getDataSource());
    }

    public static void initialize(DataSource dataSource) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection()) {
            initialize(connection);
        } catch (SQLException | IOException exception) {
            LOGGER.error("AshokMart database schema initialization failed", exception);
            throw exception;
        }
    }

    /** Package-independent overload retained for direct schema verification. */
    public static void initialize(Connection connection) throws SQLException, IOException {
        String schema = readSchema();
        int statementCount = 0;
        for (String sql : schema.split(";")) {
            String statement = sql.trim();
            if (statement.isEmpty()) {
                continue;
            }
            try (Statement schemaStatement = connection.createStatement()) {
                schemaStatement.execute(statement);
                statementCount++;
            }
        }
        LOGGER.info("AshokMart database schema initialized with {} statements", statementCount);
    }

    private static String readSchema() throws IOException {
        try (InputStream input = DatabaseInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing database schema resource: " + SCHEMA_RESOURCE);
            }
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.stream(schema.split("\\R"))
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
        }
    }
}
