package com.workrh.sickness.service;

import com.workrh.common.events.SicknessDeclaredEvent;
import com.workrh.common.security.SecurityUtils;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.sickness.api.dto.SicknessRequestDto;
import com.workrh.sickness.api.dto.SicknessResponseDto;
import com.workrh.sickness.domain.SicknessRecord;
import com.workrh.sickness.repository.SicknessRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SicknessService {

    private static final Logger log = LoggerFactory.getLogger(SicknessService.class);
    private static final long MAX_EVIDENCE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EVIDENCE_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final SicknessRepository sicknessRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SicknessService(SicknessRepository sicknessRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.sicknessRepository = sicknessRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public SicknessResponseDto declare(SicknessRequestDto request) {
        assertCanAccessEmployee(request.employeeId());
        validatePeriod(request.startDate(), request.endDate());
        SicknessRecord record = new SicknessRecord();
        record.setTenantId(TenantContext.getTenantId());
        record.setEmployeeId(request.employeeId());
        record.setStartDate(request.startDate());
        record.setEndDate(request.endDate());
        record.setComment(request.comment());
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        SicknessRecord saved = sicknessRepository.save(record);
        publishSicknessDeclared(saved);
        return toDto(saved);
    }

    public List<SicknessResponseDto> list() {
        return sicknessRepository.findAllByTenantId(TenantContext.getTenantId()).stream().map(this::toDto).toList();
    }

    public List<SicknessResponseDto> listByEmployee(Long employeeId) {
        assertCanAccessEmployee(employeeId);
        return sicknessRepository.findAllByTenantIdAndEmployeeId(TenantContext.getTenantId(), employeeId).stream()
                .map(this::toDto)
                .toList();
    }

    public SicknessResponseDto findById(Long sicknessId) {
        return toDto(getAccessibleRecord(sicknessId));
    }

    public SicknessResponseDto uploadEvidence(Long sicknessId, MultipartFile file) {
        SicknessRecord record = getAccessibleRecord(sicknessId);
        storeEvidence(record, file);
        record.setUpdatedAt(Instant.now());
        return toDto(sicknessRepository.save(record));
    }

    public EvidenceDownload downloadEvidence(Long sicknessId) {
        SicknessRecord record = getAccessibleRecord(sicknessId);
        if (record.getEvidenceContent() == null || record.getEvidenceContent().length == 0) {
            throw new NotFoundException("Sickness supporting evidence not found");
        }
        return new EvidenceDownload(
                record.getEvidenceFileName(),
                record.getEvidenceContentType(),
                record.getEvidenceContent()
        );
    }

    private SicknessRecord getAccessibleRecord(Long sicknessId) {
        SicknessRecord record = sicknessRepository.findByIdAndTenantId(sicknessId, TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Sickness record not found"));
        assertCanAccessEmployee(record.getEmployeeId());
        return record;
    }

    public List<SicknessResponseDto> listCurrentEmployee() {
        return listByEmployee(requireCurrentEmployeeId());
    }

    private void validatePeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("Sickness end date must be greater than or equal to start date");
        }
    }

    private SicknessResponseDto toDto(SicknessRecord record) {
        return new SicknessResponseDto(
                record.getId(),
                record.getEmployeeId(),
                record.getStartDate(),
                record.getEndDate(),
                record.getComment(),
                true,
                record.getEvidenceContent() != null && record.getEvidenceContent().length > 0,
                record.getEvidenceFileName(),
                record.getEvidenceUploadedAt(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    private void storeEvidence(SicknessRecord record, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Sickness supporting evidence file is required");
        }
        if (file.getSize() > MAX_EVIDENCE_SIZE_BYTES) {
            throw new BadRequestException("Sickness supporting evidence file must be 10 MB or less");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_EVIDENCE_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Sickness supporting evidence must be a PDF, JPG or PNG file");
        }
        try {
            record.setEvidenceContent(file.getBytes());
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read sickness supporting evidence file");
        }
        record.setEvidenceFileName(sanitizeFileName(file.getOriginalFilename()));
        record.setEvidenceContentType(contentType);
        record.setEvidenceUploadedAt(Instant.now());
        record.setEvidenceUploadedBy(SecurityUtils.currentUsername());
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "justificatif-maladie";
        }
        String normalized = fileName.replace('\\', '/');
        String lastSegment = normalized.substring(normalized.lastIndexOf('/') + 1);
        return lastSegment.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record EvidenceDownload(String fileName, String contentType, byte[] content) {
    }

    private void assertCanAccessEmployee(Long employeeId) {
        if (!SecurityUtils.hasAuthority("EMPLOYEE")) {
            return;
        }

        Long currentEmployeeId = requireCurrentEmployeeId();
        if (!currentEmployeeId.equals(employeeId)) {
            throw new AccessDeniedException("Employees can only access their own sickness records");
        }
    }

    private Long requireCurrentEmployeeId() {
        Long employeeId = SecurityUtils.currentEmployeeId();
        if (employeeId == null) {
            throw new AccessDeniedException("Missing employee context");
        }
        return employeeId;
    }

    private void publishSicknessDeclared(SicknessRecord record) {
        SicknessDeclaredEvent event = new SicknessDeclaredEvent(
                record.getTenantId(),
                record.getEmployeeId(),
                record.getId(),
                record.getStartDate(),
                record.getEndDate(),
                Instant.now()
        );
        try {
            kafkaTemplate.send("sickness-events", event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn(
                                    "Failed to publish sickness event for tenant {} employee {} record {}",
                                    record.getTenantId(),
                                    record.getEmployeeId(),
                                    record.getId(),
                                    exception
                            );
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to publish sickness event for tenant {} employee {} record {}",
                    record.getTenantId(),
                    record.getEmployeeId(),
                    record.getId(),
                    exception
            );
        }
    }
}
