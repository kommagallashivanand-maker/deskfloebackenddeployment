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
@Schema(description = "Response payload for a successful login — contains the JWT and user metadata")
public class LoginResponse {

    @Schema(description = "Signed JWT to be sent in the Authorization header on subsequent requests",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Token type — always Bearer", example = "Bearer")
    private String tokenType;

    @Schema(description = "Unique identifier of the authenticated user",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID userId;

    @Schema(description = "Email address of the authenticated user", example = "jane.doe@example.com")
    private String email;

    @Schema(description = "Full name of the authenticated user", example = "Jane Doe")
    private String fullName;

    @Schema(description = "Role assigned to the authenticated user", example = "EMPLOYEE")
    private Role role;
}
