package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.AuthResponse;
import com.p99soft.deskflow.dto.LoginRequest;
import com.p99soft.deskflow.dto.RegisterRequest;

/**
 * Service layer for authentication-related operations.
 * <p>
 * This interface defines the business logic for user registration and login.
 * JWT token generation will be added here once JWT support is implemented.
 * </p>
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
     * Authenticates a user using email and password.
     *
     * @param request login credentials
     * @return response containing authenticated user information
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     */
    AuthResponse login(LoginRequest request);
}
