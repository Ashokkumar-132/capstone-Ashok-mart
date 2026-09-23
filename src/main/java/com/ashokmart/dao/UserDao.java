package com.ashokmart.dao;

import com.ashokmart.model.User;

import java.sql.SQLException;
import java.util.Optional;

public interface UserDao {
    User create(User user) throws SQLException;

    Optional<User> findByEmail(String email) throws SQLException;

    Optional<User> findById(long id) throws SQLException;

    boolean existsByEmail(String email) throws SQLException;

    void updateEnabled(long id, boolean enabled) throws SQLException;
}
