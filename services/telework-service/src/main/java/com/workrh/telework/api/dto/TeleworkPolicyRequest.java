package com.workrh.telework.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TeleworkPolicyRequest(
        @NotBlank String countryCode,
        @Min(1) int annualFiscalLimitDays,
        @Min(0) int weeklyCompanyLimitDays,
        @Min(60) Integer standardDailyWorkMinutes,
        @Min(1) int socialSecurityStandardThresholdPercent,
        @Min(1) int socialSecurityFrameworkThresholdPercent,
        int shortActivityToleranceMinutes,
        boolean weeklyLimitEnabled,
        boolean socialSecurityFrameworkAgreementEligible,
        boolean partialDayCountsAsFullDay,
        boolean thresholdProrated,
        boolean thirdCountryDaysCounted,
        String taxRuleLabel,
        String legalReference,
        String taxSourceUrl,
        String socialSecuritySourceUrl,
        String notes,
        boolean active
) {
}
