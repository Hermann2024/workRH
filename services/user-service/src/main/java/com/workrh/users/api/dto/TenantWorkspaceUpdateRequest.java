package com.workrh.users.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantWorkspaceUpdateRequest(
        @NotBlank @Size(min = 2, max = 255) String companyName
) {
}
