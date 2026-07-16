package com.p99soft.deskflow.service.Impl;

import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.entity.Category;
import com.p99soft.deskflow.entity.SlaPolicy;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.exception.ResourceNotFoundException;
import com.p99soft.deskflow.repository.CategoryRepository;
import com.p99soft.deskflow.repository.SlaPolicyRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    @Override
    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        log.info("Creating ticket: title={}, creatorId={}", request.getTitle(), request.getCreatedBy());
        User creator = userRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creator User not found with id: " + request.getCreatedBy()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        User assignee = null;
        if (request.getAssignedTo() != null) {
            assignee = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assignee User not found with id: " + request.getAssignedTo()));
        }

        Priority priority = request.getPriority() != null ? request.getPriority() : Priority.MEDIUM;
        Status status = request.getStatus() != null ? request.getStatus() : Status.OPEN;

        String ticketNumber = generateNextTicketNumber();

        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(priority)
                .status(status)
                .createdBy(creator)
                .assignedTo(assignee)
                .category(category)
                .reopenCount(0)
                .build();

        // If ticket is assigned immediately on creation, set first responded timestamp
        if (assignee != null) {
            ticket.setFirstRespondedAt(LocalDateTime.now());
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created successfully: id={}, ticketNumber={}", savedTicket.getId(), savedTicket.getTicketNumber());
        return mapToResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID id) {
        log.info("Retrieving ticket by ID: {}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(UUID id, TicketRequest request) {
        log.info("Updating ticket ID: {}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        Status oldStatus = ticket.getStatus();
        Status newStatus = request.getStatus();

        // Reopen validation: Transition from RESOLVED/CLOSED back to OPEN/IN_PROGRESS
        if (newStatus != null && newStatus != oldStatus) {
            if ((oldStatus == Status.RESOLVED || oldStatus == Status.CLOSED) &&
                    (newStatus == Status.OPEN || newStatus == Status.IN_PROGRESS)) {
                log.info("Ticket reopened: incrementing reopen count for ticket ID: {}", id);
                ticket.setReopenCount(ticket.getReopenCount() + 1);
            }

            ticket.setStatus(newStatus);

            // Handle resolution/close timestamps
            if (newStatus == Status.RESOLVED) {
                ticket.setResolvedAt(LocalDateTime.now());
            } else if (newStatus == Status.CLOSED) {
                ticket.setClosedAt(LocalDateTime.now());
            }
        }

        if (request.getTitle() != null) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.getCategoryId()));
            ticket.setCategory(category);
        }

        if (request.getAssignedTo() != null) {
            User assignee = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assignee User not found with id: " + request.getAssignedTo()));

            // Set first response timestamp if it's the first time an assignee is set
            if (ticket.getAssignedTo() == null && ticket.getFirstRespondedAt() == null) {
                ticket.setFirstRespondedAt(LocalDateTime.now());
            }
            ticket.setAssignedTo(assignee);
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket updated successfully: id={}", updatedTicket.getId());
        return mapToResponse(updatedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> listTickets(int page, int size, Status status, UUID categoryId,
            UUID assignedTo) {
        log.info("Listing tickets: page={}, size={}, status={}, categoryId={}, assignedTo={}",
                page, size, status, categoryId, assignedTo);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (assignedTo != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Ticket> ticketPage = ticketRepository.findAll(spec, pageable);
        log.info("Found {} tickets on page {} of {}", ticketPage.getNumberOfElements(), ticketPage.getNumber(), ticketPage.getTotalPages());
        List<TicketResponse> responses = ticketPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<TicketResponse>builder()
                .content(responses)
                .pageNumber(ticketPage.getNumber())
                .pageSize(ticketPage.getSize())
                .totalElements(ticketPage.getTotalElements())
                .totalPages(ticketPage.getTotalPages())
                .last(ticketPage.isLast())
                .build();
    }

    private synchronized String generateNextTicketNumber() {
        Optional<String> lastNumOpt = ticketRepository.findLastTicketNumber();
        if (lastNumOpt.isEmpty()) {
            return "TKT-1001";
        }
        String lastNum = lastNumOpt.get();
        try {
            int numericPart = Integer.parseInt(lastNum.substring(4));
            return "TKT-" + (numericPart + 1);
        } catch (NumberFormatException e) {
            return "TKT-1001";
        }
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        TicketResponse response = TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .createdBy(ticket.getCreatedBy().getId())
                .assignedTo(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null)
                .categoryId(ticket.getCategory().getId())
                .firstRespondedAt(ticket.getFirstRespondedAt())
                .reopenCount(ticket.getReopenCount())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .build();

        // Calculate SLA due dates dynamically
        Optional<SlaPolicy> policyOpt = slaPolicyRepository.findByPriority(ticket.getPriority());
        if (policyOpt.isPresent()) {
            SlaPolicy policy = policyOpt.get();
            if (ticket.getCreatedAt() != null) {
                response.setResponseSlaDueAt(ticket.getCreatedAt().plusHours(policy.getResponseTimeHours()));
                response.setSlaDueAt(ticket.getCreatedAt().plusHours(policy.getResolutionTimeHours()));
            } else {
                // For pre-persisted tickets before pre-persist callback triggers
                LocalDateTime now = LocalDateTime.now();
                response.setResponseSlaDueAt(now.plusHours(policy.getResponseTimeHours()));
                response.setSlaDueAt(now.plusHours(policy.getResolutionTimeHours()));
            }
        }

        return response;
    }
}
