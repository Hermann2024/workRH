package com.workrh.sickness.service;

import com.workrh.common.events.SicknessDeclaredEvent;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.sickness.api.dto.SicknessRequestDto;
import com.workrh.sickness.api.dto.SicknessResponseDto;
import com.workrh.sickness.domain.SicknessRecord;
import com.workrh.sickness.repository.SicknessRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SicknessService {

    private final SicknessRepository sicknessRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SicknessService(SicknessRepository sicknessRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.sicknessRepository = sicknessRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public SicknessResponseDto declare(SicknessRequestDto request) {
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
        kafkaTemplate.send("sickness-events", new SicknessDeclaredEvent(
                saved.getTenantId(),
                saved.getEmployeeId(),
                saved.getId(),
                saved.getStartDate(),
                saved.getEndDate(),
                Instant.now()
        ));
        return toDto(saved);
    }

    public List<SicknessResponseDto> list() {
        return sicknessRepository.findAllByTenantId(TenantContext.getTenantId()).stream().map(this::toDto).toList();
    }

    public List<SicknessResponseDto> listByEmployee(Long employeeId) {
        return sicknessRepository.findAllByTenantIdAndEmployeeId(TenantContext.getTenantId(), employeeId).stream()
                .map(this::toDto)
                .toList();
    }

    public SicknessResponseDto findById(Long sicknessId) {
        return toDto(sicknessRepository.findByIdAndTenantId(sicknessId, TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Sickness record not found")));
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
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
