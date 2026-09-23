package com.ashokmart.dao.impl;

import com.ashokmart.dao.UserDao;
import com.ashokmart.model.User;
import com.ashokmart.model.UserRole;
import com.ashokmart.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class UserDaoImpl implements UserDao {
    private static final String USER_COLUMNS = "id, name, email, password_hash, role, enabled, created_at";
    private final DatabaseConnectionPool pool;

    public UserDaoImpl(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public User create(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role, enabled) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole().name());
            statement.setBoolean(5, user.isEnabled());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("User insert did not return a generated ID");
                }
                user.setId(keys.getLong(1));
            }
        }
        return findById(user.getId()).orElseThrow(() -> new SQLException("Created user could not be loaded"));
    }

    @Override
    public Optional<User> findByEmail(String email) throws SQLException {
        return findOne("SELECT " + USER_COLUMNS + " FROM users WHERE email = ?", email);
    }

    @Override
    public Optional<User> findById(long id) throws SQLException {
        return findOne("SELECT " + USER_COLUMNS + " FROM users WHERE id = ?", id);
    }

    @Override
    public boolean existsByEmail(String email) throws SQLException {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    @Override
    public void updateEnabled(long id, boolean enabled) throws SQLException {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET enabled = ? WHERE id = ?")) {
            statement.setBoolean(1, enabled);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    private Optional<User> findOne(String sql, Object value) throws SQLException {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (value instanceof String string) {
                statement.setString(1, string);
            } else {
                statement.setLong(1, (Long) value);
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapUser(result)) : Optional.empty();
            }
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(
                result.getLong("id"),
                result.getString("name"),
                result.getString("email"),
                result.getString("password_hash"),
                UserRole.valueOf(result.getString("role")),
                result.getBoolean("enabled"),
                result.getTimestamp("created_at").toLocalDateTime());
    }
}
