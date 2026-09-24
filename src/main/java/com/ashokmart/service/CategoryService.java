package com.ashokmart.service;

import com.ashokmart.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> listCategories();
    Optional<Category> findCategory(long categoryId);
    Optional<Category> findCategoryByName(String name);
}
