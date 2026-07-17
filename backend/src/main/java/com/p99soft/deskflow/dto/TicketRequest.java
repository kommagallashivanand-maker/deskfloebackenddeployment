package com.p99soft.deskflow.dto;

import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private Status status;
    private UUID assignedTo;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotNull(message = "Creator ID is required")
    private UUID createdBy;
}
