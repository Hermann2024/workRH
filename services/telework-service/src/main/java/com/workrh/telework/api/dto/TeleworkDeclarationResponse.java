package com.workrh.telework.api.dto;

import com.workrh.telework.domain.TeleworkStatus;
import java.time.LocalDate;

public record TeleworkDeclarationResponse(
        Long id,
        Long employeeId,
        LocalDate workDate,
        String countryCode,
        TeleworkStatus status,
        int totalWorkMinutes,
        int residenceTeleworkMinutes,
        int residenceNonTeleworkMinutes,
        int otherForeignWorkMinutes,
        String otherForeignCountryCode,
        boolean connectedToEmployerInfrastructure,
        boolean countsTowardFiscalThreshold,
        boolean countsTowardSocialSecurityTelework,
        int monthUsedDays,
        int annualUsedDays,
        int annualRemainingDays,
        int weeklyUsedDays,
        boolean annualFiscalLimitExceeded,
        boolean weeklyCompanyLimitExceeded
) {
}
