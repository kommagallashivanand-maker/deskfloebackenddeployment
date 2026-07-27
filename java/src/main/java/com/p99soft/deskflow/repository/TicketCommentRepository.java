package com.p99soft.deskflow.repository;

import com.p99soft.deskflow.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {
    List<TicketComment> findByTicketIdAndParentCommentIsNullOrderByCreatedAtAsc(UUID ticketId);
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
    long countByTicketId(UUID ticketId);
}
