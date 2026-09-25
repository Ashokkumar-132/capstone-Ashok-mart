package com.ashokmart.dao;

import com.ashokmart.model.Product;
import com.ashokmart.model.AdminProductQuery;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSummary;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ProductDao {
    Optional<Product> findById(long id) throws SQLException;
    Optional<ProductSummary> findActiveSummaryById(long id) throws SQLException;
    List<ProductSummary> search(ProductSearchCriteria criteria) throws SQLException;
    long count(ProductSearchCriteria criteria) throws SQLException;
    List<Product> findBySellerId(long sellerId, int page, int pageSize) throws SQLException;
    List<Product> findAllBySellerId(long sellerId) throws SQLException;
    Optional<Product> findByIdAndSellerId(long productId, long sellerId) throws SQLException;
    long create(Product product) throws SQLException;
    boolean updateOwned(Product product, long sellerId) throws SQLException;
    boolean updateStatus(long productId, long sellerId, boolean active) throws SQLException;
    List<ProductSummary> findAdminProducts(AdminProductQuery query) throws SQLException;
    long countAdminProducts(AdminProductQuery query) throws SQLException;
    Optional<ProductSummary> findAdminSummaryById(long productId) throws SQLException;
    boolean updateAdminStatus(long productId, boolean active) throws SQLException;
    long countByStatus(Boolean active) throws SQLException;
    long countByStock(boolean inStock) throws SQLException;
}
