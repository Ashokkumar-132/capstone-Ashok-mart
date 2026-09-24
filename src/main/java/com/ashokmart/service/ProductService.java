package com.ashokmart.service;

import com.ashokmart.model.Product;
import com.ashokmart.model.ProductPage;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSummary;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    ProductPage search(ProductSearchCriteria criteria);
    Optional<ProductSummary> findProduct(long productId);
    List<Product> findSellerProducts(long authenticatedSellerId, int page, int pageSize);
    List<Product> findAllSellerProducts(long authenticatedSellerId);
    Optional<Product> findSellerProduct(long authenticatedSellerId, long productId);
    long createSellerProduct(long authenticatedSellerId, Product product);
    void updateSellerProduct(long authenticatedSellerId, Product product);
    void updateSellerProductStatus(long authenticatedSellerId, long productId, boolean active);
    boolean isOwner(Product product, long authenticatedUserId);
}
