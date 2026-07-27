package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.CommentRequest;
import com.p99soft.deskflow.dto.CommentResponse;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.TicketComment;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.repository.TicketCommentRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.service.Impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.p99soft.deskflow.event.TicketEventPublisher;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private TicketCommentRepository commentRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityService activityService;
    @Mock
    private TicketEventPublisher ticketEventPublisher;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UUID ticketId;
    private UUID userId;
    private Ticket mockTicket;
    private User mockUser;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        userId = UUID.randomUUID();
        mockUser = User.builder().id(userId).firstName("Jane").lastName("Doe").build();
        mockTicket = Ticket.builder().id(ticketId).title("Sample Ticket").build();
    }

    @Test
    void testAddComment_WithUserMention_ParsesMention() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        TicketComment savedComment = TicketComment.builder()
                .id(UUID.randomUUID())
                .ticket(mockTicket)
                .user(mockUser)
                .content("Hey @john.doe please review this ticket")
                .build();

        when(commentRepository.save(any(TicketComment.class))).thenReturn(savedComment);

        CommentRequest request = CommentRequest.builder()
                .userId(userId)
                .content("Hey @john.doe please review this ticket")
                .build();

        CommentResponse response = commentService.addComment(ticketId, request);

        assertNotNull(response);
        assertEquals("Hey @john.doe please review this ticket", response.getContent());
        assertEquals(1, response.getMentions().size());
        assertEquals("john.doe", response.getMentions().get(0));
        verify(activityService, times(1)).logActivity(eq(mockTicket), eq(mockUser), eq("COMMENT_ADDED"), any(), any());
    }

    @Test
    void testGetThreadedComments_ReturnsNestedStructure() {
        when(ticketRepository.existsById(ticketId)).thenReturn(true);

        TicketComment parent = TicketComment.builder()
                .id(UUID.randomUUID())
                .ticket(mockTicket)
                .user(mockUser)
                .content("Parent comment")
                .build();

        TicketComment reply = TicketComment.builder()
                .id(UUID.randomUUID())
                .ticket(mockTicket)
                .user(mockUser)
                .parentComment(parent)
                .content("Child reply")
                .build();

        parent.setReplies(List.of(reply));

        when(commentRepository.findByTicketIdAndParentCommentIsNullOrderByCreatedAtAsc(ticketId))
                .thenReturn(List.of(parent));

        List<CommentResponse> responses = commentService.getThreadedComments(ticketId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Parent comment", responses.get(0).getContent());
        assertEquals(1, responses.get(0).getReplies().size());
        assertEquals("Child reply", responses.get(0).getReplies().get(0).getContent());
    }
}
