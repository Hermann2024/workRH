package com.workrh.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "employee_invitations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_invitation_token", columnNames = {"token_hash"})
})
public class EmployeeInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String countryOfResidence;
    private String phoneNumber;
    private String department;
    private String jobTitle;
    private LocalDate birthDate;
    @Enumerated(EnumType.STRING)
    private EmployeeGender gender = EmployeeGender.AUTRES;
    @Enumerated(EnumType.STRING)
    private EmploymentContractType contractType = EmploymentContractType.CDI;
    private LocalDate hireDate;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    private String invitedBy;
    private Instant expiresAt;
    private Instant acceptedAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
