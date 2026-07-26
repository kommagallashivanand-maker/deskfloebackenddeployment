package com.p99soft.deskflow.controller;

import com.p99soft.deskflow.dto.ApiErrorResponse;
import com.p99soft.deskflow.dto.CategoryResponse;
import com.p99soft.deskflow.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for fetching ticket categories.
 *
 * <p>Exposed to all authenticated users so they can populate category
 * dropdowns when creating a ticket.</p>
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categories", description = "Fetch available ticket categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'ADMIN')")
    @Operation(
        summary = "Get all categories",
        description = "Returns all available ticket categories. " +
                      "Use the category UUID when creating a ticket."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("REST request to get all categories");
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
}
