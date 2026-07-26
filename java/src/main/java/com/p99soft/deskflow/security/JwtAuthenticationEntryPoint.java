package com.p99soft.deskflow.security;

import com.p99soft.deskflow.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Invoked by Spring Security when a request reaches a protected endpoint
 * without a valid JWT.
 *
 * <p>Triggers when:</p>
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

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorResponseWriter.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                resolveMessage(request, authException),
                request.getRequestURI()
        );
    }

    /**
     * Provides a context-specific 401 message based on what was wrong
     * with the Authorization header.
     */
    private String resolveMessage(HttpServletRequest request, AuthenticationException authException) {
        if (request.getRequestURI().endsWith("/login")) {
            return authException.getMessage();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return "Access denied: Authorization header is missing";
        }
        if (!authHeader.startsWith("Bearer ")) {
            return "Access denied: Authorization header must use Bearer scheme";
        }
        return "Access denied: Token is invalid, expired, or malformed";
    }
}