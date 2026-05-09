package com.workrh.telework.api.dto;

import java.time.Instant;

public record TeleworkComplianceAuditEntryResponse(
        Long id,
        Long complianceCaseId,
        Long employeeId,
        String action,
        String actor,
        String beforeValue,
        String afterValue,
        Instant createdAt
) {
}
