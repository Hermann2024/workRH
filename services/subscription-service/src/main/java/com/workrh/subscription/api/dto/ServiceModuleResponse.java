package com.workrh.subscription.api.dto;

public record ServiceModuleResponse(
        String feature,
        String name,
        String category,
        String description,
        String backendScope,
        String actionLabel,
        String actionRoute,
        boolean enabled
) {
}
