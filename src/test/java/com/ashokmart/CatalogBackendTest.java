package com.ashokmart;

import com.ashokmart.dao.CategoryDao;
import com.ashokmart.dao.ProductDao;
import com.ashokmart.dao.impl.CategoryDaoImpl;
import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.model.Category;
import com.ashokmart.model.Product;
import com.ashokmart.model.ProductPage;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSort;
import com.ashokmart.model.ProductSummary;
import com.ashokmart.service.CatalogValidationException;
import com.ashokmart.service.CategoryService;
import com.ashokmart.service.ProductService;
import com.ashokmart.service.impl.CategoryServiceImpl;
import com.ashokmart.service.impl.ProductServiceImpl;
import com.ashokmart.util.DatabaseConfig;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogBackendTest {
    private DatabaseConnectionPool pool;
    private CategoryDao categoryDao;
    private ProductDao productDao;
    private CategoryService categoryService;
    private ProductService productService;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "catalog_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        insertFixtures();
        categoryDao = new CategoryDaoImpl(pool);
        productDao = new ProductDaoImpl(pool);
        categoryService = new CategoryServiceImpl(categoryDao);
        productService = new ProductServiceImpl(productDao);
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void categoriesCanBeListedAndLookedUp() {
        List<Category> categories = categoryService.listCategories();
        assertEquals(2, categories.size());
        assertEquals("Books", categoryService.findCategory(2).orElseThrow().getName());
        assertEquals(1, categoryService.findCategoryByName(" electronics ").orElseThrow().getId());
        assertTrue(categoryService.findCategory(999).isEmpty());
        assertTrue(categoryService.findCategory(0).isEmpty());
    }

    @Test
    void activeProductDetailsIncludeCategoryAndSellerButInactiveProductsAreHidden() {
        ProductSummary product = productService.findProduct(1).orElseThrow();
        assertEquals("Alpha Phone", product.name());
        assertEquals("Electronics", product.categoryName());
        assertEquals("Seller One", product.sellerName());
        assertEquals(new BigDecimal("10.00"), product.price());
        assertTrue(product.isInStock());
        assertTrue(productService.findProduct(4).isEmpty());
        assertTrue(productService.findProduct(999).isEmpty());
    }

    @Test
    void catalogSupportsSearchFiltersStockAndWhitelistedSorting() {
        ProductPage search = productService.search(new ProductSearchCriteria("WIRELESS", null, null, null, false,
                ProductSort.NEWEST, 1, 20));
        assertEquals(List.of("Gamma Speaker"), search.products().stream().map(ProductSummary::name).toList());

        ProductPage category = productService.search(new ProductSearchCriteria(null, 1L, null, null, false,
                ProductSort.NAME_ASC, 1, 20));
        assertEquals(List.of("Alpha Phone", "Beta Cable"), category.products().stream().map(ProductSummary::name).toList());

        ProductPage price = productService.search(new ProductSearchCriteria(null, null, new BigDecimal("6"),
                new BigDecimal("16"), false, ProductSort.PRICE_ASC, 1, 20));
        assertEquals(List.of("Alpha Phone", "Delta Desk"), price.products().stream().map(ProductSummary::name).toList());

        ProductPage stock = productService.search(new ProductSearchCriteria(null, null, null, null, true,
                ProductSort.PRICE_ASC, 1, 20));
        assertEquals(3, stock.totalResults());
        assertTrue(stock.products().stream().allMatch(ProductSummary::isInStock));

        ProductPage fallback = productService.search(new ProductSearchCriteria(null, null, null, null, false,
                null, 1, 20));
        assertEquals("Delta Desk", fallback.products().get(0).name());
    }

    @Test
    void paginationReportsTotalsAndClampsUnsafeValues() {
        ProductPage page = productService.search(new ProductSearchCriteria(null, null, null, null, false,
                ProductSort.NAME_ASC, 2, 2));
        assertEquals(2, page.currentPage());
        assertEquals(2, page.pageSize());
        assertEquals(4, page.totalResults());
        assertEquals(2, page.totalPages());
        assertEquals(List.of("Delta Desk", "Gamma Speaker"), page.products().stream().map(ProductSummary::name).toList());

        ProductPage safe = productService.search(new ProductSearchCriteria(null, null, null, null, false,
                ProductSort.NEWEST, -5, 1000));
        assertEquals(1, safe.currentPage());
        assertEquals(ProductServiceImpl.MAX_PAGE_SIZE, safe.pageSize());
        assertEquals(4, safe.products().size());
    }

    @Test
    void invalidPriceRangeAndInjectionLikeSearchAreHandledSafely() {
        assertEquals(ProductSort.NEWEST, ProductSort.fromExternal("price_asc; DROP TABLE products"));
        assertEquals(ProductSort.PRICE_DESC, ProductSort.fromExternal(" PRICE_DESC "));
        assertThrows(CatalogValidationException.class, () -> productService.search(new ProductSearchCriteria(null, null,
                new BigDecimal("20"), new BigDecimal("10"), false, ProductSort.NEWEST, 1, 20)));
        assertThrows(CatalogValidationException.class, () -> productService.search(new ProductSearchCriteria(null, null,
                new BigDecimal("-1"), null, false, ProductSort.NEWEST, 1, 20)));

        ProductPage injection = productService.search(new ProductSearchCriteria("%' OR 1=1 --", null, null, null,
                false, ProductSort.NEWEST, 1, 20));
        assertEquals(0, injection.totalResults());
    }

    @Test
    void sellerQueriesAndOwnershipUseAuthenticatedSellerId() throws SQLException {
        List<Product> sellerProducts = productService.findSellerProducts(2, 1, 20);
        assertEquals(4, sellerProducts.size());
        assertTrue(productService.isOwner(sellerProducts.get(0), 2));
        assertFalse(productService.isOwner(sellerProducts.get(0), 3));
        assertFalse(productService.isOwner(sellerProducts.get(0), 0));
        assertThrows(CatalogValidationException.class, () -> productService.findSellerProducts(0, 1, 20));

        Product rawProduct = productDao.findById(4).orElseThrow();
        assertNotNull(rawProduct.getUpdatedAt());
        assertFalse(rawProduct.isActive());
    }

    private void insertFixtures() throws SQLException {
        try (Connection connection = pool.getConnection()) {
            insertUser(connection, 1, "Buyer", "buyer@catalog.test", "BUYER");
            insertUser(connection, 2, "Seller One", "seller1@catalog.test", "SELLER");
            insertUser(connection, 3, "Seller Two", "seller2@catalog.test", "SELLER");
            insertCategory(connection, 1, "Electronics");
            insertCategory(connection, 2, "Books");
            insertProduct(connection, 1, 2, 1, "Alpha Phone", "A smart phone", "10.00", 5, true);
            insertProduct(connection, 2, 2, 1, "Beta Cable", "Cable accessory", "20.00", 0, true);
            insertProduct(connection, 3, 3, 2, "Gamma Speaker", "Wireless audio speaker", "5.00", 3, true);
            insertProduct(connection, 4, 2, 1, "Hidden Stock", "Inactive product", "1.00", 10, false);
            insertProduct(connection, 5, 2, 2, "Delta Desk", "Wooden desk", "15.00", 2, true);
        }
    }

    private void insertUser(Connection connection, long id, String name, String email, String role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (id, name, email, password_hash, role) VALUES (?, ?, ?, ?, ?)")) {
            statement.setLong(1, id);
            statement.setString(2, name);
            statement.setString(3, email);
            statement.setString(4, "test-hash");
            statement.setString(5, role);
            statement.executeUpdate();
        }
    }

    private void insertCategory(Connection connection, long id, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO categories (id, name) VALUES (?, ?)")) {
            statement.setLong(1, id);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    private void insertProduct(Connection connection, long id, long sellerId, long categoryId, String name,
                               String description, String price, int stock, boolean enabled) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO products (id, seller_id, category_id, name, description, price, stock_quantity, enabled) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, id);
            statement.setLong(2, sellerId);
            statement.setLong(3, categoryId);
            statement.setString(4, name);
            statement.setString(5, description);
            statement.setBigDecimal(6, new BigDecimal(price));
            statement.setInt(7, stock);
            statement.setBoolean(8, enabled);
            statement.executeUpdate();
        }
    }
}
