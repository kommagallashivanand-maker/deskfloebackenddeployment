package com.p99soft.deskflow.dto;

import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO representing the response payload of a support ticket")
public class TicketResponse {
    @Schema(description = "Unique database identifier of the ticket", example = "f1111111-2222-3333-4444-555555555555")
    private UUID id;

    @Schema(description = "Formatted human-readable ticket code", example = "TCK-2026-0001")
    private String ticketNumber;

    @Schema(description = "Brief title/summary of the issue", example = "VPN Connection Drop")
    private String title;

    @Schema(description = "Detailed explanation of the issue", example = "Unable to connect to the corporate VPN from home using Cisco AnyConnect.")
    private String description;

    @Schema(description = "Priority level of the ticket", example = "HIGH")
    private Priority priority;

    @Schema(description = "Current lifecycle status of the ticket", example = "OPEN")
    private Status status;

    @Schema(description = "UUID of the user who created the ticket", example = "10000000-0000-0000-0000-000000000001")
    private UUID createdBy;

    @Schema(description = "UUID of the user (agent) assigned to handle the ticket", example = "20000000-0000-0000-0000-000000000001")
    private UUID assignedTo;

    @Schema(description = "UUID of the category this ticket belongs to", example = "a6b5c4d3-e2f1-4098-b64d-91b654dc3210")
    private UUID categoryId;

    @Schema(description = "Deadline timestamp for when the agent must first respond to the ticket", example = "2026-07-17T08:00:00")
    private LocalDateTime responseSlaDueAt;

    @Schema(description = "Deadline timestamp for when the ticket must be resolved under the SLA policy", example = "2026-07-17T12:00:00")
    private LocalDateTime slaDueAt;

    @Schema(description = "Timestamp when the agent first responded/took action on the ticket", example = "2026-07-17T07:45:00")
    private LocalDateTime firstRespondedAt;

    @Schema(description = "Number of times this ticket has been reopened after resolution", example = "0")
    private Integer reopenCount;

    @Schema(description = "Timestamp when the ticket was created", example = "2026-07-17T07:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the ticket details were last updated", example = "2026-07-17T07:15:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Timestamp when the ticket was resolved by the agent", example = "2026-07-17T07:30:00")
    private LocalDateTime resolvedAt;

    @Schema(description = "Timestamp when the ticket was closed", example = "2026-07-17T07:45:00")
    private LocalDateTime closedAt;
}
