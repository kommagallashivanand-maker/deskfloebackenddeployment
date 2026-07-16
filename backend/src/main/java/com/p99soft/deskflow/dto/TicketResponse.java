package com.p99soft.deskflow.dto;

import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private UUID id;
    private String ticketNumber;
    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private UUID createdBy;
    private UUID assignedTo;
    private UUID categoryId;
    private LocalDateTime responseSlaDueAt;
    private LocalDateTime slaDueAt;
    private LocalDateTime firstRespondedAt;
    private Integer reopenCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
}
