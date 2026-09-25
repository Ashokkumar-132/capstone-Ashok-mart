package com.ashokmart.dao.impl;

import com.ashokmart.dao.ProductDao;
import com.ashokmart.model.AdminProductQuery;
import com.ashokmart.model.Product;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSort;
import com.ashokmart.model.ProductSummary;
import com.ashokmart.util.DatabaseConnectionPool;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProductDaoImpl implements ProductDao {
    private static final String PRODUCT_COLUMNS = "p.id, p.seller_id, p.category_id, p.name, p.description, p.price, "
            + "p.stock_quantity, p.image_url, p.enabled, p.created_at, p.updated_at";
    private static final String SUMMARY_COLUMNS = "p.id, p.seller_id, seller.name AS seller_name, p.category_id, "
            + "category.name AS category_name, p.name, p.description, p.price, p.stock_quantity, p.image_url, p.enabled";
    private final DatabaseConnectionPool pool;

    public ProductDaoImpl(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public Optional<Product> findById(long id) throws SQLException {
        String sql = "SELECT " + PRODUCT_COLUMNS + " FROM products p WHERE p.id = ?";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapProduct(result)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<ProductSummary> findActiveSummaryById(long id) throws SQLException {
        String sql = summaryFrom() + " WHERE p.id = ? AND p.enabled = TRUE";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapSummary(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<ProductSummary> search(ProductSearchCriteria criteria) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String sql = summaryFrom() + whereClause(criteria, parameters) + sortClause(criteria.sort()) + " LIMIT ? OFFSET ?";
        parameters.add(criteria.pageSize());
        parameters.add(criteria.offset());
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                List<ProductSummary> products = new ArrayList<>();
                while (result.next()) {
                    products.add(mapSummary(result));
                }
                return products;
            }
        }
    }

    @Override
    public long count(ProductSearchCriteria criteria) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM products p" + whereClause(criteria, parameters);
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    @Override
    public List<Product> findBySellerId(long sellerId, int page, int pageSize) throws SQLException {
        String sql = "SELECT " + PRODUCT_COLUMNS + " FROM products p WHERE p.seller_id = ? "
                + "ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sellerId);
            statement.setInt(2, pageSize);
            statement.setInt(3, (page - 1) * pageSize);
            try (ResultSet result = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (result.next()) {
                    products.add(mapProduct(result));
                }
                return products;
            }
        }
    }

    @Override
    public List<Product> findAllBySellerId(long sellerId) throws SQLException {
        String sql = "SELECT " + PRODUCT_COLUMNS + " FROM products p WHERE p.seller_id = ? "
                + "ORDER BY p.created_at DESC, p.id DESC";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sellerId);
            try (ResultSet result = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (result.next()) products.add(mapProduct(result));
                return products;
            }
        }
    }

    @Override
    public Optional<Product> findByIdAndSellerId(long productId, long sellerId) throws SQLException {
        String sql = "SELECT " + PRODUCT_COLUMNS + " FROM products p WHERE p.id = ? AND p.seller_id = ?";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            statement.setLong(2, sellerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapProduct(result)) : Optional.empty();
            }
        }
    }

    @Override
    public long create(Product product) throws SQLException {
        String sql = "INSERT INTO products (seller_id, category_id, name, description, price, stock_quantity, image_url, enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProduct(statement, product);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Product creation did not return an ID");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public boolean updateOwned(Product product, long sellerId) throws SQLException {
        String sql = "UPDATE products SET category_id = ?, name = ?, description = ?, price = ?, stock_quantity = ?, "
                + "image_url = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND seller_id = ?";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, product.getCategoryId());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setBigDecimal(4, product.getPrice());
            statement.setInt(5, product.getStockQuantity());
            statement.setString(6, product.getImageUrl());
            statement.setLong(7, product.getId());
            statement.setLong(8, sellerId);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean updateStatus(long productId, long sellerId, boolean active) throws SQLException {
        String sql = "UPDATE products SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND seller_id = ?";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setLong(2, productId);
            statement.setLong(3, sellerId);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public List<ProductSummary> findAdminProducts(AdminProductQuery query) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String sql = summaryFrom() + adminWhere(query, parameters) + " ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?";
        parameters.add(query.pageSize()); parameters.add(query.offset());
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) { List<ProductSummary> products = new ArrayList<>(); while (result.next()) products.add(mapSummary(result)); return products; }
        }
    }

    @Override
    public long countAdminProducts(AdminProductQuery query) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM products p" + adminWhere(query, parameters);
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { bind(statement, parameters); try (ResultSet result = statement.executeQuery()) { result.next(); return result.getLong(1); } }
    }

    @Override
    public Optional<ProductSummary> findAdminSummaryById(long productId) throws SQLException {
        String sql = summaryFrom() + " WHERE p.id = ?";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(mapSummary(result)) : Optional.empty(); }
        }
    }

    @Override
    public boolean updateAdminStatus(long productId, boolean active) throws SQLException {
        String sql = "UPDATE products SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setBoolean(1, active); statement.setLong(2, productId); return statement.executeUpdate() == 1; }
    }

    @Override
    public long countByStatus(Boolean active) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products" + (active == null ? "" : " WHERE enabled = ?");
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { if (active != null) statement.setBoolean(1, active); try (ResultSet result = statement.executeQuery()) { result.next(); return result.getLong(1); } }
    }

    @Override
    public long countByStock(boolean inStock) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE stock_quantity " + (inStock ? "> 0" : "= 0");
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) { result.next(); return result.getLong(1); }
    }

    private String adminWhere(AdminProductQuery query, List<Object> parameters) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (query.search() != null) { where.append(" AND (LOWER(p.name) LIKE ? OR LOWER(COALESCE(p.description, '')) LIKE ?)"); String value = "%" + query.search().toLowerCase() + "%"; parameters.add(value); parameters.add(value); }
        if (query.categoryId() != null) { where.append(" AND p.category_id = ?"); parameters.add(query.categoryId()); }
        if (query.active() != null) { where.append(" AND p.enabled = ?"); parameters.add(query.active()); }
        if (query.inStock() != null) where.append(query.inStock() ? " AND p.stock_quantity > 0" : " AND p.stock_quantity = 0");
        return where.toString();
    }

    private void bindProduct(PreparedStatement statement, Product product) throws SQLException {
        statement.setLong(1, product.getSellerId());
        statement.setLong(2, product.getCategoryId());
        statement.setString(3, product.getName());
        statement.setString(4, product.getDescription());
        statement.setBigDecimal(5, product.getPrice());
        statement.setInt(6, product.getStockQuantity());
        statement.setString(7, product.getImageUrl());
        statement.setBoolean(8, product.isActive());
    }

    private String summaryFrom() {
        return "SELECT " + SUMMARY_COLUMNS + " FROM products p "
                + "JOIN categories category ON category.id = p.category_id "
                + "JOIN users seller ON seller.id = p.seller_id";
    }

    private String whereClause(ProductSearchCriteria criteria, List<Object> parameters) {
        StringBuilder where = new StringBuilder(" WHERE p.enabled = TRUE");
        if (criteria.searchTerm() != null && !criteria.searchTerm().isBlank()) {
            where.append(" AND (LOWER(p.name) LIKE ? OR LOWER(COALESCE(p.description, '')) LIKE ?)");
            String search = "%" + criteria.searchTerm().toLowerCase() + "%";
            parameters.add(search);
            parameters.add(search);
        }
        if (criteria.categoryId() != null) {
            where.append(" AND p.category_id = ?");
            parameters.add(criteria.categoryId());
        }
        if (criteria.minimumPrice() != null) {
            where.append(" AND p.price >= ?");
            parameters.add(criteria.minimumPrice());
        }
        if (criteria.maximumPrice() != null) {
            where.append(" AND p.price <= ?");
            parameters.add(criteria.maximumPrice());
        }
        if (criteria.inStockOnly()) {
            where.append(" AND p.stock_quantity > 0");
        }
        return where.toString();
    }

    private String sortClause(ProductSort sort) {
        return switch (sort) {
            case PRICE_ASC -> " ORDER BY p.price ASC, p.id ASC";
            case PRICE_DESC -> " ORDER BY p.price DESC, p.id DESC";
            case NAME_ASC -> " ORDER BY LOWER(p.name) ASC, p.id ASC";
            case NAME_DESC -> " ORDER BY LOWER(p.name) DESC, p.id DESC";
            case NEWEST -> " ORDER BY p.created_at DESC, p.id DESC";
        };
    }

    private void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object parameter = parameters.get(index);
            int position = index + 1;
            if (parameter instanceof String value) {
                statement.setString(position, value);
            } else if (parameter instanceof Long value) {
                statement.setLong(position, value);
            } else if (parameter instanceof BigDecimal value) {
                statement.setBigDecimal(position, value);
            } else if (parameter instanceof Integer value) {
                statement.setInt(position, value);
            } else if (parameter instanceof Boolean value) {
                statement.setBoolean(position, value);
            } else {
                throw new SQLException("Unsupported catalog query parameter");
            }
        }
    }

    private Product mapProduct(ResultSet result) throws SQLException {
        return new Product(result.getLong("id"), result.getLong("seller_id"), result.getLong("category_id"),
                result.getString("name"), result.getString("description"), result.getBigDecimal("price"),
                result.getInt("stock_quantity"), result.getString("image_url"), result.getBoolean("enabled"),
                result.getTimestamp("created_at").toLocalDateTime(), result.getTimestamp("updated_at").toLocalDateTime());
    }

    private ProductSummary mapSummary(ResultSet result) throws SQLException {
        return new ProductSummary(result.getLong("id"), result.getLong("seller_id"), result.getString("seller_name"),
                result.getLong("category_id"), result.getString("category_name"), result.getString("name"),
                result.getString("description"), result.getBigDecimal("price"), result.getInt("stock_quantity"),
                result.getString("image_url"), result.getBoolean("enabled"));
    }
}
