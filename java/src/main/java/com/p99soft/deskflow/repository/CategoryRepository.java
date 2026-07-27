package com.p99soft.deskflow.repository;

import com.p99soft.deskflow.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Looks up a category by its {@link com.p99soft.deskflow.enums.CategoryType} name.
     * Used so employees can reference categories by name (e.g. "TECHNICAL") instead of UUID.
     *
     * @param name the string representation of the {@link com.p99soft.deskflow.enums.CategoryType}
     * @return the matching category if found
     */
    Optional<Category> findByName(com.p99soft.deskflow.enums.CategoryType name);
}
