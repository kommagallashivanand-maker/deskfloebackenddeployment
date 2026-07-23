package com.p99soft.deskflow.service.Impl;

import com.p99soft.deskflow.dto.ActivityResponse;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.TicketActivity;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.repository.TicketActivityRepository;
import com.p99soft.deskflow.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final TicketActivityRepository activityRepository;

    @Override
    @Transactional
    public void logActivity(Ticket ticket, User actor, String activityType, String oldValue, String newValue) {
        log.info("Logging activity for ticket ID: {}, type: {}, old: {}, new: {}", ticket.getId(), activityType, oldValue, newValue);
        TicketActivity activity = TicketActivity.builder()
                .ticket(ticket)
                .actor(actor)
                .activityType(activityType)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getTicketActivities(UUID ticketId) {
        log.info("Fetching activity history for ticket ID: {}", ticketId);
        List<TicketActivity> activities = activityRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        return activities.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private ActivityResponse mapToResponse(TicketActivity activity) {
        String actorName = null;
        if (activity.getActor() != null) {
            actorName = activity.getActor().getFirstName() + " " + activity.getActor().getLastName();
        }
        return ActivityResponse.builder()
                .id(activity.getId())
                .ticketId(activity.getTicket().getId())
                .actorId(activity.getActor() != null ? activity.getActor().getId() : null)
                .actorName(actorName)
                .activityType(activity.getActivityType())
                .oldValue(activity.getOldValue())
                .newValue(activity.getNewValue())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
