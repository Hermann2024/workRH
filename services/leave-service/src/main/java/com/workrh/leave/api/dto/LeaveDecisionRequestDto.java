package com.workrh.leave.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LeaveDecisionRequestDto(@NotBlank String comment) {
}
