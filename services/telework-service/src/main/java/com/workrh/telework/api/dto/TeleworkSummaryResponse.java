package com.workrh.telework.api.dto;

public record TeleworkSummaryResponse(
        Long employeeId,
        int annualUsedDays,
        int annualRemainingDays,
        int monthUsedDays,
        boolean annualFiscalLimitExceeded,
        TeleworkPolicySnapshot policy,
        TeleworkFiscalSummary fiscal,
        TeleworkSocialSecuritySummary socialSecurity
) {
}
