package com.workrh.users.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeInvitationAcceptRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String password
) {
}
