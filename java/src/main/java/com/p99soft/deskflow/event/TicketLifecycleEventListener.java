package com.p99soft.deskflow.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketLifecycleEventListener {

    private final JdbcTemplate jdbcTemplate;

    @Async
    @EventListener
    public void handleLifecycleEvent(TicketLifecycleEvent event) {
        String eventType = event.getEventType();
        log.info("Processing ticket lifecycle event: type={}, ticketId={}", eventType, event.getTicketId());
        
        UUID batchId = UUID.randomUUID();
        String source = "backend-java";
        Timestamp startTime = Timestamp.from(Instant.now());
        
        String tableName = "raw.raw_" + eventType;
        String insertSql = "INSERT INTO " + tableName + " (payload, batch_id, source) VALUES (?::jsonb, ?, ?)";
        
        boolean isSuccess = false;
        String errorMessage = null;
        
        try {
            jdbcTemplate.update(insertSql, event.getPayload(), batchId, source);
            log.info("Successfully inserted event into raw table: {} with batch_id: {}", tableName, batchId);
            isSuccess = true;
        } catch (Exception e) {
            log.error("Failed to insert event into raw table: {}", tableName, e);
            errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown database error";
            
            // Insert into audit.rejected_events
            String rejectedSql = "INSERT INTO audit.rejected_events (event_id, event_type, rejection_reason, payload, source, batch_id) VALUES (?, ?, ?, ?::jsonb, ?, ?)";
            try {
                String eventIdStr = event.getEventId() != null ? event.getEventId().toString() : null;
                jdbcTemplate.update(rejectedSql, eventIdStr, eventType, errorMessage, event.getPayload(), source, batchId);
                log.info("Successfully logged failed event to audit.rejected_events");
            } catch (Exception re) {
                log.error("Failed to insert into audit.rejected_events", re);
            }
        } finally {
            Timestamp endTime = Timestamp.from(Instant.now());
            long latencyMs = endTime.getTime() - startTime.getTime();
            
            int eventsReceived = 1;
            int eventsLoaded = isSuccess ? 1 : 0;
            int eventsFailed = isSuccess ? 0 : 1;
            String status = isSuccess ? "SUCCESS" : "FAILED";
            
            // Insert into audit.ingestion_audit
            String auditSql = "INSERT INTO audit.ingestion_audit (batch_id, source, batch_start_time, batch_end_time, events_received, events_loaded, events_failed, latency_ms, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try {
                jdbcTemplate.update(auditSql, batchId, source, startTime, endTime, eventsReceived, eventsLoaded, eventsFailed, java.math.BigDecimal.valueOf(latencyMs), status);
                log.info("Successfully recorded ingestion audit: batchId={}, status={}, latencyMs={}", batchId, status, latencyMs);
            } catch (Exception ae) {
                log.error("Failed to insert into audit.ingestion_audit", ae);
            }
        }
    }
}
