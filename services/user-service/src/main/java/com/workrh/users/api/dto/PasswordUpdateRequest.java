package com.workrh.users.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(@NotBlank String password) {
}
