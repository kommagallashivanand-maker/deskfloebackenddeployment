package com.p99soft.deskflow.service;

import com.p99soft.deskflow.config.JwtService;
import com.p99soft.deskflow.dto.LoginRequest;
import com.p99soft.deskflow.dto.LoginResponse;
import com.p99soft.deskflow.dto.RegisterRequest;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.enums.Role;
import com.p99soft.deskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public String register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        String assignedRole = request.getRole() != null ? request.getRole().toUpperCase() : Role.EMPLOYEE.name();
        try {
            Role.valueOf(assignedRole);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid role specified. Must be ADMIN, AGENT, or EMPLOYEE");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .status("ACTIVE")
                .employeeCode(request.getEmployeeCode())
                .build();

        userRepository.save(user);
        return assignedRole;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);
        String role = jwtService.extractRole(token);

        return LoginResponse.builder()
                .token(token)
                .role(role)
                .build();
    }
}
