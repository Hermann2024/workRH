package com.workrh.telework.api.dto;

public record TeleworkComplianceEmployeeRisk(
        Long employeeId,
        String countryCode,
        int annualUsedDays,
        int annualLimitDays,
        boolean fiscalLimitExceeded,
        boolean a1Required,
        boolean article13Required,
        String likelyApplicableLegislationCountryCode,
        String riskLevel,
        String recommendation
) {
}
