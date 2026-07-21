package com.p99soft.deskflow.controller;

import com.p99soft.deskflow.dto.ApiErrorResponse;
import com.p99soft.deskflow.dto.LoginRequest;
import com.p99soft.deskflow.dto.LoginResponse;
import com.p99soft.deskflow.dto.RegisterRequest;
import com.p99soft.deskflow.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and signup")
public class AuthenticationController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Provisions a new employee, agent, or admin account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data or email already exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Map<String, String>> register(@RequestBody @Valid RegisterRequest request) {
        String role = authService.register(request);
        
        String formattedRole = role.charAt(0) + role.substring(1).toLowerCase();
        Map<String, String> response = new HashMap<>();
        response.put("message", formattedRole + " registered successfully");
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login to retrieve JWT token", description = "Validates user credentials and returns a JWT token containing the user's role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account inactive",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
