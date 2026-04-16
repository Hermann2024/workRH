package com.workrh.telework.api.dto;

public record TeleworkPolicySnapshot(
        String countryCode,
        int annualFiscalLimitDays,
        int annualFiscalRemainingDays,
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
        int weeklyUsedDays,
        boolean annualFiscalLimitExceeded,
        boolean weeklyCompanyLimitExceeded
) {
}
