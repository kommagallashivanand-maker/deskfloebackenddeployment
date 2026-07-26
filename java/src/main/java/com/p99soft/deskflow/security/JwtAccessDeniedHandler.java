package com.p99soft.deskflow.security;

import com.p99soft.deskflow.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Invoked by Spring Security when an authenticated user attempts to access
 * a resource they do not have the required role for.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>EMPLOYEE or AGENT calling {@code POST /api/v1/auth/register} (ADMIN only)</li>
 *   <li>EMPLOYEE calling {@code PUT /api/v1/tickets/{id}} (AGENT/ADMIN only)</li>
 * </ul>
 *
 * <p>Returns a structured JSON {@code 403 Forbidden} response consistent
 * with the rest of the API's error format ({@link ApiErrorResponse}).</p>
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final String ACCESS_DENIED_MESSAGE =
            "Access denied: you do not have the required role to perform this action";

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ErrorResponseWriter.write(
                response,
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ACCESS_DENIED_MESSAGE,
                request.getRequestURI()
        );
    }
}
