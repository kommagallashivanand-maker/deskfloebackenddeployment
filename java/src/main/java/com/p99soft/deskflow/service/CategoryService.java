package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.CategoryResponse;

import java.util.List;

/**
 * Service for category-related operations.
 */
public interface CategoryService {

    /**
     * Returns all available ticket categories.
     * Used by the frontend to populate category dropdowns when creating a ticket.
     */
    List<CategoryResponse> getAllCategories();
}
