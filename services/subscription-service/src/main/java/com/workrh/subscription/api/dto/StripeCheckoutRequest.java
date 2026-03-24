package com.workrh.subscription.api.dto;

import com.workrh.subscription.domain.PlanCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StripeCheckoutRequest(
        @NotNull PlanCode planCode,
        @Min(1) int seatsPurchased,
        @NotEmpty List<String> paymentMethodTypes,
        boolean smsOptionEnabled,
        boolean advancedAuditOptionEnabled,
        boolean advancedExportOptionEnabled,
        @Email String customerEmail,
        @NotBlank String successUrl,
        @NotBlank String cancelUrl
) {
}
