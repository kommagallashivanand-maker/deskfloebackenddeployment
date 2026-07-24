package com.p99soft.deskflow.exception;

import com.p99soft.deskflow.dto.ApiErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for the entire application.
 *
 * <p>All business and framework exceptions are translated here into the
 * consistent {@link ApiErrorResponse} format. No controller or service
 * should handle HTTP status mapping — that belongs here.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ //
    // Domain exceptions
    // ------------------------------------------------------------------ //

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(
            InvalidStatusTransitionException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // ------------------------------------------------------------------ //
    // Validation exceptions
    // ------------------------------------------------------------------ //

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (first, second) -> first   // keep first message on duplicate fields
                ));

        return buildWithErrors(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, String> fieldErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            return path.contains(".")
                                    ? path.substring(path.lastIndexOf('.') + 1)
                                    : path;
                        },
                        v -> v.getMessage(),
                        (first, second) -> first
                ));

        return buildWithErrors(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    // ------------------------------------------------------------------ //
    // Infrastructure / framework exceptions
    // ------------------------------------------------------------------ //

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, WebRequest request) {
        String message = "Data integrity violation: " + (ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage());
        return build(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, WebRequest request) {
        String message = "Malformed JSON or invalid enum value: " + (ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    // ------------------------------------------------------------------ //
    // Security exceptions
    // ------------------------------------------------------------------ //

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied: " + ex.getMessage(), request);
    }

    // ------------------------------------------------------------------ //
    // Catch-all
    // ------------------------------------------------------------------ //

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(
            Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage(), request);
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .details(request.getDescription(false))
                        .build()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildWithErrors(
            HttpStatus status, String message, WebRequest request, Map<String, String> errors) {
        return ResponseEntity.status(status).body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .details(request.getDescription(false))
                        .errors(errors)
                        .build()
        );
    }
}
