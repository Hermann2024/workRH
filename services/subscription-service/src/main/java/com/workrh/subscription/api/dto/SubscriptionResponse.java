package com.workrh.subscription.api.dto;

import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.util.Set;

public record SubscriptionResponse(
        Long id,
        String tenantId,
        PlanCode planCode,
        PlanCode pendingPlanCode,
        SubscriptionStatus status,
        int seatsPurchased,
        boolean smsOptionEnabled,
        boolean advancedAuditOptionEnabled,
        boolean advancedExportOptionEnabled,
        boolean cancelAtPeriodEnd,
        String cancellationReason,
        LocalDate startsAt,
        LocalDate renewsAt,
        LocalDate cancelledAt,
        String stripeCustomerEmail,
        String stripeCheckoutSessionId,
        String stripeSubscriptionId,
        boolean previewAllFeaturesActive,
        Set<String> entitlements
) {
}
