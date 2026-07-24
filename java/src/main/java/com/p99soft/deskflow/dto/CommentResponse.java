package com.p99soft.deskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO representing a ticket comment with threaded replies and parsed user mentions")
public class CommentResponse {

    @Schema(description = "Unique ID of the comment")
    private UUID id;

    @Schema(description = "Ticket ID")
    private UUID ticketId;

    @Schema(description = "User ID of the commenter")
    private UUID userId;

    @Schema(description = "Full name or email of the user")
    private String userName;

    @Schema(description = "Comment text content")
    private String content;

    @Schema(description = "Parent comment ID if this is a reply")
    private UUID parentCommentId;

    @Schema(description = "Extracted @user mentions in the comment text")
    @Builder.Default
    private List<String> mentions = new ArrayList<>();

    @Schema(description = "List of nested threaded replies")
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
}
