package com.workrh.sickness.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SicknessResponseDto(
        Long id,
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}
