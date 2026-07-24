package com.p99soft.deskflow.dto;

import com.p99soft.deskflow.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for user registration")
public class RegisterRequest {

    @Schema(description = "Optional employee code for identification", example = "EMP-2026-001")
    private String employeeCode;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    @Schema(description = "User's first name", example = "Jane")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "User's email address (must be unique)", example = "jane.doe@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "User's password (minimum 8 characters)", example = "Str0ngP@ssw0rd")
    private String password;

    @NotNull(message = "Role is required")
    @Schema(description = "User's role (EMPLOYEE, AGENT, ADMIN)", example = "EMPLOYEE")
    private Role role;

    @Schema(description = "Optional team ID to assign the user to", example = "b5c6d7e8-f9a0-4b1c-9d2e-3f4a5b6c7d8e")
    private UUID teamId;
}
