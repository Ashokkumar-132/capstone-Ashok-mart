package com.ashokmart.service;
import com.ashokmart.model.*;
import java.util.Optional;
public interface AdminProductService {
    AdminProductPage getProducts(long adminId, AdminProductQuery query);
    Optional<ProductSummary> getProduct(long adminId, long productId);
    void setStatus(long adminId, long productId, boolean active);
}
