package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.TicketRequest;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.exception.InvalidStatusTransitionException;
import com.p99soft.deskflow.mapper.TicketMapper;
import com.p99soft.deskflow.repository.CategoryRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.service.Impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketStateMachineTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private StorageService storageService;
    @Mock
    private ActivityService activityService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private UUID ticketId;
    private Ticket mockTicket;
    private User mockUser;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        mockUser = User.builder().id(UUID.randomUUID()).firstName("John").lastName("Doe").build();
        mockTicket = Ticket.builder()
                .id(ticketId)
                .ticketNumber("TICK-1001")
                .title("Initial Ticket")
                .description("Test Description")
                .status(Status.OPEN)
                .priority(Priority.MEDIUM)
                .createdBy(mockUser)
                .reopenCount(0)
                .build();
    }

    @Test
    void testValidTransition_OpenToTriaged() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
        when(ticketMapper.mapToResponse(any(Ticket.class))).thenReturn(TicketResponse.builder().id(ticketId).status(Status.TRIAGED).build());

        TicketRequest request = TicketRequest.builder().status(Status.TRIAGED).build();
        TicketResponse response = ticketService.updateTicket(ticketId, request);

        assertNotNull(response);
        assertEquals(Status.TRIAGED, mockTicket.getStatus());
        verify(activityService, times(1)).logActivity(eq(mockTicket), eq(mockUser), eq("STATUS_TRANSITION"), eq("OPEN"), eq("TRIAGED"));
    }

    @Test
    void testValidTransition_OpenToInProgress() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
        when(ticketMapper.mapToResponse(any(Ticket.class))).thenReturn(TicketResponse.builder().id(ticketId).status(Status.IN_PROGRESS).build());

        TicketRequest request = TicketRequest.builder().status(Status.IN_PROGRESS).build();
        TicketResponse response = ticketService.updateTicket(ticketId, request);

        assertNotNull(response);
        assertEquals(Status.IN_PROGRESS, mockTicket.getStatus());
        verify(activityService, times(1)).logActivity(eq(mockTicket), eq(mockUser), eq("STATUS_TRANSITION"), eq("OPEN"), eq("IN_PROGRESS"));
    }

    @Test
    void testInvalidTransition_OpenToResolved_ThrowsException() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mockTicket));

        TicketRequest request = TicketRequest.builder().status(Status.RESOLVED).build();

        InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class, () -> {
            ticketService.updateTicket(ticketId, request);
        });

        assertTrue(ex.getMessage().contains("Invalid status transition from OPEN to RESOLVED"));
        verify(ticketRepository, never()).save(any());
        verify(activityService, never()).logActivity(any(), any(), any(), any(), any());
    }

    @Test
    void testReopenTicket_IncrementsReopenCount() {
        mockTicket.setStatus(Status.RESOLVED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
        when(ticketMapper.mapToResponse(any(Ticket.class))).thenReturn(TicketResponse.builder().id(ticketId).status(Status.OPEN).reopenCount(1).build());

        TicketRequest request = TicketRequest.builder().status(Status.OPEN).build();
        ticketService.updateTicket(ticketId, request);

        assertEquals(1, mockTicket.getReopenCount());
        assertEquals(Status.OPEN, mockTicket.getStatus());
    }
}
