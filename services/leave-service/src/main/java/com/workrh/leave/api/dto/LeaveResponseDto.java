package com.workrh.leave.api.dto;

import com.workrh.leave.domain.LeaveStatus;
import com.workrh.leave.domain.LeaveType;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveResponseDto(
        Long id,
        Long employeeId,
        LeaveType type,
        LeaveStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String comment,
        boolean evidenceRequired,
        boolean evidenceUploaded,
        String evidenceFileName,
        String evidenceSha256,
        Instant evidenceUploadedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
