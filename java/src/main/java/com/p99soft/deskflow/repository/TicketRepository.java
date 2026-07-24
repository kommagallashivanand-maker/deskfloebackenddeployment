package com.p99soft.deskflow.repository;

import com.p99soft.deskflow.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {
    @Query("SELECT t.ticketNumber FROM Ticket t ORDER BY t.ticketNumber DESC LIMIT 1")
    Optional<String> findLastTicketNumber();
}
