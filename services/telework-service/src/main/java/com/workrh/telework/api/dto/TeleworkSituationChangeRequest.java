package com.workrh.telework.api.dto;

import com.workrh.telework.domain.TeleworkSituationChangeType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TeleworkSituationChangeRequest(
        @NotNull Long employeeId,
        @NotNull TeleworkSituationChangeType type,
        @NotNull LocalDate effectiveDate,
        String previousValue,
        String newValue,
        String reason
) {
}
