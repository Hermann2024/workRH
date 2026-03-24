package com.workrh.common.events;

import java.time.Instant;
import java.time.LocalDate;

public record TeleworkDeclaredEvent(
        String tenantId,
        Long employeeId,
        Long declarationId,
        LocalDate workDate,
        String status,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "telework.declared";
    }
}
