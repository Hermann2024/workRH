package com.workrh.subscription.api.dto;

public record FeatureCheckResponse(
        String tenantId,
        String feature,
        boolean allowed,
        String reason,
        String planCode
) {
}
