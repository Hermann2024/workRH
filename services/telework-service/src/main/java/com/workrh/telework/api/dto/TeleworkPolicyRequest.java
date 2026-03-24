package com.workrh.telework.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TeleworkPolicyRequest(
        @NotBlank String countryCode,
        @Min(1) int annualFiscalLimitDays,
        @Min(0) int weeklyCompanyLimitDays,
        boolean weeklyLimitEnabled,
        boolean active
) {
}
