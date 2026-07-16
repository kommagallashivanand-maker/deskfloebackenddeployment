package com.p99soft.deskflow.dto;

import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequest {
    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private UUID assignedTo;
    private UUID categoryId;
    private UUID createdBy;
}
