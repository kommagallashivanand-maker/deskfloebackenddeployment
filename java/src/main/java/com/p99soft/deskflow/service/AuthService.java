package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.AuthResponse;
import com.p99soft.deskflow.dto.LoginRequest;
import com.p99soft.deskflow.dto.LoginResponse;
import com.p99soft.deskflow.dto.RegisterRequest;

/**
 * Service layer for authentication-related operations.
 */
public interface AuthService {

    /**
     * Registers a new user account with BCrypt password hashing.
     *
     * @param request user registration details
     * @return response containing user information
     * @throws IllegalArgumentException if email is already registered
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user using email and password and returns a signed JWT.
     *
     * @param request login credentials
     * @return {@link LoginResponse} containing the JWT, token type, and user metadata
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     */
    LoginResponse login(LoginRequest request);
}
