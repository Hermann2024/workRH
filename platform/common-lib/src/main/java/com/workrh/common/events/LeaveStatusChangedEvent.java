package com.workrh.common.events;

import java.time.Instant;
import java.time.LocalDate;

public record LeaveStatusChangedEvent(
        String tenantId,
        Long employeeId,
        Long leaveId,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "leave.status.changed";
    }
}
