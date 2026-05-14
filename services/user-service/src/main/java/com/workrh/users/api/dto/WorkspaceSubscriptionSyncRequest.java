package com.workrh.users.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WorkspaceSubscriptionSyncRequest(
        @NotBlank String planCode,
        @Min(1) int seatsPurchased,
        String ownerEmail,
        boolean active
) {
}
