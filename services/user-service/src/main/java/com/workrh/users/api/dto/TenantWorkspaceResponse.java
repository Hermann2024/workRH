package com.workrh.users.api.dto;

import java.time.Instant;

public record TenantWorkspaceResponse(
        String tenantId,
        String companyName,
        String ownerEmail,
        String planCode,
        Integer seatsPurchased,
        long seatsUsed,
        long activeEmployees,
        boolean seatLimitExceeded,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
