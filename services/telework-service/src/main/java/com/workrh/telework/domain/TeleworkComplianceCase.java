package com.workrh.telework.domain;

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
@Table(name = "telework_compliance_cases", uniqueConstraints = {
        @UniqueConstraint(name = "uk_compliance_case_period_employee", columnNames = {"tenantId", "employeeId", "year", "month"})
})
public class TeleworkComplianceCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private Long employeeId;
    private int year;
    private int month;
    private String countryCode;
    @Enumerated(EnumType.STRING)
    private EmploymentSector employmentSector = EmploymentSector.PRIVATE;
    @Enumerated(EnumType.STRING)
    private ComplianceCaseStatus status = ComplianceCaseStatus.DRAFT;
    @Enumerated(EnumType.STRING)
    private ComplianceStepStatus ccssA1Status = ComplianceStepStatus.NOT_STARTED;
    @Enumerated(EnumType.STRING)
    private ComplianceStepStatus teleworkAgreementStatus = ComplianceStepStatus.NOT_STARTED;
    @Enumerated(EnumType.STRING)
    private ComplianceStepStatus equipmentStatus = ComplianceStepStatus.NOT_STARTED;
    @Enumerated(EnumType.STRING)
    private ComplianceStepStatus healthSafetyStatus = ComplianceStepStatus.NOT_STARTED;
    @Enumerated(EnumType.STRING)
    private ComplianceStepStatus accidentCoverageStatus = ComplianceStepStatus.NOT_STARTED;
    @Enumerated(EnumType.STRING)
    private ComplianceStepStatus dataProtectionStatus = ComplianceStepStatus.NOT_STARTED;
    @Enumerated(EnumType.STRING)
    private ComplianceStepStatus residenceCountryRulesStatus = ComplianceStepStatus.NOT_STARTED;
    @Enumerated(EnumType.STRING)
    private ComplianceStepStatus legalWatchStatus = ComplianceStepStatus.NOT_STARTED;
    private LocalDate a1SubmittedAt;
    private LocalDate a1ValidUntil;
    private LocalDate legalSourcesReviewedAt;
    private LocalDate nextLegalReviewAt;
    private String legalSourcesVersion = "2026-05-08";
    private String notes;
    private String validatedBy;
    private Instant validatedAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
