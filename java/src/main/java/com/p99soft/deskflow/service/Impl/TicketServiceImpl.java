package com.p99soft.deskflow.service.Impl;

import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.entity.Category;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.enums.CategoryType;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.exception.ResourceNotFoundException;
import com.p99soft.deskflow.repository.CategoryRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.service.TicketService;
import com.p99soft.deskflow.mapper.TicketMapper;
import com.p99soft.deskflow.entity.TicketAttachment;
import com.p99soft.deskflow.service.StorageService;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.p99soft.deskflow.exception.InvalidStatusTransitionException;
import com.p99soft.deskflow.service.ActivityService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
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

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;
    private final StorageService storageService;
    private final ActivityService activityService;

    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = Map.of(
            Status.OPEN, Set.of(Status.TRIAGED, Status.IN_PROGRESS, Status.CLOSED),
            Status.TRIAGED, Set.of(Status.IN_PROGRESS, Status.ON_HOLD, Status.CLOSED),
            Status.IN_PROGRESS, Set.of(Status.ON_HOLD, Status.RESOLVED, Status.CLOSED),
            Status.ON_HOLD, Set.of(Status.IN_PROGRESS, Status.CLOSED),
            Status.RESOLVED, Set.of(Status.CLOSED, Status.OPEN, Status.IN_PROGRESS),
            Status.CLOSED, Set.of(Status.OPEN, Status.IN_PROGRESS)
    );

    @Override
    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        return createTicketInternal(request, null);
    }

    @Override
    @Transactional
    public TicketResponse createTicket(TicketRequest request, List<MultipartFile> files) {
        return createTicketInternal(request, files);
    }

    private TicketResponse createTicketInternal(TicketRequest request, List<MultipartFile> files) {
        log.info("Creating ticket: title={}, creatorId={}, fileCount={}",
                request.getTitle(), request.getCreatedBy(), files != null ? files.size() : 0);

        User creator = userRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creator User not found with id: " + request.getCreatedBy()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));

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

        if (assignee != null) {
            ticket.setFirstRespondedAt(LocalDateTime.now(java.time.ZoneId.systemDefault()));
        }

        processAttachments(ticket, files);

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created successfully: id={}, ticketNumber={}", savedTicket.getId(), savedTicket.getTicketNumber());
        return ticketMapper.mapToResponse(savedTicket);
    }

    private void processAttachments(Ticket ticket, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                uploadAndAttachFile(ticket, file);
            }
        }
    }

    private void uploadAndAttachFile(Ticket ticket, MultipartFile file) {
        try {
            String fileUrl = storageService.uploadFile(file);
            TicketAttachment attachment = TicketAttachment.builder()
                    .ticket(ticket)
                    .fileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();
            ticket.getAttachments().add(attachment);
        } catch (IOException e) {
            log.error("Failed to upload attachment file during ticket creation", e);
            throw new IllegalArgumentException("Could not store file attachment: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID id) {
        log.info("Retrieving ticket by ID: {}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
        return ticketMapper.mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(UUID id, TicketRequest request) {
        log.info("Updating ticket ID: {}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        handleStatusTransition(ticket, request.getStatus());

        if (request.getTitle() != null) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }

        updateCategory(ticket, request.getCategoryId());
        updateAssignee(ticket, request.getAssignedTo());

        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket updated successfully: id={}", updatedTicket.getId());
        return ticketMapper.mapToResponse(updatedTicket);
    }

    private void handleStatusTransition(Ticket ticket, Status newStatus) {
        Status oldStatus = ticket.getStatus();
        if (newStatus == null || newStatus == oldStatus) {
            return;
        }

        Set<Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            log.error("Invalid status transition attempted from {} to {} for ticket ID: {}", oldStatus, newStatus, ticket.getId());
            throw new InvalidStatusTransitionException("Invalid status transition from " + oldStatus + " to " + newStatus);
        }

        // Reopen validation: Transition from RESOLVED/CLOSED back to OPEN/IN_PROGRESS
        if ((oldStatus == Status.RESOLVED || oldStatus == Status.CLOSED) &&
                (newStatus == Status.OPEN || newStatus == Status.IN_PROGRESS)) {
            log.info("Ticket reopened: incrementing reopen count for ticket ID: {}", ticket.getId());
            ticket.setReopenCount(ticket.getReopenCount() + 1);
        }

        ticket.setStatus(newStatus);

        // Handle resolution/close timestamps
        if (newStatus == Status.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now(java.time.ZoneId.systemDefault()));
        } else if (newStatus == Status.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now(java.time.ZoneId.systemDefault()));
        }

        // Log audit activity
        activityService.logActivity(ticket, ticket.getCreatedBy(), "STATUS_TRANSITION", oldStatus.name(), newStatus.name());
    }

    private void updateCategory(Ticket ticket, UUID categoryId) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + categoryId));
            ticket.setCategory(category);
        }
    }

    private void updateAssignee(Ticket ticket, UUID assignedTo) {
        if (assignedTo != null) {
            User assignee = userRepository.findById(assignedTo)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assignee User not found with id: " + assignedTo));

            // Set first response timestamp if it's the first time an assignee is set
            if (ticket.getAssignedTo() == null && ticket.getFirstRespondedAt() == null) {
                ticket.setFirstRespondedAt(LocalDateTime.now(java.time.ZoneId.systemDefault()));
            }
            ticket.setAssignedTo(assignee);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> listTickets(int page, int size, Status status, Priority priority, UUID categoryId,
            UUID assignedTo, String search, String sortBy, String sortDir) {
        log.info("Listing tickets: page={}, size={}, status={}, priority={}, categoryId={}, assignedTo={}, search={}, sortBy={}, sortDir={}",
                page, size, status, priority, categoryId, assignedTo, search, sortBy, sortDir);
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (assignedTo != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedTo));
            }
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate numberMatch = cb.like(cb.lower(root.get("ticketNumber")), pattern);
                predicates.add(cb.or(titleMatch, descMatch, numberMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Ticket> ticketPage = ticketRepository.findAll(spec, pageable);
        log.info("Found {} tickets on page {} of {}", ticketPage.getNumberOfElements(), ticketPage.getNumber(), ticketPage.getTotalPages());
        List<TicketResponse> responses = ticketPage.getContent().stream()
                .map(ticketMapper::mapToResponse)
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
}
