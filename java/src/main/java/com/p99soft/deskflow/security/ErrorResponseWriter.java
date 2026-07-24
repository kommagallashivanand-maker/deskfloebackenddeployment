package com.p99soft.deskflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.p99soft.deskflow.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Shared utility for writing structured JSON error responses from
 * Spring Security handlers that operate outside the MVC dispatcher
 * (i.e. {@link JwtAuthenticationEntryPoint} and {@link JwtAccessDeniedHandler}).
 *
 * <p>These handlers cannot use {@code @ControllerAdvice} because they run
 * before the servlet is reached, so they write directly to the response stream.
 * This class centralises that logic to avoid duplication.</p>
 */
final class ErrorResponseWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ErrorResponseWriter() {
        // utility class — no instances
    }

    /**
     * Writes an {@link ApiErrorResponse} as JSON directly to the HTTP response.
     *
     * @param response   the servlet response to write to
     * @param httpStatus the HTTP status code (e.g. 401, 403)
     * @param statusText the HTTP status reason phrase (e.g. "Unauthorized")
     * @param message    the human-readable error message
     * @param uri        the request URI for the {@code details} field
     */
    static void write(
            HttpServletResponse response,
            int httpStatus,
            String statusText,
            String message,
            String uri
    ) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(httpStatus)
                .error(statusText)
                .message(message)
                .details("uri=" + uri)
                .build();

        MAPPER.writeValue(response.getOutputStream(), body);
    }
}
