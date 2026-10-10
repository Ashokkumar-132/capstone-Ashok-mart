package com.ashokmart.util;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Seeds the real catalog with repeatable, database-backed marketplace products. */
public final class ProductCatalogSeeder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCatalogSeeder.class);
    private static final String RESOURCE = "/catalog-seed.psv";
    private static final String CATALOG_SELLER_EMAIL = "catalog-seller@ashokmart.local";

    private ProductCatalogSeeder() {
    }

    public static SeedReport seed(DatabaseConnectionPool pool) throws SQLException, IOException {
        try (Connection connection = pool.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                SeedReport report = seed(connection);
                connection.commit();
                LOGGER.info("AshokMart catalog seed complete: inserted={}, existing={}, categories={}, total={}",
                        report.insertedProducts(), report.existingProducts(), report.categories(), report.totalProducts());
                return report;
            } catch (SQLException | IOException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    static SeedReport seed(Connection connection) throws SQLException, IOException {
        Map<String, Long> categoryIds = ensureCategories(connection);
        long sellerId = findOrCreateSeller(connection);
        List<SeedProduct> products = readProducts();
        int inserted = 0;
        int existing = 0;
        String existsSql = "SELECT 1 FROM products WHERE name = ?";
        String insertSql = "INSERT INTO products (seller_id, category_id, name, description, price, stock_quantity, image_url, enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
        try (PreparedStatement exists = connection.prepareStatement(existsSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (SeedProduct product : products) {
                if (!categoryIds.containsKey(product.category())) {
                    throw new SQLException("Seed product references unknown category: " + product.category());
                }
                exists.setString(1, product.name());
                try (ResultSet result = exists.executeQuery()) {
                    if (result.next()) {
                        existing++;
                        continue;
                    }
                }
                insert.setLong(1, sellerId);
                insert.setLong(2, categoryIds.get(product.category()));
                insert.setString(3, product.name());
                insert.setString(4, product.description());
                insert.setBigDecimal(5, product.price());
                insert.setInt(6, product.stockQuantity());
                insert.setString(7, product.imageUrl());
                insert.addBatch();
                inserted++;
                if (inserted % 50 == 0) {
                    insert.executeBatch();
                }
            }
            insert.executeBatch();
        }
        long total = countProducts(connection);
        return new SeedReport(inserted, existing, categoryIds.size(), total);
    }

    private static Map<String, Long> ensureCategories(Connection connection) throws SQLException {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("Electronics", "Smart devices, audio, computing and home technology");
        descriptions.put("Fashion & Clothing", "Everyday clothing, footwear, watches and accessories");
        descriptions.put("Home & Kitchen", "Useful cookware, appliances, storage and household essentials");
        descriptions.put("Beauty & Personal Care", "Personal care, grooming, skincare and beauty products");
        descriptions.put("Grocery & Food", "Pantry staples, snacks, beverages and cooking ingredients");
        descriptions.put("Sports & Fitness", "Equipment, apparel and accessories for active lifestyles");
        descriptions.put("Books & Stationery", "Books, notebooks, writing supplies and study essentials");
        descriptions.put("Toys & Games", "Creative, educational and family play products");
        descriptions.put("Mobile Accessories", "Cases, chargers, cables and accessories for mobile devices");
        descriptions.put("Furniture & Home Decor", "Furniture, lighting, organisation and decorative accents");
        String findSql = "SELECT id FROM categories WHERE LOWER(name) = LOWER(?)";
        String insertSql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        Map<String, Long> ids = new LinkedHashMap<>();
        try (PreparedStatement find = connection.prepareStatement(findSql);
             PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            for (Map.Entry<String, String> category : descriptions.entrySet()) {
                find.setString(1, category.getKey());
                try (ResultSet result = find.executeQuery()) {
                    if (result.next()) {
                        ids.put(category.getKey(), result.getLong(1));
                        continue;
                    }
                }
                insert.setString(1, category.getKey());
                insert.setString(2, category.getValue());
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Category creation did not return an ID: " + category.getKey());
                    }
                    ids.put(category.getKey(), keys.getLong(1));
                }
            }
        }
        return ids;
    }

    private static long findOrCreateSeller(Connection connection) throws SQLException {
        String findSql = "SELECT id FROM users WHERE role = 'SELLER' AND enabled = TRUE ORDER BY id LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(findSql); ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                return result.getLong(1);
            }
        }
        String insertSql = "INSERT INTO users (name, email, password_hash, role, enabled) VALUES (?, ?, ?, 'SELLER', TRUE)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "AshokMart Catalog Seller");
            statement.setString(2, CATALOG_SELLER_EMAIL);
            statement.setString(3, BCrypt.hashpw(java.util.UUID.randomUUID().toString(), BCrypt.gensalt(10)));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Catalog seller creation did not return an ID");
                }
                return keys.getLong(1);
            }
        }
    }

    private static List<SeedProduct> readProducts() throws IOException {
        List<SeedProduct> products = new ArrayList<>();
        try (InputStream input = ProductCatalogSeeder.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing catalog seed resource: " + RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\|", -1);
                    if (fields.length != 6) {
                        throw new IOException("Invalid catalog seed row at line " + lineNumber);
                    }
                    BigDecimal price;
                    int stock;
                    try {
                        price = new BigDecimal(fields[3]).setScale(2);
                        stock = Integer.parseInt(fields[4]);
                    } catch (NumberFormatException exception) {
                        throw new IOException("Invalid price or stock at catalog seed line " + lineNumber, exception);
                    }
                    if (price.signum() < 0 || stock < 0 || fields[1].isBlank() || fields[2].isBlank()) {
                        throw new IOException("Invalid product values at catalog seed line " + lineNumber);
                    }
                    products.add(new SeedProduct(fields[0], fields[1], fields[2], price, stock, fields[5]));
                }
            }
        }
        return products;
    }

    private static long countProducts(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM products")) {
            result.next();
            return result.getLong(1);
        }
    }

    public record SeedReport(int insertedProducts, int existingProducts, int categories, long totalProducts) {
    }

    private record SeedProduct(String category, String name, String description, BigDecimal price,
                               int stockQuantity, String imageUrl) {
    }
}
