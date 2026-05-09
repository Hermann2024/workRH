package com.workrh.telework.api.dto;

import com.workrh.telework.domain.TeleworkSituationChangeType;
import java.time.Instant;
import java.time.LocalDate;

public record TeleworkSituationChangeResponse(
        Long id,
        Long employeeId,
        TeleworkSituationChangeType type,
        LocalDate effectiveDate,
        String previousValue,
        String newValue,
        String reason,
        String recordedBy,
        Instant recordedAt
) {
}
