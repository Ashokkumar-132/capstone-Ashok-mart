package com.ashokmart.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/** Encapsulates one application-wide HikariCP datasource. */
public final class DatabaseConnectionPool implements AutoCloseable {
    public static final String CONTEXT_ATTRIBUTE = DatabaseConnectionPool.class.getName();

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnectionPool.class);
    private final HikariDataSource dataSource;

    public DatabaseConnectionPool(DatabaseConfig databaseConfig) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(databaseConfig.getJdbcUrl());
        hikariConfig.setUsername(databaseConfig.getUsername());
        hikariConfig.setPassword(databaseConfig.getPassword());
        hikariConfig.setMaximumPoolSize(databaseConfig.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(databaseConfig.getMinimumIdle());
        hikariConfig.setConnectionTimeout(databaseConfig.getConnectionTimeoutMs());
        hikariConfig.setPoolName("AshokMart-HikariPool");
        this.dataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("AshokMart HikariCP pool initialized: maxPoolSize={}, minIdle={}",
                databaseConfig.getMaximumPoolSize(), databaseConfig.getMinimumIdle());
    }

    public static DatabaseConnectionPool createDefault() {
        return new DatabaseConnectionPool(DatabaseConfig.load());
    }

    /** Returns a pooled connection; callers must close it with try-with-resources. */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    HikariDataSource getDataSource() {
        return dataSource;
    }

    public boolean isClosed() {
        return dataSource.isClosed();
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("AshokMart HikariCP pool shut down");
        }
    }
}
