package com.ashokmart.dao;

import com.ashokmart.model.Category;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CategoryDao {
    List<Category> findAll() throws SQLException;
    Optional<Category> findById(long id) throws SQLException;
    Optional<Category> findByName(String name) throws SQLException;
}
