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
@Schema(description = "Request payload to create or update a support ticket")
public class TicketRequest {

    @NotBlank(message = "Title is required")
    @Schema(description = "Brief title or summary of the issue", example = "VPN Connection Drop")
    private String title;

    @Schema(description = "Detailed description of the issue",
            example = "Unable to connect to the corporate VPN from home using Cisco AnyConnect.")
    private String description;

    @Schema(description = "Priority level of the ticket", example = "HIGH",
            allowableValues = {"LOW", "MEDIUM", "HIGH", "URGENT"})
    private Priority priority;

    @Schema(description = "Current lifecycle status of the ticket", example = "OPEN",
            allowableValues = {"OPEN", "TRIAGED", "IN_PROGRESS", "ON_HOLD", "RESOLVED", "CLOSED"})
    private Status status;

    @Schema(description = "UUID of the agent assigned to handle the ticket")
    private UUID assignedTo;

    @NotNull(message = "Category ID is required")
    @Schema(description = "UUID of the category this ticket belongs to")
    private UUID categoryId;

    @NotNull(message = "Creator ID is required")
    @Schema(description = "UUID of the user raising this ticket")
    private UUID createdBy;
}
