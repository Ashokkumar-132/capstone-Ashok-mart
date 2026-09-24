package com.ashokmart.dao.impl;

import com.ashokmart.dao.CategoryDao;
import com.ashokmart.model.Category;
import com.ashokmart.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CategoryDaoImpl implements CategoryDao {
    private static final String COLUMNS = "id, name, description, created_at";
    private final DatabaseConnectionPool pool;

    public CategoryDaoImpl(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM categories ORDER BY name ASC, id ASC";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (result.next()) {
                categories.add(map(result));
            }
            return categories;
        }
    }

    @Override
    public Optional<Category> findById(long id) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM categories WHERE id = ?", statement -> statement.setLong(1, id));
    }

    @Override
    public Optional<Category> findByName(String name) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM categories WHERE LOWER(name) = LOWER(?)", statement -> statement.setString(1, name));
    }

    private Optional<Category> findOne(String sql, Binder binder) throws SQLException {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private Category map(ResultSet result) throws SQLException {
        return new Category(result.getLong("id"), result.getString("name"), result.getString("description"),
                result.getTimestamp("created_at").toLocalDateTime());
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
