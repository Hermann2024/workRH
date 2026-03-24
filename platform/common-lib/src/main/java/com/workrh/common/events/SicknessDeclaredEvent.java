package com.workrh.common.events;

import java.time.Instant;
import java.time.LocalDate;

public record SicknessDeclaredEvent(
        String tenantId,
        Long employeeId,
        Long sicknessId,
        LocalDate startDate,
        LocalDate endDate,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "sickness.declared";
    }
}
