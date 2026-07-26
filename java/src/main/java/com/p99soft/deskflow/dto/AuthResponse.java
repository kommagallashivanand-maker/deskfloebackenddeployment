package com.p99soft.deskflow.dto;

import com.p99soft.deskflow.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response payload for successful authentication")
public class AuthResponse {

    @Schema(description = "Unique identifier of the authenticated user", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID userId;

    @Schema(description = "User's email address", example = "jane.doe@example.com")
    private String email;

    @Schema(description = "User's full name", example = "Jane Doe")
    private String fullName;

    @Schema(description = "User's assigned role", example = "EMPLOYEE")
    private Role role;

    @Schema(description = "Success message", example = "User registered successfully")
    private String message;
}
