package com.workrh.telework.api.dto;

import java.time.Instant;

public record TeleworkPolicyResponse(
        Long id,
        String countryCode,
        int annualFiscalLimitDays,
        int weeklyCompanyLimitDays,
        int standardDailyWorkMinutes,
        int socialSecurityStandardThresholdPercent,
        int socialSecurityFrameworkThresholdPercent,
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
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
