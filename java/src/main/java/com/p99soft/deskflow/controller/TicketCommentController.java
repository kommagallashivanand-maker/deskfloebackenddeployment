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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ticket Comments & Activity Timeline", description = "Endpoints for ticket comments, threaded replies, user mentions, and state transition audit logs")
public class TicketCommentController {

    private final CommentService commentService;
    private final ActivityService activityService;

    @PostMapping("/{ticketId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment or threaded reply", description = "Add a new comment or nested reply to a support ticket. Parses @user mentions in the comment text.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Comment added successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket or user not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<CommentResponse> addComment(
            @Parameter(description = "UUID of the ticket", example = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
            @PathVariable UUID ticketId,
            @Valid @RequestBody CommentRequest request) {
        log.info("REST request to add comment on ticket ID: {}", ticketId);
        CommentResponse response = commentService.addComment(ticketId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{ticketId}/comments")
    @Operation(summary = "Get threaded comments for a ticket", description = "Retrieve all top-level comments and their nested replies for a support ticket.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved threaded comments",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CommentResponse.class)))),
        @ApiResponse(responseCode = "404", description = "Ticket not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<CommentResponse>> getThreadedComments(
            @Parameter(description = "UUID of the ticket", example = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
            @PathVariable UUID ticketId) {
        log.info("REST request to get threaded comments for ticket ID: {}", ticketId);
        List<CommentResponse> responses = commentService.getThreadedComments(ticketId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{ticketId}/activities")
    @Operation(summary = "Get activity audit timeline for a ticket", description = "Retrieve chronological state transition and activity audit history for a support ticket.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved activity audit history",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ActivityResponse.class))))
    })
    public ResponseEntity<List<ActivityResponse>> getTicketActivities(
            @Parameter(description = "UUID of the ticket", example = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
            @PathVariable UUID ticketId) {
        log.info("REST request to get activity audit timeline for ticket ID: {}", ticketId);
        List<ActivityResponse> responses = activityService.getTicketActivities(ticketId);
        return ResponseEntity.ok(responses);
    }
}
