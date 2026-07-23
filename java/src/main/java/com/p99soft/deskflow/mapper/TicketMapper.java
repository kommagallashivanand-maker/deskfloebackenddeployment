package com.p99soft.deskflow.mapper;

import com.p99soft.deskflow.dto.AttachmentResponse;
import com.p99soft.deskflow.dto.TicketResponse;
import com.p99soft.deskflow.entity.SlaPolicy;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.repository.SlaPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TicketMapper {

    private final SlaPolicyRepository slaPolicyRepository;
    private final com.p99soft.deskflow.service.StorageService storageService;



    public TicketResponse mapToResponse(Ticket ticket) {
        TicketResponse response = TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .createdBy(ticket.getCreatedBy().getId())
                .assignedTo(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null)
                .categoryId(ticket.getCategory().getId())
                .firstRespondedAt(ticket.getFirstRespondedAt())
                .reopenCount(ticket.getReopenCount())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .build();

        // Calculate SLA due dates dynamically
        Optional<SlaPolicy> policyOpt = slaPolicyRepository.findByPriority(ticket.getPriority());
        if (policyOpt.isPresent()) {
            SlaPolicy policy = policyOpt.get();
            if (ticket.getCreatedAt() != null) {
                response.setResponseSlaDueAt(ticket.getCreatedAt().plusHours(policy.getResponseTimeHours()));
                response.setSlaDueAt(ticket.getCreatedAt().plusHours(policy.getResolutionTimeHours()));
            } else {
                LocalDateTime now = LocalDateTime.now(java.time.ZoneId.systemDefault());
                response.setResponseSlaDueAt(now.plusHours(policy.getResponseTimeHours()));
                response.setSlaDueAt(now.plusHours(policy.getResolutionTimeHours()));
            }
        }

        if (ticket.getAttachments() != null) {
            List<AttachmentResponse> attachmentResponses = ticket.getAttachments().stream()
                    .map(att -> AttachmentResponse.builder()
                            .id(att.getId())
                            .fileName(att.getFileName())
                            .fileUrl(storageService.generatePresignedUrl(att.getFileUrl()))
                            .fileType(att.getFileType())
                            .fileSize(att.getFileSize())
                            .createdAt(att.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
            response.setAttachments(attachmentResponses);
        }

        return response;
    }
}
