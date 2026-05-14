package com.workrh.notification.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmployeeInvitationEmailRequest(
        @Email @NotBlank String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String companyName,
        @NotBlank String invitationUrl
) {
}

