package com.workrh.telework.api.dto;

import java.time.Instant;

public record TeleworkPolicyResponse(
        Long id,
        String countryCode,
        int annualFiscalLimitDays,
        int weeklyCompanyLimitDays,
        boolean weeklyLimitEnabled,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
