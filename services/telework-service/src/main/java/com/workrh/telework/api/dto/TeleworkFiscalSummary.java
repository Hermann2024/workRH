package com.workrh.telework.api.dto;

public record TeleworkFiscalSummary(
        double thresholdUnitsUsed,
        int thresholdDaysUsed,
        int thresholdDaysRemaining,
        int thresholdLimitDays,
        int residenceTeleworkDays,
        int residenceOtherWorkDays,
        int otherForeignWorkDays,
        int totalTrackedWorkMinutes,
        int luxembourgWorkMinutes,
        int outsideLuxembourgWorkMinutes,
        int luxembourgTaxableWorkMinutes,
        int foreignTaxableWorkMinutes,
        double luxembourgTaxableSharePercent,
        double foreignTaxableSharePercent,
        boolean thresholdExceeded,
        boolean toleranceRuleApplied,
        boolean partialDayCountsAsFullDay,
        String taxRuleLabel,
        String explanation
) {
}
