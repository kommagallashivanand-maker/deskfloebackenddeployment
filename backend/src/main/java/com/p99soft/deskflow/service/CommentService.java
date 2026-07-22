package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.CommentRequest;
import com.p99soft.deskflow.dto.CommentResponse;

import java.util.List;
import java.util.UUID;

public interface CommentService {
    CommentResponse addComment(UUID ticketId, CommentRequest request);
    List<CommentResponse> getThreadedComments(UUID ticketId);
}
