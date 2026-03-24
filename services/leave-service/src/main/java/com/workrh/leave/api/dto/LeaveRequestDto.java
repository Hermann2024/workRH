package com.workrh.leave.api.dto;

import com.workrh.leave.domain.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LeaveRequestDto(
        @NotNull Long employeeId,
        @NotNull LeaveType type,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String comment
) {
}
