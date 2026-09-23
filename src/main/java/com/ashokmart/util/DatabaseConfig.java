package com.ashokmart.util;

/**
 * Immutable database settings loaded from system properties, environment
 * variables, or safe local defaults. System properties take precedence.
 */
public final class DatabaseConfig {
    public static final String JDBC_URL_PROPERTY = "ashokmart.db.url";
    public static final String USER_PROPERTY = "ashokmart.db.user";
    public static final String PASSWORD_PROPERTY = "ashokmart.db.password";
    public static final String MAX_POOL_SIZE_PROPERTY = "ashokmart.db.maxPoolSize";
    public static final String MIN_IDLE_PROPERTY = "ashokmart.db.minIdle";
    public static final String CONNECTION_TIMEOUT_PROPERTY = "ashokmart.db.connectionTimeoutMs";

    public static final String DEFAULT_JDBC_URL = "jdbc:h2:./data/ashokmart;DB_CLOSE_ON_EXIT=FALSE";
    public static final String DEFAULT_USER = "sa";
    public static final String DEFAULT_PASSWORD = "";
    public static final int DEFAULT_MAX_POOL_SIZE = 10;
    public static final int DEFAULT_MIN_IDLE = 2;
    public static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30_000L;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long connectionTimeoutMs;

    public DatabaseConfig(String jdbcUrl, String username, String password,
                          int maximumPoolSize, int minimumIdle, long connectionTimeoutMs) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("JDBC URL must not be blank");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Database username must not be blank");
        }
        if (maximumPoolSize < 1) {
            throw new IllegalArgumentException("Maximum pool size must be at least 1");
        }
        if (minimumIdle < 0 || minimumIdle > maximumPoolSize) {
            throw new IllegalArgumentException("Minimum idle must be between 0 and maximum pool size");
        }
        if (connectionTimeoutMs < 250L) {
            throw new IllegalArgumentException("Connection timeout must be at least 250 milliseconds");
        }
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password == null ? "" : password;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public static DatabaseConfig load() {
        return new DatabaseConfig(
                setting(JDBC_URL_PROPERTY, "ASHOKMART_DB_URL", DEFAULT_JDBC_URL),
                setting(USER_PROPERTY, "ASHOKMART_DB_USER", DEFAULT_USER),
                setting(PASSWORD_PROPERTY, "ASHOKMART_DB_PASSWORD", DEFAULT_PASSWORD),
                integerSetting(MAX_POOL_SIZE_PROPERTY, "ASHOKMART_DB_MAX_POOL_SIZE", DEFAULT_MAX_POOL_SIZE),
                integerSetting(MIN_IDLE_PROPERTY, "ASHOKMART_DB_MIN_IDLE", DEFAULT_MIN_IDLE),
                longSetting(CONNECTION_TIMEOUT_PROPERTY, "ASHOKMART_DB_CONNECTION_TIMEOUT_MS", DEFAULT_CONNECTION_TIMEOUT_MS));
    }

    public static DatabaseConfig forTesting(String jdbcUrl) {
        return new DatabaseConfig(jdbcUrl, DEFAULT_USER, DEFAULT_PASSWORD, 2, 0, 5_000L);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    private static String setting(String property, String environment, String defaultValue) {
        String systemValue = System.getProperty(property);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String environmentValue = System.getenv(environment);
        return environmentValue == null || environmentValue.isBlank() ? defaultValue : environmentValue;
    }

    private static int integerSetting(String property, String environment, int defaultValue) {
        String value = setting(property, environment, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer database setting: " + property, exception);
        }
    }

    private static long longSetting(String property, String environment, long defaultValue) {
        String value = setting(property, environment, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid numeric database setting: " + property, exception);
        }
    }
}
