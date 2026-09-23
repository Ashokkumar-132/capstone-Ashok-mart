package com.ashokmart.util;

/**
 * Development/test H2 configuration. Production configuration can be supplied
 * through environment variables in a later deployment commit.
 */
public final class DatabaseConfig {
    public static final String URL = environmentOrDefault(
            "ASHOKMART_DB_URL", "jdbc:h2:mem:ashokmart_dev;DB_CLOSE_DELAY=-1");
    public static final String USER = environmentOrDefault("ASHOKMART_DB_USER", "sa");
    public static final String PASSWORD = System.getenv().getOrDefault("ASHOKMART_DB_PASSWORD", "");

    private DatabaseConfig() {
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
