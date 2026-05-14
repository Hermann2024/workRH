package com.workrh.users.api.dto;

import com.workrh.users.domain.Role;
import com.workrh.users.domain.EmployeeGender;
import com.workrh.users.domain.EmploymentContractType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeCreateRequest(
        @Email String email,
        @NotBlank String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String countryOfResidence,
        String phoneNumber,
        String department,
        String jobTitle,
        LocalDate birthDate,
        EmployeeGender gender,
        EmploymentContractType contractType,
        boolean crossBorderWorker,
        LocalDate hireDate,
        @NotEmpty Set<Role> roles
) {
}
