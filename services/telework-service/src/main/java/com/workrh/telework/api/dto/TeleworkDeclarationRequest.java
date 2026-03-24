package com.workrh.telework.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TeleworkDeclarationRequest(@NotNull Long employeeId, @NotNull LocalDate workDate, String countryCode) {
}
