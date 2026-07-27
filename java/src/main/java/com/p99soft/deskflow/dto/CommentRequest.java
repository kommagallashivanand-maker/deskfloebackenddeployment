package com.p99soft.deskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "DTO for posting a comment or threaded reply on a ticket")
public class CommentRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "UUID of the user posting the comment", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID userId;

    @NotBlank(message = "Comment content cannot be blank")
    @Schema(description = "Content of the comment. Supports @username or @email mentions", example = "I checked the logs @john.doe. Please review.")
    private String content;

    @Schema(description = "Optional parent comment UUID for nested threaded replies", example = "b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID parentCommentId;
}
