package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;

import java.util.UUID;

public interface TicketService {
    TicketResponse createTicket(TicketRequest request);
    TicketResponse getTicketById(UUID id);
    TicketResponse updateTicket(UUID id, TicketRequest request);
    PageResponse<TicketResponse> listTickets(int page, int size, Status status, Priority priority, UUID categoryId, UUID assignedTo, String sortBy, String sortDir);
}
