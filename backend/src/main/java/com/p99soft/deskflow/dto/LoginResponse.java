package com.p99soft.deskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO representing the user login response containing the JWT token")
public class LoginResponse {

    @Schema(description = "Access token generated for the user", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Builder.Default
    @Schema(description = "Type of authorization token", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "Assigned user role", example = "ADMIN")
    private String role;
}
