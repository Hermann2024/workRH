package com.workrh.reporting.api.dto;

import java.util.List;

public record DashboardResponse(
        int totalEmployeesTracked,
        int totalUsedDays,
        int totalRemainingDays,
        int fiscalAlerts,
        int weeklyAlerts,
        List<EmployeeDashboardItem> employees
) {

    public record EmployeeDashboardItem(
            Long employeeId,
            int usedDays,
            int annualUsedDays,
            int remainingDays,
            int weeklyUsedDays,
            boolean annualFiscalLimitExceeded,
            boolean weeklyCompanyLimitExceeded
    ) {
    }
}
