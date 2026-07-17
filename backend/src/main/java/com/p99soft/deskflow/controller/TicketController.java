package com.p99soft.deskflow.controller;

import com.p99soft.deskflow.dto.ApiErrorResponse;
import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tickets", description = "Endpoints for managing customer support tickets")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(
        summary = "Create a new ticket", 
        description = "Creates a ticket using the provided details. All mandatory fields (title, priority, categoryId, createdBy) must be present and valid."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ticket successfully created", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload or validation constraint violation", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error occurred", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> createTicket(@RequestBody @Valid TicketRequest request) {
        log.info("REST request to create ticket: {}", request.getTitle());
        TicketResponse response = ticketService.createTicket(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a ticket by ID", 
        description = "Retrieves complete details of a single ticket using its unique database UUID identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket details successfully retrieved", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid UUID format provided", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket not found with the specified ID", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error occurred", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> getTicketById(
            @Parameter(description = "Unique UUID of the ticket to retrieve", required = true, example = "f1111111-2222-3333-4444-555555555555")
            @PathVariable UUID id) {
        log.info("REST request to get ticket by ID: {}", id);
        TicketResponse response = ticketService.getTicketById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing ticket", 
        description = "Updates an existing ticket partially. Only the non-null properties supplied in the body will be updated."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket successfully updated", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or JSON structure", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket, Category, or Assignee User not found", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error occurred", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> updateTicket(
            @Parameter(description = "Unique UUID of the ticket to update", required = true, example = "f1111111-2222-3333-4444-555555555555")
            @PathVariable UUID id,
            @RequestBody TicketRequest request) {
        log.info("REST request to update ticket ID: {}", id);
        TicketResponse response = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "List all tickets with pagination and filtering", 
        description = "Retrieve a paginated list of tickets. Supports filtering by status, priority, category, and assignee agent."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of tickets", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error occurred", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<TicketResponse>> listTickets(
            @Parameter(description = "Zero-indexed page number to retrieve", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "Filter tickets by status (e.g., OPEN, IN_PROGRESS, RESOLVED, CLOSED)")
            @RequestParam(required = false) Status status,
            
            @Parameter(description = "Filter tickets by priority (e.g., LOW, MEDIUM, HIGH, URGENT)")
            @RequestParam(required = false) Priority priority,
            
            @Parameter(description = "Filter tickets by Category UUID", example = "a6b5c4d3-e2f1-4098-b64d-91b654dc3210")
            @RequestParam(name = "category", required = false) UUID categoryId,
            
            @Parameter(description = "Filter tickets by Assignee User UUID", example = "20000000-0000-0000-0000-000000000001")
            @RequestParam(name = "assignee", required = false) UUID assignedTo,
            
            @Parameter(description = "Field name to sort the results by", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sorting direction ('asc' for ascending, 'desc' for descending)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("REST request to list tickets: page={}, size={}, status={}, priority={}, categoryId={}, assignedTo={}, sortBy={}, sortDir={}",
                page, size, status, priority, categoryId, assignedTo, sortBy, sortDir);
        PageResponse<TicketResponse> response = ticketService.listTickets(page, size, status, priority, categoryId, assignedTo, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }
}
