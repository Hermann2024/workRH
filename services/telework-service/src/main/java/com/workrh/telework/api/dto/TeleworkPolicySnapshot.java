package com.workrh.telework.api.dto;

public record TeleworkPolicySnapshot(
        String countryCode,
        int annualFiscalLimitDays,
        int annualFiscalRemainingDays,
        int weeklyCompanyLimitDays,
        boolean weeklyLimitEnabled,
        int weeklyUsedDays,
        boolean annualFiscalLimitExceeded,
        boolean weeklyCompanyLimitExceeded
) {
}
