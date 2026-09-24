package com.ashokmart;

import com.ashokmart.dao.ProductDao;
import com.ashokmart.dao.impl.CategoryDaoImpl;
import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.model.Product;
import com.ashokmart.service.ProductManagementException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerProductBackendTest {
    private DatabaseConnectionPool pool;
    private ProductDao productDao;
    private ProductService productService;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "seller_products_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        TestDatabaseFixtures.insertSellerCatalog(pool);
        productDao = new ProductDaoImpl(pool);
        productService = new ProductServiceImpl(productDao, new CategoryServiceImpl(new CategoryDaoImpl(pool)));
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void daoScopesReadsAndMutationsToSellerOwnership() throws Exception {
        Product created = new Product(0, 3, 1, "Seller Product", "Description", new BigDecimal("12.50"), 8, "/images/p.jpg", true, null, null);
        long id = productDao.create(created);

        assertTrue(productDao.findByIdAndSellerId(id, 3).isPresent());
        assertTrue(productDao.findByIdAndSellerId(id, 4).isEmpty());
        Product changed = new Product(id, 999, 1, "Updated", "Changed", new BigDecimal("14.00"), 5, null, true, null, null);
        assertTrue(productDao.updateOwned(changed, 3));
        assertFalse(productDao.updateOwned(changed, 4));
        assertFalse(productDao.updateStatus(id, 4, false));
        assertTrue(productDao.updateStatus(id, 3, false));
        assertFalse(productDao.findByIdAndSellerId(id, 3).orElseThrow().isActive());
    }

    @Test
    void serviceAssignsAuthenticatedSellerAndReturnsOnlyTheirProducts() {
        Product submitted = product(999, "New product");
        long id = productService.createSellerProduct(3, submitted);

        Product saved = productService.findSellerProduct(3, id).orElseThrow();
        assertEquals(3, saved.getSellerId());
        assertEquals(1, productService.findAllSellerProducts(3).size());
        assertTrue(productService.findSellerProduct(4, id).isEmpty());
    }

    @Test
    void serviceRejectsInvalidFieldsAndInvalidCategories() {
        assertThrows(ProductManagementException.class, () -> productService.createSellerProduct(3, product(3, " ")));
        Product negativePrice = product(3, "Valid");
        negativePrice.setPrice(new BigDecimal("-1.00"));
        assertThrows(ProductManagementException.class, () -> productService.createSellerProduct(3, negativePrice));
        Product negativeStock = product(3, "Valid");
        negativeStock.setStockQuantity(-1);
        assertThrows(ProductManagementException.class, () -> productService.createSellerProduct(3, negativeStock));
        Product invalidCategory = product(99, "Valid");
        invalidCategory.setCategoryId(99);
        assertThrows(ProductManagementException.class, () -> productService.createSellerProduct(3, invalidCategory));
        Product invalidImage = product(1, "Valid");
        invalidImage.setImageUrl("file:///etc/passwd");
        assertThrows(ProductManagementException.class, () -> productService.createSellerProduct(3, invalidImage));
    }

    @Test
    void crossSellerUpdateIsRejectedWithoutChangingProduct() {
        Product otherSellerProduct = product(2, "Other seller product");
        assertThrows(ProductManagementException.class, () -> productService.updateSellerProduct(3, otherSellerProduct));
        assertEquals("Other seller product", productService.findSellerProduct(4, 2).orElseThrow().getName());
    }

    private Product product(long sellerId, String name) {
        return new Product(sellerId == 2 ? 2 : 0, sellerId, 1, name, "Description", new BigDecimal("10.00"), 4, null, true, null, null);
    }
}
