package com.workrh.common.events;

import java.time.Instant;

public record ThresholdAlertEvent(
        String tenantId,
        Long employeeId,
        int annualUsedDays,
        int annualLimit,
        int alertStagePercent,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "telework.threshold.alert";
    }
}
