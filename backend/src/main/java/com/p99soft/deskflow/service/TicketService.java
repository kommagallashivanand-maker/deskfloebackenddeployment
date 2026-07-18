package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface TicketService {
    TicketResponse createTicket(TicketRequest request);
    TicketResponse createTicket(TicketRequest request, List<MultipartFile> files);
    TicketResponse getTicketById(UUID id);
    TicketResponse updateTicket(UUID id, TicketRequest request);
    PageResponse<TicketResponse> listTickets(int page, int size, Status status, Priority priority, UUID categoryId, UUID assignedTo, String sortBy, String sortDir);
}
