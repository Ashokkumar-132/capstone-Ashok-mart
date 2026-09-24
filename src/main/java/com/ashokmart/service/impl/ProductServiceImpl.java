package com.ashokmart.service.impl;

import com.ashokmart.dao.ProductDao;
import com.ashokmart.model.Product;
import com.ashokmart.model.ProductPage;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSort;
import com.ashokmart.model.ProductSummary;
import com.ashokmart.service.CatalogValidationException;
import com.ashokmart.service.CategoryService;
import com.ashokmart.service.ProductManagementException;
import com.ashokmart.service.ProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ProductServiceImpl implements ProductService {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_PAGE = 1_000_000;
    private static final Pattern SAFE_IMAGE = Pattern.compile("^(https?://|/)[^\\s]+$", Pattern.CASE_INSENSITIVE);
    private final ProductDao productDao;
    private final CategoryService categoryService;

    public ProductServiceImpl(ProductDao productDao) {
        this(productDao, null);
    }

    public ProductServiceImpl(ProductDao productDao, CategoryService categoryService) {
        this.productDao = productDao;
        this.categoryService = categoryService;
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
        if (productId <= 0) return Optional.empty();
        try {
            return productDao.findActiveSummaryById(productId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Product lookup is temporarily unavailable", exception);
        }
    }

    @Override
    public List<Product> findSellerProducts(long authenticatedSellerId, int page, int pageSize) {
        requireSeller(authenticatedSellerId);
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        try {
            return List.copyOf(productDao.findBySellerId(authenticatedSellerId, safePage, safePageSize));
        } catch (SQLException exception) {
            throw new IllegalStateException("Seller products are temporarily unavailable", exception);
        }
    }

    @Override
    public List<Product> findAllSellerProducts(long authenticatedSellerId) {
        requireSeller(authenticatedSellerId);
        try {
            return List.copyOf(productDao.findAllBySellerId(authenticatedSellerId));
        } catch (SQLException exception) {
            throw new IllegalStateException("Seller products are temporarily unavailable", exception);
        }
    }

    @Override
    public Optional<Product> findSellerProduct(long authenticatedSellerId, long productId) {
        requireSeller(authenticatedSellerId);
        if (productId <= 0) return Optional.empty();
        try {
            return productDao.findByIdAndSellerId(productId, authenticatedSellerId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Seller product is temporarily unavailable", exception);
        }
    }

    @Override
    public long createSellerProduct(long authenticatedSellerId, Product product) {
        requireSeller(authenticatedSellerId);
        Product prepared = validateAndPrepare(authenticatedSellerId, product);
        prepared.setActive(true);
        try {
            return productDao.create(prepared);
        } catch (SQLException exception) {
            throw new IllegalStateException("Product could not be created", exception);
        }
    }

    @Override
    public void updateSellerProduct(long authenticatedSellerId, Product product) {
        requireSeller(authenticatedSellerId);
        if (product == null || product.getId() <= 0) throw new ProductManagementException("A valid product is required");
        Product prepared = validateAndPrepare(authenticatedSellerId, product);
        try {
            if (!productDao.updateOwned(prepared, authenticatedSellerId)) {
                throw new ProductManagementException("Product not found");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Product could not be updated", exception);
        }
    }

    @Override
    public void updateSellerProductStatus(long authenticatedSellerId, long productId, boolean active) {
        requireSeller(authenticatedSellerId);
        if (productId <= 0) throw new ProductManagementException("A valid product is required");
        try {
            if (!productDao.updateStatus(productId, authenticatedSellerId, active)) {
                throw new ProductManagementException("Product not found");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Product status could not be updated", exception);
        }
    }

    @Override
    public boolean isOwner(Product product, long authenticatedUserId) {
        return product != null && authenticatedUserId > 0 && product.getSellerId() == authenticatedUserId;
    }

    private Product validateAndPrepare(long sellerId, Product product) {
        if (product == null) throw new ProductManagementException("Product data is required");
        String name = product.getName() == null ? "" : product.getName().trim();
        if (name.isEmpty() || name.length() > 255) throw new ProductManagementException("Product name is required and must be 255 characters or fewer");
        String description = product.getDescription() == null ? null : product.getDescription().trim();
        if (description != null && description.isEmpty()) description = null;
        if (description != null && description.length() > 4000) throw new ProductManagementException("Description must be 4000 characters or fewer");
        if (product.getCategoryId() <= 0 || categoryService == null || categoryService.findCategory(product.getCategoryId()).isEmpty()) {
            throw new ProductManagementException("Please select a valid category");
        }
        if (product.getPrice() == null || product.getPrice().signum() < 0) throw new ProductManagementException("Price must be zero or greater");
        BigDecimal price;
        try {
            price = product.getPrice().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ProductManagementException("Price can have at most two decimal places");
        }
        if (product.getStockQuantity() < 0) throw new ProductManagementException("Stock must be zero or greater");
        String imageUrl = product.getImageUrl() == null ? null : product.getImageUrl().trim();
        if (imageUrl != null && imageUrl.isEmpty()) imageUrl = null;
        if (imageUrl != null && (imageUrl.length() > 1000 || !SAFE_IMAGE.matcher(imageUrl).matches())) {
            throw new ProductManagementException("Image must be a valid HTTP(S) URL or application path");
        }
        Product prepared = new Product(product.getId(), sellerId, product.getCategoryId(), name, description,
                price, product.getStockQuantity(), imageUrl, product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
        return prepared;
    }

    private void requireSeller(long sellerId) {
        if (sellerId <= 0) throw new ProductManagementException("You must be logged in as a seller");
    }

    private ProductSearchCriteria normalize(ProductSearchCriteria criteria) {
        if (criteria == null) return new ProductSearchCriteria(null, null, null, null, false, ProductSort.NEWEST, DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
        BigDecimal minimum = normalizePrice(criteria.minimumPrice());
        BigDecimal maximum = normalizePrice(criteria.maximumPrice());
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) throw new CatalogValidationException("Minimum price cannot exceed maximum price");
        if (criteria.categoryId() != null && criteria.categoryId() <= 0) throw new CatalogValidationException("Category must be positive");
        String search = criteria.searchTerm() == null ? null : criteria.searchTerm().trim().toLowerCase();
        if (search != null && search.length() > 100) search = search.substring(0, 100);
        return new ProductSearchCriteria(search, criteria.categoryId(), minimum, maximum, criteria.inStockOnly(),
                criteria.sort() == null ? ProductSort.NEWEST : criteria.sort(), normalizePage(criteria.page()), normalizePageSize(criteria.pageSize()));
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return null;
        if (price.signum() < 0) throw new CatalogValidationException("Price filters cannot be negative");
        return price;
    }

    private int normalizePageSize(int pageSize) { return pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE); }
    private int normalizePage(int page) { return page <= 0 ? DEFAULT_PAGE : Math.min(page, MAX_PAGE); }
}
