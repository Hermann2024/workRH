package com.workrh.subscription.api.dto;

import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SubscriptionRequest(
        @NotNull PlanCode planCode,
        @NotNull SubscriptionStatus status,
        @Min(1) int seatsPurchased,
        boolean smsOptionEnabled,
        boolean advancedAuditOptionEnabled,
        boolean advancedExportOptionEnabled,
        @NotNull LocalDate startsAt,
        LocalDate renewsAt
) {
}
