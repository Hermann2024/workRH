package com.workrh.reporting.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TaxSimulationRequest(
        @NotNull Long employeeId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal annualGrossSalary,
        @Min(1) int annualContractWorkDays
) {
}
