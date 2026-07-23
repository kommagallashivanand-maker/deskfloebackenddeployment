package com.p99soft.deskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO representing an audit activity feed record for a ticket state transition")
public class ActivityResponse {

    @Schema(description = "Unique ID of the activity entry")
    private UUID id;

    @Schema(description = "Ticket ID")
    private UUID ticketId;

    @Schema(description = "ID of the user who performed the transition")
    private UUID actorId;

    @Schema(description = "Name/Email of the actor")
    private String actorName;

    @Schema(description = "Type of activity e.g. STATUS_TRANSITION")
    private String activityType;

    @Schema(description = "Previous status value")
    private String oldValue;

    @Schema(description = "New status value")
    private String newValue;

    @Schema(description = "Timestamp of the state change")
    private LocalDateTime createdAt;
}
