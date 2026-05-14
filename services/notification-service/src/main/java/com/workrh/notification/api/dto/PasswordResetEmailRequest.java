package com.workrh.notification.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetEmailRequest(
        @Email @NotBlank String email,
        @NotBlank String firstName,
        @NotBlank String companyName,
        @NotBlank String resetUrl
) {
}
