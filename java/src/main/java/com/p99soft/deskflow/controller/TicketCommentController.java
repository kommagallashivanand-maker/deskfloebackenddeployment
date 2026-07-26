package com.p99soft.deskflow.controller;

import com.p99soft.deskflow.dto.ActivityResponse;
import com.p99soft.deskflow.dto.ApiErrorResponse;
import com.p99soft.deskflow.dto.CommentRequest;
import com.p99soft.deskflow.dto.CommentResponse;
import com.p99soft.deskflow.service.ActivityService;
import com.p99soft.deskflow.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ticket Comments & Activity Timeline",
     description = "Ticket comments, threaded replies, mentions, and audit activity logs")
public class TicketCommentController {

    private final CommentService  commentService;
    private final ActivityService activityService;

    /**
     * All authenticated users can post comments on tickets.
     */
    @PostMapping("/{ticketId}/comments")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'ADMIN')")
    @Operation(
        summary = "Add a comment or threaded reply",
        description = "Adds a comment or nested reply to a ticket. Parses @user mentions. " +
                      "Accessible by all roles."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Comment added successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket or user not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<CommentResponse> addComment(
            @Parameter(description = "UUID of the ticket") @PathVariable UUID ticketId,
            @Valid @RequestBody CommentRequest request) {
        log.info("REST request to add comment on ticket ID: {}", ticketId);
        return new ResponseEntity<>(commentService.addComment(ticketId, request), HttpStatus.CREATED);
    }

    /**
     * All authenticated users can read comments.
     */
    @GetMapping("/{ticketId}/comments")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'ADMIN')")
    @Operation(
        summary = "Get threaded comments for a ticket",
        description = "Retrieves all top-level comments and nested replies. Accessible by all roles."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved threaded comments",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = CommentResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<CommentResponse>> getThreadedComments(
            @Parameter(description = "UUID of the ticket") @PathVariable UUID ticketId) {
        log.info("REST request to get threaded comments for ticket ID: {}", ticketId);
        return ResponseEntity.ok(commentService.getThreadedComments(ticketId));
    }

    /**
     * All authenticated users can view the audit activity timeline.
     */
    @GetMapping("/{ticketId}/activities")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'ADMIN')")
    @Operation(
        summary = "Get activity audit timeline for a ticket",
        description = "Retrieves chronological state transitions and audit history. Accessible by all roles."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved activity audit history",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ActivityResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<ActivityResponse>> getTicketActivities(
            @Parameter(description = "UUID of the ticket") @PathVariable UUID ticketId) {
        log.info("REST request to get activity audit timeline for ticket ID: {}", ticketId);
        return ResponseEntity.ok(activityService.getTicketActivities(ticketId));
    }
}
