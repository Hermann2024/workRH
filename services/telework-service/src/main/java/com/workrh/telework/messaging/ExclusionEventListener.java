package com.workrh.telework.messaging;

import com.workrh.common.events.LeaveStatusChangedEvent;
import com.workrh.common.events.SicknessDeclaredEvent;
import com.workrh.telework.domain.ExclusionPeriod;
import com.workrh.telework.domain.ExclusionType;
import com.workrh.telework.repository.ExclusionPeriodRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExclusionEventListener {

    private final ExclusionPeriodRepository exclusionPeriodRepository;

    public ExclusionEventListener(ExclusionPeriodRepository exclusionPeriodRepository) {
        this.exclusionPeriodRepository = exclusionPeriodRepository;
    }

    @KafkaListener(topics = "leave-events", groupId = "telework-service")
    public void onLeaveApproved(LeaveStatusChangedEvent event) {
        if (!"APPROVED".equals(event.status())) {
            return;
        }
        ExclusionPeriod exclusion = new ExclusionPeriod();
        exclusion.setTenantId(event.tenantId());
        exclusion.setEmployeeId(event.employeeId());
        exclusion.setType(ExclusionType.LEAVE);
        exclusion.setStartDate(event.startDate());
        exclusion.setEndDate(event.endDate());
        exclusion.setSourceReference("leave:" + event.leaveId());
        exclusionPeriodRepository.save(exclusion);
    }

    @KafkaListener(topics = "sickness-events", groupId = "telework-service")
    public void onSicknessDeclared(SicknessDeclaredEvent event) {
        ExclusionPeriod exclusion = new ExclusionPeriod();
        exclusion.setTenantId(event.tenantId());
        exclusion.setEmployeeId(event.employeeId());
        exclusion.setType(ExclusionType.SICKNESS);
        exclusion.setStartDate(event.startDate());
        exclusion.setEndDate(event.endDate());
        exclusion.setSourceReference("sickness:" + event.sicknessId());
        exclusionPeriodRepository.save(exclusion);
    }
}
