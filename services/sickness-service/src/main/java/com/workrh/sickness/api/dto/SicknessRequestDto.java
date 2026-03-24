package com.workrh.sickness.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SicknessRequestDto(
        @NotNull Long employeeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String comment
) {
}
