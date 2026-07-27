package com.p99soft.deskflow.service;

import com.p99soft.deskflow.event.TicketLifecycleEvent;
import com.p99soft.deskflow.event.TicketLifecycleEventListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketLifecycleEventTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TicketLifecycleEventListener eventListener;

    @Test
    void testHandleLifecycleEvent_TicketCreated() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String payload = "{\"event_type\": \"ticket_created\", \"ticket_id\": \"" + ticketId + "\"}";

        TicketLifecycleEvent event = new TicketLifecycleEvent(this, "ticket_created", ticketId, actorId, eventId, payload);
        eventListener.handleLifecycleEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(payload), any(UUID.class), eq("backend-java"));

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("raw.raw_ticket_created"));
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    void testHandleLifecycleEvent_StatusChanged() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String payload = "{\"event_type\": \"status_changed\", \"ticket_id\": \"" + ticketId + "\"}";

        TicketLifecycleEvent event = new TicketLifecycleEvent(this, "status_changed", ticketId, actorId, eventId, payload);
        eventListener.handleLifecycleEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(payload), any(UUID.class), eq("backend-java"));

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("raw.raw_status_changed"));
    }

    @Test
    void testHandleLifecycleEvent_FailurePath() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String payload = "{\"event_type\": \"ticket_created\", \"ticket_id\": \"" + ticketId + "\"}";

        // Mock raw insert to throw exception
        doThrow(new RuntimeException("DB Connection Refused"))
                .when(jdbcTemplate).update(contains("raw.raw_ticket_created"), eq(payload), any(UUID.class), eq("backend-java"));

        TicketLifecycleEvent event = new TicketLifecycleEvent(this, "ticket_created", ticketId, actorId, eventId, payload);
        eventListener.handleLifecycleEvent(event);

        // Verify it logs to audit.rejected_events
        verify(jdbcTemplate).update(
                contains("audit.rejected_events"),
                eq(eventId.toString()),
                eq("ticket_created"),
                contains("DB Connection Refused"),
                eq(payload),
                eq("backend-java"),
                any(UUID.class)
        );

        // Verify it logs a FAILED row to audit.ingestion_audit
        verify(jdbcTemplate).update(
                contains("audit.ingestion_audit"),
                any(UUID.class),
                eq("backend-java"),
                any(Timestamp.class),
                any(Timestamp.class),
                eq(1),
                eq(0),
                eq(1),
                any(java.math.BigDecimal.class),
                eq("FAILED")
        );
    }
}
