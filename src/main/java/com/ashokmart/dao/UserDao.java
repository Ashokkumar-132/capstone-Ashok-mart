package com.ashokmart.dao;

import com.ashokmart.model.User;
import com.ashokmart.model.AdminUserQuery;
import com.ashokmart.model.UserRole;

import java.sql.SQLException;
import java.util.Optional;
import java.util.List;

public interface UserDao {
    User create(User user) throws SQLException;

    Optional<User> findByEmail(String email) throws SQLException;

    Optional<User> findById(long id) throws SQLException;

    boolean existsByEmail(String email) throws SQLException;

    void updateEnabled(long id, boolean enabled) throws SQLException;

    List<User> findAllUsers() throws SQLException;
    List<User> findUsers(AdminUserQuery query) throws SQLException;
    long countUsers(AdminUserQuery query) throws SQLException;
    long countUsers() throws SQLException;
    long countUsersByRole(UserRole role) throws SQLException;
    long countUsersByStatus(boolean enabled) throws SQLException;
    long countUsersByRoleAndStatus(UserRole role, boolean enabled) throws SQLException;
    boolean updateUserStatus(long id, boolean enabled) throws SQLException;
}
