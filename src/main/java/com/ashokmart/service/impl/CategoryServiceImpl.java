package com.ashokmart.service.impl;

import com.ashokmart.dao.CategoryDao;
import com.ashokmart.model.Category;
import com.ashokmart.service.CategoryService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class CategoryServiceImpl implements CategoryService {
    private final CategoryDao categoryDao;

    public CategoryServiceImpl(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    @Override
    public List<Category> listCategories() {
        try {
            return List.copyOf(categoryDao.findAll());
        } catch (SQLException exception) {
            throw new IllegalStateException("Categories are temporarily unavailable", exception);
        }
    }

    @Override
    public Optional<Category> findCategory(long categoryId) {
        if (categoryId <= 0) {
            return Optional.empty();
        }
        try {
            return categoryDao.findById(categoryId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Category lookup is temporarily unavailable", exception);
        }
    }

    @Override
    public Optional<Category> findCategoryByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            return categoryDao.findByName(name.trim());
        } catch (SQLException exception) {
            throw new IllegalStateException("Category lookup is temporarily unavailable", exception);
        }
    }
}
