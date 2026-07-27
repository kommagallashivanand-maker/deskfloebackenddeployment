package com.p99soft.deskflow.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String formatUtcTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        }
        return DateTimeFormatter.ISO_INSTANT.format(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void publish(String eventType, UUID ticketId, UUID actorId, Map<String, Object> payloadMap) {
        try {
            UUID eventId = UUID.randomUUID();
            payloadMap.put("event_id", eventId.toString());
            payloadMap.put("event_type", eventType);
            payloadMap.put("event_timestamp", formatUtcTimestamp(null));
            payloadMap.put("ticket_id", ticketId.toString());

            String jsonPayload = objectMapper.writeValueAsString(payloadMap);
            log.debug("Publishing event: {} - {}", eventType, jsonPayload);
            eventPublisher.publishEvent(new TicketLifecycleEvent(this, eventType, ticketId, actorId, eventId, jsonPayload));
        } catch (Exception e) {
            log.error("Failed to publish ticket lifecycle event: {}", eventType, e);
        }
    }

    public void publishTicketCreated(Ticket ticket, int responseTimeHours, int resolutionTimeHours) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticket_number", ticket.getTicketNumber());
        payload.put("title", ticket.getTitle());
        payload.put("description", ticket.getDescription());
        payload.put("priority", ticket.getPriority() != null ? ticket.getPriority().name() : null);
        payload.put("status", ticket.getStatus() != null ? ticket.getStatus().name() : null);
        payload.put("category_id", ticket.getCategory() != null ? ticket.getCategory().getId().toString() : null);
        payload.put("created_by", ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId().toString() : null);
        payload.put("team_id", (ticket.getCreatedBy() != null && ticket.getCreatedBy().getTeam() != null) ? 
                ticket.getCreatedBy().getTeam().getId().toString() : null);
        payload.put("assigned_to", ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId().toString() : null);
        payload.put("created_at", formatUtcTimestamp(ticket.getCreatedAt()));
        payload.put("response_time_hours", responseTimeHours);
        payload.put("resolution_time_hours", resolutionTimeHours);

        UUID actorId = ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId() : null;
        publish("ticket_created", ticket.getId(), actorId, payload);
    }

    public void publishStatusChanged(Ticket ticket, Status oldStatus, Status newStatus, UUID actorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticket_number", ticket.getTicketNumber());
        payload.put("old_status", oldStatus != null ? oldStatus.name() : null);
        payload.put("new_status", newStatus != null ? newStatus.name() : null);
        payload.put("changed_by", actorId != null ? actorId.toString() : null);
        payload.put("changed_at", formatUtcTimestamp(LocalDateTime.now()));

        publish("status_changed", ticket.getId(), actorId, payload);
    }

    public void publishAssigned(Ticket ticket, UUID assignedTo, UUID assignedBy) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticket_number", ticket.getTicketNumber());
        payload.put("assigned_to", assignedTo != null ? assignedTo.toString() : null);
        payload.put("assigned_by", assignedBy != null ? assignedBy.toString() : null);
        payload.put("assigned_at", formatUtcTimestamp(LocalDateTime.now()));

        publish("assigned", ticket.getId(), assignedBy, payload);
    }

    public void publishResolved(Ticket ticket, UUID resolvedBy) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticket_number", ticket.getTicketNumber());
        payload.put("resolved_by", resolvedBy != null ? resolvedBy.toString() : null);
        payload.put("resolved_at", formatUtcTimestamp(LocalDateTime.now()));

        publish("resolved", ticket.getId(), resolvedBy, payload);
    }

    public void publishSlaBreached(Ticket ticket, String breachType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticket_number", ticket.getTicketNumber());
        payload.put("breached_at", formatUtcTimestamp(LocalDateTime.now()));
        payload.put("breach_type", breachType);

        publish("sla_breached", ticket.getId(), null, payload);
    }

    public void publishFirstResponse(Ticket ticket, UUID respondedBy, LocalDateTime respondedAt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticket_number", ticket.getTicketNumber());
        payload.put("responded_by", respondedBy != null ? respondedBy.toString() : null);
        payload.put("first_responded_at", formatUtcTimestamp(respondedAt));

        publish("first_response", ticket.getId(), respondedBy, payload);
    }

    public void publishTicketReopened(Ticket ticket, UUID reopenedBy, int reopenCount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticket_number", ticket.getTicketNumber());
        payload.put("reopened_by", reopenedBy != null ? reopenedBy.toString() : null);
        payload.put("reopened_at", formatUtcTimestamp(LocalDateTime.now()));
        payload.put("reopen_count", reopenCount);

        publish("ticket_reopened", ticket.getId(), reopenedBy, payload);
    }
}
