package com.p99soft.deskflow.scheduler;

import com.p99soft.deskflow.entity.SlaPolicy;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import com.p99soft.deskflow.event.TicketEventPublisher;
import com.p99soft.deskflow.repository.SlaPolicyRepository;
import com.p99soft.deskflow.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaBreachSchedulerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    @Mock
    private TicketEventPublisher ticketEventPublisher;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SlaBreachScheduler slaBreachScheduler;

    @Test
    void testCheckSlaBreaches_TriggersResponseBreach() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .priority(Priority.HIGH)
                .status(Status.OPEN)
                .createdAt(LocalDateTime.now().minusHours(5))
                .build();

        SlaPolicy policy = SlaPolicy.builder()
                .responseTimeHours(2)
                .resolutionTimeHours(24)
                .build();

        when(ticketRepository.findAll()).thenReturn(Collections.singletonList(ticket));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(policy));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(ticket.getId().toString()), eq("Response SLA"))).thenReturn(0);

        slaBreachScheduler.checkSlaBreaches();

        verify(ticketEventPublisher).publishSlaBreached(eq(ticket), eq("Response SLA"));
    }

    @Test
    void testCheckSlaBreaches_SkipsTriggerIfAlreadyRecorded() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .priority(Priority.HIGH)
                .status(Status.OPEN)
                .createdAt(LocalDateTime.now().minusHours(5))
                .build();

        SlaPolicy policy = SlaPolicy.builder()
                .responseTimeHours(2)
                .resolutionTimeHours(24)
                .build();

        when(ticketRepository.findAll()).thenReturn(Collections.singletonList(ticket));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(policy));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(ticket.getId().toString()), eq("Response SLA"))).thenReturn(1);

        slaBreachScheduler.checkSlaBreaches();

        verify(ticketEventPublisher, never()).publishSlaBreached(eq(ticket), eq("Response SLA"));
    }
}
