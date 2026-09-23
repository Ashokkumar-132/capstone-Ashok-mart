package com.ashokmart.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Initializes the structural H2 schema for development or tests. */
public final class SchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaInitializer.class);
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    private SchemaInitializer() {
    }

    /** Opens the configured development H2 connection and applies schema.sql. */
    public static void initialize() throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection(
                DatabaseConfig.URL, DatabaseConfig.USER, DatabaseConfig.PASSWORD)) {
            initialize(connection);
        }
    }

    /** Applies schema.sql to an existing connection, which is useful for tests. */
    public static void initialize(Connection connection) throws SQLException, IOException {
        String schema = readSchema();
        int statementCount = 0;
        for (String sql : schema.split(";")) {
            String statement = sql.trim();
            if (statement.isEmpty()) {
                continue;
            }
            try (Statement sqlStatement = connection.createStatement()) {
                sqlStatement.execute(statement);
                statementCount++;
            }
        }
        LOGGER.info("AshokMart database schema initialized with {} statements", statementCount);
    }

    private static String readSchema() throws IOException {
        try (InputStream input = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
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
