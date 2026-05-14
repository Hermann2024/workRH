package com.workrh.users.api.dto;

import com.workrh.users.domain.EmploymentContractType;
import com.workrh.users.domain.EmployeeGender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record EmployeeInvitationRequest(
        @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String countryOfResidence,
        String phoneNumber,
        String department,
        String jobTitle,
        LocalDate birthDate,
        EmployeeGender gender,
        EmploymentContractType contractType,
        LocalDate hireDate
) {
}
