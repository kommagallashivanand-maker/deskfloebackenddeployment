package com.p99soft.deskflow.service;

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
import com.p99soft.deskflow.service.Impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private User creator;
    private User assignee;
    private Category category;
    private SlaPolicy slaPolicy;
    private Ticket ticket;
    private UUID ticketId;
    private UUID creatorId;
    private UUID assigneeId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        creator = User.builder()
                .id(creatorId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .role("USER")
                .status("ACTIVE")
                .build();

        assignee = User.builder()
                .id(assigneeId)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .role("AGENT")
                .status("ACTIVE")
                .build();

        category = Category.builder()
                .id(categoryId)
                .name(com.p99soft.deskflow.enums.CategoryType.TECHNICAL)
                .description("Tech support")
                .build();

        slaPolicy = SlaPolicy.builder()
                .priority(Priority.HIGH)
                .responseTimeHours(4)
                .resolutionTimeHours(24)
                .build();

        ticket = Ticket.builder()
                .id(ticketId)
                .ticketNumber("TKT-1001")
                .title("Cannot connect to VPN")
                .description("Timeout error")
                .priority(Priority.HIGH)
                .status(Status.OPEN)
                .createdBy(creator)
                .category(category)
                .reopenCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateTicket_Success() {
        TicketRequest request = TicketRequest.builder()
                .title("Cannot connect to VPN")
                .description("Timeout error")
                .priority(Priority.HIGH)
                .categoryId(categoryId)
                .createdBy(creatorId)
                .build();

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(ticketRepository.findLastTicketNumber()).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.createTicket(request);

        assertNotNull(response);
        assertEquals("TKT-1001", response.getTicketNumber());
        assertEquals(Status.OPEN, response.getStatus());
        assertEquals(creatorId, response.getCreatedBy());
        assertEquals(categoryId, response.getCategoryId());
        assertNotNull(response.getSlaDueAt());
        assertNotNull(response.getResponseSlaDueAt());

        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    void testGetTicketById_Success() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.getTicketById(ticketId);

        assertNotNull(response);
        assertEquals(ticketId, response.getId());
        assertEquals("TKT-1001", response.getTicketNumber());
        verify(ticketRepository, times(1)).findById(ticketId);
    }

    @Test
    void testGetTicketById_NotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicketById(ticketId));
        verify(ticketRepository, times(1)).findById(ticketId);
    }

    @Test
    void testUpdateTicket_StatusTransitionToResolved() {
        TicketRequest request = TicketRequest.builder()
                .status(Status.RESOLVED)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);

        assertNotNull(response);
        assertEquals(Status.RESOLVED, response.getStatus());
        assertNotNull(response.getResolvedAt());
        assertEquals(0, response.getReopenCount());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    void testUpdateTicket_ReopenTransition() {
        // Prepare ticket already resolved
        ticket.setStatus(Status.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());

        TicketRequest request = TicketRequest.builder()
                .status(Status.IN_PROGRESS)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);

        assertNotNull(response);
        assertEquals(Status.IN_PROGRESS, response.getStatus());
        assertEquals(1, response.getReopenCount());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    void testListTickets_WithFilters() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        PageResponse<TicketResponse> responses = ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId);

        assertNotNull(responses);
        assertEquals(1, responses.getContent().size());
        assertEquals(0, responses.getPageNumber());
        assertEquals(1, responses.getTotalElements());
        verify(ticketRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }
}
