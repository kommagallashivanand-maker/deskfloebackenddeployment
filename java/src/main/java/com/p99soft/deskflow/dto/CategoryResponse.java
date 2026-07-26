package com.p99soft.deskflow.dto;

import com.p99soft.deskflow.enums.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing a ticket category")
public class CategoryResponse {

    @Schema(description = "Unique identifier of the category")
    private UUID id;

    @Schema(description = "Category type", example = "TECHNICAL")
    private CategoryType name;

    @Schema(description = "Human-readable description of the category")
    private String description;
}