package com.workrh.reporting.api.dto;

import java.math.BigDecimal;

public record TaxSimulationResponse(
        Long employeeId,
        int year,
        int month,
        int annualTeleworkDays,
        int annualFiscalLimitDays,
        int annualContractWorkDays,
        boolean thresholdExceeded,
        BigDecimal annualGrossSalary,
        BigDecimal salaryPerWorkDay,
        BigDecimal luxembourgTaxableSalary,
        BigDecimal foreignTaxableSalary,
        String calculationRule,
        String disclaimer
) {
}
