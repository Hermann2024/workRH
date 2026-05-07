package com.workrh.subscription.api.dto;

import java.util.List;

public record CatalogReadinessResponse(
        boolean stripeCheckoutAvailable,
        boolean supportAcknowledgementEmailAvailable,
        boolean smsDeliveryAvailable,
        boolean enterprisePlanAvailable,
        List<String> warnings
) {
}
