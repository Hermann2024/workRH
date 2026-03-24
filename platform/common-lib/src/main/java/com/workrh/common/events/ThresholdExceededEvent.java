package com.workrh.common.events;

import java.time.Instant;

public record ThresholdExceededEvent(
        String tenantId,
        Long employeeId,
        int annualUsedDays,
        int annualLimit,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "telework.threshold.exceeded";
    }
}
