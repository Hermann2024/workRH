package com.workrh.telework.api.dto;

import com.workrh.telework.domain.ComplianceCaseStatus;
import com.workrh.telework.domain.ComplianceStepStatus;
import com.workrh.telework.domain.EmploymentSector;
import java.time.LocalDate;

public record TeleworkComplianceCaseRequest(
        Long employeeId,
        Integer year,
        Integer month,
        String countryCode,
        EmploymentSector employmentSector,
        ComplianceCaseStatus status,
        ComplianceStepStatus ccssA1Status,
        ComplianceStepStatus teleworkAgreementStatus,
        ComplianceStepStatus equipmentStatus,
        ComplianceStepStatus healthSafetyStatus,
        ComplianceStepStatus accidentCoverageStatus,
        ComplianceStepStatus dataProtectionStatus,
        ComplianceStepStatus residenceCountryRulesStatus,
        ComplianceStepStatus legalWatchStatus,
        LocalDate a1SubmittedAt,
        LocalDate a1ValidUntil,
        LocalDate legalSourcesReviewedAt,
        LocalDate nextLegalReviewAt,
        String legalSourcesVersion,
        String notes
) {
}
