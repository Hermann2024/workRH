package com.workrh.leave.service;

import com.workrh.common.events.LeaveStatusChangedEvent;
import com.workrh.common.security.SecurityUtils;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.leave.api.dto.LeaveDecisionRequestDto;
import com.workrh.leave.api.dto.LeaveRequestDto;
import com.workrh.leave.api.dto.LeaveResponseDto;
import com.workrh.leave.domain.LeaveRequestEntity;
import com.workrh.leave.domain.LeaveStatus;
import com.workrh.leave.domain.LeaveType;
import com.workrh.leave.repository.LeaveRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);
    private static final long MAX_EVIDENCE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EVIDENCE_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );
    private static final EnumSet<LeaveType> EVIDENCE_REQUIRED_TYPES = EnumSet.of(
            LeaveType.PATERNITY,
            LeaveType.MOVING,
            LeaveType.MARRIAGE,
            LeaveType.BIRTH_OR_ADOPTION,
            LeaveType.FAMILY_CARE,
            LeaveType.BEREAVEMENT,
            LeaveType.MEDICAL_APPOINTMENT,
            LeaveType.TRAINING,
            LeaveType.ADMINISTRATIVE
    );

    private final LeaveRepository leaveRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LeaveService(LeaveRepository leaveRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.leaveRepository = leaveRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public LeaveResponseDto create(LeaveRequestDto request) {
        assertCanAccessEmployee(request.employeeId());
        validatePeriod(request.startDate(), request.endDate());
        LeaveRequestEntity entity = new LeaveRequestEntity();
        entity.setTenantId(TenantContext.getTenantId());
        entity.setEmployeeId(request.employeeId());
        entity.setType(request.type());
        entity.setStatus(LeaveStatus.REQUESTED);
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setComment(request.comment());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        LeaveRequestEntity saved = leaveRepository.save(entity);
        publishLeaveStatusChanged(saved);
        return toDto(saved);
    }

    public LeaveResponseDto findById(Long leaveId) {
        return toDto(getAccessibleLeave(leaveId));
    }

    public LeaveResponseDto uploadEvidence(Long leaveId, MultipartFile file) {
        LeaveRequestEntity entity = getAccessibleLeave(leaveId);
        if (!requiresEvidence(entity.getType())) {
            throw new BadRequestException("This leave type does not require supporting evidence");
        }
        if (entity.getStatus() != LeaveStatus.REQUESTED) {
            throw new BadRequestException("Supporting evidence can only be uploaded while the leave request is pending");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Supporting evidence file is required");
        }
        if (file.getSize() > MAX_EVIDENCE_SIZE_BYTES) {
            throw new BadRequestException("Supporting evidence file must be 10 MB or less");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_EVIDENCE_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Supporting evidence must be a PDF, JPG or PNG file");
        }

        try {
            entity.setEvidenceContent(file.getBytes());
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read supporting evidence file");
        }
        entity.setEvidenceFileName(sanitizeFileName(file.getOriginalFilename()));
        entity.setEvidenceContentType(contentType);
        entity.setEvidenceUploadedAt(Instant.now());
        entity.setEvidenceUploadedBy(SecurityUtils.currentUsername());
        entity.setUpdatedAt(Instant.now());
        return toDto(leaveRepository.save(entity));
    }

    public EvidenceDownload downloadEvidence(Long leaveId) {
        LeaveRequestEntity entity = getAccessibleLeave(leaveId);
        if (entity.getEvidenceContent() == null || entity.getEvidenceContent().length == 0) {
            throw new NotFoundException("Supporting evidence not found");
        }
        return new EvidenceDownload(
                entity.getEvidenceFileName(),
                entity.getEvidenceContentType(),
                entity.getEvidenceContent()
        );
    }

    public LeaveResponseDto approve(Long leaveId, LeaveDecisionRequestDto request) {
        LeaveRequestEntity entity = getAccessibleLeave(leaveId);
        if (requiresEvidence(entity.getType()) && entity.getEvidenceContent() == null) {
            throw new BadRequestException("Supporting evidence is required before approval");
        }
        return transitionLeave(leaveId, LeaveStatus.APPROVED, request.comment());
    }

    public LeaveResponseDto reject(Long leaveId, LeaveDecisionRequestDto request) {
        return transitionLeave(leaveId, LeaveStatus.REJECTED, request.comment());
    }

    public LeaveResponseDto cancel(Long leaveId, LeaveDecisionRequestDto request) {
        return transitionLeave(leaveId, LeaveStatus.CANCELLED, request.comment());
    }

    public List<LeaveResponseDto> list() {
        return leaveRepository.findAllByTenantId(TenantContext.getTenantId()).stream().map(this::toDto).toList();
    }

    public List<LeaveResponseDto> listByEmployee(Long employeeId) {
        assertCanAccessEmployee(employeeId);
        return leaveRepository.findAllByTenantIdAndEmployeeId(TenantContext.getTenantId(), employeeId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<LeaveResponseDto> listCurrentEmployee() {
        return listByEmployee(requireCurrentEmployeeId());
    }

    private LeaveResponseDto transitionLeave(Long leaveId, LeaveStatus status, String comment) {
        LeaveRequestEntity entity = getAccessibleLeave(leaveId);
        if (entity.getStatus() != LeaveStatus.REQUESTED && status != LeaveStatus.CANCELLED) {
            throw new BadRequestException("Only requested leave can be approved or rejected");
        }
        entity.setStatus(status);
        entity.setComment(comment);
        entity.setUpdatedAt(Instant.now());
        LeaveRequestEntity saved = leaveRepository.save(entity);
        publishLeaveStatusChanged(saved);
        return toDto(saved);
    }

    private LeaveRequestEntity getLeave(Long leaveId) {
        return leaveRepository.findByIdAndTenantId(leaveId, TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
    }

    private LeaveRequestEntity getAccessibleLeave(Long leaveId) {
        LeaveRequestEntity entity = getLeave(leaveId);
        assertCanAccessEmployee(entity.getEmployeeId());
        return entity;
    }

    private void assertCanAccessEmployee(Long employeeId) {
        if (!SecurityUtils.hasAuthority("EMPLOYEE")) {
            return;
        }

        Long currentEmployeeId = requireCurrentEmployeeId();
        if (!currentEmployeeId.equals(employeeId)) {
            throw new AccessDeniedException("Employees can only access their own leave requests");
        }
    }

    private Long requireCurrentEmployeeId() {
        Long employeeId = SecurityUtils.currentEmployeeId();
        if (employeeId == null) {
            throw new AccessDeniedException("Missing employee context");
        }
        return employeeId;
    }

    private void validatePeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("Leave end date must be greater than or equal to start date");
        }
    }

    private LeaveResponseDto toDto(LeaveRequestEntity entity) {
        return new LeaveResponseDto(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getType(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getComment(),
                requiresEvidence(entity.getType()),
                entity.getEvidenceContent() != null && entity.getEvidenceContent().length > 0,
                entity.getEvidenceFileName(),
                entity.getEvidenceUploadedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private boolean requiresEvidence(LeaveType type) {
        return EVIDENCE_REQUIRED_TYPES.contains(type);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "justificatif";
        }
        String normalized = fileName.replace('\\', '/');
        String lastSegment = normalized.substring(normalized.lastIndexOf('/') + 1);
        return lastSegment.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record EvidenceDownload(String fileName, String contentType, byte[] content) {
    }

    private void publishLeaveStatusChanged(LeaveRequestEntity entity) {
        LeaveStatusChangedEvent event = new LeaveStatusChangedEvent(
                entity.getTenantId(),
                entity.getEmployeeId(),
                entity.getId(),
                entity.getStatus().name(),
                entity.getStartDate(),
                entity.getEndDate(),
                Instant.now()
        );
        try {
            kafkaTemplate.send("leave-events", event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn(
                                    "Failed to publish leave event for tenant {} employee {} leave {}",
                                    entity.getTenantId(),
                                    entity.getEmployeeId(),
                                    entity.getId(),
                                    exception
                            );
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to publish leave event for tenant {} employee {} leave {}",
                    entity.getTenantId(),
                    entity.getEmployeeId(),
                    entity.getId(),
                    exception
            );
        }
    }
}
