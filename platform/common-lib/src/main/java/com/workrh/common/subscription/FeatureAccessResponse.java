package com.workrh.common.subscription;

public record FeatureAccessResponse(
        String tenantId,
        String feature,
        boolean allowed,
        String reason,
        String planCode
) {
}
