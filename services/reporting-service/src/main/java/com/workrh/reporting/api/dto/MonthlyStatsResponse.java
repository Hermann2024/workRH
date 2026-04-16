package com.workrh.reporting.api.dto;

import java.util.List;

public record MonthlyStatsResponse(
        int year,
        int trackedEmployees,
        int peakUsedDays,
        int totalAlertMonths,
        List<MonthlyStatItem> months
) {

    public record MonthlyStatItem(
            int month,
            int usedDays,
            int remainingDays,
            int fiscalAlerts,
            int weeklyAlerts,
            int employeesTracked
    ) {
    }
}
