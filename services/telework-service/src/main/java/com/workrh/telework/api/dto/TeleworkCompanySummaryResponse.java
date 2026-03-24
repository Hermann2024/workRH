package com.workrh.telework.api.dto;

import java.util.List;

public record TeleworkCompanySummaryResponse(
        int totalEmployeesTracked,
        int totalAnnualUsedDays,
        int totalAnnualRemainingDays,
        int totalEmployeesOverFiscalLimit,
        int totalEmployeesOverWeeklyPolicy,
        List<TeleworkSummaryResponse> employees
) {
}
