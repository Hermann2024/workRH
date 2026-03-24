package com.workrh.subscription.api.dto;

import com.workrh.subscription.domain.PlanCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubscriptionChangeRequest(
        @NotNull PlanCode targetPlanCode,
        @Min(1) int seatsPurchased,
        boolean smsOptionEnabled,
        boolean advancedAuditOptionEnabled,
        boolean advancedExportOptionEnabled
) {
}
