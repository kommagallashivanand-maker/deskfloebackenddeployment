package com.p99soft.deskflow.dto;

import com.p99soft.deskflow.enums.Priority;
import com.p99soft.deskflow.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO representing a request to create or update a support ticket")
public class TicketRequest {
    @NotBlank(message = "Title is required")
    @Schema(description = "Brief title/summary of the issue", example = "VPN Connection Drop")
    private String title;

    @Schema(description = "Detailed explanation of the issue", example = "Unable to connect to the corporate VPN from home using Cisco AnyConnect.")
    private String description;

    @Schema(description = "Priority level of the ticket", example = "HIGH")
    private Priority priority;

    @Schema(description = "Current lifecycle status of the ticket", example = "OPEN")
    private Status status;

    @Schema(description = "UUID of the user (agent) assigned to handle the ticket", example = "20000000-0000-0000-0000-000000000001")
    private UUID assignedTo;

    @NotNull(message = "Category ID is required")
    @Schema(description = "UUID of the category this ticket belongs to", example = "a6b5c4d3-e2f1-4098-b64d-91b654dc3210")
    private UUID categoryId;

    @NotNull(message = "Creator ID is required")
    @Schema(description = "UUID of the user who is raising this ticket", example = "10000000-0000-0000-0000-000000000001")
    private UUID createdBy;
}
