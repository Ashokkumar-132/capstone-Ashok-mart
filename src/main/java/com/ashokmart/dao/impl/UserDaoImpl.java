package com.ashokmart.dao.impl;

import com.ashokmart.dao.UserDao;
import com.ashokmart.model.AdminUserQuery;
import com.ashokmart.model.User;
import com.ashokmart.model.UserRole;
import com.ashokmart.util.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
                if (!keys.next()) throw new SQLException("User insert did not return a generated ID");
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
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    @Override
    public void updateEnabled(long id, boolean enabled) throws SQLException {
        updateUserStatus(id, enabled);
    }

    @Override
    public boolean updateUserStatus(long id, boolean enabled) throws SQLException {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET enabled = ? WHERE id = ?")) {
            statement.setBoolean(1, enabled);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public List<User> findAllUsers() throws SQLException {
        return queryUsers("SELECT " + USER_COLUMNS + " FROM users ORDER BY created_at DESC, id DESC", List.of());
    }

    @Override
    public List<User> findUsers(AdminUserQuery query) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String where = buildWhere(query, parameters);
        String sql = "SELECT " + USER_COLUMNS + " FROM users " + where
                + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        parameters.add(query.getPageSize());
        parameters.add((query.getPage() - 1) * query.getPageSize());
        return queryUsers(sql, parameters);
    }

    @Override
    public long countUsers(AdminUserQuery query) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String where = buildWhere(query, parameters);
        return count("SELECT COUNT(*) FROM users " + where, parameters);
    }

    @Override
    public long countUsers() throws SQLException {
        return count("SELECT COUNT(*) FROM users", List.of());
    }

    @Override
    public long countUsersByRole(UserRole role) throws SQLException {
        if (role == null) return 0;
        return count("SELECT COUNT(*) FROM users WHERE role = ?", List.of(role.name()));
    }

    @Override
    public long countUsersByStatus(boolean enabled) throws SQLException {
        return count("SELECT COUNT(*) FROM users WHERE enabled = ?", List.of(enabled));
    }

    @Override
    public long countUsersByRoleAndStatus(UserRole role, boolean enabled) throws SQLException {
        if (role == null) return 0;
        return count("SELECT COUNT(*) FROM users WHERE role = ? AND enabled = ?", List.of(role.name(), enabled));
    }

    private String buildWhere(AdminUserQuery query, List<Object> parameters) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        if (!query.getSearch().isBlank()) {
            where.append(" AND (LOWER(name) LIKE ? OR LOWER(email) LIKE ?)");
            String search = "%" + query.getSearch().toLowerCase(java.util.Locale.ROOT) + "%";
            parameters.add(search);
            parameters.add(search);
        }
        if (query.getRole() != null) {
            where.append(" AND role = ?");
            parameters.add(query.getRole().name());
        }
        if (query.getEnabled() != null) {
            where.append(" AND enabled = ?");
            parameters.add(query.getEnabled());
        }
        return where.toString();
    }

    private List<User> queryUsers(String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (result.next()) users.add(mapUser(result));
                return users;
            }
        }
    }

    private long count(String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object parameter = parameters.get(index);
            if (parameter instanceof String value) statement.setString(index + 1, value);
            else if (parameter instanceof Boolean value) statement.setBoolean(index + 1, value);
            else if (parameter instanceof Integer value) statement.setInt(index + 1, value);
            else if (parameter instanceof Long value) statement.setLong(index + 1, value);
            else statement.setObject(index + 1, parameter);
        }
    }

    private Optional<User> findOne(String sql, Object value) throws SQLException {
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (value instanceof String string) statement.setString(1, string);
            else statement.setLong(1, (Long) value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapUser(result)) : Optional.empty();
            }
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(result.getLong("id"), result.getString("name"), result.getString("email"),
                result.getString("password_hash"), UserRole.valueOf(result.getString("role")),
                result.getBoolean("enabled"), result.getTimestamp("created_at").toLocalDateTime());
    }
}
