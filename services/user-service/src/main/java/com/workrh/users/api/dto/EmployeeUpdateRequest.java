package com.workrh.users.api.dto;

import com.workrh.users.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeUpdateRequest(
        @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String countryOfResidence,
        String phoneNumber,
        String department,
        String jobTitle,
        boolean crossBorderWorker,
        LocalDate hireDate,
        @NotEmpty Set<Role> roles,
        Boolean active
) {
}
