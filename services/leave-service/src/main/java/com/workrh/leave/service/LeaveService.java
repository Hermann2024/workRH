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
import com.workrh.leave.repository.LeaveRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

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
        return toDto(leaveRepository.save(entity));
    }

    public LeaveResponseDto findById(Long leaveId) {
        return toDto(getAccessibleLeave(leaveId));
    }

    public LeaveResponseDto approve(Long leaveId, LeaveDecisionRequestDto request) {
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
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
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
