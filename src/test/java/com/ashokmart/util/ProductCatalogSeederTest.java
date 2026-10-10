package com.ashokmart.util;

import com.ashokmart.dao.impl.CategoryDaoImpl;
import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCatalogSeederTest {
    private DatabaseConnectionPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.close();
        }
    }

    @Test
    void seedsAtLeast150ProductsAcrossRequiredCategoriesWithValidReferences() throws Exception {
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:seed_" + UUID.randomUUID()));
        DatabaseInitializer.initialize(pool);

        ProductCatalogSeeder.SeedReport report = ProductCatalogSeeder.seed(pool);

        assertEquals(150, report.insertedProducts());
        assertEquals(150, report.totalProducts());
        assertEquals(10, report.categories());
        try (Connection connection = pool.getConnection();
             ResultSet result = connection.createStatement().executeQuery(
                     "SELECT c.name, COUNT(p.id) AS product_count FROM categories c "
                             + "JOIN products p ON p.category_id = c.id GROUP BY c.name")) {
            Map<String, Integer> counts = new java.util.HashMap<>();
            while (result.next()) {
                counts.put(result.getString("name"), result.getInt("product_count"));
            }
            assertEquals(20, counts.get("Electronics"));
            assertEquals(20, counts.get("Fashion & Clothing"));
            assertEquals(20, counts.get("Home & Kitchen"));
            assertEquals(15, counts.get("Beauty & Personal Care"));
            assertEquals(15, counts.get("Grocery & Food"));
            assertEquals(15, counts.get("Sports & Fitness"));
            assertEquals(15, counts.get("Books & Stationery"));
            assertEquals(10, counts.get("Toys & Games"));
            assertEquals(10, counts.get("Mobile Accessories"));
            assertEquals(10, counts.get("Furniture & Home Decor"));
        }
        try (Connection connection = pool.getConnection();
             ResultSet result = connection.createStatement().executeQuery(
                     "SELECT COUNT(*) FROM products p JOIN users u ON u.id = p.seller_id "
                             + "JOIN categories c ON c.id = p.category_id "
                             + "WHERE u.role = 'SELLER' AND p.price >= 0 AND p.stock_quantity >= 0")) {
            result.next();
            assertEquals(150, result.getInt(1));
        }
    }

    @Test
    void reseedingDoesNotDuplicateProductsOrCreateExtraCategories() throws Exception {
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:seed_repeat_" + UUID.randomUUID()));
        DatabaseInitializer.initialize(pool);

        ProductCatalogSeeder.SeedReport first = ProductCatalogSeeder.seed(pool);
        ProductCatalogSeeder.SeedReport second = ProductCatalogSeeder.seed(pool);

        assertEquals(150, first.insertedProducts());
        assertEquals(0, first.existingProducts());
        assertEquals(0, second.insertedProducts());
        assertEquals(150, second.existingProducts());
        assertEquals(150, second.totalProducts());
        try (Connection connection = pool.getConnection();
             ResultSet result = connection.createStatement().executeQuery(
                     "SELECT COUNT(*), COUNT(DISTINCT name) FROM products")) {
            result.next();
            assertEquals(result.getInt(1), result.getInt(2));
            assertTrue(result.getInt(1) >= 150);
        }
    }

    @Test
    void seededProductsAreAvailableThroughCatalogSearchAndCategoryFiltering() throws Exception {
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:seed_catalog_" + UUID.randomUUID()));
        DatabaseInitializer.initialize(pool);
        ProductCatalogSeeder.seed(pool);

        long electronicsId = new CategoryDaoImpl(pool).findByName("Electronics").orElseThrow().getId();
        ProductDaoImpl products = new ProductDaoImpl(pool);
        var electronics = products.search(new ProductSearchCriteria(null, electronicsId, null, null, true,
                ProductSort.NAME_ASC, 1, 25));
        var wireless = products.search(new ProductSearchCriteria("wireless", null, null, null, true,
                ProductSort.PRICE_ASC, 1, 25));

        assertEquals(20, electronics.size());
        assertFalse(wireless.isEmpty());
        assertTrue(wireless.stream().allMatch(product -> product.getName().toLowerCase().contains("wireless")
                || product.getDescription().toLowerCase().contains("wireless")));
    }
}
