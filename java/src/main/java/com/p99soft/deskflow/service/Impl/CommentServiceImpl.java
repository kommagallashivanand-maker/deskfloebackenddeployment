package com.p99soft.deskflow.service.Impl;

import com.p99soft.deskflow.dto.CommentRequest;
import com.p99soft.deskflow.dto.CommentResponse;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.TicketComment;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.exception.ResourceNotFoundException;
import com.p99soft.deskflow.repository.TicketCommentRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.service.ActivityService;
import com.p99soft.deskflow.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9._-]+)");

    @Override
    @Transactional
    public CommentResponse addComment(UUID ticketId, CommentRequest request) {
        log.info("Adding comment to ticket ID: {} by user ID: {}", ticketId, request.getUserId());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        TicketComment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent comment not found with id: " + request.getParentCommentId()));
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .user(user)
                .parentComment(parentComment)
                .content(request.getContent())
                .build();

        TicketComment savedComment = commentRepository.save(comment);

        // Audit log comment addition
        activityService.logActivity(ticket, user, "COMMENT_ADDED", null, savedComment.getId().toString());

        return mapToResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getThreadedComments(UUID ticketId) {
        log.info("Fetching threaded comments for ticket ID: {}", ticketId);
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }

        List<TicketComment> topLevelComments = commentRepository
                .findByTicketIdAndParentCommentIsNullOrderByCreatedAtAsc(ticketId);
        return topLevelComments.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private CommentResponse mapToResponse(TicketComment comment) {
        String userName = comment.getUser() != null
                ? comment.getUser().getFirstName() + " " + comment.getUser().getLastName()
                : null;

        List<String> mentions = extractMentions(comment.getContent());

        List<CommentResponse> replies = new ArrayList<>();
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            replies = comment.getReplies().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket().getId())
                .userId(comment.getUser() != null ? comment.getUser().getId() : null)
                .userName(userName)
                .content(comment.getContent())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .mentions(mentions)
                .replies(replies)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private List<String> extractMentions(String text) {
        List<String> mentions = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return mentions;
        }
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }
}
