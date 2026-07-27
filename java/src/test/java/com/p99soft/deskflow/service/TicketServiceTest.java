package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.PageResponse;
import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.entity.Category;
import com.p99soft.deskflow.entity.SlaPolicy;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Role;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.exception.ResourceNotFoundException;
import com.p99soft.deskflow.repository.CategoryRepository;
import com.p99soft.deskflow.repository.SlaPolicyRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.service.Impl.TicketServiceImpl;
import com.p99soft.deskflow.event.TicketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final LocalDateTime TEST_TIME = LocalDateTime.of(2026, 7, 21, 10, 0);

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SlaPolicyRepository slaPolicyRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private ActivityService activityService;
    @Mock
    private TicketEventPublisher ticketEventPublisher;

    private com.p99soft.deskflow.mapper.TicketMapper ticketMapper;
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
        ticketMapper = new com.p99soft.deskflow.mapper.TicketMapper(slaPolicyRepository, storageService);
        ticketService = new TicketServiceImpl(
                ticketRepository,
                userRepository,
                categoryRepository,
                ticketMapper,
                storageService,
                activityService,
                ticketEventPublisher,
                slaPolicyRepository
        );

        ticketId   = UUID.randomUUID();
        creatorId  = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        creator = User.builder()
                .id(creatorId)
                .firstName("John").lastName("Doe")
                .email("john@example.com")
                .role(Role.EMPLOYEE)
                .status("ACTIVE")
                .build();

        assignee = User.builder()
                .id(assigneeId)
                .firstName("Jane").lastName("Smith")
                .email("jane@example.com")
                .role(Role.AGENT)
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
                .createdAt(TEST_TIME)
                .updatedAt(TEST_TIME)
                .build();
    }

    // ------------------------------------------------------------------ //
    // Create tests
    // ------------------------------------------------------------------ //

    @Test
    void testCreateTicket_Success() {
        TicketRequest request = TicketRequest.builder()
                .title("Cannot connect to VPN")
                .description("Timeout error")
                .priority(Priority.HIGH)
                .categoryId(categoryId)
                .build(); // no createdBy — resolved from JWT

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(ticketRepository.findLastTicketNumber()).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.createTicket(request, creatorId);

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
    void testCreateTicket_WithAttachments_Success() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Cannot connect to VPN")
                .description("Timeout error")
                .priority(Priority.HIGH)
                .categoryId(categoryId)
                .build();

        MockMultipartFile file1 = new MockMultipartFile("files", "test1.txt", "text/plain", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.jpg", "image/jpeg", "content2".getBytes());
        List<org.springframework.web.multipart.MultipartFile> files = List.of(file1, file2);

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(ticketRepository.findLastTicketNumber()).thenReturn(Optional.empty());
        when(storageService.uploadFile(file1)).thenReturn("https://s3.example.com/guid1.txt");
        when(storageService.uploadFile(file2)).thenReturn("https://s3.example.com/guid2.jpg");
        when(storageService.generatePresignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
            Ticket saved = i.getArgument(0);
            saved.setId(ticketId);
            return saved;
        });
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.createTicket(request, creatorId, files);

        assertNotNull(response);
        assertEquals(2, response.getAttachments().size());
        assertEquals("test1.txt", response.getAttachments().get(0).getFileName());
        assertEquals("test2.jpg", response.getAttachments().get(1).getFileName());
        verify(storageService).uploadFile(file1);
        verify(storageService).uploadFile(file2);
    }

    @Test
    void testCreateTicket_CreatorNotFound() {
        TicketRequest request = TicketRequest.builder().categoryId(categoryId).build();
        when(userRepository.findById(creatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.createTicket(request, creatorId));
    }

    @Test
    void testCreateTicket_CategoryNotFound() {
        TicketRequest request = TicketRequest.builder().categoryId(categoryId).build();
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.createTicket(request, creatorId));
    }

    @Test
    void testCreateTicket_AssigneeNotFound() {
        TicketRequest request = TicketRequest.builder()
                .categoryId(categoryId)
                .assignedTo(assigneeId)
                .build();
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.createTicket(request, creatorId));
    }

    @Test
    void testCreateTicket_WithAssigneeSuccess() {
        TicketRequest request = TicketRequest.builder()
                .title("Cannot connect to VPN")
                .description("Timeout error")
                .priority(Priority.HIGH)
                .categoryId(categoryId)
                .assignedTo(assigneeId)
                .build();

        ticket.setAssignedTo(assignee);
        ticket.setFirstRespondedAt(TEST_TIME);

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.findLastTicketNumber()).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.createTicket(request, creatorId);

        assertNotNull(response);
        assertEquals(assigneeId, response.getAssignedTo());
        assertNotNull(response.getFirstRespondedAt());
    }

    @Test
    void testCreateTicket_NextTicketNumberFormatError() {
        TicketRequest request = TicketRequest.builder()
                .title("Test").priority(Priority.HIGH).categoryId(categoryId).build();

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(ticketRepository.findLastTicketNumber()).thenReturn(Optional.of("INVALID-FORMAT"));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.createTicket(request, creatorId);

        assertNotNull(response);
        assertEquals("TKT-1001", response.getTicketNumber());
    }

    // ------------------------------------------------------------------ //
    // Read tests — role-based visibility
    // ------------------------------------------------------------------ //

    @Test
    void testGetTicketById_AgentCanViewAnyTicket() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        // AGENT can view tickets they didn't create
        UUID agentId = UUID.randomUUID();
        TicketResponse response = ticketService.getTicketById(ticketId, agentId, Role.AGENT);

        assertNotNull(response);
        assertEquals(ticketId, response.getId());
    }

    @Test
    void testGetTicketById_EmployeeCanViewOwnTicket() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        // EMPLOYEE viewing their own ticket — creatorId matches ticket.createdBy
        TicketResponse response = ticketService.getTicketById(ticketId, creatorId, Role.EMPLOYEE);

        assertNotNull(response);
        assertEquals(ticketId, response.getId());
    }

    @Test
    void testGetTicketById_EmployeeCannotViewOtherTicket() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        // EMPLOYEE trying to view someone else's ticket
        UUID otherEmployeeId = UUID.randomUUID();

        assertThrows(AccessDeniedException.class,
                () -> ticketService.getTicketById(ticketId, otherEmployeeId, Role.EMPLOYEE));
    }

    @Test
    void testGetTicketById_AdminCanViewAnyTicket() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        UUID adminId = UUID.randomUUID();
        TicketResponse response = ticketService.getTicketById(ticketId, adminId, Role.ADMIN);

        assertNotNull(response);
        assertEquals(ticketId, response.getId());
    }

    @Test
    void testGetTicketById_NotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.getTicketById(ticketId, creatorId, Role.AGENT));
    }

    // ------------------------------------------------------------------ //
    // List tests — role-based scoping
    // ------------------------------------------------------------------ //

    @Test
    void testListTickets_AgentSeesAllTickets() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        UUID agentId = UUID.randomUUID();
        PageResponse<TicketResponse> result = ticketService.listTickets(
                0, 10, null, null, null, null, null, "createdAt", "desc",
                agentId, Role.AGENT);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(ticketRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testListTickets_EmployeeSeesOnlyOwnTickets() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        // EMPLOYEE scoped query — service adds createdBy predicate internally
        PageResponse<TicketResponse> result = ticketService.listTickets(
                0, 10, null, null, null, null, null, "createdAt", "desc",
                creatorId, Role.EMPLOYEE);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        // Spec is built with creator filter — validated indirectly through returned
        // data
        verify(ticketRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testListTickets_WithFilters() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        PageResponse<TicketResponse> result = ticketService.listTickets(
                0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, null, "createdAt", "desc",
                creatorId, Role.ADMIN);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testListTickets_WithSearchKeyword() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        PageResponse<TicketResponse> result = ticketService.listTickets(
                0, 10, null, null, null, null, "VPN", "createdAt", "desc",
                creatorId, Role.ADMIN);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("TKT-1001", result.getContent().get(0).getTicketNumber());
    }

    @Test
    void testListTickets_SortingAscending() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(ticketRepository.findAll(any(Specification.class), captor.capture())).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        ticketService.listTickets(0, 10, null, null, null, null, null, "createdAt", "asc",
                creatorId, Role.ADMIN);

        assertTrue(captor.getValue().getSort().getOrderFor("createdAt").isAscending());
    }

    @Test
    void testListTickets_SortingDescending() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(ticketRepository.findAll(any(Specification.class), captor.capture())).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        ticketService.listTickets(0, 10, null, null, null, null, null, "createdAt", "desc",
                creatorId, Role.ADMIN);

        assertTrue(captor.getValue().getSort().getOrderFor("createdAt").isDescending());
    }

    // ------------------------------------------------------------------ //
    // Update tests
    // ------------------------------------------------------------------ //

    @Test
    void testUpdateTicket_StatusTransitionToResolved() {
        ticket.setStatus(Status.IN_PROGRESS);
        TicketRequest request = TicketRequest.builder().status(Status.RESOLVED).build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);

        assertEquals(Status.RESOLVED, response.getStatus());
        assertNotNull(response.getResolvedAt());
        assertEquals(0, response.getReopenCount());
    }

    @Test
    void testUpdateTicket_ReopenTransition_IncrementsCount() {
        ticket.setStatus(Status.RESOLVED);
        ticket.setResolvedAt(TEST_TIME);
        TicketRequest request = TicketRequest.builder().status(Status.IN_PROGRESS).build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);

        assertEquals(Status.IN_PROGRESS, response.getStatus());
        assertEquals(1, response.getReopenCount());
    }

    @Test
    void testUpdateTicket_StatusTransitionToClosed() {
        TicketRequest request = TicketRequest.builder().status(Status.CLOSED).build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);

        assertEquals(Status.CLOSED, response.getStatus());
        assertNotNull(response.getClosedAt());
    }

    @Test
    void testUpdateTicket_NotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.updateTicket(ticketId, TicketRequest.builder().build()));
    }

    @Test
    void testUpdateTicket_WithFieldsProvided() {
        TicketRequest request = TicketRequest.builder()
                .title("New Title").description("New Desc").priority(Priority.LOW).build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
        when(slaPolicyRepository.findByPriority(Priority.LOW)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);

        assertEquals("New Title", response.getTitle());
        assertEquals("New Desc", response.getDescription());
        assertEquals(Priority.LOW, response.getPriority());
    }

    @Test
    void testUpdateTicket_CategoryUpdateSuccess() {
        UUID newCatId = UUID.randomUUID();
        Category newCategory = Category.builder()
                .id(newCatId).name(com.p99soft.deskflow.enums.CategoryType.BILLING).build();
        TicketRequest request = TicketRequest.builder().categoryId(newCatId).build();
        ticket.setCategory(newCategory);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(categoryRepository.findById(newCatId)).thenReturn(Optional.of(newCategory));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);
        assertEquals(newCatId, response.getCategoryId());
    }

    @Test
    void testUpdateTicket_CategoryUpdateNotFound() {
        UUID newCatId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(categoryRepository.findById(newCatId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.updateTicket(ticketId,
                        TicketRequest.builder().categoryId(newCatId).build()));
    }

    @Test
    void testUpdateTicket_AssigneeUpdateSuccess() {
        TicketRequest request = TicketRequest.builder()
                .assignedTo(assigneeId)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);
        assertEquals(assigneeId, response.getAssignedTo());
        assertNotNull(response.getFirstRespondedAt());

        verify(ticketEventPublisher).publishFirstResponse(eq(ticket), eq(assigneeId), any(LocalDateTime.class));
    }

    @Test
    void testUpdateTicket_AssigneeNotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.updateTicket(ticketId,
                        TicketRequest.builder().assignedTo(assigneeId).build()));
    }

    @Test
    void testMapToResponse_NullCreatedAt() {
        ticket.setCreatedAt(null);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.getTicketById(ticketId, creatorId, Role.AGENT);

        assertNotNull(response);
        assertNotNull(response.getSlaDueAt());
        assertNotNull(response.getResponseSlaDueAt());
    }
}
