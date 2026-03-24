package com.workrh.users.api.dto;

import com.workrh.users.domain.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String countryOfResidence,
        String phoneNumber,
        String department,
        String jobTitle,
        boolean crossBorderWorker,
        LocalDate hireDate,
        boolean active,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
