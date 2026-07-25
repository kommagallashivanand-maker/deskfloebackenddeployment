package com.p99soft.deskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard structure for all error responses returned by the API")
public class ApiErrorResponse {

    @Schema(description = "Timestamp when the error occurred", example = "2026-07-17T07:52:32")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP Status Code value", example = "400")
    private int status;

    @Schema(description = "HTTP Error Description", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed error message", example = "Validation failed")
    private String message;

    @Schema(description = "Request path or details where the error occurred", example = "uri=/api/v1/tickets")
    private String details;

    @Schema(description = "Field-specific validation errors (only populated for HTTP 400 validation failures)", 
            example = "{\"title\": \"Title is required\", \"priority\": \"Priority is required\"}")
    private Map<String, String> errors;
}
