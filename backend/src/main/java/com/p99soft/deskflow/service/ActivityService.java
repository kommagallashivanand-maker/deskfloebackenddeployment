package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.ActivityResponse;
import com.p99soft.deskflow.entity.Ticket;
import com.p99soft.deskflow.entity.User;

import java.util.List;
import java.util.UUID;

public interface ActivityService {
    void logActivity(Ticket ticket, User actor, String activityType, String oldValue, String newValue);
    List<ActivityResponse> getTicketActivities(UUID ticketId);
}
