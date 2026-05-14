package com.workrh.users.api.dto;

import com.workrh.users.domain.EmploymentContractType;
import com.workrh.users.domain.EmployeeGender;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeInvitationResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String countryOfResidence,
        String phoneNumber,
        String department,
        String jobTitle,
        LocalDate birthDate,
        EmployeeGender gender,
        EmploymentContractType contractType,
        LocalDate hireDate,
        String token,
        boolean emailSent,
        String status,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt
) {
}
