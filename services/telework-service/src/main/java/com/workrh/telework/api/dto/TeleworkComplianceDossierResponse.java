package com.workrh.telework.api.dto;

import java.time.LocalDate;
import java.util.List;

public record TeleworkComplianceDossierResponse(
        int year,
        int month,
        String countryCode,
        LocalDate generatedAt,
        String overallStatus,
        int employeesReviewed,
        int declarationsReviewed,
        int fiscalAlerts,
        int socialSecurityAlerts,
        int a1DeclarationsRequired,
        int article13Cases,
        List<TeleworkComplianceChecklistItem> checklist,
        List<TeleworkComplianceEmployeeRisk> employeeRisks,
        List<String> officialSources
) {
}
