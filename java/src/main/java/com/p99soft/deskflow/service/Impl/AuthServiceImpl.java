package com.p99soft.deskflow.service.Impl;

import com.p99soft.deskflow.dto.AuthResponse;
import com.p99soft.deskflow.dto.LoginRequest;
import com.p99soft.deskflow.dto.RegisterRequest;
import com.p99soft.deskflow.entity.Team;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.exception.ResourceNotFoundException;
import com.p99soft.deskflow.repository.TeamRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.security.UserDetailsImpl;
import com.p99soft.deskflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService}.
 * <p>
 * Handles user registration with BCrypt password hashing and authentication
 * via Spring Security's {@link AuthenticationManager}.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Team not found with id: " + request.getTeamId()));
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .employeeCode(request.getEmployeeCode())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(hashedPassword)
                .role(request.getRole())
                .status("ACTIVE")
                .team(team)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: id={}, email={}, role={}", 
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFirstName() + " " + savedUser.getLastName())
                .role(savedUser.getRole())
                .message("User registered successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating user: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        log.info("User authenticated successfully: id={}, email={}, role={}", 
                user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole())
                .message("Login successful")
                .build();
    }
}
