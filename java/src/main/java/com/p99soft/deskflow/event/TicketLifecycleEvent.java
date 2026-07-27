package com.p99soft.deskflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class TicketLifecycleEvent extends ApplicationEvent {
    private final String eventType;
    private final UUID ticketId;
    private final UUID actorId;
    private final UUID eventId;
    private final String payload;

    public TicketLifecycleEvent(Object source, String eventType, UUID ticketId, UUID actorId, UUID eventId, String payload) {
        super(source);
        this.eventType = eventType;
        this.ticketId = ticketId;
        this.actorId = actorId;
        this.eventId = eventId;
        this.payload = payload;
    }
}
