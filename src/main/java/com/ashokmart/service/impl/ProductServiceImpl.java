package com.ashokmart.service.impl;

import com.ashokmart.dao.ProductDao;
import com.ashokmart.model.Product;
import com.ashokmart.model.ProductPage;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSort;
import com.ashokmart.model.ProductSummary;
import com.ashokmart.service.CatalogValidationException;
import com.ashokmart.service.ProductService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class ProductServiceImpl implements ProductService {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_PAGE = 1_000_000;
    private final ProductDao productDao;

    public ProductServiceImpl(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public ProductPage search(ProductSearchCriteria criteria) {
        ProductSearchCriteria normalized = normalize(criteria);
        try {
            List<ProductSummary> products = productDao.search(normalized);
            long totalResults = productDao.count(normalized);
            int totalPages = totalResults == 0 ? 0 : (int) ((totalResults + normalized.pageSize() - 1) / normalized.pageSize());
            return new ProductPage(products, normalized.page(), normalized.pageSize(), totalResults, totalPages);
        } catch (SQLException exception) {
            throw new IllegalStateException("Products are temporarily unavailable", exception);
        }
    }

    @Override
    public Optional<ProductSummary> findProduct(long productId) {
        if (productId <= 0) {
            return Optional.empty();
        }
        try {
            return productDao.findActiveSummaryById(productId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Product lookup is temporarily unavailable", exception);
        }
    }

    @Override
    public List<Product> findSellerProducts(long authenticatedSellerId, int page, int pageSize) {
        if (authenticatedSellerId <= 0) {
            throw new CatalogValidationException("A valid authenticated seller is required");
        }
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        try {
            return List.copyOf(productDao.findBySellerId(authenticatedSellerId, safePage, safePageSize));
        } catch (SQLException exception) {
            throw new IllegalStateException("Seller products are temporarily unavailable", exception);
        }
    }

    @Override
    public boolean isOwner(Product product, long authenticatedUserId) {
        return product != null && authenticatedUserId > 0 && product.getSellerId() == authenticatedUserId;
    }

    private ProductSearchCriteria normalize(ProductSearchCriteria criteria) {
        if (criteria == null) {
            return new ProductSearchCriteria(null, null, null, null, false, ProductSort.NEWEST,
                    DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
        }
        BigDecimal minimum = normalizePrice(criteria.minimumPrice());
        BigDecimal maximum = normalizePrice(criteria.maximumPrice());
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new CatalogValidationException("Minimum price cannot exceed maximum price");
        }
        if (criteria.categoryId() != null && criteria.categoryId() <= 0) {
            throw new CatalogValidationException("Category must be positive");
        }
        String search = criteria.searchTerm() == null ? null : criteria.searchTerm().trim().toLowerCase();
        if (search != null && search.length() > 100) {
            search = search.substring(0, 100);
        }
        return new ProductSearchCriteria(search, criteria.categoryId(), minimum, maximum, criteria.inStockOnly(),
                criteria.sort() == null ? ProductSort.NEWEST : criteria.sort(),
                normalizePage(criteria.page()), normalizePageSize(criteria.pageSize()));
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            return null;
        }
        if (price.signum() < 0) {
            throw new CatalogValidationException("Price filters cannot be negative");
        }
        return price;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int normalizePage(int page) {
        if (page <= 0) {
            return DEFAULT_PAGE;
        }
        return Math.min(page, MAX_PAGE);
    }
}
