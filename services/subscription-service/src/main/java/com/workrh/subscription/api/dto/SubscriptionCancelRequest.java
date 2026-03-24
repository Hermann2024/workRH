package com.workrh.subscription.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionCancelRequest(@NotBlank String reason) {
}
