package com.workrh.users.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 2, max = 80) String firstName,
        @NotBlank @Size(min = 2, max = 80) String lastName,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        @Min(1) @Max(500) int seatsPurchased,
        String planCode
) {
}
