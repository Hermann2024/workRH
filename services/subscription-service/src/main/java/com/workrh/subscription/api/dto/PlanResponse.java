package com.workrh.subscription.api.dto;

import com.workrh.subscription.domain.BillingCycle;
import com.workrh.subscription.domain.FeatureCode;
import com.workrh.subscription.domain.PlanCode;
import java.math.BigDecimal;
import java.util.Set;

public record PlanResponse(
        Long id,
        PlanCode code,
        String name,
        BigDecimal monthlyPrice,
        String stripePriceId,
        BillingCycle billingCycle,
        Integer minEmployees,
        Integer maxEmployees,
        boolean recommended,
        boolean customPricing,
        boolean active,
        Set<FeatureCode> features
) {
}
