package com.workrh.telework.service;

import com.workrh.common.security.SecurityUtils;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.NotFoundException;
import com.workrh.telework.api.dto.TeleworkComplianceAuditEntryResponse;
import com.workrh.telework.api.dto.TeleworkComplianceCaseRequest;
import com.workrh.telework.api.dto.TeleworkComplianceCaseResponse;
import com.workrh.telework.api.dto.TeleworkComplianceEvidenceRequest;
import com.workrh.telework.api.dto.TeleworkComplianceEvidenceResponse;
import com.workrh.telework.api.dto.TeleworkSituationChangeRequest;
import com.workrh.telework.api.dto.TeleworkSituationChangeResponse;
import com.workrh.telework.domain.ComplianceCaseStatus;
import com.workrh.telework.domain.ComplianceStepStatus;
import com.workrh.telework.domain.EmploymentSector;
import com.workrh.telework.domain.TeleworkComplianceAuditEntry;
import com.workrh.telework.domain.TeleworkComplianceCase;
import com.workrh.telework.domain.TeleworkComplianceEvidence;
import com.workrh.telework.domain.TeleworkSituationChange;
import com.workrh.telework.repository.TeleworkComplianceAuditEntryRepository;
import com.workrh.telework.repository.TeleworkComplianceCaseRepository;
import com.workrh.telework.repository.TeleworkComplianceEvidenceRepository;
import com.workrh.telework.repository.TeleworkSituationChangeRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeleworkComplianceWorkflowService {

    private final TeleworkComplianceCaseRepository caseRepository;
    private final TeleworkComplianceEvidenceRepository evidenceRepository;
    private final TeleworkSituationChangeRepository situationChangeRepository;
    private final TeleworkComplianceAuditEntryRepository auditEntryRepository;

    public TeleworkComplianceWorkflowService(
            TeleworkComplianceCaseRepository caseRepository,
            TeleworkComplianceEvidenceRepository evidenceRepository,
            TeleworkSituationChangeRepository situationChangeRepository,
            TeleworkComplianceAuditEntryRepository auditEntryRepository) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
        this.situationChangeRepository = situationChangeRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    public List<TeleworkComplianceCaseResponse> list(int year, int month) {
        String tenantId = TenantContext.getTenantId();
        return caseRepository.findAllByTenantIdAndYearAndMonth(tenantId, year, month).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TeleworkComplianceCaseResponse createOrUpdate(TeleworkComplianceCaseRequest request) {
        String tenantId = TenantContext.getTenantId();
        int year = request.year() != null ? request.year() : LocalDate.now().getYear();
        int month = request.month() != null ? request.month() : LocalDate.now().getMonthValue();
        TeleworkComplianceCase complianceCase = caseRepository
                .findByTenantIdAndEmployeeIdAndYearAndMonth(tenantId, request.employeeId(), year, month)
                .orElseGet(() -> newCase(tenantId, request.employeeId(), year, month));

        String before = snapshot(complianceCase);
        applyRequest(complianceCase, request);
        complianceCase.setUpdatedAt(Instant.now());
        TeleworkComplianceCase saved = caseRepository.save(complianceCase);
        audit(saved, before.equals("NEW") ? "CASE_CREATED" : "CASE_UPDATED", before, snapshot(saved));
        return toResponse(saved);
    }

    @Transactional
    public TeleworkComplianceCaseResponse update(Long caseId, TeleworkComplianceCaseRequest request) {
        TeleworkComplianceCase complianceCase = requireCase(caseId);
        String before = snapshot(complianceCase);
        applyRequest(complianceCase, request);
        complianceCase.setUpdatedAt(Instant.now());
        TeleworkComplianceCase saved = caseRepository.save(complianceCase);
        audit(saved, "CASE_UPDATED", before, snapshot(saved));
        return toResponse(saved);
    }

    @Transactional
    public TeleworkComplianceCaseResponse addEvidence(Long caseId, TeleworkComplianceEvidenceRequest request) {
        TeleworkComplianceCase complianceCase = requireCase(caseId);
        TeleworkComplianceEvidence evidence = new TeleworkComplianceEvidence();
        evidence.setTenantId(complianceCase.getTenantId());
        evidence.setComplianceCaseId(complianceCase.getId());
        evidence.setEvidenceType(request.evidenceType());
        evidence.setLabel(request.label());
        evidence.setReference(request.reference());
        evidence.setFileUrl(request.fileUrl());
        evidence.setUploadedBy(actor());
        evidenceRepository.save(evidence);
        audit(complianceCase, "EVIDENCE_ADDED", "", request.evidenceType() + ":" + request.label());
        return toResponse(complianceCase);
    }

    @Transactional
    public TeleworkSituationChangeResponse recordSituationChange(TeleworkSituationChangeRequest request) {
        String tenantId = TenantContext.getTenantId();
        TeleworkSituationChange change = new TeleworkSituationChange();
        change.setTenantId(tenantId);
        change.setEmployeeId(request.employeeId());
        change.setType(request.type());
        change.setEffectiveDate(request.effectiveDate());
        change.setPreviousValue(request.previousValue());
        change.setNewValue(request.newValue());
        change.setReason(request.reason());
        change.setRecordedBy(actor());
        TeleworkSituationChange saved = situationChangeRepository.save(change);

        caseRepository.findAllByTenantIdAndYearAndMonth(tenantId, request.effectiveDate().getYear(), request.effectiveDate().getMonthValue()).stream()
                .filter(item -> item.getEmployeeId().equals(request.employeeId()))
                .findFirst()
                .ifPresent(complianceCase -> audit(complianceCase, "SITUATION_CHANGE_RECORDED", request.previousValue(), request.newValue()));
        return toResponse(saved);
    }

    @Transactional
    public TeleworkComplianceCaseResponse validateForPayroll(Long caseId) {
        TeleworkComplianceCase complianceCase = requireCase(caseId);
        String before = snapshot(complianceCase);
        complianceCase.setStatus(ComplianceCaseStatus.READY_FOR_PAYROLL);
        complianceCase.setValidatedBy(actor());
        complianceCase.setValidatedAt(Instant.now());
        complianceCase.setUpdatedAt(Instant.now());
        TeleworkComplianceCase saved = caseRepository.save(complianceCase);
        audit(saved, "CASE_VALIDATED_FOR_PAYROLL", before, snapshot(saved));
        return toResponse(saved);
    }

    public byte[] exportCasesCsv(int year, int month) {
        StringBuilder csv = new StringBuilder("employeeId;year;month;country;sector;status;ccssA1;agreement;equipment;healthSafety;accident;dataProtection;residenceRules;legalWatch;legalVersion;notes\n");
        for (TeleworkComplianceCaseResponse item : list(year, month)) {
            csv.append(item.employeeId()).append(';')
                    .append(item.year()).append(';')
                    .append(item.month()).append(';')
                    .append(nullToEmpty(item.countryCode())).append(';')
                    .append(item.employmentSector()).append(';')
                    .append(item.status()).append(';')
                    .append(item.ccssA1Status()).append(';')
                    .append(item.teleworkAgreementStatus()).append(';')
                    .append(item.equipmentStatus()).append(';')
                    .append(item.healthSafetyStatus()).append(';')
                    .append(item.accidentCoverageStatus()).append(';')
                    .append(item.dataProtectionStatus()).append(';')
                    .append(item.residenceCountryRulesStatus()).append(';')
                    .append(item.legalWatchStatus()).append(';')
                    .append(nullToEmpty(item.legalSourcesVersion())).append(';')
                    .append(nullToEmpty(item.notes()).replace(';', ','))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private TeleworkComplianceCase newCase(String tenantId, Long employeeId, int year, int month) {
        TeleworkComplianceCase complianceCase = new TeleworkComplianceCase();
        complianceCase.setTenantId(tenantId);
        complianceCase.setEmployeeId(employeeId);
        complianceCase.setYear(year);
        complianceCase.setMonth(month);
        complianceCase.setCountryCode("FR");
        return complianceCase;
    }

    private void applyRequest(TeleworkComplianceCase complianceCase, TeleworkComplianceCaseRequest request) {
        complianceCase.setCountryCode(valueOrDefault(request.countryCode(), complianceCase.getCountryCode()));
        complianceCase.setEmploymentSector(request.employmentSector() != null ? request.employmentSector() : defaultValue(complianceCase.getEmploymentSector(), EmploymentSector.PRIVATE));
        complianceCase.setStatus(request.status() != null ? request.status() : defaultValue(complianceCase.getStatus(), ComplianceCaseStatus.IN_REVIEW));
        complianceCase.setCcssA1Status(request.ccssA1Status() != null ? request.ccssA1Status() : defaultValue(complianceCase.getCcssA1Status(), ComplianceStepStatus.TO_PREPARE));
        complianceCase.setTeleworkAgreementStatus(request.teleworkAgreementStatus() != null ? request.teleworkAgreementStatus() : defaultValue(complianceCase.getTeleworkAgreementStatus(), ComplianceStepStatus.TO_PREPARE));
        complianceCase.setEquipmentStatus(request.equipmentStatus() != null ? request.equipmentStatus() : defaultValue(complianceCase.getEquipmentStatus(), ComplianceStepStatus.TO_PREPARE));
        complianceCase.setHealthSafetyStatus(request.healthSafetyStatus() != null ? request.healthSafetyStatus() : defaultValue(complianceCase.getHealthSafetyStatus(), ComplianceStepStatus.TO_PREPARE));
        complianceCase.setAccidentCoverageStatus(request.accidentCoverageStatus() != null ? request.accidentCoverageStatus() : defaultValue(complianceCase.getAccidentCoverageStatus(), ComplianceStepStatus.TO_PREPARE));
        complianceCase.setDataProtectionStatus(request.dataProtectionStatus() != null ? request.dataProtectionStatus() : defaultValue(complianceCase.getDataProtectionStatus(), ComplianceStepStatus.TO_PREPARE));
        complianceCase.setResidenceCountryRulesStatus(request.residenceCountryRulesStatus() != null ? request.residenceCountryRulesStatus() : defaultValue(complianceCase.getResidenceCountryRulesStatus(), ComplianceStepStatus.TO_PREPARE));
        complianceCase.setLegalWatchStatus(request.legalWatchStatus() != null ? request.legalWatchStatus() : defaultValue(complianceCase.getLegalWatchStatus(), ComplianceStepStatus.TO_PREPARE));
        complianceCase.setA1SubmittedAt(request.a1SubmittedAt());
        complianceCase.setA1ValidUntil(request.a1ValidUntil());
        complianceCase.setLegalSourcesReviewedAt(request.legalSourcesReviewedAt());
        complianceCase.setNextLegalReviewAt(request.nextLegalReviewAt());
        complianceCase.setLegalSourcesVersion(valueOrDefault(request.legalSourcesVersion(), complianceCase.getLegalSourcesVersion()));
        complianceCase.setNotes(request.notes());
    }

    private TeleworkComplianceCase requireCase(Long caseId) {
        return caseRepository.findByIdAndTenantId(caseId, TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Telework compliance case not found"));
    }

    private void audit(TeleworkComplianceCase complianceCase, String action, String beforeValue, String afterValue) {
        TeleworkComplianceAuditEntry entry = new TeleworkComplianceAuditEntry();
        entry.setTenantId(complianceCase.getTenantId());
        entry.setComplianceCaseId(complianceCase.getId());
        entry.setEmployeeId(complianceCase.getEmployeeId());
        entry.setAction(action);
        entry.setActor(actor());
        entry.setBeforeValue(beforeValue);
        entry.setAfterValue(afterValue);
        auditEntryRepository.save(entry);
    }

    private TeleworkComplianceCaseResponse toResponse(TeleworkComplianceCase complianceCase) {
        String tenantId = TenantContext.getTenantId();
        return new TeleworkComplianceCaseResponse(
                complianceCase.getId(),
                complianceCase.getEmployeeId(),
                complianceCase.getYear(),
                complianceCase.getMonth(),
                complianceCase.getCountryCode(),
                complianceCase.getEmploymentSector(),
                complianceCase.getStatus(),
                complianceCase.getCcssA1Status(),
                complianceCase.getTeleworkAgreementStatus(),
                complianceCase.getEquipmentStatus(),
                complianceCase.getHealthSafetyStatus(),
                complianceCase.getAccidentCoverageStatus(),
                complianceCase.getDataProtectionStatus(),
                complianceCase.getResidenceCountryRulesStatus(),
                complianceCase.getLegalWatchStatus(),
                complianceCase.getA1SubmittedAt(),
                complianceCase.getA1ValidUntil(),
                complianceCase.getLegalSourcesReviewedAt(),
                complianceCase.getNextLegalReviewAt(),
                complianceCase.getLegalSourcesVersion(),
                complianceCase.getNotes(),
                complianceCase.getValidatedBy(),
                complianceCase.getValidatedAt(),
                complianceCase.getCreatedAt(),
                complianceCase.getUpdatedAt(),
                evidenceRepository.findAllByTenantIdAndComplianceCaseIdOrderByUploadedAtDesc(tenantId, complianceCase.getId()).stream().map(this::toResponse).toList(),
                situationChangeRepository.findTop50ByTenantIdAndEmployeeIdOrderByRecordedAtDesc(tenantId, complianceCase.getEmployeeId()).stream().map(this::toResponse).toList(),
                auditEntryRepository.findAllByTenantIdAndComplianceCaseIdOrderByCreatedAtDesc(tenantId, complianceCase.getId()).stream().map(this::toResponse).toList()
        );
    }

    private TeleworkComplianceEvidenceResponse toResponse(TeleworkComplianceEvidence evidence) {
        return new TeleworkComplianceEvidenceResponse(evidence.getId(), evidence.getEvidenceType(), evidence.getLabel(), evidence.getReference(), evidence.getFileUrl(), evidence.getUploadedBy(), evidence.getUploadedAt());
    }

    private TeleworkSituationChangeResponse toResponse(TeleworkSituationChange change) {
        return new TeleworkSituationChangeResponse(change.getId(), change.getEmployeeId(), change.getType(), change.getEffectiveDate(), change.getPreviousValue(), change.getNewValue(), change.getReason(), change.getRecordedBy(), change.getRecordedAt());
    }

    private TeleworkComplianceAuditEntryResponse toResponse(TeleworkComplianceAuditEntry entry) {
        return new TeleworkComplianceAuditEntryResponse(entry.getId(), entry.getComplianceCaseId(), entry.getEmployeeId(), entry.getAction(), entry.getActor(), entry.getBeforeValue(), entry.getAfterValue(), entry.getCreatedAt());
    }

    private String snapshot(TeleworkComplianceCase complianceCase) {
        if (complianceCase.getId() == null) {
            return "NEW";
        }
        return complianceCase.getStatus() + "|" + complianceCase.getCcssA1Status() + "|" + complianceCase.getTeleworkAgreementStatus() + "|" + complianceCase.getEmploymentSector();
    }

    private String actor() {
        String username = SecurityUtils.currentUsername();
        return username == null ? "system" : username;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }

    private <T> T defaultValue(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
