package com.p99soft.deskflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.p99soft.deskflow.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles all requests that reach a protected endpoint without a valid JWT.
 *
 * <p>Spring Security calls this when authentication is required but either:</p>
 * <ul>
 *   <li>No {@code Authorization} header is present</li>
 *   <li>The JWT is expired, malformed, or has an invalid signature</li>
 * </ul>
 *
 * <p>Returns a structured JSON {@code 401 Unauthorized} response consistent
 * with the rest of the API's error format ({@link ApiErrorResponse}).</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(resolveMessage(request))
                .details("uri=" + request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Returns a user-friendly message based on what was wrong with the request.
     */
    private String resolveMessage(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null) {
            return "Access denied: Authorization header is missing";
        }
        if (!authHeader.startsWith("Bearer ")) {
            return "Access denied: Authorization header must start with 'Bearer '";
        }
        return "Access denied: Token is invalid, expired, or malformed";
    }
}
