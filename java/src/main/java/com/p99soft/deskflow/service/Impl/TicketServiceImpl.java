package com.p99soft.deskflow.service.Impl;

import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.entity.Category;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.TicketAttachment;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Role;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.exception.InvalidStatusTransitionException;
import com.p99soft.deskflow.exception.ResourceNotFoundException;
import com.p99soft.deskflow.mapper.TicketMapper;
import com.p99soft.deskflow.repository.CategoryRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.service.ActivityService;
import com.p99soft.deskflow.service.StorageService;
import com.p99soft.deskflow.service.TicketService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository  ticketRepository;
    private final UserRepository    userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper      ticketMapper;
    private final StorageService    storageService;
    private final ActivityService   activityService;

    /** Valid status transitions enforced by the state machine. */
    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = Map.of(
            Status.OPEN,        Set.of(Status.TRIAGED, Status.IN_PROGRESS, Status.CLOSED),
            Status.TRIAGED,     Set.of(Status.IN_PROGRESS, Status.ON_HOLD, Status.CLOSED),
            Status.IN_PROGRESS, Set.of(Status.ON_HOLD, Status.RESOLVED, Status.CLOSED),
            Status.ON_HOLD,     Set.of(Status.IN_PROGRESS, Status.CLOSED),
            Status.RESOLVED,    Set.of(Status.CLOSED, Status.OPEN, Status.IN_PROGRESS),
            Status.CLOSED,      Set.of(Status.OPEN, Status.IN_PROGRESS)
    );

    // ------------------------------------------------------------------ //
    // Create
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public TicketResponse createTicket(TicketRequest request, UUID creatorId) {
        return createTicketInternal(request, creatorId, null);
    }

    @Override
    @Transactional
    public TicketResponse createTicket(TicketRequest request, UUID creatorId, List<MultipartFile> files) {
        return createTicketInternal(request, creatorId, files);
    }

    private TicketResponse createTicketInternal(TicketRequest request, UUID creatorId, List<MultipartFile> files) {
        log.info("Creating ticket: title={}, creatorId={}, fileCount={}",
                request.getTitle(), creatorId, files != null ? files.size() : 0);

        User creator = resolveUser(creatorId, "Creator");
        Category category = resolveCategory(request.getCategoryId());
        User assignee = request.getAssignedTo() != null
                ? resolveUser(request.getAssignedTo(), "Assignee")
                : null;

        Priority priority = request.getPriority() != null ? request.getPriority() : Priority.MEDIUM;
        Status  status   = request.getStatus()   != null ? request.getStatus()   : Status.OPEN;

        Ticket ticket = Ticket.builder()
                .ticketNumber(generateNextTicketNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(priority)
                .status(status)
                .createdBy(creator)
                .assignedTo(assignee)
                .category(category)
                .reopenCount(0)
                .build();

        if (assignee != null) {
            ticket.setFirstRespondedAt(LocalDateTime.now(ZoneId.systemDefault()));
        }

        processAttachments(ticket, files);

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket created: id={}, ticketNumber={}", saved.getId(), saved.getTicketNumber());
        return ticketMapper.mapToResponse(saved);
    }

    // ------------------------------------------------------------------ //
    // Read
    // ------------------------------------------------------------------ //

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID id, UUID currentUserId, Role currentRole) {
        log.info("Retrieving ticket id={} for userId={}, role={}", id, currentUserId, currentRole);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        // EMPLOYEE can only view tickets they created
        if (currentRole == Role.EMPLOYEE
                && !ticket.getCreatedBy().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to view this ticket");
        }

        return ticketMapper.mapToResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> listTickets(
            int page, int size,
            Status status, Priority priority,
            UUID categoryId, UUID assignedTo,
            String search, String sortBy, String sortDir,
            UUID currentUserId, Role currentRole) {

        log.info("Listing tickets: page={}, size={}, status={}, priority={}, role={}",
                page, size, status, priority, currentRole);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Ticket> spec = buildListSpec(
                status, priority, categoryId, assignedTo, search, currentUserId, currentRole);

        Page<Ticket> ticketPage = ticketRepository.findAll(spec, pageable);
        List<TicketResponse> responses = ticketPage.getContent().stream()
                .map(ticketMapper::mapToResponse)
                .toList();

        log.info("Found {} tickets (page {}/{})",
                ticketPage.getTotalElements(), ticketPage.getNumber(), ticketPage.getTotalPages());

        return PageResponse.<TicketResponse>builder()
                .content(responses)
                .pageNumber(ticketPage.getNumber())
                .pageSize(ticketPage.getSize())
                .totalElements(ticketPage.getTotalElements())
                .totalPages(ticketPage.getTotalPages())
                .last(ticketPage.isLast())
                .build();
    }

    // ------------------------------------------------------------------ //
    // Update
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public TicketResponse updateTicket(UUID id, TicketRequest request) {
        log.info("Updating ticket id={}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        handleStatusTransition(ticket, request.getStatus());

        if (request.getTitle()       != null) ticket.setTitle(request.getTitle());
        if (request.getDescription() != null) ticket.setDescription(request.getDescription());
        if (request.getPriority()    != null) ticket.setPriority(request.getPriority());

        updateCategory(ticket, request.getCategoryId());
        updateAssignee(ticket, request.getAssignedTo());

        Ticket updated = ticketRepository.save(ticket);
        log.info("Ticket updated: id={}", updated.getId());
        return ticketMapper.mapToResponse(updated);
    }

    // ------------------------------------------------------------------ //
    // Private — specification builder
    // ------------------------------------------------------------------ //

    /**
     * Builds a JPA {@link Specification} for the ticket list query.
     *
     * <p>Key rule: if the caller is an EMPLOYEE, a mandatory filter on
     * {@code createdBy = currentUserId} is added so they only see their own tickets.</p>
     */
    private Specification<Ticket> buildListSpec(
            Status status, Priority priority,
            UUID categoryId, UUID assignedTo,
            String search, UUID currentUserId, Role currentRole) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Scope to own tickets for EMPLOYEE role
            if (currentRole == Role.EMPLOYEE) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), currentUserId));
            }
            if (status     != null) predicates.add(cb.equal(root.get("status"),               status));
            if (priority   != null) predicates.add(cb.equal(root.get("priority"),             priority));
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"),   categoryId));
            if (assignedTo != null) predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedTo));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")),        pattern),
                        cb.like(cb.lower(root.get("description")),  pattern),
                        cb.like(cb.lower(root.get("ticketNumber")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ------------------------------------------------------------------ //
    // Private — state machine
    // ------------------------------------------------------------------ //

    private void handleStatusTransition(Ticket ticket, Status newStatus) {
        Status oldStatus = ticket.getStatus();
        if (newStatus == null || newStatus == oldStatus) return;

        Set<Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition from " + oldStatus + " to " + newStatus);
        }

        if ((oldStatus == Status.RESOLVED || oldStatus == Status.CLOSED)
                && (newStatus == Status.OPEN || newStatus == Status.IN_PROGRESS)) {
            ticket.setReopenCount(ticket.getReopenCount() + 1);
        }

        ticket.setStatus(newStatus);

        if (newStatus == Status.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now(ZoneId.systemDefault()));
        } else if (newStatus == Status.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now(ZoneId.systemDefault()));
        }

        activityService.logActivity(ticket, ticket.getCreatedBy(),
                "STATUS_TRANSITION", oldStatus.name(), newStatus.name());
    }

    // ------------------------------------------------------------------ //
    // Private — helpers
    // ------------------------------------------------------------------ //

    private User resolveUser(UUID userId, String role) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        role + " user not found with id: " + userId));
    }

    private Category resolveCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + categoryId));
    }

    private void updateCategory(Ticket ticket, UUID categoryId) {
        if (categoryId != null) {
            ticket.setCategory(resolveCategory(categoryId));
        }
    }

    private void updateAssignee(Ticket ticket, UUID assignedTo) {
        if (assignedTo != null) {
            User assignee = resolveUser(assignedTo, "Assignee");
            if (ticket.getAssignedTo() == null && ticket.getFirstRespondedAt() == null) {
                ticket.setFirstRespondedAt(LocalDateTime.now(ZoneId.systemDefault()));
            }
            ticket.setAssignedTo(assignee);
        }
    }

    private void processAttachments(Ticket ticket, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;
        files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(f -> uploadAndAttachFile(ticket, f));
    }

    private void uploadAndAttachFile(Ticket ticket, MultipartFile file) {
        try {
            String fileUrl = storageService.uploadFile(file);
            ticket.getAttachments().add(
                    TicketAttachment.builder()
                            .ticket(ticket)
                            .fileName(file.getOriginalFilename())
                            .fileUrl(fileUrl)
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .build()
            );
        } catch (IOException e) {
            log.error("Failed to upload attachment for ticket: {}", e.getMessage());
            throw new IllegalArgumentException("Could not store file attachment: " + e.getMessage(), e);
        }
    }

    private synchronized String generateNextTicketNumber() {
        Optional<String> last = ticketRepository.findLastTicketNumber();
        if (last.isEmpty()) return "TKT-1001";
        try {
            int num = Integer.parseInt(last.get().substring(4));
            return "TKT-" + (num + 1);
        } catch (NumberFormatException e) {
            return "TKT-1001";
        }
    }
}
