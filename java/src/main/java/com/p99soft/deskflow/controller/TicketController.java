package com.p99soft.deskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tickets", description = "Endpoints for managing customer support tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private final Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Only EMPLOYEE can create tickets.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "Create a new ticket (JSON)",
        description = "Creates a ticket. Allowed for EMPLOYEE only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ticket successfully created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient role — only EMPLOYEE can create tickets",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> createTicket(@RequestBody @Valid TicketRequest request) {
        log.info("REST request to create ticket (JSON): {}", request.getTitle());
        return new ResponseEntity<>(ticketService.createTicket(request), HttpStatus.CREATED);
    }

    /**
     * Only EMPLOYEE can create tickets with attachments.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "Create a new ticket with attachments (Multipart)",
        description = "Creates a ticket with file attachments. Allowed for EMPLOYEE only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ticket and attachments successfully created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient role",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> createTicketMultipart(
            @RequestPart("ticket") String ticketJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws Exception {
        log.info("REST request to create ticket with attachments (Multipart): filesCount={}",
                files != null ? files.size() : 0);

        TicketRequest request = objectMapper.readValue(ticketJson, TicketRequest.class);
        Set<ConstraintViolation<TicketRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return new ResponseEntity<>(ticketService.createTicket(request, files), HttpStatus.CREATED);
    }

    /**
     * All authenticated users can view a ticket.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'ADMIN')")
    @Operation(
        summary = "Get a ticket by ID",
        description = "Retrieves full details of a ticket. Accessible by all roles."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket details successfully retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> getTicketById(
            @Parameter(description = "UUID of the ticket to retrieve", required = true)
            @PathVariable UUID id) {
        log.info("REST request to get ticket by ID: {}", id);
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    /**
     * AGENT and ADMIN can update tickets (reassign, change status, etc.).
     * EMPLOYEE cannot modify ticket details after creation.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(
        summary = "Update an existing ticket",
        description = "Updates ticket fields. Allowed for AGENT and ADMIN only. " +
                      "EMPLOYEEs cannot modify tickets after creation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket successfully updated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient role — EMPLOYEE cannot update tickets",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket, Category, or Assignee not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> updateTicket(
            @Parameter(description = "UUID of the ticket to update", required = true)
            @PathVariable UUID id,
            @RequestBody TicketRequest request) {
        log.info("REST request to update ticket ID: {}", id);
        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }

    /**
     * All authenticated users can list and filter tickets.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'ADMIN')")
    @Operation(
        summary = "List tickets with pagination and filtering",
        description = "Returns a paginated list of tickets. Accessible by all roles."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of tickets",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<TicketResponse>> listTickets(
            @Parameter(description = "Zero-indexed page number", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Records per page", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Filter by status")
            @RequestParam(required = false) Status status,

            @Parameter(description = "Filter by priority")
            @RequestParam(required = false) Priority priority,

            @Parameter(description = "Filter by Category UUID")
            @RequestParam(name = "category", required = false) UUID categoryId,

            @Parameter(description = "Filter by Assignee UUID")
            @RequestParam(name = "assignee", required = false) UUID assignedTo,

            @Parameter(description = "Free-text search across number, title, description")
            @RequestParam(required = false) String search,

            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction: asc or desc", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("REST request to list tickets: page={}, size={}, status={}, priority={}", page, size, status, priority);
        return ResponseEntity.ok(
                ticketService.listTickets(page, size, status, priority, categoryId, assignedTo, search, sortBy, sortDir)
        );
    }
}
