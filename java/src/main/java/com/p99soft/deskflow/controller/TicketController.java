package com.p99soft.deskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p99soft.deskflow.dto.ApiErrorResponse;
import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.security.UserDetailsImpl;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

/**
 * REST controller for ticket management.
 *
 * <p>The authenticated user's identity is resolved from the JWT via
 * {@link AuthenticationPrincipal} — the frontend never needs to pass
 * {@code createdBy} or user context manually.</p>
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tickets", description = "Endpoints for managing customer support tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ObjectMapper  objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private final Validator validator =
            jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    // ------------------------------------------------------------------ //
    // Create
    // ------------------------------------------------------------------ //

    /**
     * EMPLOYEE only. {@code createdBy} is resolved from the JWT — not required in the request body.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "Create a new ticket (JSON)",
        description = "Creates a ticket. EMPLOYEE only. The creator is resolved automatically from the JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ticket created",
            content = @Content(schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient role",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> createTicket(
            @RequestBody @Valid TicketRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        log.info("Create ticket (JSON): title={}, creatorId={}", request.getTitle(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request, principal.getId()));
    }

    /**
     * EMPLOYEE only. Multipart variant with file attachments.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "Create a new ticket with attachments (Multipart)",
        description = "Creates a ticket with file attachments. EMPLOYEE only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ticket and attachments created",
            content = @Content(schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient role",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> createTicketMultipart(
            @RequestPart("ticket") String ticketJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl principal) throws Exception {
        log.info("Create ticket (Multipart): filesCount={}, creatorId={}",
                files != null ? files.size() : 0, principal.getId());

        TicketRequest request = objectMapper.readValue(ticketJson, TicketRequest.class);
        Set<ConstraintViolation<TicketRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request, principal.getId(), files));
    }

    // ------------------------------------------------------------------ //
    // Read
    // ------------------------------------------------------------------ //

    /**
     * All roles. EMPLOYEE can only view tickets they created.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'ADMIN')")
    @Operation(
        summary = "Get a ticket by ID",
        description = "Retrieves a ticket. EMPLOYEE can only view their own tickets."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket retrieved",
            content = @Content(schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "EMPLOYEE accessing another employee's ticket",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        log.info("Get ticket id={} by userId={}", id, principal.getId());
        return ResponseEntity.ok(
                ticketService.getTicketById(id, principal.getId(), principal.getUser().getRole()));
    }

    /**
     * All roles. EMPLOYEE sees only their own tickets; AGENT and ADMIN see all.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'ADMIN')")
    @Operation(
        summary = "List tickets with pagination and filtering",
        description = "EMPLOYEE sees only their own tickets. AGENT and ADMIN see all tickets."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tickets retrieved",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<TicketResponse>> listTickets(
            @Parameter(description = "Zero-indexed page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Records per page")         @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by status")         @RequestParam(required = false) Status status,
            @Parameter(description = "Filter by priority")       @RequestParam(required = false) Priority priority,
            @Parameter(description = "Filter by category UUID")  @RequestParam(name = "category", required = false) UUID categoryId,
            @Parameter(description = "Filter by assignee UUID")  @RequestParam(name = "assignee", required = false) UUID assignedTo,
            @Parameter(description = "Free-text search")         @RequestParam(required = false) String search,
            @Parameter(description = "Sort field")               @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: asc|desc") @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        log.info("List tickets: page={}, size={}, status={}, role={}", page, size, status, principal.getUser().getRole());
        return ResponseEntity.ok(ticketService.listTickets(
                page, size, status, priority, categoryId, assignedTo, search, sortBy, sortDir,
                principal.getId(), principal.getUser().getRole()));
    }

    // ------------------------------------------------------------------ //
    // Update
    // ------------------------------------------------------------------ //

    /**
     * AGENT and ADMIN only.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(
        summary = "Update an existing ticket",
        description = "AGENT and ADMIN only. EMPLOYEE cannot modify tickets after creation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket updated",
            content = @Content(schema = @Schema(implementation = TicketResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status transition or validation error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "EMPLOYEE cannot update tickets",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket, Category, or Assignee not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID id,
            @RequestBody TicketRequest request) {
        log.info("Update ticket id={}", id);
        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }
}
