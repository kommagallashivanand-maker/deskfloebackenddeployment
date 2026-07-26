package com.p99soft.deskflow.scheduler;

import com.p99soft.deskflow.entity.SlaPolicy;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.event.TicketEventPublisher;
import com.p99soft.deskflow.repository.SlaPolicyRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaBreachScheduler {

    private final TicketRepository ticketRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final TicketEventPublisher ticketEventPublisher;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 60000) // Evaluates active SLA breaches once every minute
    @Transactional(readOnly = true)
    public void checkSlaBreaches() {
        log.debug("Evaluating tickets for SLA breaches...");
        List<Ticket> activeTickets = ticketRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Ticket ticket : activeTickets) {
            SlaPolicy policy = slaPolicyRepository.findByPriority(ticket.getPriority()).orElse(null);
            if (policy == null) {
                continue;
            }

            // 1. Evaluate Response SLA Breach
            if (ticket.getFirstRespondedAt() == null) {
                LocalDateTime responseDue = ticket.getCreatedAt().plusHours(policy.getResponseTimeHours());
                if (now.isAfter(responseDue)) {
                    if (!isBreachEventTriggered(ticket.getId().toString(), "Response SLA")) {
                        ticketEventPublisher.publishSlaBreached(ticket, "Response SLA");
                        log.info("Response SLA breached and event published for ticket ID: {}", ticket.getId());
                    }
                }
            }

            // 2. Evaluate Resolution SLA Breach
            if (ticket.getStatus() != Status.RESOLVED && ticket.getStatus() != Status.CLOSED) {
                LocalDateTime resolutionDue = ticket.getCreatedAt().plusHours(policy.getResolutionTimeHours());
                if (now.isAfter(resolutionDue)) {
                    if (!isBreachEventTriggered(ticket.getId().toString(), "Resolution SLA")) {
                        ticketEventPublisher.publishSlaBreached(ticket, "Resolution SLA");
                        log.info("Resolution SLA breached and event published for ticket ID: {}", ticket.getId());
                    }
                }
            }
        }
    }

    private boolean isBreachEventTriggered(String ticketId, String breachType) {
        String sql = "SELECT COUNT(*) FROM raw.raw_sla_breached WHERE (payload->>'ticket_id') = ? AND (payload->>'breach_type') = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ticketId, breachType);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Failed to query raw.raw_sla_breached for ticketId={}, breachType={}", ticketId, breachType, e);
            // If query fails (e.g. table does not exist or empty), allow triggering to be safe
            return false;
        }
    }
}
