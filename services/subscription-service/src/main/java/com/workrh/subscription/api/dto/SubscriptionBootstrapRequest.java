package com.workrh.subscription.api.dto;

import com.workrh.subscription.domain.PlanCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SubscriptionBootstrapRequest(
        @Email @NotBlank String ownerEmail,
        @Min(1) @Max(500) int seatsPurchased,
        PlanCode planCode
) {
}
