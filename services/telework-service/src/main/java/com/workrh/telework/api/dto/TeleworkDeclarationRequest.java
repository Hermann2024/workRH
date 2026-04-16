package com.workrh.telework.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TeleworkDeclarationRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate workDate,
        String countryCode,
        @Min(1) Integer totalWorkMinutes,
        @Min(0) Integer residenceTeleworkMinutes,
        @Min(0) Integer residenceNonTeleworkMinutes,
        @Min(0) Integer otherForeignWorkMinutes,
        String otherForeignCountryCode,
        Boolean connectedToEmployerInfrastructure
) {
}
