package com.workrh.users.api.dto;

public record PasswordResetRequestResponse(
        boolean accepted,
        boolean emailSent
) {
}
