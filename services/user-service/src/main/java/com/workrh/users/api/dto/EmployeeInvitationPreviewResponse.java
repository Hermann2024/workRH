package com.workrh.users.api.dto;

import java.time.Instant;

public record EmployeeInvitationPreviewResponse(
        String tenantId,
        String companyName,
        String email,
        String firstName,
        String lastName,
        Instant expiresAt
) {
}
