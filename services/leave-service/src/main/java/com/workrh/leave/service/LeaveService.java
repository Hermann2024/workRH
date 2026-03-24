package com.workrh.leave.service;

import com.workrh.common.events.LeaveStatusChangedEvent;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LeaveService(LeaveRepository leaveRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.leaveRepository = leaveRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public LeaveResponseDto create(LeaveRequestDto request) {
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
        return toDto(getLeave(leaveId));
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
        return leaveRepository.findAllByTenantIdAndEmployeeId(TenantContext.getTenantId(), employeeId).stream()
                .map(this::toDto)
                .toList();
    }

    private LeaveResponseDto transitionLeave(Long leaveId, LeaveStatus status, String comment) {
        LeaveRequestEntity entity = getLeave(leaveId);
        if (entity.getStatus() != LeaveStatus.REQUESTED && status != LeaveStatus.CANCELLED) {
            throw new BadRequestException("Only requested leave can be approved or rejected");
        }
        entity.setStatus(status);
        entity.setComment(comment);
        entity.setUpdatedAt(Instant.now());
        LeaveRequestEntity saved = leaveRepository.save(entity);
        kafkaTemplate.send("leave-events", new LeaveStatusChangedEvent(
                saved.getTenantId(),
                saved.getEmployeeId(),
                saved.getId(),
                saved.getStatus().name(),
                saved.getStartDate(),
                saved.getEndDate(),
                Instant.now()
        ));
        return toDto(saved);
    }

    private LeaveRequestEntity getLeave(Long leaveId) {
        return leaveRepository.findByIdAndTenantId(leaveId, TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
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
}
