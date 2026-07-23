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
import com.p99soft.deskflow.service.StorageService;
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
import org.springframework.mock.web.MockMultipartFile;

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
    @Mock
    private StorageService storageService;

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
        ticketService = new TicketServiceImpl(ticketRepository, userRepository, categoryRepository, ticketMapper, storageService);

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
    void testCreateTicket_WithAttachments_Success() throws Exception {
        TicketRequest request = TicketRequest.builder()
                .title("Cannot connect to VPN")
                .description("Timeout error")
                .priority(Priority.HIGH)
                .categoryId(categoryId)
                .createdBy(creatorId)
                .build();

        MockMultipartFile file1 = new MockMultipartFile("files", "test1.txt", "text/plain", "file content 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.jpg", "image/jpeg", "file content 2".getBytes());
        java.util.List<org.springframework.web.multipart.MultipartFile> files = java.util.List.of(file1, file2);

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(ticketRepository.findLastTicketNumber()).thenReturn(Optional.empty());
        when(storageService.uploadFile(file1)).thenReturn("https://test-bucket.s3.amazonaws.com/uploads/guid1.txt");
        when(storageService.uploadFile(file2)).thenReturn("https://test-bucket.s3.amazonaws.com/uploads/guid2.jpg");
        when(storageService.generatePresignedUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(ticketId);
            return saved;
        });
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.createTicket(request, files);

        assertNotNull(response);
        assertEquals("TKT-1001", response.getTicketNumber());
        assertEquals(2, response.getAttachments().size());
        assertEquals("test1.txt", response.getAttachments().get(0).getFileName());
        assertEquals("test2.jpg", response.getAttachments().get(1).getFileName());

        verify(storageService, times(1)).uploadFile(file1);
        verify(storageService, times(1)).uploadFile(file2);
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
        PageResponse<TicketResponse> responses = ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, "createdAt", "desc");
        PageResponse<TicketResponse> responses = ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, null, "createdAt", "desc");
        assertNotNull(responses);
        assertEquals(1, responses.getContent().size());
        assertEquals(0, responses.getPageNumber());
        assertEquals(1, responses.getTotalElements());
        verify(ticketRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testListTickets_SortingAscending() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(ticketRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, "createdAt", "asc");
        ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, null, "createdAt", "asc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertNotNull(capturedPageable);
        assertTrue(capturedPageable.getSort().getOrderFor("createdAt").isAscending());
    }

    @Test
    void testListTickets_SortingDescending() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(ticketRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, "createdAt", "desc");
        ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, null, "createdAt", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertNotNull(capturedPageable);
        assertTrue(capturedPageable.getSort().getOrderFor("createdAt").isDescending());
    }

    @Test
    void testListTickets_SortingByResolvedAt() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(ticketRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, "resolvedAt", "desc");
        ticketService.listTickets(0, 10, Status.OPEN, Priority.HIGH, categoryId, assigneeId, null, "resolvedAt", "desc");

        Pageable capturedPageable = pageableCaptor.getValue();
        assertNotNull(capturedPageable);
        assertTrue(capturedPageable.getSort().getOrderFor("resolvedAt").isDescending());
    }

    @Test
    void testListTickets_WithSearchKeyword() {
        Page<Ticket> page = new PageImpl<>(Collections.singletonList(ticket));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        PageResponse<TicketResponse> responses = ticketService.listTickets(0, 10, null, null, null, null, "VPN", "createdAt", "desc");

        assertNotNull(responses);
        assertEquals(1, responses.getContent().size());
        assertEquals("TKT-1001", responses.getContent().get(0).getTicketNumber());
        verify(ticketRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testCreateTicket_CreatorNotFound() {
        TicketRequest request = TicketRequest.builder()
                .createdBy(creatorId)
                .build();
        when(userRepository.findById(creatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.createTicket(request));
    }

    @Test
    void testCreateTicket_CategoryNotFound() {
        TicketRequest request = TicketRequest.builder()
                .createdBy(creatorId)
                .categoryId(categoryId)
                .build();
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.createTicket(request));
    }

    @Test
    void testCreateTicket_AssigneeNotFound() {
        TicketRequest request = TicketRequest.builder()
                .createdBy(creatorId)
                .categoryId(categoryId)
                .assignedTo(assigneeId)
                .build();
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.createTicket(request));
    }

    @Test
    void testCreateTicket_WithAssigneeSuccess() {
        TicketRequest request = TicketRequest.builder()
                .title("Cannot connect to VPN")
                .description("Timeout error")
                .priority(Priority.HIGH)
                .categoryId(categoryId)
                .createdBy(creatorId)
                .assignedTo(assigneeId)
                .build();

        ticket.setAssignedTo(assignee);
        ticket.setFirstRespondedAt(LocalDateTime.now());

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.findLastTicketNumber()).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.createTicket(request);
        assertNotNull(response);
        assertEquals(assigneeId, response.getAssignedTo());
        assertNotNull(response.getFirstRespondedAt());
    }

    @Test
    void testCreateTicket_NextTicketNumberFormatError() {
        TicketRequest request = TicketRequest.builder()
                .title("Cannot connect to VPN")
                .description("Timeout error")
                .priority(Priority.HIGH)
                .categoryId(categoryId)
                .createdBy(creatorId)
                .build();

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(ticketRepository.findLastTicketNumber()).thenReturn(Optional.of("INVALID-FORMAT"));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.createTicket(request);
        assertNotNull(response);
        assertEquals("TKT-1001", response.getTicketNumber());
    }

    @Test
    void testUpdateTicket_NotFound() {
        TicketRequest request = TicketRequest.builder().build();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.updateTicket(ticketId, request));
    }

    @Test
    void testUpdateTicket_StatusTransitionToClosed() {
        TicketRequest request = TicketRequest.builder()
                .status(Status.CLOSED)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);
        assertNotNull(response);
        assertEquals(Status.CLOSED, response.getStatus());
        assertNotNull(response.getClosedAt());
    }

    @Test
    void testUpdateTicket_CategoryUpdateSuccess() {
        UUID newCategoryId = UUID.randomUUID();
        Category newCategory = Category.builder()
                .id(newCategoryId)
                .name(com.p99soft.deskflow.enums.CategoryType.BILLING)
                .description("Billing support")
                .build();

        TicketRequest request = TicketRequest.builder()
                .categoryId(newCategoryId)
                .build();

        ticket.setCategory(newCategory);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);
        assertNotNull(response);
        assertEquals(newCategoryId, response.getCategoryId());
    }

    @Test
    void testUpdateTicket_CategoryUpdateNotFound() {
        UUID newCategoryId = UUID.randomUUID();
        TicketRequest request = TicketRequest.builder()
                .categoryId(newCategoryId)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.updateTicket(ticketId, request));
    }

    @Test
    void testUpdateTicket_AssigneeUpdateSuccess() {
        TicketRequest request = TicketRequest.builder()
                .assignedTo(assigneeId)
                .build();

        ticket.setAssignedTo(assignee);
        ticket.setFirstRespondedAt(LocalDateTime.now());

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);
        assertNotNull(response);
        assertEquals(assigneeId, response.getAssignedTo());
        assertNotNull(response.getFirstRespondedAt());
    }

    @Test
    void testUpdateTicket_AssigneeUpdateNotFound() {
        TicketRequest request = TicketRequest.builder()
                .assignedTo(assigneeId)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.updateTicket(ticketId, request));
    }

    @Test
    void testMapToResponse_NullCreatedAt() {
        ticket.setCreatedAt(null);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.getTicketById(ticketId);
        assertNotNull(response);
        assertNotNull(response.getSlaDueAt());
        assertNotNull(response.getResponseSlaDueAt());
    }

    @Test
    void testUpdateTicket_WithFieldsProvided() {
        TicketRequest request = TicketRequest.builder()
                .title("New Title")
                .description("New Description")
                .priority(Priority.LOW)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(slaPolicyRepository.findByPriority(Priority.LOW)).thenReturn(Optional.of(slaPolicy));

        TicketResponse response = ticketService.updateTicket(ticketId, request);
        assertNotNull(response);
        assertEquals("New Title", response.getTitle());
        assertEquals("New Description", response.getDescription());
        assertEquals(Priority.LOW, response.getPriority());
    }
}
