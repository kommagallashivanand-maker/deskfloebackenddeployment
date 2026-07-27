package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Role;
import com.p99soft.deskflow.enums.Status;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    /**
     * Creates a ticket. {@code creatorId} is extracted from the JWT — not supplied by the client.
     */
    TicketResponse createTicket(TicketRequest request, UUID creatorId);

    /**
     * Creates a ticket with file attachments. {@code creatorId} is extracted from the JWT.
     */
    TicketResponse createTicket(TicketRequest request, UUID creatorId, List<MultipartFile> files);

    /**
     * Retrieves a single ticket.
     * EMPLOYEE may only view tickets they created. AGENT and ADMIN can view all.
     *
     * @param id            the ticket UUID
     * @param currentUserId the authenticated user's UUID
     * @param currentRole   the authenticated user's role
     */
    TicketResponse getTicketById(UUID id, UUID currentUserId, Role currentRole);

    TicketResponse updateTicket(UUID id, TicketRequest request);

    /**
     * Returns a paginated list of tickets.
     * EMPLOYEE sees only their own tickets. AGENT and ADMIN see all.
     *
     * @param currentUserId the authenticated user's UUID
     * @param currentRole   the authenticated user's role
     */
    PageResponse<TicketResponse> listTickets(
            int page, int size,
            Status status, Priority priority,
            UUID categoryId, UUID assignedTo,
            String search, String sortBy, String sortDir,
            UUID currentUserId, Role currentRole
    );
}
