package com.p99soft.deskflow.controller;

import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.service.TicketService;
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
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@RequestBody @Valid TicketRequest request) {
        log.info("REST request to create ticket: {}", request.getTitle());
        TicketResponse response = ticketService.createTicket(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable UUID id) {
        log.info("REST request to get ticket by ID: {}", id);
        TicketResponse response = ticketService.getTicketById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID id,
            @RequestBody @Valid TicketRequest request) {
        log.info("REST request to update ticket ID: {}", id);
        TicketResponse response = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TicketResponse>> listTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(name = "category", required = false) UUID categoryId,
            @RequestParam(name = "assignee", required = false) UUID assignedTo,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("REST request to list tickets: page={}, size={}, status={}, priority={}, categoryId={}, assignedTo={}, sortBy={}, sortDir={}",
                page, size, status, priority, categoryId, assignedTo, sortBy, sortDir);
        PageResponse<TicketResponse> response = ticketService.listTickets(page, size, status, priority, categoryId, assignedTo, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }
}
