package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.CategoryResponse;
import com.p99soft.deskflow.entity.Category;
import com.p99soft.deskflow.enums.CategoryType;
import com.p99soft.deskflow.repository.CategoryRepository;
import com.p99soft.deskflow.service.Impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void testGetAllCategories_ReturnsAll() {
        List<Category> categories = List.of(
                buildCategory(CategoryType.TECHNICAL,       "Tech support"),
                buildCategory(CategoryType.BILLING,         "Billing support"),
                buildCategory(CategoryType.ACCOUNT_ACCESS,  "Account access")
        );
        when(categoryRepository.findAll()).thenReturn(categories);

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void testGetAllCategories_EmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void testGetAllCategories_MapsFieldsCorrectly() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name(CategoryType.GENERAL)
                .description("General enquiries")
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertEquals(1, result.size());
        CategoryResponse response = result.get(0);
        assertEquals(categoryId,          response.getId());
        assertEquals(CategoryType.GENERAL, response.getName());
        assertEquals("General enquiries", response.getDescription());
    }

    @Test
    void testGetAllCategories_AllCategoryTypesPresent() {
        List<Category> allTypes = List.of(
                buildCategory(CategoryType.TECHNICAL,       "Technical"),
                buildCategory(CategoryType.BILLING,         "Billing"),
                buildCategory(CategoryType.ACCOUNT_ACCESS,  "Account Access"),
                buildCategory(CategoryType.FEATURE_REQUEST, "Feature Request"),
                buildCategory(CategoryType.GENERAL,         "General")
        );
        when(categoryRepository.findAll()).thenReturn(allTypes);

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertEquals(5, result.size());
        assertTrue(result.stream().anyMatch(r -> r.getName() == CategoryType.TECHNICAL));
        assertTrue(result.stream().anyMatch(r -> r.getName() == CategoryType.BILLING));
        assertTrue(result.stream().anyMatch(r -> r.getName() == CategoryType.ACCOUNT_ACCESS));
        assertTrue(result.stream().anyMatch(r -> r.getName() == CategoryType.FEATURE_REQUEST));
        assertTrue(result.stream().anyMatch(r -> r.getName() == CategoryType.GENERAL));
    }

    @Test
    void testGetAllCategories_NullDescription_HandledGracefully() {
        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name(CategoryType.TECHNICAL)
                .description(null)
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertEquals(1, result.size());
        assertNull(result.get(0).getDescription());
    }

    // ------------------------------------------------------------------ //
    // Helper
    // ------------------------------------------------------------------ //

    private Category buildCategory(CategoryType type, String description) {
        return Category.builder()
                .id(UUID.randomUUID())
                .name(type)
                .description(description)
                .build();
    }
}
